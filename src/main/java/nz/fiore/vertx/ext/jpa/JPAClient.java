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
package nz.fiore.vertx.ext.jpa;

import io.reactivex.Single;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.UpdateResult;
import nz.fiore.vertx.ext.jpa.impl.JPAClientImpl;
import nz.fiore.vertx.ext.jpa.sql.JPAConnection;
import nz.fiore.vertx.ext.jpa.util.RestrinctionHandler;

import javax.sql.DataSource;
import java.util.UUID;

/**
 * @author <a href="mailto:fiorenzo.pizza@gmail.com">Fiorenzo Pizza</a>
 */
public interface JPAClient extends SQLClient
{

   /**
    * The default data source provider is C3P0
    */
   String DEFAULT_PROVIDER_CLASS = "io.vertx.ext.jdbc.spi.impl.C3P0DataSourceProvider";

   /**
    * The name of the default data source
    */
   String DEFAULT_DS_NAME = "DEFAULT_DS";

   /**
    * Create a JDBC client which maintains its own data source.
    *
    * @param vertx  the Vert.x instance
    * @param config the configuration
    * @return the client
    */
   static JPAClient createNonShared(io.vertx.reactivex.core.Vertx vertx, JsonObject config)
   {
      return new JPAClientImpl(vertx.getDelegate(), config, UUID.randomUUID().toString());
   }

   /**
    * Create a JDBC client which maintains its own data source.
    *
    * @param vertx  the Vert.x instance
    * @param config the configuration
    * @return the client
    */
   static JPAClient createNonShared(Vertx vertx, JsonObject config)
   {
      return new JPAClientImpl(vertx, config, UUID.randomUUID().toString());
   }

   /**
    * Create a JDBC client which shares its data source with any other JDBC clients created with the same
    * data source name
    *
    * @param vertx          the Vert.x instance
    * @param config         the configuration
    * @param dataSourceName the data source name
    * @return the client
    */
   static JPAClient createShared(io.vertx.reactivex.core.Vertx vertx, JsonObject config, String dataSourceName)
   {
      return new JPAClientImpl(vertx.getDelegate(), config, dataSourceName);
   }

   /**
    * Create a JDBC client which shares its data source with any other JDBC clients created with the same
    * data source name
    *
    * @param vertx          the Vert.x instance
    * @param config         the configuration
    * @param dataSourceName the data source name
    * @return the client
    */
   static JPAClient createShared(Vertx vertx, JsonObject config, String dataSourceName)
   {
      return new JPAClientImpl(vertx, config, dataSourceName);
   }

   /**
    * Like {@link #createShared(io.vertx.core.Vertx, JsonObject, String)} but with the default data source name
    *
    * @param vertx  the Vert.x instance
    * @param config the configuration
    * @return the client
    */
   static JPAClient createShared(Vertx vertx, JsonObject config)
   {
      return new JPAClientImpl(vertx, config, DEFAULT_DS_NAME);
   }

   /**
    * Like {@link #createShared(io.vertx.core.Vertx, JsonObject, String)} but with the default data source name
    *
    * @param vertx  the Vert.x instance
    * @param config the configuration
    * @return the client
    */
   static JPAClient createShared(io.vertx.reactivex.core.Vertx vertx, JsonObject config)
   {
      return new JPAClientImpl(vertx.getDelegate(), config, DEFAULT_DS_NAME);
   }

   /**
    * Create a client using a pre-existing data source
    *
    * @param vertx      the Vert.x instance
    * @param dataSource the datasource
    * @return the client
    */

   static JPAClient create(Vertx vertx, DataSource dataSource)
   {
      return new JPAClientImpl(vertx, dataSource);
   }

   /**
    * Returns a connection that can be used to perform SQL operations on. It's important to remember
    * to close the connection when you are done, so it is returned to the pool.
    *
    * @param handler the handler which is called when the <code>JdbcConnection</code> object is ready for use.
    */

   public JPAClient getJPAConnection(Handler<AsyncResult<JPAConnection>> handler);

