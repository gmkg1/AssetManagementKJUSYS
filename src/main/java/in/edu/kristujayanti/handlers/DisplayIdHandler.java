package in.edu.kristujayanti.handlers;

import in.edu.kristujayanti.services.AssetsService;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DisplayIdHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LoggerFactory.getLogger(DisplayIdHandler.class);

  private final AssetsService assetsService;

  public DisplayIdHandler(AssetsService assetsService) {
    this.assetsService = assetsService;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();

    try {
      LOGGER.info("Handling request for DisplayIdHandler API");

      JsonObject payload = routingContext.body().asJsonObject();
      if (payload == null) {
        throw new IllegalArgumentException("Request body is required");
      }

      String assetTagId = payload.getString("assetTagId");
      String locationId = payload.getString("locationId");

      String displayId = assetsService.generateAssetDisplayId(assetTagId, locationId);

      JsonObject responseData = new JsonObject().put("displayId", displayId);

      response.putHeader("content-type", "application/json")
              .setStatusCode(200)
              .end(responseData.encode());

    } catch (IllegalArgumentException e) {
      LOGGER.warn("Validation error in DisplayIdHandler API", e);
      response.putHeader("content-type", "application/json")
              .setStatusCode(400)
              .end(new JsonObject().put("error", e.getMessage()).encode());
    } catch (Exception e) {
      LOGGER.error("Error in DisplayIdHandler API", e);
      response.putHeader("content-type", "application/json")
              .setStatusCode(500)
              .end(new JsonObject().put("error", "Internal Server Error").encode());
    }
  }
}
