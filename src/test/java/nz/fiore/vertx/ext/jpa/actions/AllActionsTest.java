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
package nz.fiore.vertx.ext.jpa.actions;

import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import nz.fiore.vertx.ext.jpa.AbstractBaseTest;
import nz.fiore.vertx.ext.jpa.sql.JPAConnection;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:fiorenzo.pizza@gmail.com">Fiorenzo Pizza</a>
 */
@RunWith(VertxUnitRunner.class)
public class AllActionsTest extends AbstractBaseTest
{

   @Test
   public void create(TestContext context)
   {
      Async async = context.async();
      jpaClient.create(CREATE_TABLE_QUERY, result -> {
         if (result.succeeded())
         {

         }
         else
         {
            result.cause().printStackTrace();
         }
         async.complete();
      });
   }

   @Test
   public void createWithTypoError(TestContext context)
   {
      Async async = context.async();
      jpaClient.create(CREATE_TABLE_TYPO_ERROR_QUERY, result -> {
         Assert.assertFalse(result.succeeded());
         async.complete();
      });
   }

   @Test
   public void createWithTypeError(TestContext context)
   {
      Async async = context.async();
      jpaClient.create(CREATE_TABLE_TYPE_ERROR_QUERY, result -> {
         Assert.assertFalse(result.succeeded());
         Assert.assertTrue(result.cause().getMessage().contains("type not found"));
         async.complete();
      });
   }

   @Test
   public void delete(TestContext context)
   {
      Async async = context.async();
      jpaClient.getJPAConnection(conn -> {
         JPAConnection connection = conn.result();
         connection.create(CREATE_TABLE_QUERY, result -> {
            System.out.println("create table");
            if (result.succeeded())
            {

            }
            else
            {
               result.cause().printStackTrace();
            }
            connection.persist(TABLE, whiskyP.toJson(), result_p -> {
               System.out.println("persist");
               Assert.assertTrue(result_p.succeeded());
               connection.delete(TABLE, new JsonObject().put(TABLE_KEY, whiskyP.uuid), result_d -> {
                  System.out.println("delete");
                  Assert.assertTrue(result_d.succeeded());
                  async.complete();
               });
            });
         });
      });
   }

   @Test
   public void rxDelete(TestContext context)
   {
      Async async = context.async();
      jpaClient.rxGetConnection()
               .flatMap(conn -> {
                  Single<ResultSet> resa = conn.rxCreate(CREATE_TABLE_QUERY)
                           .flatMap(result1 -> conn.rxPersist(TABLE, whiskyP.toJson()))
                           .flatMap(result3 -> conn.rxDelete(TABLE, new JsonObject().put(TABLE_KEY, whiskyP.uuid)))
                           .flatMap(result3 -> {
                              Assert.assertEquals(result3.getUpdated(), 1);
                              return conn.rxQuery(SELECT_COUNT_AS_NUM_QUERY, new JsonObject());
                           })
                           .doOnSuccess(success -> {
                              Assert.assertEquals(success.getRows().get(0).getInteger(COUNT_ALIAS).intValue(), 0);
                           });
                  return resa.doAfterTerminate(conn::close);
               }).subscribe(resultSet -> {
         // Subscribe to the final result
         Assert.assertEquals(resultSet.getResults().size(), 1);
         async.complete();
      }, err -> {
         err.printStackTrace();
         async.complete();
      });
   }

   @Test
   public void find(TestContext context)
   {
      Async async = context.async();
      jpaClient.getJPAConnection(conn -> {
         JPAConnection connection = conn.result();
         connection.create(CREATE_TABLE_QUERY, result -> {
            System.out.println("create table");
            if (result.succeeded())
            {

            }
            else
            {
               result.cause().printStackTrace();
            }
            connection.persist(TABLE, whiskyP.toJson(), result_p -> {
               System.out.println("persist");
               Assert.assertTrue(result_p.succeeded());
               connection.find(TABLE, new JsonObject().put(TABLE_KEY, whiskyU.uuid), result_m -> {
                  System.out.println("find");
                  Assert.assertTrue(result_m.succeeded());
                  Assert.assertTrue(result_m.result().getRows().size() > 0);
                  async.complete();
               });
            });
         });
      });
   }

   @Test
   public void rxFind(TestContext context)
   {
      Async async = context.async();
      jpaClient.rxGetConnection()
               .flatMap(conn -> {
                  Single<ResultSet> resa = conn.rxCreate(CREATE_TABLE_QUERY)
                           .flatMap(result1 -> conn.rxPersist(TABLE, whiskyP.toJson()))
                           .flatMap(result2 -> conn
                                    .rxFind(TABLE, new JsonObject().put(TABLE_KEY, whiskyU.uuid)))
                           .flatMap(result3 -> {
                              Assert.assertEquals(result3.getResults().size(), 1);
                              return conn.rxQuery(SELECT_COUNT_AS_NUM_QUERY, new JsonObject());
                           })
                           .doOnSuccess(success -> {
                              Assert.assertEquals(success.getRows().get(0).getInteger(COUNT_ALIAS).intValue(), 1);
                           });
                  return resa.doAfterTerminate(conn::close);
               }).subscribe(resultSet -> {
         // Subscribe to the final result
         Assert.assertTrue(resultSet.getResults().size() > 0);
         async.complete();
      }, err -> {
         err.printStackTrace();
         async.complete();
      });
   }

