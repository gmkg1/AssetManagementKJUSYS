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

public class ExportReportsHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExportReportsHandler.class);

  private final AssetsService assetsService;

  public ExportReportsHandler(AssetsService assetsService) {
    this.assetsService = assetsService;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    try {
      LOGGER.info("Handling request for ExportReportsHandler");
      String categoryId = routingContext.request().getParam("categoryId");
      String assetName = routingContext.request().getParam("assetName");
      String assetIds = routingContext.request().getParam("assetIds");

      String csvContent = assetsService.exportReports(categoryId, assetName, assetIds);

      response.putHeader("content-type", "text/csv")
          .putHeader("content-disposition", "attachment; filename=\"report.csv\"")
          .setStatusCode(200)
          .end(csvContent);

    } catch (IllegalArgumentException e) {
      LOGGER.warn("Validation error in ExportReportsHandler", e);
      ResponseUtil.createResponse(
        response,
        ResponseType.ERROR,
        StatusCode.BAD_REQUEST,
        new JsonObject().put("error", e.getMessage()),
        new JsonArray()
      );
    } catch (Exception e) {
      LOGGER.error("Error in ExportReportsHandler", e);
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
