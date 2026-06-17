package in.edu.kristujayanti.handlers;

import in.edu.kristujayanti.enums.ResponseType;
import in.edu.kristujayanti.enums.StatusCode;
import in.edu.kristujayanti.services.AssetsService;
import in.edu.kristujayanti.util.PaginatedResult;
import in.edu.kristujayanti.util.ResponseUtil;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupedReportsHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LoggerFactory.getLogger(GroupedReportsHandler.class);

  private final AssetsService assetsService;

  public GroupedReportsHandler(AssetsService assetsService) {
    this.assetsService = assetsService;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    try {
      LOGGER.info("Handling request for GroupedReportsHandler");

      String categoryId = routingContext.request().getParam("categoryId");
      
      int page = Integer.parseInt(
        routingContext.request().getParam("page") != null
          ? routingContext.request().getParam("page")
          : "1"
      );

      int pageSize = Integer.parseInt(
        routingContext.request().getParam("pageSize") != null
          ? routingContext.request().getParam("pageSize")
          : "8"
      );

      PaginatedResult<JsonObject> result = assetsService.getGroupedReports(categoryId, page, pageSize);

      JsonArray reports = new JsonArray();
      for (JsonObject report : result.getData()) {
        reports.add(report);
      }

      JsonObject responseData = new JsonObject()
          .put("reports", reports)
          .put("categoryId", categoryId != null ? categoryId : "")
          .put("totalRecords", result.getTotalRecords())
          .put("currentPage", result.getCurrentPage())
          .put("pageSize", result.getPageSize())
          .put("totalPages", result.getTotalPages());

      ResponseUtil.createResponse(
        response,
        ResponseType.SUCCESS,
        StatusCode.TWOHUNDRED,
        responseData,
        new JsonArray()
      );

    } catch (IllegalArgumentException e) {
      LOGGER.warn("Validation error in GroupedReportsHandler", e);
      ResponseUtil.createResponse(
        response,
        ResponseType.ERROR,
        StatusCode.BAD_REQUEST,
        new JsonObject().put("error", e.getMessage()),
        new JsonArray()
      );
    } catch (Exception e) {
      LOGGER.error("Error in GroupedReportsHandler", e);
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
