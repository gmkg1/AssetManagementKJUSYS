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

public class CreateLicensesHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(CreateLicensesHandler.class);

  private final AssetsService assetsService;

  public CreateLicensesHandler(AssetsService assetsService) {
    this.assetsService = assetsService;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();

    try {
      LOGGER.info("Handling request for CreateLicensesHandler");

      JsonObject payload = routingContext.body().asJsonObject();
      JsonObject result = assetsService.saveLicensesAndWarranty(payload);

      ResponseUtil.createResponse(
          response,
          ResponseType.SUCCESS,
          StatusCode.TWOHUNDRED,
          result,
          new JsonArray()
      );
    } catch (IllegalArgumentException e) {
      LOGGER.warn("Validation error in CreateLicensesHandler", e);
      ResponseUtil.createResponse(
          response,
          ResponseType.ERROR,
          StatusCode.BAD_REQUEST,
          new JsonObject().put("error", e.getMessage()),
          new JsonArray()
      );
    } catch (Exception e) {
      LOGGER.error("Error in CreateLicensesHandler", e);
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
