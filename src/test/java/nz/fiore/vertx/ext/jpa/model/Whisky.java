package nz.fiore.vertx.ext.jpa.model;

import io.vertx.core.json.JsonObject;

import java.math.BigDecimal;
import java.time.Instant;

public class Whisky
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

   public Whisky(String uuid, String name, String collection_name, Instant date, BigDecimal amount)
   {
      this.uuid = uuid;
      this.name = name;
      this.collection_name = collection_name;
      this.date = date;
      this.amount = amount;
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
