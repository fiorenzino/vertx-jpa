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
public class JDBCMerge extends AbstractJDBCAction<UpdateResult>
{
/*
UPDATE table_name SET column1=value1,column2=value2,... WHERE some_column=some_value;
 */

  private final String table;
  private final JsonObject params;
  private final JsonObject key;
  private final JsonObject in;
  private String sql;


  public JDBCMerge(Vertx vertx, JPAStatementHelper helper, SQLOptions options, ContextInternal ctx, JsonObject params, String table, JsonObject key) {
    super(vertx, helper, options, ctx);
    this.params = params;
    this.key = key;
    this.table = table;
    this.in = new JsonObject();
    init();
  }

  private void init() {
    StringJoiner toSet = new StringJoiner("=?, ", "UPDATE " + table + " SET ", "=? ");
    StringJoiner where = new StringJoiner(",", " WHERE ", " ");
    this.params.stream().forEach(
      coppia -> {
        in.put("p_" + coppia.getKey(), coppia.getValue());
        toSet.add(coppia.getKey());
      }
    );
    this.key.stream().forEach(
      coppia -> {
        where.add(coppia.getKey() + "=?");
        this.in.put("k_" + coppia.getKey(), coppia.getValue());
      }
    );
    this.sql = toSet.toString() + where.toString();
  }

  @Override
  public UpdateResult execute(Connection conn) throws SQLException
  {
    final boolean returKeys = true;
    try (PreparedStatement statement = conn.prepareStatement(sql, Statement.NO_GENERATED_KEYS)) {


      helper.fillStatement(statement, in);

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
    return "merge";
  }
}
