package nz.fiore.vertx.ext.jpa.impl.actions;

import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;
import io.vertx.ext.sql.SQLOptions;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by fiorenzo on 18/01/17.
 */
public class JPACreate extends AbstractJDBCAction<Void>
{

  private final String sql;

  public JPACreate(Vertx vertx, SQLOptions options, ContextInternal ctx, String sql)
  {
    super(vertx, options, ctx);
    this.sql = sql;
  }

  @Override
  public Void execute(Connection conn) throws SQLException
  {
    try (Statement stmt = conn.createStatement())
    {
      // apply statement options
      applyStatementOptions(stmt);

      boolean isResultSet = stmt.execute(sql);
      // If the execute statement happens to return a result set, we should close it in case
      // the connection pool doesn't.
      if (isResultSet)
      {
        while (stmt.getMoreResults())
        {
          try (java.sql.ResultSet rs = stmt.getResultSet())
          {
            // TODO: is this correct? just ignore?
          }
          ;
        }
      }
      return null;
    }
  }

  @Override
  protected String name()
  {
    return "create";
  }
}
