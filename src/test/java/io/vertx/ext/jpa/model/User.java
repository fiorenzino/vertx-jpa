package io.vertx.ext.jpa.model;

import io.vertx.core.json.JsonObject;

import java.time.Instant;
import java.util.UUID;

public class User
{
  public String uuid;
  public String name;
  public String surname;
  public Integer age;
  public Instant birthday;

  public User()
  {
  }

  public static User create()
  {
    return new User(UUID.randomUUID().toString(), "fiorenzo", "pizza", 42, Instant.parse("1975-02-11T10:15:30Z"));
  }

  public User(String uuid, String name, String surname, Integer age, Instant birthday)
  {
    this.uuid = uuid;
    this.name = name;
    this.surname = surname;
    this.age = age;
    this.birthday = birthday;
  }

  public JsonObject toJson()
  {
    JsonObject json = new JsonObject();
    json.put("uuid", this.uuid);
    json.put("name", this.name);
    json.put("surname", this.surname);
    json.put("age", this.age);
    json.put("birthday", this.birthday);
    return json;
  }
}
