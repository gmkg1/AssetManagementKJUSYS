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

public class ExportAssetsHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExportAssetsHandler.class);

  private final AssetsService assetsService;

  public ExportAssetsHandler(AssetsService assetsService) {
    this.assetsService = assetsService;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    try {
      LOGGER.info("Handling request for ExportAssetsHandler");
      String assetName = routingContext.request().getParam("assetName");
      String assetTagName = routingContext.request().getParam("assetTagName");
      String categoryId = routingContext.request().getParam("categoryId");
      String locationId = routingContext.request().getParam("locationId");
      String statusId = routingContext.request().getParam("statusId");
      String purchaseDateFrom = routingContext.request().getParam("purchaseDateFrom");
      String purchaseDateTo = routingContext.request().getParam("purchaseDateTo");

      String csvContent = assetsService.exportAssets(
          assetName,
          assetTagName,
          categoryId,
          locationId,
          statusId,
          purchaseDateFrom,
          purchaseDateTo
      );

      response.putHeader("content-type", "text/csv")
          .putHeader("content-disposition", "attachment; filename=\"assets.csv\"")
          .setStatusCode(200)
          .end(csvContent);

    } catch (IllegalArgumentException e) {
      LOGGER.warn("Validation error in ExportAssetsHandler", e);
      ResponseUtil.createResponse(
        response,
        ResponseType.ERROR,
        StatusCode.BAD_REQUEST,
        new JsonObject().put("error", e.getMessage()),
        new JsonArray()
      );
    } catch (Exception e) {
      LOGGER.error("Error in ExportAssetsHandler", e);
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
