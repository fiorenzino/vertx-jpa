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

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import nz.fiore.vertx.ext.jpa.model.Whisky;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * @author <a href="mailto:fiorenzo.pizza@gmail.com">Fiorenzo Pizza</a>
 */
public abstract class AbstractBaseTest
{

   public static String TABLE = "whiskies";
   public static String CREATE_TABLE_QUERY = "create table IF NOT EXISTS " + TABLE
            + " (uuid varchar(255), name varchar(255), collection_name varchar(255), date datetime NULL, amount decimal(19,4) )";
   public static String TABLE_KEY = "uuid";
   public static String CREATE_TABLE_TYPO_ERROR_QUERY = "create table  " + TABLE
            + " (uuid varchar(255), name varchar(255), collection_name varchar(255), date datetime NULL, amount decimal(19,4) ";
   public static String CREATE_TABLE_TYPE_ERROR_QUERY = "create table " + TABLE
            + " (uuid varchar(255), name varchar(255), collection_name varchar(255), date dt NULL, amount decimal(19,4) )";
   public static String SELECT_QUERY = "select * from " + TABLE;
   public static String SELECT_COUNT_AS_NUM_QUERY = "select count(*) as NUM from " + TABLE;
   public static String COUNT_ALIAS = "NUM";
   static JsonObject config = new JsonObject()
            .put("url", "jdbc:hsqldb:mem:test?shutdown=true")
            .put("driver_class", "org.hsqldb.jdbcDriver")
            .put("max_pool_size", 30);
   protected static JPAClient jpaClient;
   protected static Whisky whiskyP;
   protected static Whisky whiskyP1;
   protected static Whisky whiskyP2;
   protected static Whisky whiskyU;

   private static Vertx vertx;

   @BeforeClass
   public static void setUp()
   {
      System.out.println("setUp");
      vertx = Vertx.vertx();
      jpaClient = JPAClient.createShared(vertx, config);
      whiskyP = new Whisky(UUID.randomUUID().toString(), "flower");
      whiskyP1 = new Whisky(UUID.randomUUID().toString(), "p1 flower");
      whiskyP2 = new Whisky(UUID.randomUUID().toString(), "p2 flower", "whisky collection", Instant.now(),
               new BigDecimal(33L));
      whiskyU = new Whisky(whiskyP.toJson());
      whiskyU.name = "flower UP";
   }

   @AfterClass
   public static void shutDown()
   {
      jpaClient.create(CREATE_TABLE_QUERY, result -> {
      });
      System.out.println("shutDown");
      vertx.close();
      jpaClient.close();
   }
}