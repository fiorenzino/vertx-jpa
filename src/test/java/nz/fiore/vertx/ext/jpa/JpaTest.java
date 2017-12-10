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
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * @author <a href="mailto:fiorenzo.pizza@gmail.com">Fiorenzo Pizza</a>
 */
@RunWith(VertxUnitRunner.class)
public class JpaTest
{

  JsonObject config = new JsonObject()
    .put("url", "jdbc:hsqldb:mem:test?shutdown=true")
    .put("driver_class", "org.hsqldb.jdbcDriver")
    .put("max_pool_size", 30);
  JPAClient jpaClient;
  Whisky whiskyP;
  Whisky whiskyP1;
  Whisky whiskyU;

  private Vertx vertx;

  @Before
  public void setUp(TestContext context)
  {
    vertx = Vertx.vertx();
    jpaClient = JPAClient.createShared(vertx, config);
    whiskyP = new Whisky();
    whiskyP.name = "flower";
    whiskyP.uuid = UUID.randomUUID().toString();

    whiskyP1 = new Whisky();
    whiskyP1.name = " uno flower";
    whiskyP1.uuid = UUID.randomUUID().toString();

    whiskyU = new Whisky(whiskyP.toJson());
    whiskyU.name = "flower UP";

  }

  @Test
  public void test(TestContext context)
  {
    Async async = context.async();
    // Connect to the database
    jpaClient.rxGetConnection().flatMap(conn -> {

      // Now chain some statements using flatmap composition
      Single<ResultSet> resa = conn.rxCreate(
        "create table whiskies "
          + " (uuid varchar(255), "
          + "  name varchar(255),"
          + "  collection_name varchar(255), "
          + "  date datetime NULL, "
          + "   amount decimal(19,4) )")
        .flatMap(result1 -> conn.rxPersist("whiskies", whiskyP.toJson()))
        .flatMap(result1 -> conn.rxPersist("whiskies", whiskyP1.toJson()))
        .flatMap(result2 -> conn
          .rxMerge("whiskies", whiskyU.toJson(), new JsonObject().put("uuid", whiskyU.uuid)))
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

class Whisky
{
  public String uuid;
  public String name;
  public String collection_name;
  public Instant date;
  public BigDecimal amount;

  public Whisky()
  {
  }

  public Whisky(String uuid, String name)
  {
    this.uuid = uuid;
    this.name = name;
  }

  public Whisky(JsonObject json)
  {
    super();
    fromJson(json, this);
  }

  public Whisky fromJson(JsonObject json, Whisky whisky)
  {
    if (json.getValue("uuid") instanceof String)
    {
      whisky.uuid = json.getString("uuid");
    }
    if (json.getValue("name") instanceof String)
    {
      whisky.name = json.getString("name");
    }
    if (json.getValue("collection_name") instanceof String)
    {
      whisky.collection_name = json.getString("collection_name");
    }
    try
    {
      Object automaticVerified_dateObj = json.getString("date");
      if (automaticVerified_dateObj instanceof String)
      {
        whisky.date = Instant.parse((String) automaticVerified_dateObj);
      }
      else if (automaticVerified_dateObj instanceof Instant)
      {
        whisky.date = json.getInstant("date");
      }
    }
    catch (Exception e)
    {

    }
    try
    {
      if (json.containsKey("amount") && json.getString("amount") != null
        && !json.getString("amount").trim().isEmpty())
      {
        String amount = json.getString("amount");
        whisky.amount = new BigDecimal(amount);
      }
    }
    catch (Exception e)
    {

    }

    return whisky;
  }

  public JsonObject toJson()
  {
    JsonObject json = new JsonObject();
    json.put("uuid", this.uuid);
    json.put("name", this.name);
    json.put("collection_name", this.collection_name);
    json.put("date", this.date);
    if (this.amount != null)
    {
      json.put("amount", this.amount.toString());

    }
    else
    {
      BigDecimal amount = null;
      json.put("amount", amount);
    }
    return json;
  }

}

