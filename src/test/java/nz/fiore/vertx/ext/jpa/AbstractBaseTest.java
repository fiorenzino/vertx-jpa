package nz.fiore.vertx.ext.jpa;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import nz.fiore.vertx.ext.jpa.model.Whisky;
import org.junit.Before;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public abstract class AbstractBaseTest
{

   public static String TABLE = "whiskies";
   public static String CREATE_TABLE_QUERY = "create table " + TABLE
            + " (uuid varchar(255), name varchar(255), collection_name varchar(255), date datetime NULL, amount decimal(19,4) )";
   public static String TABLE_KEY = "uuid";
   public static String CREATE_TABLE_TYPO_ERROR_QUERY = "create table  " + TABLE
            + " (uuid varchar(255), name varchar(255), collection_name varchar(255), date datetime NULL, amount decimal(19,4) ";
   public static String CREATE_TABLE_TYPE_ERROR_QUERY = "create table " + TABLE
            + " (uuid varchar(255), name varchar(255), collection_name varchar(255), date dt NULL, amount decimal(19,4) )";
   public static String SELECT_QUERY = "select * from " + TABLE;
   public static String SELECT_COUNT_AS_NUM_QUERY = "select count(*) as NUM from " + TABLE;
   public static String COUNT_ALIAS = "NUM";
   JsonObject config = new JsonObject()
            .put("url", "jdbc:hsqldb:mem:test?shutdown=true")
            .put("driver_class", "org.hsqldb.jdbcDriver")
            .put("max_pool_size", 30);
   protected JPAClient jpaClient;
   protected Whisky whiskyP;
   protected Whisky whiskyP1;
   protected Whisky whiskyP2;
   protected Whisky whiskyU;

   private Vertx vertx;

   @Before
   public void setUp()
   {
      vertx = Vertx.vertx();
      jpaClient = JPAClient.createShared(vertx, config);
      whiskyP = new Whisky(UUID.randomUUID().toString(), "flower");
      whiskyP1 = new Whisky(UUID.randomUUID().toString(), "p1 flower");
      whiskyP2 = new Whisky(UUID.randomUUID().toString(), "p2 flower", "whisky collection", Instant.now(),
               new BigDecimal(33L));
      whiskyU = new Whisky(whiskyP.toJson());
      whiskyU.name = "flower UP";
   }
}