package in.edu.kristujayanti.handlers;

import in.edu.kristujayanti.enums.ResponseType;
import in.edu.kristujayanti.enums.StatusCode;
import in.edu.kristujayanti.services.AssetsService;
import in.edu.kristujayanti.util.ResponseUtil;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class GetLocationsListHandler implements Handler<RoutingContext> {
  private final AssetsService assetsService;
  public GetLocationsListHandler(AssetsService assetsService) { this.assetsService = assetsService; }
  @Override
  public void handle(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    try {
      JsonArray result = assetsService.getDistinctLocations();
      ResponseUtil.createResponse(response, ResponseType.SUCCESS, StatusCode.TWOHUNDRED,
        new JsonObject().put("locations", result), new JsonArray());
    } catch (Exception e) {
      ResponseUtil.createResponse(response, ResponseType.ERROR, StatusCode.INTERNAL_SERVER_ERROR,
        new JsonObject().put("error", "Internal Server Error"), new JsonArray());
    }
  }
}
