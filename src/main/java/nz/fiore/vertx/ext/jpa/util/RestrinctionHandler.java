package nz.fiore.vertx.ext.jpa.util;

@FunctionalInterface
public interface RestrinctionHandler<JsonObject, String, StringBuffer>
{
  void handle(JsonObject params, String table, StringBuffer toSql);

}
