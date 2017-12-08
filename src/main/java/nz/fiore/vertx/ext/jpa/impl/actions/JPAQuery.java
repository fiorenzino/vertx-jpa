package nz.fiore.vertx.ext.jpa.impl.actions;

import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLOptions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by fiorenzo on 18/01/17.
 */
public class JPAQuery extends AbstractJDBCAction<ResultSet>
{
/*
SELECT * from INTO table_name  WHERE column = :VAR1 ;
 */

  private final JsonObject params;
  private final JsonObject orderedParams;
  private String sql;

  public JPAQuery(Vertx vertx, JPAStatementHelper helper, SQLOptions options, ContextInternal ctx, JsonObject params,
    String sql)
  {
    super(vertx, helper, options, ctx);
    this.params = params;
    this.sql = sql;
    this.orderedParams = new JsonObject();
    init();
  }

  private void init()
  {
    Pattern findParametersPattern = Pattern.compile("(?<!')(:[\\w]*)(?!')");
    Matcher matcher = findParametersPattern.matcher(sql);
    while (matcher.find())
    {
      orderedParams.put(matcher.group().substring(1), params.getValue(matcher.group().substring(1)));
    }
    sql = sql.replaceAll(findParametersPattern.pattern(), "?");

  }

  @Override
  public ResultSet execute(Connection conn) throws SQLException
  {
    try (PreparedStatement statement = conn.prepareStatement(sql))
    {
      // apply statement options
      applyStatementOptions(statement);

      helper.fillStatement(statement, orderedParams);
      boolean retResult = statement.execute();

      io.vertx.ext.sql.ResultSet resultSet = null;

      if (retResult)
      {
        io.vertx.ext.sql.ResultSet ref = null;
        // normal return only
        while (retResult)
        {
          try (java.sql.ResultSet rs = statement.getResultSet())
          {
            // 1st rs
            if (ref == null)
            {
              resultSet = helper.asList(rs);
              ref = resultSet;
            }
            else
            {
              ref.setNext(helper.asList(rs));
              ref = ref.getNext();
            }
          }
          retResult = statement.getMoreResults();
        }
      }

      return resultSet;
    }
  }

  @Override
  protected String name()
  {
    return "query";
  }
}



