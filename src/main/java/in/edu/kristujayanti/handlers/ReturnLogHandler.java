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

public class ReturnLogHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER =
    LoggerFactory.getLogger(ReturnLogHandler.class);

  private final AssetsService assetsService;

  public ReturnLogHandler(AssetsService assetsService) {
    this.assetsService = assetsService;
  }

  @Override
  public void handle(RoutingContext routingContext) {

    HttpServerResponse response =
      routingContext.response();

    try {

      LOGGER.info("Handling request for ReturnLogHandler");

      int page = Integer.parseInt(
        routingContext.request()
          .getParam("page") == null
          ? "1"
          : routingContext.request().getParam("page")
      );

      int pageSize = Integer.parseInt(
        routingContext.request()
          .getParam("pageSize") == null
          ? "10"
          : routingContext.request().getParam("pageSize")
      );

      PaginatedResult<JsonObject> result =
        assetsService.getReturnLogs(page, pageSize);

      ResponseUtil.createResponse(
        response,
        ResponseType.SUCCESS,
        StatusCode.TWOHUNDRED,
        new JsonObject()
          .put("data", new JsonArray(result.getData()))
          .put("totalRecords", result.getTotalRecords())
          .put("currentPage", result.getCurrentPage())
          .put("pageSize", result.getPageSize())
          .put("totalPages", result.getTotalPages()),
        new JsonArray()
      );

    } catch (Exception e) {

      LOGGER.error("Error in ReturnLogHandler", e);

      ResponseUtil.createResponse(
        response,
        ResponseType.ERROR,
        StatusCode.INTERNAL_SERVER_ERROR,
        new JsonObject()
          .put("error", "Internal Server Error"),
        new JsonArray()
      );
    }
  }
}
