package nz.fiore.vertx.ext.jpa.actions;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import nz.fiore.vertx.ext.jpa.AbstractBaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:fiorenzo.pizza@gmail.com">Fiorenzo Pizza</a>
 */
@RunWith(VertxUnitRunner.class)
public class JPACreateTest extends AbstractBaseTest
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
}
