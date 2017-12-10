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
public class JPADeleteTest extends AbstractBaseTest
{

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
}