   /**
    * Executes the given prepared statement which may be an <code>INSERT</code>
    * statement with the given parameters, this method acquires a connection from the the pool and executes the SQL
    * statement and returns it back after the execution.
    *
    * @param table   the table to execute.
    * @param params  these are the parameters name with values to fill the statement.
    * @param handler the handler which is called once the operation completes.
    * @see java.sql.Statement#executeUpdate(String)
    * @see java.sql.PreparedStatement#executeUpdate(String)
    */

   default JPAClient persist(String table, JsonObject params, Handler<AsyncResult<UpdateResult>> handler)
   {
      getJPAConnection(getJPAConnection -> {
         if (getJPAConnection.failed())
         {
            handler.handle(Future.failedFuture(getJPAConnection.cause()));
         }
         else
         {
            final JPAConnection conn = getJPAConnection.result();

            conn.persist(table, params, query -> {
               if (query.failed())
               {
                  conn.close(close -> {
                     if (close.failed())
                     {
                        handler.handle(Future.failedFuture(close.cause()));
                     }
                     else
                     {
                        handler.handle(Future.failedFuture(query.cause()));
                     }
                  });
               }
               else
               {
                  conn.close(close -> {
                     if (close.failed())
                     {
                        handler.handle(Future.failedFuture(close.cause()));
                     }
                     else
                     {
                        handler.handle(Future.succeededFuture(query.result()));
                     }
                  });
               }
            });
         }
      });
      return this;
   }

   /**
    * Executes the given prepared statement which may be an <code>SELECT</code> WITH NAMED PARAMETERS
    * statement with the given parameters, this method acquires a connection from the the pool and executes the SQL
    * statement and returns it back after the execution.
    *
    * @param sql     the table to execute.
    * @param params  these are the parameters name with values to fill the statement.
    * @param handler the handler which is called once the operation completes.
    * @see java.sql.Statement#executeUpdate(String)
    * @see java.sql.PreparedStatement#executeUpdate(String)
    */

   default JPAClient query(String sql, JsonObject params, Handler<AsyncResult<ResultSet>> handler)
   {
      getJPAConnection(getJPAConnection -> {
         if (getJPAConnection.failed())
         {
            handler.handle(Future.failedFuture(getJPAConnection.cause()));
         }
         else
         {
            final JPAConnection conn = getJPAConnection.result();

            conn.query(sql, params, query -> {
               if (query.failed())
               {
                  conn.close(close -> {
                     if (close.failed())
                     {
                        handler.handle(Future.failedFuture(close.cause()));
                     }
                     else
                     {
                        handler.handle(Future.failedFuture(query.cause()));
                     }
                  });
               }
               else
               {
                  conn.close(close -> {
                     if (close.failed())
                     {
                        handler.handle(Future.failedFuture(close.cause()));
                     }
                     else
                     {
                        handler.handle(Future.succeededFuture(query.result()));
                     }
                  });
               }
            });
         }
      });
      return this;
   }

   /**
    * Executes the given prepared statement which may be an <code>SELECT</code> WITH NAMED PARAMETERS GENERATED generated dynamically
    * statement with the given parameters, this method acquires a connection from the the pool and executes the SQL
    * statement and returns it back after the execution.
    *
    * @param table               the table to execute.
    * @param params              these are the parameters name with values to fill the statement.
    * @param restrinctionHandler the restrinctionHandler which is called to generate dynamically the sql query.
    * @param handler             the handler which is called once the operation completes.
    * @see java.sql.Statement#executeUpdate(String)
    * @see java.sql.PreparedStatement#executeUpdate(String)
    */

   default JPAClient query(String table, JsonObject params,
            RestrinctionHandler<JsonObject, String, StringBuffer> restrinctionHandler,
            Handler<AsyncResult<ResultSet>> handler)
   {
      getJPAConnection(getJPAConnection -> {
         if (getJPAConnection.failed())
         {
            handler.handle(Future.failedFuture(getJPAConnection.cause()));
         }
         else
         {
            final JPAConnection conn = getJPAConnection.result();

            conn.query(table, params, restrinctionHandler, query -> {
               if (query.failed())
               {
                  conn.close(close -> {
                     if (close.failed())
                     {
                        handler.handle(Future.failedFuture(close.cause()));
                     }
                     else
                     {
                        handler.handle(Future.failedFuture(query.cause()));
                     }
                  });
               }
               else
               {
                  conn.close(close -> {
                     if (close.failed())
                     {
                        handler.handle(Future.failedFuture(close.cause()));
                     }
                     else
                     {
                        handler.handle(Future.succeededFuture(query.result()));
                     }
                  });
               }
            });
         }
      });
      return this;
   }

