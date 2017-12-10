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

package nz.fiore.vertx.ext.jpa;

import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:fiorenzo.pizza@gmail.com">Fiorenzo Pizza</a>
 */
@RunWith(VertxUnitRunner.class)
public class JpaTest extends AbstractBaseTest
{

  @Test
  public void test(TestContext context)
  {
    Async async = context.async();
    // Connect to the database
    jpaClient.rxGetConnection().flatMap(conn -> {

      // Now chain some statements using flatmap composition
      Single<ResultSet> resa = conn.rxCreate(CREATE_TABLE)
        .flatMap(result1 -> conn.rxPersist(TABLE, whiskyP.toJson()))
        .flatMap(result1 -> conn.rxPersist(TABLE, whiskyP1.toJson()))
        .flatMap(result2 -> conn
          .rxMerge(TABLE, whiskyU.toJson(), new JsonObject().put("uuid", whiskyU.uuid)))
        .flatMap(result3 -> conn.rxQuery("select * from whiskies",
          new JsonObject()))
        .flatMap(result3 -> {
          if (result3 != null && result3.getRows() != null)
          {
            result3.getRows().forEach(
              row -> {
                System.out.println(row);
              }
            );
          }
          return conn.rxQuery("select * from whiskies where name like :NAME",
            new JsonObject().put("NAME", "%uno%"));
        })
        .doOnError(error -> {
          System.out.println("ERROR IN LIKE");
          System.out.println(error.getCause());
        }).doOnSuccess(success -> {
          System.out.println("LIKE UNO QUERY");
          if (success != null && success.getRows() != null)
          {
            success.getRows().forEach(
              row -> {
                System.out.println(row);
              }
            );
          }
        });

      return resa.doAfterTerminate(conn::close);

    }).subscribe(resultSet -> {
      // Subscribe to the final result
      System.out.println("Results : " + resultSet.getResults());
      async.complete();
    }, err -> {
      System.out.println("Database problem");
      err.printStackTrace();
      async.complete();
    });
  }
}
