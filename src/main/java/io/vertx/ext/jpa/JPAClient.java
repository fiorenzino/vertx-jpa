package io.vertx.ext.jpa;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jpa.impl.JPAClientImpl;
import io.vertx.ext.jpa.sql.JPAConnection;
import io.vertx.ext.jpa.util.RestrinctionHandler;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.UpdateResult;

import javax.sql.DataSource;
import java.util.UUID;

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
   * Create a client using a pre-existing data source
   *
   * @param vertx      the Vert.x instance
   * @param dataSource the datasource
   * @return the client
   */
  @GenIgnore
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
  @Fluent
  public JPAClient getJPAConnection(Handler<AsyncResult<JPAConnection>> handler);


  /**
   * Executes the given prepared statement which may be an <code>INSERT</code>
   * statement with the given parameters, this method acquires a connection from the the pool and executes the SQL
   * statement and returns it back after the execution.
   *
   * @param table         the table to execute.
   * @param params        these are the parameters name with values to fill the statement.
   * @param handler the handler which is called once the operation completes.
   * @see java.sql.Statement#executeUpdate(String)
   * @see java.sql.PreparedStatement#executeUpdate(String)
   */

  @Fluent
  default JPAClient persist(String table, JsonObject params, Handler<AsyncResult<UpdateResult>> handler) {
    getJPAConnection(getJPAConnection -> {
      if (getJPAConnection.failed()) {
        handler.handle(Future.failedFuture(getJPAConnection.cause()));
      } else {
        final JPAConnection conn = getJPAConnection.result();

        conn.persist(table,  params, query -> {
          if (query.failed()) {
            conn.close(close -> {
              if (close.failed()) {
                handler.handle(Future.failedFuture(close.cause()));
              } else {
                handler.handle(Future.failedFuture(query.cause()));
              }
            });
          } else {
            conn.close(close -> {
              if (close.failed()) {
                handler.handle(Future.failedFuture(close.cause()));
              } else {
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
   * @param sql         the table to execute.
   * @param params        these are the parameters name with values to fill the statement.
   * @param handler the handler which is called once the operation completes.
   * @see java.sql.Statement#executeUpdate(String)
   * @see java.sql.PreparedStatement#executeUpdate(String)
   */

  @Fluent
  default JPAClient query(String sql, JsonObject params, Handler<AsyncResult<ResultSet>> handler) {
    getJPAConnection(getJPAConnection -> {
      if (getJPAConnection.failed()) {
        handler.handle(Future.failedFuture(getJPAConnection.cause()));
      } else {
        final JPAConnection conn = getJPAConnection.result();

        conn.query(sql,  params, query -> {
          if (query.failed()) {
            conn.close(close -> {
              if (close.failed()) {
                handler.handle(Future.failedFuture(close.cause()));
              } else {
                handler.handle(Future.failedFuture(query.cause()));
              }
            });
          } else {
            conn.close(close -> {
              if (close.failed()) {
                handler.handle(Future.failedFuture(close.cause()));
              } else {
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
   * @param table         the table to execute.
   * @param params        these are the parameters name with values to fill the statement.
   * @param restrinctionHandler the restrinctionHandler which is called to generate dynamically the sql query.
   * @param handler the handler which is called once the operation completes.
   * @see java.sql.Statement#executeUpdate(String)
   * @see java.sql.PreparedStatement#executeUpdate(String)
   */

  @Fluent
  default JPAClient query(String table, JsonObject params, RestrinctionHandler<JsonObject, String, StringBuffer> restrinctionHandler, Handler<AsyncResult<ResultSet>> handler) {
    getJPAConnection(getJPAConnection -> {
      if (getJPAConnection.failed()) {
        handler.handle(Future.failedFuture(getJPAConnection.cause()));
      } else {
        final JPAConnection conn = getJPAConnection.result();

        conn.query(table,  params, restrinctionHandler, query -> {
          if (query.failed()) {
            conn.close(close -> {
              if (close.failed()) {
                handler.handle(Future.failedFuture(close.cause()));
              } else {
                handler.handle(Future.failedFuture(query.cause()));
              }
            });
          } else {
            conn.close(close -> {
              if (close.failed()) {
                handler.handle(Future.failedFuture(close.cause()));
              } else {
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
   * @param table         the table to execute.
   * @param params        these are the parameters to fill the statement.
   * @param key           name and value of table key.
   * @param handler the handler which is called once the operation completes.
   * @see java.sql.Statement#executeUpdate(String)
   * @see java.sql.PreparedStatement#executeUpdate(String)
   */
  @Fluent
  default JPAClient merge(String table, JsonObject params, JsonObject key, Handler<AsyncResult<UpdateResult>> handler) {
    getJPAConnection(getJPAConnection -> {
      if (getJPAConnection.failed()) {
        handler.handle(Future.failedFuture(getJPAConnection.cause()));
      } else {
        final JPAConnection conn = getJPAConnection.result();

        conn.merge( table,  params,  key, query -> {
          if (query.failed()) {
            conn.close(close -> {
              if (close.failed()) {
                handler.handle(Future.failedFuture(close.cause()));
              } else {
                handler.handle(Future.failedFuture(query.cause()));
              }
            });
          } else {
            conn.close(close -> {
              if (close.failed()) {
                handler.handle(Future.failedFuture(close.cause()));
              } else {
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
   * @param table         the table to execute.
   * @param key           name and value of table key.
   * @param handler the handler which is called once the operation completes.
   * @see java.sql.Statement#executeUpdate(String)
   * @see java.sql.PreparedStatement#executeUpdate(String)
   */
  @Fluent
  default JPAClient delete(String table, JsonObject key, Handler<AsyncResult<UpdateResult>> handler) {
    getJPAConnection(getJPAConnection -> {
      if (getJPAConnection.failed()) {
        handler.handle(Future.failedFuture(getJPAConnection.cause()));
      } else {
        final JPAConnection conn = getJPAConnection.result();

        conn.delete(table,  key, query -> {
          if (query.failed()) {
            conn.close(close -> {
              if (close.failed()) {
                handler.handle(Future.failedFuture(close.cause()));
              } else {
                handler.handle(Future.failedFuture(query.cause()));
              }
            });
          } else {
            conn.close(close -> {
              if (close.failed()) {
                handler.handle(Future.failedFuture(close.cause()));
              } else {
                handler.handle(Future.succeededFuture(query.result()));
              }
            });
          }
        });
      }
    });
    return this;
  }

}
