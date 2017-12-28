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
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLOptions;

import java.sql.*;
import java.util.StringJoiner;

/**
 * @author <a href="mailto:fiorenzo.pizza@gmail.com">Fiorenzo Pizza</a>
 */
public class JPAFind extends AbstractJDBCAction<ResultSet>
{
/*
SELECT * FROM  table_name WHERE some_column=some_value;
 */

   private final String table;
   private final JsonObject key;
   private final JsonObject in;
   private String sql;

   public JPAFind(Vertx vertx, JPAStatementHelper helper, SQLOptions options, ContextInternal ctx, String table,
            JsonObject key)
   {
      super(vertx, helper, options, ctx);
      this.key = key;
      this.table = table;
      this.in = new JsonObject();
      init();
   }

   private void init()
   {
      StringJoiner where = new StringJoiner(",", " WHERE ", " ");
      this.key.stream().forEach(
               coppia -> {
                  where.add(coppia.getKey() + "=?");
                  this.in.put("k_" + coppia.getKey(), coppia.getValue());
               }
      );
      this.sql = "select * FROM " + table + where.toString();
   }

   @Override
   public ResultSet execute(Connection conn) throws SQLException
   {
      try (PreparedStatement statement = conn.prepareStatement(sql))
      {
         // apply statement options
         applyStatementOptions(statement);

         helper.fillStatement(statement, in);
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
      return "find";
   }
}
