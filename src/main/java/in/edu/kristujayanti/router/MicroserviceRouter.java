package in.edu.kristujayanti.router;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import in.edu.kristujayanti.constants.CommonKeys;
import in.edu.kristujayanti.constants.ContextRoutingURLName;
import in.edu.kristujayanti.constants.MicroserviceRoutingURLNames;
import in.edu.kristujayanti.handlers.*;
import in.edu.kristujayanti.services.AssetsService;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.redis.client.Redis;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * MicroserviceRouter sets up the main application routes and handlers.
 * It extends the RouterBase to utilize common properties and methods for
 * routing.
 */
public class MicroserviceRouter extends RouterBase {

  /**
   * Constructs a ReportOrchestratorRouter with necessary dependencies.
   *
   * @param router                 the Vert.x router
   * @param redisCommandConnection the Redis connection
   * @param mongoDatabase          the MongoDB database
   * @param mongoClient            the MongoDB client
   * @param client                 the Vert.x WebClient
   */
  public MicroserviceRouter(Router router, Redis redisCommandConnection, MongoDatabase mongoDatabase,
      MongoClient mongoClient, WebClient client, JsonObject apiInfo, Vertx vertx) {
    super(router, redisCommandConnection, mongoDatabase, mongoClient, client, apiInfo, vertx);
  }

  /**
   * Sets up the application routes with handlers.
   */
  public void setUpRouters() {
    // Define allowed headers for CORS
    Set<String> allowHeaders = Stream.of(
        CommonKeys.CONTENT_TYPE,
        CommonKeys.X_AUTH_CORRELATION_ID,
        HttpHeaders.AUTHORIZATION.toString(),
        "Access-Control-Allow-Origin").collect(Collectors.toSet());

    // Define allowed HTTP methods for CORS
    Set<HttpMethod> allowMethods = Stream.of(
        HttpMethod.GET,
        HttpMethod.POST,
        HttpMethod.PUT).collect(Collectors.toSet());

    // Setup CORS and Body Handlers
    this.router.route().handler(CorsHandler.create()
        .addOrigin("*")
        .allowCredentials(true)
        .allowedHeaders(allowHeaders)
        .allowedMethods(allowMethods));

    // add routes here
    // health route
    // HealthService healthService = new HealthService(this.mongoDatabase);

    AssetsService assetsService = new AssetsService(this.mongoDatabase);

    // normal assets API
    addRoute(
        HttpMethod.GET,
        MicroserviceRoutingURLNames.ASSETS_URL,
        new AssetsHandler(assetsService));

    // search assets API
    addRoute(
        HttpMethod.GET,
        MicroserviceRoutingURLNames.ASSETS_SEARCH,
        new AssetsSearchHandler(assetsService));

    // category count API
    addRoute(
        HttpMethod.GET,
        MicroserviceRoutingURLNames.CATEGORIES,
        new GetCategoryCountHandler(assetsService));

    // issued assets detailed API
    addRoute(
        HttpMethod.GET,
        MicroserviceRoutingURLNames.ISSUEDASSETS,
        new IssuedAssetsHandler(assetsService));
    addRoute(
        HttpMethod.POST,
        MicroserviceRoutingURLNames.ISSUEASSET,
        new IssueAssetCreateHandler(assetsService));
    addRoute(
        HttpMethod.POST,
        MicroserviceRoutingURLNames.RETURN_ASSET,
        new ReturnAssetCreateHandler(assetsService));
    addRoute(
        HttpMethod.GET,
        MicroserviceRoutingURLNames.ASSETSTATUSSUMMARY,
        new GetAssetsStatusSummary(assetsService));
    // return logs API
    addRoute(
        HttpMethod.GET,
        MicroserviceRoutingURLNames.RETURN,
        new ReturnLogHandler(assetsService));
    addRoute(
        HttpMethod.GET,
        MicroserviceRoutingURLNames.GRP,
        new AssetGroupingHandler(assetsService));
    addRoute(
        HttpMethod.GET,
        MicroserviceRoutingURLNames.STATUS,
        new GetStatusCountHandler(assetsService));
    addRoute(
        HttpMethod.GET,
        MicroserviceRoutingURLNames.LOCATIONSLIST,
        new GetLocationsListHandler(assetsService));
    addRoute(
        HttpMethod.GET,
        MicroserviceRoutingURLNames.STATUSESLIST,
        new GetStatusesListHandler(assetsService));

    // New CRUD APIs for assets and tags
    addRoute(
        HttpMethod.POST,
        MicroserviceRoutingURLNames.CREATE_ASSET,
        new CreateAssetHandler(assetsService));
    addRoute(
        HttpMethod.POST,
        MicroserviceRoutingURLNames.CREATE_ASSET_TAG,
        new CreateAssetTagHandler(assetsService));
    addRoute(
        HttpMethod.PUT,
        MicroserviceRoutingURLNames.EDIT_ASSET,
        new EditAssetHandler(assetsService));
    addRoute(
        HttpMethod.GET,
        MicroserviceRoutingURLNames.ASSET_DETAILS,
        new GetAssetDetailsHandler(assetsService));
    addRoute(
        HttpMethod.GET,
        MicroserviceRoutingURLNames.ASSET_TAGS_LIST,
        new GetAssetTagsListHandler(assetsService));
    addRoute(
        HttpMethod.GET,
        MicroserviceRoutingURLNames.CATEGORIES_LIST,
        new GetCategoriesListHandler(assetsService));
    addRoute(
        HttpMethod.GET,
        MicroserviceRoutingURLNames.UNISSUED_ASSET_NAMES,
        new GetUnissuedAssetNamesHandler(assetsService));
    addRoute(
        HttpMethod.GET,
        MicroserviceRoutingURLNames.GET_LICENSES,
        new GetLicensesHandler(assetsService));
    addRoute(
        HttpMethod.GET,
        MicroserviceRoutingURLNames.GET_ASSET_COMPONENTS,
        new GetAssetComponentsHandler(assetsService));

    addRoute(
        HttpMethod.POST,
        MicroserviceRoutingURLNames.GENERATE_DISPLAY_ID,
        new DisplayIdHandler(assetsService));

    addRoute(
        HttpMethod.POST,
        MicroserviceRoutingURLNames.CREATE_LICENSES,
        new CreateLicensesHandler(assetsService));
  }

  /**
   * Helper method to add routes with the specified method, path, and handler.
   *
   * @param method  the HTTP method
   * @param path    the URL path
   * @param handler the request handler
   */
  private void addRoute(HttpMethod method, String path, Handler<RoutingContext> handler) {
    this.router.route(method, ContextRoutingURLName.MICROSERVICE_CONTEXT_URL_NAME.concat(path))
        .handler(BodyHandler.create())
        .blockingHandler(handler);
  }
}
