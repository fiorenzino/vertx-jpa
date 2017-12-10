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

@RunWith(VertxUnitRunner.class)
public class JPAMergeTest extends AbstractBaseTest
{

   @Test
   public void merge(TestContext context)
   {
      Async async = context.async();
      jpaClient.getJPAConnection(conn -> {
         JPAConnection connection = conn.result();
         connection.create(CREATE_TABLE_QUERY, result -> {
            System.out.println("create table");
            Assert.assertTrue(result.succeeded());
            connection.persist(TABLE, whiskyP.toJson(), result_p -> {
               System.out.println("persist");
               Assert.assertTrue(result_p.succeeded());
               connection.merge(TABLE, whiskyU.toJson(), new JsonObject().put(TABLE_KEY, whiskyU.uuid), result_m -> {
                  System.out.println("merge");
                  Assert.assertTrue(result_m.succeeded());
                  Assert.assertEquals(result_m.result().getUpdated(), 1);
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
                              Assert.assertEquals(result3.getUpdated(), 1);
                              return conn.rxQuery(SELECT_COUNT_AS_NUM_QUERY, new JsonObject());
                           })
                           .doOnSuccess(success -> {
                              Assert.assertEquals(success.getRows().get(0).getInteger(COUNT_ALIAS).intValue(), 1);
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
}
