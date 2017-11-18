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
public class JDBCDelete extends AbstractJDBCAction<UpdateResult>
{
/*
DELETE FROM table_name WHERE some_column=some_value;
 */

  private final String table;
  private final JsonObject key;
  private final JsonArray in;
  private String sql;

  public JDBCDelete(Vertx vertx, JPAStatementHelper helper, SQLOptions options, ContextInternal ctx, String table,
    JsonObject key)
  {
    super(vertx, helper, options, ctx);
    this.key = key;
    this.table = table;
    this.in = new JsonArray();
    init();
  }

  private void init()
  {
    StringJoiner where = new StringJoiner("=? AND ", "DELETE FROM " + table + " WHERE ", "=? ");
    this.key.stream().forEach(
      coppia -> {
        where.add(coppia.getKey());
        this.in.add(coppia.getValue());
      }
    );
    this.sql = where.toString();
  }

  @Override
  public UpdateResult execute(Connection conn) throws SQLException
  {
    final boolean returKeys = true;
    try (PreparedStatement statement = conn.prepareStatement(sql, Statement.NO_GENERATED_KEYS))
    {

      helper.fillStatement(statement, in);

      int updated = statement.executeUpdate();
      JsonArray keys = new JsonArray();

      // Create JsonArray of keys
      if (returKeys)
      {
        ResultSet rs = null;
        try
        {
          // the resource might also fail
          // specially on oracle DBMS
          rs = statement.getGeneratedKeys();
          if (rs != null)
          {
            while (rs.next())
            {
              Object key = rs.getObject(1);
              if (key != null)
              {
                keys.add(helper.convertSqlValue(key));
              }
            }
          }
        }
        catch (SQLException e)
        {
          // do not crash if no permissions
        }
        finally
        {
          if (rs != null)
          {
            try
            {
              rs.close();
            }
            catch (SQLException e)
            {
              // ignore close error
            }
          }
        }
      }

      return new UpdateResult(updated, keys);
    }
  }

  @Override
  protected String name()
  {
    return "delete";
  }
}
