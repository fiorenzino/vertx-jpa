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
package nz.fiore.vertx.ext.jpa.impl.actions;

import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.json.JsonObject;
import nz.fiore.vertx.ext.jpa.util.RestrinctionHandler;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLOptions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:fiorenzo.pizza@gmail.com">Fiorenzo Pizza</a>
 */
public class JPAHandlerQuery extends AbstractJDBCAction<ResultSet>
{
/*
SELECT * from INTO table_name  WHERE column = :VAR1 ;
 */

  private final JsonObject params;
  private final JsonObject orderedParams;
  private String table;
  private String sql;
  private final RestrinctionHandler<JsonObject, String, StringBuffer> restictionHandler;

  public JPAHandlerQuery(Vertx vertx, JPAStatementHelper helper, SQLOptions options, ContextInternal ctx, String table,
    JsonObject params, RestrinctionHandler<JsonObject, String, StringBuffer> restictionHandler)
  {
    super(vertx, helper, options, ctx);
    this.params = params;
    this.table = table;
    this.orderedParams = new JsonObject();
    this.restictionHandler = restictionHandler;
    init();
  }

  private void init()
  {
    StringBuffer sb = new StringBuffer();
    restictionHandler.handle(params, table, sb);
    Pattern findParametersPattern = Pattern.compile("(?<!')(:[\\w]*)(?!')");
    Matcher matcher = findParametersPattern.matcher(sb.toString());
    while (matcher.find())
    {
      orderedParams.put(matcher.group().substring(1), params.getValue(matcher.group().substring(1)));
    }
    sql = sb.toString().replaceAll(findParametersPattern.pattern(), "?");

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

      ResultSet resultSet = null;

      if (retResult)
      {
        ResultSet ref = null;
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



