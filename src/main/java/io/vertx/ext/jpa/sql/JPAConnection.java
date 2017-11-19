package io.vertx.ext.jpa.sql;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jpa.util.RestrinctionHandler;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;

public interface JPAConnection extends SQLConnection
{

  /**
   * Executes the given prepared statement which may be an <code>INSERT</code>
   * statement with the given parameters
   *
   * @param table         the table to execute.
   * @param params        these are the parameters name with values to fill the statement.
   * @param resultHandler the handler which is called once the operation completes.
   * @see java.sql.Statement#executeUpdate(String)
   * @see java.sql.PreparedStatement#executeUpdate(String)
   */
  @Fluent
  SQLConnection persist(String table, JsonObject params, Handler<AsyncResult<UpdateResult>> resultHandler);


  /**
   * Executes the given prepared statement which may be an <code>UPDATE</code>
   * statement with the given parameters
   *
   * @param table         the table to execute.
   * @param params        these are the parameters to fill the statement.
   * @param key           name and value of table key.
   * @param resultHandler the handler which is called once the operation completes.
   * @see java.sql.Statement#executeUpdate(String)
   * @see java.sql.PreparedStatement#executeUpdate(String)
   */
  @Fluent
  SQLConnection merge(String table, JsonObject params, JsonObject key, Handler<AsyncResult<UpdateResult>> resultHandler);

  /**
   * Executes the given prepared statement which may be an <code>DELETE</code>
   * statement with the given parameters
   *
   * @param table         the table to execute.
   * @param key           name and value of table key.
   * @param resultHandler the handler which is called once the operation completes.
   * @see java.sql.Statement#executeUpdate(String)
   * @see java.sql.PreparedStatement#executeUpdate(String)
   */
  @Fluent
  SQLConnection delete(String table, JsonObject key, Handler<AsyncResult<UpdateResult>> resultHandler);

  /**
   * Executes the given prepared statement which may be an <code>SELECT</code>
   * statement with the named parameters
   *
   * @param table         the table to execute.
   * @param params        these are the parameters name with values to fill the statement.
   * @param resultHandler the handler which is called once the operation completes.
   * @see java.sql.Statement#executeUpdate(String)
   * @see java.sql.PreparedStatement#executeUpdate(String)
   */
  @Fluent
  SQLConnection query(String table, JsonObject params, Handler<AsyncResult<ResultSet>> resultHandler);

  /**
   * Executes the given prepared statement which may be an <code>SELECT</code>
   * statement using a restrictions handler
   *
   * @param table         the table to execute.
   * @param params        these are the parameters name with values to fill the statement.
   * @param resultHandler the handler which is called once the operation completes.
   * @see java.sql.Statement#executeUpdate(String)
   * @see java.sql.PreparedStatement#executeUpdate(String)
   */
  @Fluent
  SQLConnection query(String table, JsonObject params, RestrinctionHandler<JsonObject, String, StringBuffer> restictionHandler, Handler<AsyncResult<ResultSet>> resultHandler);
}