   @Test
   public void merge(TestContext context)
   {
      Async async = context.async();
      jpaClient.getJPAConnection(conn -> {
         JPAConnection connection = conn.result();
         connection.create(CREATE_TABLE_QUERY, result -> {
            System.out.println("create table");
            if (result.succeeded())
            {

            }
            else
            {
               result.cause().printStackTrace();
            }
            connection.persist(TABLE, whiskyP.toJson(), result_p -> {
               System.out.println("persist");
               Assert.assertTrue(result_p.succeeded());
               connection.merge(TABLE, whiskyU.toJson(), new JsonObject().put(TABLE_KEY, whiskyU.uuid), result_m -> {
                  System.out.println("merge");
                  Assert.assertTrue(result_m.succeeded());
                  Assert.assertTrue(result_m.result().getUpdated() > 0);
                  async.complete();
               });
            });
         });
      });
   }

   @Test
   public void rxMerge(TestContext context)
   {
      Async async = context.async();
      jpaClient.rxGetConnection()
               .flatMap(conn -> {
                  Single<ResultSet> resa = conn.rxCreate(CREATE_TABLE_QUERY)
                           .flatMap(result1 -> conn.rxPersist(TABLE, whiskyP.toJson()))
                           .flatMap(result3 -> conn
                                    .rxMerge(TABLE, whiskyU.toJson(), new JsonObject().put(TABLE_KEY, whiskyU.uuid)))
                           .flatMap(result3 -> {
                              Assert.assertTrue(result3.getUpdated() > 0);
                              return conn.rxQuery(SELECT_COUNT_AS_NUM_QUERY, new JsonObject());
                           })
                           .doOnSuccess(success -> {
                              Assert.assertTrue(success.getRows().get(0).getInteger(COUNT_ALIAS).intValue() > 0);
                           });
                  return resa.doAfterTerminate(conn::close);
               }).subscribe(resultSet -> {
         // Subscribe to the final result
         Assert.assertTrue(resultSet.getResults().size() > 0);
         async.complete();
      }, err -> {
         err.printStackTrace();
         async.complete();
      });
   }

   @Test
   public void persist(TestContext context)
   {
      Async async = context.async();
      jpaClient.getJPAConnection(conn -> {
         JPAConnection connection = conn.result();
         connection.create(CREATE_TABLE_QUERY, result -> {
            System.out.println("create table");
            if (result.succeeded())
            {

            }
            else
            {
               result.cause().printStackTrace();
            }
            connection.persist(TABLE, whiskyP.toJson(), result_p -> {
               System.out.println("persist");
               Assert.assertTrue(result_p.succeeded());
               async.complete();
            });
         });
      });
   }

   @Test
   public void rxPersist(TestContext context)
   {
      Async async = context.async();
      jpaClient.rxGetConnection()
               .flatMap(conn -> {
                  Single<ResultSet> resa = conn.rxCreate(CREATE_TABLE_QUERY)
                           .flatMap(result1 -> conn.rxPersist(TABLE, whiskyP.toJson()))
                           .flatMap(result3 -> {
                              Assert.assertEquals(result3.getUpdated(), 1);
                              return conn.rxQuery(SELECT_COUNT_AS_NUM_QUERY, new JsonObject());
                           })
                           .doOnSuccess(success -> {
                              Assert.assertTrue(success.getRows().get(0).getInteger(COUNT_ALIAS).intValue() > 0);
                           });
                  return resa.doAfterTerminate(conn::close);
               }).subscribe(resultSet -> {
         // Subscribe to the final result
         Assert.assertTrue(resultSet.getResults().size() > 0);
         async.complete();
      }, err -> {
         err.printStackTrace();
         async.complete();
      });
   }

   @Test
   public void query(TestContext context)
   {
      Async async = context.async();
      jpaClient.getJPAConnection(conn -> {
         JPAConnection connection = conn.result();
         connection.create(CREATE_TABLE_QUERY, result -> {
            System.out.println("create table");
            if (result.succeeded())
            {

            }
            else
            {
               result.cause().printStackTrace();
            }
            connection.persist(TABLE, whiskyP.toJson(), result_p -> {
               System.out.println("persist");
               Assert.assertTrue(result_p.succeeded());
               connection.query("select * from " + TABLE + " where name = :NAME ",
                        new JsonObject().put("NAME", whiskyP.name), result_q -> {
                           System.out.println("query");
                           Assert.assertTrue(result_q.succeeded());
                           Assert.assertTrue(result_q.result().getRows().size() > 0);
                           async.complete();
                        });
            });
         });
      });
   }

   @Test
   public void rxQuery(TestContext context)
   {
      Async async = context.async();
      jpaClient.rxGetConnection()
               .flatMap(conn -> {
                  Single<ResultSet> resa = conn.rxCreate(CREATE_TABLE_QUERY)
                           .flatMap(result1 -> conn.rxPersist(TABLE, whiskyP.toJson()))
                           .flatMap(result3 -> conn.rxQuery(SELECT_COUNT_AS_NUM_QUERY, new JsonObject()))
                           .doOnSuccess(success -> {
                              Assert.assertTrue(success.getRows().get(0).getInteger(COUNT_ALIAS).intValue() > 0);
                           });
                  return resa.doAfterTerminate(conn::close);
               }).subscribe(resultSet -> {
         // Subscribe to the final result
         Assert.assertTrue(resultSet.getResults().size() > 0);
         async.complete();
      }, err -> {
         err.printStackTrace();
         async.complete();
      });
   }

}
