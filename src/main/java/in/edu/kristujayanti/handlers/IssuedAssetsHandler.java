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

public class IssuedAssetsHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER =
    LoggerFactory.getLogger(IssuedAssetsHandler.class);

  private final AssetsService assetsService;

  public IssuedAssetsHandler(AssetsService assetsService) {
    this.assetsService = assetsService;
  }

  @Override
  public void handle(RoutingContext routingContext) {

    HttpServerResponse response =
      routingContext.response();

    try {

      LOGGER.info("Handling request for IssuedAssetsHandler");

      int page = Integer.parseInt(
        routingContext.request()
          .getParam("page") != null
          ? routingContext.request().getParam("page")
          : "1"
      );

      int pageSize = Integer.parseInt(
        routingContext.request()
          .getParam("pageSize") != null
          ? routingContext.request().getParam("pageSize")
          : "10"
      );
      String assetName = routingContext.request().getParam("assetName");
      String category = routingContext.request().getParam("category");
      String issuedTo = routingContext.request().getParam("issuedTo");
      String type = routingContext.request().getParam("type");
      String issueDate = routingContext.request().getParam("issueDate");

      page = Math.max(page, 1);
      pageSize = Math.max(1, Math.min(pageSize, 100));

      PaginatedResult<JsonObject> result =
        assetsService.getIssuedAssetsDetailed(
          page,
          pageSize,
          assetName,
          category,
          issuedTo,
          type,
          issueDate
        );

      JsonArray assets = new JsonArray();

      result.getData().forEach(assets::add);

      JsonObject responseData = new JsonObject()
        .put("assets", assets)
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

    } catch (Exception e) {

      LOGGER.error(
        "Error in IssuedAssetsHandler",
        e
      );

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