   /**
    * Executes the given prepared statement which may be an <code>UPDATE</code>
    * statement with the given parameters, this method acquires a connection from the the pool and executes the SQL
    * statement and returns it back after the execution.
    *
    * @param table   the table to execute.
    * @param params  these are the parameters to fill the statement.
    * @param key     name and value of table key.
    * @param handler the handler which is called once the operation completes.
    * @see java.sql.Statement#executeUpdate(String)
    * @see java.sql.PreparedStatement#executeUpdate(String)
    */

   default JPAClient merge(String table, JsonObject params, JsonObject key, Handler<AsyncResult<UpdateResult>> handler)
   {
      getJPAConnection(getJPAConnection -> {
         if (getJPAConnection.failed())
         {
            handler.handle(Future.failedFuture(getJPAConnection.cause()));
         }
         else
         {
            final JPAConnection conn = getJPAConnection.result();

            conn.merge(table, params, key, query -> {
               if (query.failed())
               {
                  conn.close(close -> {
                     if (close.failed())
                     {
                        handler.handle(Future.failedFuture(close.cause()));
                     }
                     else
                     {
                        handler.handle(Future.failedFuture(query.cause()));
                     }
                  });
               }
               else
               {
                  conn.close(close -> {
                     if (close.failed())
                     {
                        handler.handle(Future.failedFuture(close.cause()));
                     }
                     else
                     {
                        handler.handle(Future.succeededFuture(query.result()));
                     }
                  });
               }
            });
         }
      });
      return this;
   }

   /**
    * Executes the given prepared statement which may be an <code>UPDATE</code>
    * statement with the given parameters, this method acquires a connection from the the pool and executes the SQL
    * statement and returns it back after the execution.
    *
    * @param table   the table to execute.
    * @param key     name and value of table key.
    * @param handler the handler which is called once the operation completes.
    * @see java.sql.Statement#executeUpdate(String)
    * @see java.sql.PreparedStatement#executeUpdate(String)
    */

   default JPAClient delete(String table, JsonObject key, Handler<AsyncResult<UpdateResult>> handler)
   {
      getJPAConnection(getJPAConnection -> {
         if (getJPAConnection.failed())
         {
            handler.handle(Future.failedFuture(getJPAConnection.cause()));
         }
         else
         {
            final JPAConnection conn = getJPAConnection.result();

            conn.delete(table, key, query -> {
               if (query.failed())
               {
                  conn.close(close -> {
                     if (close.failed())
                     {
                        handler.handle(Future.failedFuture(close.cause()));
                     }
                     else
                     {
                        handler.handle(Future.failedFuture(query.cause()));
                     }
                  });
               }
               else
               {
                  conn.close(close -> {
                     if (close.failed())
                     {
                        handler.handle(Future.failedFuture(close.cause()));
                     }
                     else
                     {
                        handler.handle(Future.succeededFuture(query.result()));
                     }
                  });
               }
            });
         }
      });
      return this;
   }

   /**
    * Executes the given prepared statement which may be an <code>CREATE TABLE</code>
    * statement with the given parameters, this method acquires a connection from the the pool and executes the SQL
    * statement and returns it back after the execution.
    *
    * @param sql     the sql to execute.
    * @param handler the handler which is called once the operation completes.
    * @see java.sql.Statement#executeUpdate(String)
    * @see java.sql.PreparedStatement#executeUpdate(String)
    */
   default JPAClient create(String sql, Handler<AsyncResult<Void>> handler)
   {
      getJPAConnection(getJPAConnection -> {
         if (getJPAConnection.failed())
         {
            handler.handle(Future.failedFuture(getJPAConnection.cause()));
         }
         else
         {
            final JPAConnection conn = getJPAConnection.result();

            conn.create(sql, query -> {
               if (query.failed())
               {
                  conn.close(close -> {
                     if (close.failed())
                     {
                        handler.handle(Future.failedFuture(close.cause()));
                     }
                     else
                     {
                        handler.handle(Future.failedFuture(query.cause()));
                     }
                  });
               }
               else
               {
                  conn.close(close -> {
                     if (close.failed())
                     {
                        handler.handle(Future.failedFuture(close.cause()));
                     }
                     else
                     {
                        handler.handle(Future.succeededFuture(query.result()));
                     }
                  });
               }
            });
         }
      });
      return this;
   }

