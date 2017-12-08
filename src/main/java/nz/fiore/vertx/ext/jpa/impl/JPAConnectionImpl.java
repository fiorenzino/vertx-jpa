/*
 * Copyright (c) 2011-2014 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *     The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 *
 *     The Apache License v2.0 is available at
 *     http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package nz.fiore.vertx.ext.jpa.impl;

import io.reactivex.Single;
import io.vertx.core.*;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.TaskQueue;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.spi.metrics.PoolMetrics;
import io.vertx.ext.jdbc.impl.actions.*;
import nz.fiore.vertx.ext.jpa.impl.actions.*;
import nz.fiore.vertx.ext.jpa.sql.JPAConnection;
import nz.fiore.vertx.ext.jpa.util.RestrinctionHandler;
import io.vertx.ext.sql.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * @author <a href="mailto:nscavell@redhat.com">Nick Scavelli</a>
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
class JPAConnectionImpl implements JPAConnection
{

  static final Logger log = LoggerFactory.getLogger(JPAConnectionImpl.class);

  private final Vertx vertx;
  final Connection conn;
  private final ContextInternal ctx;
  private final PoolMetrics metrics;
  final Object metric;
  private final TaskQueue statementsQueue = new TaskQueue();

  private final JPAStatementHelper helper;
  private final JDBCStatementHelper jdbchelper;

  private SQLOptions options;

  public JPAConnectionImpl(Context context, JPAStatementHelper helper, JDBCStatementHelper jdbchelper, Connection conn,
    PoolMetrics metrics, Object metric)
  {
    this.vertx = context.owner();
    this.helper = helper;
    this.jdbchelper = jdbchelper;
    this.conn = conn;
    this.metrics = metrics;
    this.metric = metric;
    this.ctx = (ContextInternal) context;
  }

  @Override
  public JPAConnection setOptions(SQLOptions options)
  {
    this.options = options;
    return this;
  }

  @Override
  public JPAConnection setAutoCommit(boolean autoCommit, Handler<AsyncResult<Void>> resultHandler)
  {
    new JDBCAutoCommit(vertx, options, ctx, autoCommit).execute(conn, statementsQueue, resultHandler);
    return this;
  }

  @Override
  public JPAConnection execute(String sql, Handler<AsyncResult<Void>> resultHandler)
  {
    new JDBCExecute(vertx, options, ctx, sql).execute(conn, statementsQueue, resultHandler);
    return this;
  }

  @Override
  public JPAConnection query(String sql, Handler<AsyncResult<ResultSet>> resultHandler)
  {
    new JDBCQuery(vertx, jdbchelper, options, ctx, sql, null).execute(conn, statementsQueue, resultHandler);
    return this;
  }

  @Override
  public JPAConnection queryStream(String sql, Handler<AsyncResult<SQLRowStream>> handler)
  {
    new StreamQuery(vertx, jdbchelper, options, ctx, statementsQueue, sql, null)
      .execute(conn, statementsQueue, handler);
    return this;
  }

  @Override
  public JPAConnection queryStreamWithParams(String sql, JsonArray params, Handler<AsyncResult<SQLRowStream>> handler)
  {
    new StreamQuery(vertx, jdbchelper, options, ctx, statementsQueue, sql, params)
      .execute(conn, statementsQueue, handler);
    return this;
  }

  @Override
  public JPAConnection queryWithParams(String sql, JsonArray params, Handler<AsyncResult<ResultSet>> resultHandler)
  {
    new JDBCQuery(vertx, jdbchelper, options, ctx, sql, params).execute(conn, statementsQueue, resultHandler);
    return this;
  }

  @Override
  public JPAConnection update(String sql, Handler<AsyncResult<UpdateResult>> resultHandler)
  {
    new JDBCUpdate(vertx, jdbchelper, options, ctx, sql, null).execute(conn, statementsQueue, resultHandler);
    return this;
  }

  @Override
  public JPAConnection updateWithParams(String sql, JsonArray params, Handler<AsyncResult<UpdateResult>> resultHandler)
  {
    new JDBCUpdate(vertx, jdbchelper, options, ctx, sql, params).execute(conn, statementsQueue, resultHandler);
    return this;
  }

  @Override
  public JPAConnection call(String sql, Handler<AsyncResult<ResultSet>> resultHandler)
  {
    new JDBCCallable(vertx, jdbchelper, options, ctx, sql, null, null).execute(conn, statementsQueue, resultHandler);
    return this;
  }

  @Override
  public JPAConnection callWithParams(String sql, JsonArray params, JsonArray outputs,
    Handler<AsyncResult<ResultSet>> resultHandler)
  {
    new JDBCCallable(vertx, jdbchelper, options, ctx, sql, params, outputs)
      .execute(conn, statementsQueue, resultHandler);
    return this;
  }

  @Override
  public void close(Handler<AsyncResult<Void>> handler)
  {
    if (metrics != null)
    {
      metrics.end(metric, true);
    }
    new JDBCClose(vertx, options, ctx).execute(conn, statementsQueue, handler);
  }

  @Override
  public void close()
  {
    close(ar -> {
      if (ar.failed())
      {
        log.error("Failure in closing connection", ar.cause());
      }
    });
  }

  @Override
  public JPAConnection commit(Handler<AsyncResult<Void>> handler)
  {
    new JDBCCommit(vertx, options, ctx).execute(conn, statementsQueue, handler);
    return this;
  }

  @Override
  public JPAConnection rollback(Handler<AsyncResult<Void>> handler)
  {
    new JDBCRollback(vertx, options, ctx).execute(conn, statementsQueue, handler);
    return this;
  }

  @Override
  public JPAConnection getTransactionIsolation(Handler<AsyncResult<TransactionIsolation>> handler)
  {
    ctx.executeBlocking((Future<TransactionIsolation> f) -> {
      try
      {
        TransactionIsolation txIsolation = TransactionIsolation.from(conn.getTransactionIsolation());

        if (txIsolation != null)
        {
          f.complete(txIsolation);
        }
        else
        {
          f.fail("Unknown isolation level");
        }
      }
      catch (SQLException e)
      {
        f.fail(e);
      }
    }, statementsQueue, handler);

    return this;
  }

  @Override
  public JPAConnection batch(List<String> sqlStatements, Handler<AsyncResult<List<Integer>>> handler)
  {
    new JDBCBatch(vertx, jdbchelper, options, ctx, sqlStatements).execute(conn, statementsQueue, handler);
    return this;
  }

  @Override
  public JPAConnection batchWithParams(String statement, List<JsonArray> args,
    Handler<AsyncResult<List<Integer>>> handler)
  {
    new JDBCBatch(vertx, jdbchelper, options, ctx, statement, args).execute(conn, statementsQueue, handler);
    return this;
  }

  @Override
  public JPAConnection batchCallableWithParams(String statement, List<JsonArray> inArgs, List<JsonArray> outArgs,
    Handler<AsyncResult<List<Integer>>> handler)
  {
    new JDBCBatch(vertx, jdbchelper, options, ctx, statement, inArgs, outArgs).execute(conn, statementsQueue, handler);
    return this;
  }

  @Override
  public JPAConnection setTransactionIsolation(TransactionIsolation isolation, Handler<AsyncResult<Void>> handler)
  {
    ctx.executeBlocking((Future<Void> f) -> {
      try
      {
        conn.setTransactionIsolation(isolation.getType());
        f.complete(null);
      }
      catch (SQLException e)
      {
        f.fail(e);
      }
    }, statementsQueue, handler);

    return this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <C> C unwrap()
  {
    return (C) conn;
  }

  @Override
  public JPAConnection persist(String table, JsonObject params, Handler<AsyncResult<UpdateResult>> resultHandler)
  {
    new JPAPersist(vertx, helper, options, ctx, params, table).execute(conn, statementsQueue, resultHandler);
    return this;
  }

  public Single<UpdateResult> rxPersist(String table, JsonObject params)
  {
    return new io.vertx.reactivex.core.impl.AsyncResultSingle<UpdateResult>(handler -> {
      persist(table, params, handler);
    });
  }

  @Override
  public JPAConnection merge(String table, JsonObject params, JsonObject key,
    Handler<AsyncResult<UpdateResult>> resultHandler)
  {
    new JPAMerge(vertx, helper, options, ctx, params, table, key).execute(conn, statementsQueue, resultHandler);
    return this;
  }

  public Single<UpdateResult> rxMerge(String table, JsonObject params, JsonObject key)
  {
    return new io.vertx.reactivex.core.impl.AsyncResultSingle<UpdateResult>(handler -> {
      merge(table, params, key, handler);
    });
  }

  @Override
  public JPAConnection delete(String table, JsonObject key, Handler<AsyncResult<UpdateResult>> resultHandler)
  {
    new JPADelete(vertx, helper, options, ctx, table, key).execute(conn, statementsQueue, resultHandler);
    return this;
  }

  public Single<UpdateResult> rxDelete(String table, JsonObject key)
  {
    return new io.vertx.reactivex.core.impl.AsyncResultSingle<UpdateResult>(handler -> {
      delete(table, key, handler);
    });
  }

  @Override
  public JPAConnection query(String sql, JsonObject params, Handler<AsyncResult<ResultSet>> resultHandler)
  {
    new JPAQuery(vertx, helper, options, ctx, params, sql).execute(conn, statementsQueue, resultHandler);
    return this;
  }

  public Single<ResultSet> rxQuery(String sql, JsonObject params)
  {
    return new io.vertx.reactivex.core.impl.AsyncResultSingle<ResultSet>(handler -> {
      query(sql, params, handler);
    });
  }

  @Override
  public JPAConnection query(String table, JsonObject params,
    RestrinctionHandler<JsonObject, String, StringBuffer> restictionHandler,
    Handler<AsyncResult<ResultSet>> resultHandler)
  {
    new JPAHandlerQuery(vertx, helper, options, ctx, table, params, restictionHandler)
      .execute(conn, statementsQueue, resultHandler);
    return this;
  }

  public Single<ResultSet> rxQuery(String sql, JsonObject params,
    RestrinctionHandler<JsonObject, String, StringBuffer> restictionHandler)
  {
    return new io.vertx.reactivex.core.impl.AsyncResultSingle<ResultSet>(handler -> {
      query(sql, params, restictionHandler, handler);
    });
  }

}
