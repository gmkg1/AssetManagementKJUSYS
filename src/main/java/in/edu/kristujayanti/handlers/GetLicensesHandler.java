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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Required imports
import org.bson.Document;
import org.bson.types.ObjectId;
import com.mongodb.client.MongoCollection;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.util.Date;

public class GetLicensesHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER =
    LoggerFactory.getLogger(GetLicensesHandler.class);

  private final AssetsService assetsService;

  public GetLicensesHandler(AssetsService assetsService) {
    this.assetsService = assetsService;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();

    try {
      LOGGER.info("Handling request for GetLicensesHandler");

      String assetId = routingContext.pathParam("assetId");
      if (assetId == null || assetId.isBlank()) {
        assetId = routingContext.request().getParam("assetId");
      }
      JsonObject result = assetsService.getLicensesAndWarrantyByAssetId(assetId);

      ResponseUtil.createResponse(
        response,
        ResponseType.SUCCESS,
        StatusCode.TWOHUNDRED,
        result,
        new JsonArray()
      );
    } catch (IllegalArgumentException e) {
      LOGGER.warn("Validation error in GetLicensesHandler", e);
      ResponseUtil.createResponse(
        response,
        ResponseType.ERROR,
        StatusCode.BAD_REQUEST,
        new JsonObject().put("error", e.getMessage()),
        new JsonArray()
      );
    } catch (Exception e) {
      LOGGER.error("Error in GetLicensesHandler", e);
      ResponseUtil.createResponse(
        response,
        ResponseType.ERROR,
        StatusCode.INTERNAL_SERVER_ERROR,
        new JsonObject().put("error", "Internal Server Error"),
        new JsonArray()
      );
    }
  }
}
