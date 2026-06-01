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

public class AssetsHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AssetsHandler.class);

    private final AssetsService assetsService;

    public AssetsHandler(AssetsService assetsService) {
        this.assetsService = assetsService;
    }

    @Override
    public void handle(RoutingContext routingContext) {

        HttpServerResponse response =
                routingContext.response();

        try {

            LOGGER.info("Handling request for AssetsHandler");

            String path =
                    routingContext.normalizedPath();

            JsonArray result;

            // CATEGORY COUNT API
            if (path.contains("category-count")) {

                result =
                        assetsService.getAssetCountGroupedByCategory();
            }

            // ISSUED ASSETS DETAILED API
            else if (path.contains("issued-assets")) {

                result =
                        assetsService.getIssuedAssetsDetailed();
            }

            // NORMAL ASSETS API
            else {

                result =
                        assetsService.getAssets();
            }

            ResponseUtil.createResponse(
                    response,
                    ResponseType.SUCCESS,
                    StatusCode.TWOHUNDRED,
                    new JsonObject().put("assets", result),
                    new JsonArray()
            );

        } catch (Exception e) {

            LOGGER.error("Error in AssetsHandler", e);

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