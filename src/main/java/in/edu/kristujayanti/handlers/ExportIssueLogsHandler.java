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

public class ExportIssueLogsHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExportIssueLogsHandler.class);

  private final AssetsService assetsService;

  public ExportIssueLogsHandler(AssetsService assetsService) {
    this.assetsService = assetsService;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    try {
      LOGGER.info("Handling request for ExportIssueLogsHandler");
      String assetName = routingContext.request().getParam("assetName");
      String category = routingContext.request().getParam("category");
      String issuedTo = routingContext.request().getParam("issuedTo");
      String type = routingContext.request().getParam("type");
      String issueDate = routingContext.request().getParam("issueDate");

      String csvContent = assetsService.exportIssueLogs(
          assetName,
          category,
          issuedTo,
          type,
          issueDate
      );

      response.putHeader("content-type", "text/csv")
          .putHeader("content-disposition", "attachment; filename=\"issue_logs.csv\"")
          .setStatusCode(200)
          .end(csvContent);

    } catch (IllegalArgumentException e) {
      LOGGER.warn("Validation error in ExportIssueLogsHandler", e);
      ResponseUtil.createResponse(
        response,
        ResponseType.ERROR,
        StatusCode.BAD_REQUEST,
        new JsonObject().put("error", e.getMessage()),
        new JsonArray()
      );
    } catch (Exception e) {
      LOGGER.error("Error in ExportIssueLogsHandler", e);
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