   /**
    * Executes the given prepared statement which may be an <code>SELECT * FROM TABLE</code>
    * statement with the given parameters, this method acquires a connection from the the pool and executes the SQL
    * statement and returns it back after the execution.
    *
    * @param table   the table to execute.
    * @param key     name and value of table key.
    * @param handler the handler which is called once the operation completes.
    * @see java.sql.Statement#executeUpdate(String)
    * @see java.sql.PreparedStatement#executeUpdate(String)
    */

   default JPAClient find(String table, JsonObject key, Handler<AsyncResult<ResultSet>> handler)
   {
      getJPAConnection(getJPAConnection -> {
         if (getJPAConnection.failed())
         {
            handler.handle(Future.failedFuture(getJPAConnection.cause()));
         }
         else
         {
            final JPAConnection conn = getJPAConnection.result();

            conn.find(table, key, query -> {
               if (query.failed())
               {
                  conn.close(close -> {
                     if (close.failed())
                     {
                        handler.handle(Future.failedFuture(close.cause()));
                     }
                     else
                     {
                        handler.handle(Future.failedFuture(query.cause()));
                     }
                  });
               }
               else
               {
                  conn.close(close -> {
                     if (close.failed())
                     {
                        handler.handle(Future.failedFuture(close.cause()));
                     }
                     else
                     {
                        handler.handle(Future.succeededFuture(query.result()));
                     }
                  });
               }
            });
         }
      });
      return this;
   }

   /**
    * Returns a connection that can be used to perform SQL operations on. It's important to remember
    * to close the connection when you are done, so it is returned to the pool.
    *
    * @return
    */
   default Single<JPAConnection> rxGetConnection()
   {
      return new io.vertx.reactivex.core.impl.AsyncResultSingle<JPAConnection>(handler -> {
         getJPAConnection(handler);
      });
   }

   default Single<ResultSet> rxQuery(String sql, JsonObject params,
            RestrinctionHandler<JsonObject, String, StringBuffer> restictionHandler)
   {
      return new io.vertx.reactivex.core.impl.AsyncResultSingle<ResultSet>(handler -> {
         query(sql, params, restictionHandler, handler);
      });
   }

   default Single<ResultSet> rxQuery(String sql, JsonObject params)
   {
      return new io.vertx.reactivex.core.impl.AsyncResultSingle<ResultSet>(handler -> {
         this.query(sql, params, handler);
      });
   }

   default Single<UpdateResult> rxDelete(String table, JsonObject key)
   {
      return new io.vertx.reactivex.core.impl.AsyncResultSingle<UpdateResult>(handler -> {
         delete(table, key, handler);
      });
   }

   default Single<UpdateResult> rxMerge(String table, JsonObject params, JsonObject key)
   {
      return new io.vertx.reactivex.core.impl.AsyncResultSingle<UpdateResult>(handler -> {
         merge(table, params, key, handler);
      });
   }

   default Single<UpdateResult> rxPersist(String table, JsonObject params)
   {
      return new io.vertx.reactivex.core.impl.AsyncResultSingle<UpdateResult>(handler -> {
         persist(table, params, handler);
      });
   }

   default Single<Void> rxCreate(String sql)
   {
      return new io.vertx.reactivex.core.impl.AsyncResultSingle<Void>(handler -> {
         create(sql, handler);
      });
   }

   default Single<ResultSet> rxFind(String table, JsonObject key)
   {
      return new io.vertx.reactivex.core.impl.AsyncResultSingle<ResultSet>(handler -> {
         find(table, key, handler);
      });
   }
}
