package io.vertx.ext.jpa.impl;

import io.vertx.core.*;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.Shareable;
import io.vertx.core.spi.metrics.PoolMetrics;
import io.vertx.core.spi.metrics.VertxMetrics;
import io.vertx.ext.jdbc.impl.actions.AbstractJDBCAction;
import io.vertx.ext.jdbc.impl.actions.JDBCQuery;
import io.vertx.ext.jdbc.impl.actions.JDBCStatementHelper;
import io.vertx.ext.jdbc.impl.actions.JDBCUpdate;
import io.vertx.ext.jdbc.spi.DataSourceProvider;
import io.vertx.ext.jpa.JPAClient;
import io.vertx.ext.jpa.impl.actions.JPAStatementHelper;
import io.vertx.ext.jpa.sql.JPAConnection;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class JPAClientImpl implements JPAClient
{
  private static final String DS_LOCAL_MAP_NAME = "__vertx.JPAClientImpl.datasources";

  private final Vertx vertx;
  private final JPAClientImpl.DataSourceHolder holder;

  // We use this executor to execute getConnection requests
  private final ExecutorService exec;
  private final DataSource ds;
  private final PoolMetrics metrics;
  // Helper that can do param and result transforms, its behavior is defined by the
  // initial config and immutable after that moment. It is safe to reuse since there
  // is no state involved
  private final JDBCStatementHelper jdbchelper;
  private final JPAStatementHelper helper;

  /*
  Create client with specific datasource
   */
  public JPAClientImpl(Vertx vertx, DataSource dataSource)
  {
    Objects.requireNonNull(vertx);
    Objects.requireNonNull(dataSource);
    this.vertx = vertx;
    this.holder = new JPAClientImpl.DataSourceHolder((VertxInternal) vertx, dataSource);
    this.exec = holder.exec();
    this.ds = dataSource;
    this.metrics = holder.metrics;
    this.jdbchelper = new JDBCStatementHelper();
    this.helper = new JPAStatementHelper();
    setupCloseHook();
  }

  /*
  Create client with shared datasource
   */
  public JPAClientImpl(Vertx vertx, JsonObject config, String datasourceName)
  {
    Objects.requireNonNull(vertx);
    Objects.requireNonNull(config);
    Objects.requireNonNull(datasourceName);
    this.vertx = vertx;
    this.holder = lookupHolder(datasourceName, config);
    this.exec = holder.exec();
    this.ds = holder.ds();
    this.metrics = holder.metrics;
    this.jdbchelper = new JDBCStatementHelper(config);
    this.helper = new JPAStatementHelper(config);
    setupCloseHook();
  }

  private void setupCloseHook()
  {
    Context ctx = Vertx.currentContext();
    if (ctx != null && ctx.owner() == vertx)
    {
      ctx.addCloseHook(holder::close);
    }
  }

  @Override
  public void close()
  {
    holder.close(null);
  }

  @Override
  public void close(Handler<AsyncResult<Void>> completionHandler)
  {
    holder.close(completionHandler);
  }

  @Override
  public JPAClientImpl update(String sql, Handler<AsyncResult<UpdateResult>> resultHandler)
  {
    ContextInternal ctx = (ContextInternal) vertx.getOrCreateContext();
    executeDirect(ctx, new JDBCUpdate(vertx, jdbchelper, null, ctx, sql, null), resultHandler);
    return this;
  }

  @Override
  public JPAClientImpl updateWithParams(String sql, JsonArray in, Handler<AsyncResult<UpdateResult>> resultHandler)
  {
    ContextInternal ctx = (ContextInternal) vertx.getOrCreateContext();
    executeDirect(ctx, new JDBCUpdate(vertx, jdbchelper, null, ctx, sql, in), resultHandler);
    return this;
  }

  @Override
  public JPAClientImpl query(String sql, Handler<AsyncResult<ResultSet>> resultHandler)
  {
    ContextInternal ctx = (ContextInternal) vertx.getOrCreateContext();
    executeDirect(ctx, new JDBCQuery(vertx, jdbchelper, null, ctx, sql, null), resultHandler);
    return this;
  }

  @Override
  public JPAClientImpl queryWithParams(String sql, JsonArray in, Handler<AsyncResult<ResultSet>> resultHandler)
  {
    ContextInternal ctx = (ContextInternal) vertx.getOrCreateContext();
    executeDirect(ctx, new JDBCQuery(vertx, jdbchelper, null, ctx, sql, in), resultHandler);
    return this;
  }

  private <T> void executeDirect(Context ctx, AbstractJDBCAction<T> action, Handler<AsyncResult<T>> handler)
  {
    getConnection(ctx, ar1 -> {
      Future<T> fut = Future.future();
      fut.setHandler(ar2 -> vertx.runOnContext(v -> handler.handle(ar2)));
      if (ar1.succeeded())
      {
        JPAConnectionImpl conn = (JPAConnectionImpl) ar1.result();
        try
        {
          T result = action.execute(conn.conn);
          fut.complete(result);
        }
        catch (Exception e)
        {
          fut.fail(e);
        }
        finally
        {
          if (metrics != null)
          {
            metrics.end(conn.metric, true);
          }
          try
          {
            conn.conn.close();
          }
          catch (Exception e)
          {
            JPAConnectionImpl.log.error("Failure in closing connection", ar1.cause());
          }
        }
      }
      else
      {
        fut.fail(ar1.cause());
      }
    });
  }

  private void getConnection(Context ctx, Handler<AsyncResult<SQLConnection>> handler)
  {
    boolean enabled = metrics != null && metrics.isEnabled();
    Object queueMetric = enabled ? metrics.submitted() : null;
    PoolMetrics metrics = enabled ? this.metrics : null;
    exec.execute(() -> {
      Future<SQLConnection> res = Future.future();
      res.setHandler(handler);
      try
      {
        /*
        This can block until a connection is free.
        We don't want to do that while running on a worker as we can enter a deadlock situation as the worker
        might have obtained a connection, and won't release it until it is run again
        There is a general principle here:
        *User code* should be executed on a worker and can potentially block, it's up to the *user* to deal with
        deadlocks that might occur there.
        If the *service code* internally blocks waiting for a resource that might be obtained by *user code*, then
        this can cause deadlock, so the service should ensure it never does this, by executing such code
        (e.g. getConnection) on a different thread to the worker pool.
        We don't want to use the vert.x internal pool for this as the threads might end up all blocked preventing
        other important operations from occurring (e.g. async file access)
        */
        Connection conn = ds.getConnection();
        Object execMetric = null;
        if (metrics != null)
        {
          execMetric = metrics.begin(queueMetric);
        }
        // wrap it
        res.complete(new JPAConnectionImpl(ctx, helper, jdbchelper, conn, metrics, execMetric));
      }
      catch (SQLException e)
      {
        if (metrics != null)
        {
          metrics.rejected(queueMetric);
        }
        res.fail(e);
      }
    });
  }

  @Override
  public JPAClient getConnection(Handler<AsyncResult<SQLConnection>> handler)
  {
    Context ctx = vertx.getOrCreateContext();
    getConnection(ctx, ar -> ctx.runOnContext(v -> handler.handle(ar)));
    return this;
  }

  private void getJPAConnection(Context ctx, Handler<AsyncResult<JPAConnection>> handler)
  {
    boolean enabled = metrics != null && metrics.isEnabled();
    Object queueMetric = enabled ? metrics.submitted() : null;
    PoolMetrics metrics = enabled ? this.metrics : null;
    exec.execute(() -> {
      Future<JPAConnection> res = Future.future();
      res.setHandler(handler);
      try
      {
        /*
        This can block until a connection is free.
        We don't want to do that while running on a worker as we can enter a deadlock situation as the worker
        might have obtained a connection, and won't release it until it is run again
        There is a general principle here:
        *User code* should be executed on a worker and can potentially block, it's up to the *user* to deal with
        deadlocks that might occur there.
        If the *service code* internally blocks waiting for a resource that might be obtained by *user code*, then
        this can cause deadlock, so the service should ensure it never does this, by executing such code
        (e.g. getConnection) on a different thread to the worker pool.
        We don't want to use the vert.x internal pool for this as the threads might end up all blocked preventing
        other important operations from occurring (e.g. async file access)
        */
        Connection conn = ds.getConnection();
        Object execMetric = null;
        if (metrics != null)
        {
          execMetric = metrics.begin(queueMetric);
        }
        // wrap it
        res.complete(new JPAConnectionImpl(ctx, helper, jdbchelper, conn, metrics, execMetric));
      }
      catch (SQLException e)
      {
        if (metrics != null)
        {
          metrics.rejected(queueMetric);
        }
        res.fail(e);
      }
    });
  }

  public JPAClient getJPAConnection(Handler<AsyncResult<JPAConnection>> handler)
  {
    Context ctx = vertx.getOrCreateContext();
    getJPAConnection(ctx, ar -> ctx.runOnContext(v -> handler.handle(ar)));
    return this;
  }

  private JPAClientImpl.DataSourceHolder lookupHolder(String datasourceName, JsonObject config)
  {
    synchronized (vertx)
    {
      LocalMap<String, JPAClientImpl.DataSourceHolder> map = vertx.sharedData().getLocalMap(DS_LOCAL_MAP_NAME);
      JPAClientImpl.DataSourceHolder theHolder = map.get(datasourceName);
      if (theHolder == null)
      {
        theHolder = new JPAClientImpl.DataSourceHolder((VertxInternal) vertx, config, map, datasourceName);
      }
      else
      {
        theHolder.incRefCount();
      }
      return theHolder;
    }
  }

  private class DataSourceHolder implements Shareable
  {

    private final VertxInternal vertx;
    private final LocalMap<String, JPAClientImpl.DataSourceHolder> map;
    DataSourceProvider provider;
    JsonObject config;
    DataSource ds;
    PoolMetrics metrics;
    ExecutorService exec;
    private int refCount = 1;
    private final String name;

    DataSourceHolder(VertxInternal vertx, DataSource ds)
    {
      this.ds = ds;
      this.metrics = vertx.metricsSPI() != null ?
        vertx.metricsSPI().createMetrics(ds, "datasource", UUID.randomUUID().toString(), -1) :
        null;
      this.vertx = vertx;
      this.map = null;
      this.name = null;
    }

    DataSourceHolder(VertxInternal vertx, JsonObject config, LocalMap<String, JPAClientImpl.DataSourceHolder> map,
      String name)
    {
      this.config = config;
      this.map = map;
      this.vertx = vertx;
      this.name = name;

      map.put(name, this);
    }

    synchronized DataSource ds()
    {
      if (ds == null)
      {
        String providerClass = config.getString("provider_class");
        if (providerClass == null)
        {
          providerClass = DEFAULT_PROVIDER_CLASS;
        }

        VertxMetrics vertxMetrics = vertx.metricsSPI();
        if (Thread.currentThread().getContextClassLoader() != null)
        {
          try
          {
            // Try with the TCCL
            Class clazz = Thread.currentThread().getContextClassLoader().loadClass(providerClass);
            provider = (DataSourceProvider) clazz.newInstance();
            ds = provider.getDataSource(config);
            int poolSize = provider.maximumPoolSize(ds, config);
            metrics = vertxMetrics != null ? vertxMetrics.createMetrics(ds, "datasource", name, poolSize) : null;
            return ds;
          }
          catch (ClassNotFoundException e)
          {
            // Next try.
          }
          catch (InstantiationException | SQLException | IllegalAccessException e)
          {
            throw new RuntimeException(e);
          }
        }

        try
        {
          // Try with the classloader of the current class.
          Class clazz = this.getClass().getClassLoader().loadClass(providerClass);
          provider = (DataSourceProvider) clazz.newInstance();
          ds = provider.getDataSource(config);
          int poolSize = provider.maximumPoolSize(ds, config);
          metrics = vertxMetrics != null ? vertxMetrics.createMetrics(ds, "datasource", name, poolSize) : null;
          return ds;
        }
        catch (ClassNotFoundException | InstantiationException | SQLException | IllegalAccessException e)
        {
          throw new RuntimeException(e);
        }
      }

      return ds;
    }

    synchronized ExecutorService exec()
    {
      if (exec == null)
      {
        exec = new ThreadPoolExecutor(1, 1,
          1000L, TimeUnit.MILLISECONDS,
          new LinkedBlockingQueue<>(),
          (r -> new Thread(r, "vertx-jdbc-service-get-connection-thread")));
      }
      return exec;
    }

    void incRefCount()
    {
      refCount++;
    }

    void close(Handler<AsyncResult<Void>> completionHandler)
    {
      synchronized (vertx)
      {
        if (--refCount == 0)
        {
          if (metrics != null)
          {
            metrics.close();
          }
          Future<Void> f1 = Future.future();
          Future<Void> f2 = Future.future();
          if (completionHandler != null)
          {
            CompositeFuture.all(f1, f2).<Void>map(f -> null).setHandler(completionHandler);
          }
          if (provider != null)
          {
            vertx.executeBlocking(future -> {
              try
              {
                provider.close(ds);
                future.complete();
              }
              catch (SQLException e)
              {
                future.fail(e);
              }
            }, f2.completer());
          }
          else
          {
            f2.complete();
          }
          try
          {
            if (exec != null)
            {
              exec.shutdown();
            }
            if (map != null)
            {
              map.remove(name);
              if (map.isEmpty())
              {
                map.close();
              }
            }
            f1.complete();
          }
          catch (Throwable t)
          {
            f1.fail(t);
          }
        }
        else
        {
          if (completionHandler != null)
          {
            completionHandler.handle(Future.succeededFuture());
          }
        }
      }
    }
  }

}
