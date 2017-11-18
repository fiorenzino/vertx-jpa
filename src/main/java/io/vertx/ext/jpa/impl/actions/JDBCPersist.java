package io.vertx.ext.jpa.impl.actions;

import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLOptions;
import io.vertx.ext.sql.UpdateResult;

import java.sql.*;
import java.util.StringJoiner;

/**
 * Created by fiorenzo on 18/01/17.
 */
public class JDBCPersist extends AbstractJDBCAction<UpdateResult>
{
/*
INSERT INTO table_name (column1,column2,column3,...) VALUES (value1,value2,value3,...);
 */

  private final String table;
  private final JsonObject params;
  private String sql;


  public JDBCPersist(Vertx vertx, JPAStatementHelper helper, SQLOptions options, ContextInternal ctx,  JsonObject params, String table) {
    super(vertx, helper, options, ctx);
    this.params = params;
    this.table = table;
    init();
  }

  private void init() {
    StringJoiner columns = new StringJoiner(",", "INSERT INTO " + table + " ( ", " ) ");
    StringJoiner values = new StringJoiner(",", " VALUES ( ", " )");

    params.stream().forEach(
      coppia -> {
        columns.add(coppia.getKey());
        values.add("?");
      }
    );
    this.sql = columns.toString() + values.toString();
  }

  @Override
  public UpdateResult execute(Connection conn) throws SQLException
  {
    final boolean returKeys = true;
    try (PreparedStatement statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

      helper.fillStatement(statement, params);

      int updated = statement.executeUpdate();
      JsonArray keys = new JsonArray();

      // Create JsonArray of keys
      if (returKeys) {
        ResultSet rs = null;
        try {
          // the resource might also fail
          // specially on oracle DBMS
          rs = statement.getGeneratedKeys();
          if (rs != null) {
            while (rs.next()) {
              Object key = rs.getObject(1);
              if (key != null) {
                keys.add(helper.convertSqlValue(key));
              }
            }
          }
        } catch (SQLException e) {
          // do not crash if no permissions
        } finally {
          if (rs != null) {
            try {
              rs.close();
            } catch (SQLException e) {
              // ignore close error
            }
          }
        }
      }

      return new UpdateResult(updated, keys);
    }
  }


  @Override
  protected String name() {
    return "persist";
  }
}
