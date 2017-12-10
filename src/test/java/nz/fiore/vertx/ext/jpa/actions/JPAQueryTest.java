package nz.fiore.vertx.ext.jpa.actions;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import nz.fiore.vertx.ext.jpa.AbstractBaseTest;
import nz.fiore.vertx.ext.jpa.sql.JPAConnection;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class JPAQueryTest extends AbstractBaseTest
{

   @Test
   public void query(TestContext context)
   {
      Async async = context.async();
      jpaClient.getJPAConnection(conn -> {
         JPAConnection connection = conn.result();
         connection.create(CREATE_TABLE, result -> {
            System.out.println("create table");
            Assert.assertTrue(result.succeeded());
            connection.persist(TABLE, whiskyP.toJson(), result_p -> {
               System.out.println("persist");
               Assert.assertTrue(result_p.succeeded());
               connection.query("selct * from " + TABLE + " where name = :NAME ",
                        new JsonObject().put("NAME", whiskyU.name), result_q-> {
                           System.out.println("merge");
                           Assert.assertTrue(result_q.succeeded());
                           Assert.assertEquals(result_q.result().getRows(), 1);
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

      async.complete();
   }
}
