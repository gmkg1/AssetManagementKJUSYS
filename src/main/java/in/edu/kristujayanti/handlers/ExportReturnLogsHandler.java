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

public class ExportReturnLogsHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExportReturnLogsHandler.class);

  private final AssetsService assetsService;

  public ExportReturnLogsHandler(AssetsService assetsService) {
    this.assetsService = assetsService;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    try {
      LOGGER.info("Handling request for ExportReturnLogsHandler");
      String name = routingContext.request().getParam("name");
      String classification = routingContext.request().getParam("classification");
      String total = routingContext.request().getParam("total");
      String returnType = routingContext.request().getParam("returnType");
      String returnTo = routingContext.request().getParam("returnTo");
      String returnDate = routingContext.request().getParam("returnDate");

      String csvContent = assetsService.exportReturnLogs(
          name,
          classification,
          total,
          returnType,
          returnTo,
          returnDate
      );

      response.putHeader("content-type", "text/csv")
          .putHeader("content-disposition", "attachment; filename=\"return_logs.csv\"")
          .setStatusCode(200)
          .end(csvContent);

    } catch (IllegalArgumentException e) {
      LOGGER.warn("Validation error in ExportReturnLogsHandler", e);
      ResponseUtil.createResponse(
        response,
        ResponseType.ERROR,
        StatusCode.BAD_REQUEST,
        new JsonObject().put("error", e.getMessage()),
        new JsonArray()
      );
    } catch (Exception e) {
      LOGGER.error("Error in ExportReturnLogsHandler", e);
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
