package nz.fiore.vertx.ext.jpa.actions;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import nz.fiore.vertx.ext.jpa.AbstractBaseTest;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class JPAQueryTest extends AbstractBaseTest
{

   @Test
   public void query(TestContext context)
   {
      Async async = context.async();

      async.complete();
   }

   @Test
   public void rxQuery(TestContext context)
   {
      Async async = context.async();

      async.complete();
   }
}
