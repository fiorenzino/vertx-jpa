package nz.fiore.vertx.ext.jpa.sql;

import io.reactivex.Single;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import nz.fiore.vertx.ext.jpa.util.RestrinctionHandler;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;

public interface JPAConnection extends SQLConnection
{

  /**
   * Executes the given prepared statement which may be an <code>CREATE TABLE</code>.
   *
   * @param sql           the table to execute.
   * @param resultHandler the handler which is called once the operation completes.
   * @see java.sql.Statement#executeUpdate(String)
   * @see java.sql.PreparedStatement#executeUpdate(String)
   */
  @Fluent
  SQLConnection create(String sql, Handler<AsyncResult<Void>> resultHandler);

  /**
   * Executes the given prepared statement which may be an <code>CREATE TABLE</code>.
   *
   * @param sql           the table to execute.
   * @see java.sql.Statement#executeUpdate(String)
   * @see java.sql.PreparedStatement#executeUpdate(String)
   */
  @Fluent
  Single<Void> rxCreate(String sql);

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
   * Executes the given prepared statement which may be an <code>INSERT</code>
   * statement with the given parameters
   *
   * @param table  the table to execute.
   * @param params these are the parameters name with values to fill the statement.
   * @see java.sql.Statement#executeUpdate(String)
   * @see java.sql.PreparedStatement#executeUpdate(String)
   */
  @Fluent
  Single<UpdateResult> rxPersist(String table, JsonObject params);

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
  SQLConnection merge(String table, JsonObject params, JsonObject key,
    Handler<AsyncResult<UpdateResult>> resultHandler);

  /**
   * Executes the given prepared statement which may be an <code>UPDATE</code>
   * statement with the given parameters
   *
   * @param table  the table to execute.
   * @param params these are the parameters to fill the statement.
   * @param key    name and value of table key.
   * @see java.sql.Statement#executeUpdate(String)
   * @see java.sql.PreparedStatement#executeUpdate(String)
   */
  @Fluent
  Single<UpdateResult> rxMerge(String table, JsonObject params, JsonObject key);

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
   * Executes the given prepared statement which may be an <code>DELETE</code>
   * statement with the given parameters
   *
   * @param table the table to execute.
   * @param key   name and value of table key.
   * @see java.sql.Statement#executeUpdate(String)
   * @see java.sql.PreparedStatement#executeUpdate(String)
   */
  @Fluent
  Single<UpdateResult> rxDelete(String table, JsonObject key);

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
   * statement with the named parameters
   *
   * @param sql    the table to execute.
   * @param params these are the parameters name with values to fill the statement.
   * @see java.sql.Statement#executeUpdate(String)
   * @see java.sql.PreparedStatement#executeUpdate(String)
   */
  @Fluent
  Single<ResultSet> rxQuery(String sql, JsonObject params);

  /**
   * Executes the given prepared statement which may be an <code>SELECT</code>
   * statement using a restrictions handler
   *
   * @param table             the table to execute.
   * @param params            these are the parameters name with values to fill the statement.
   * @param restictionHandler the handler which is called to generate the query.
   * @param resultHandler     the handler which is called once the operation completes.
   * @see java.sql.Statement#executeUpdate(String)
   * @see java.sql.PreparedStatement#executeUpdate(String)
   */
  @Fluent
  SQLConnection query(String table, JsonObject params,
    RestrinctionHandler<JsonObject, String, StringBuffer> restictionHandler,
    Handler<AsyncResult<ResultSet>> resultHandler);

  /**
   * Executes the given prepared statement which may be an <code>SELECT</code>
   * statement using a restrictions handler
   *
   * @param sql               the table to execute.
   * @param params            these are the parameters name with values to fill the statement.
   * @param restictionHandler the handler which is called once the operation completes.
   * @see java.sql.Statement#executeUpdate(String)
   * @see java.sql.PreparedStatement#executeUpdate(String)
   */
  @Fluent
  Single<ResultSet> rxQuery(String sql, JsonObject params,
    RestrinctionHandler<JsonObject, String, StringBuffer> restictionHandler);
}
