package in.edu.kristujayanti.constants;

/**
 * MicroserviceRoutingURLNames interface defines constants for various
 * routing URL paths used in the application.
 * These constants can be used throughout the application to ensure consistency
 * and avoid hardcoding URL paths.
 */
public interface MicroserviceRoutingURLNames {
    // Wildcard URL path for general API routing
    String API_WILDCARD_URL = "*";

    String HEALTH_URL = "/health";

    String ASSETS_URL = "/assets";
    String ASSETS_SEARCH = "/assets-search";
    String CATEGORIES = "/categories";
    String RETURN = "/return-logs";
    String ASSETSTATUSSUMMARY = "/asset-status-summary";
    String ISSUEDASSETS = "/issued-assets";
    String ISSUEASSET = "/issue-asset";
    String RETURN_ASSET = "/return-asset";
    String GRP = "/grp";
    String STATUS = "/status";
    String LOCATIONSLIST = "/locations-list";
    String STATUSESLIST = "/statuses-list";

    String CREATE_ASSET = "/create-asset";
    String CREATE_ASSET_TAG = "/create-asset-tag";
    String EDIT_ASSET = "/edit-asset";
    String ASSET_DETAILS = "/asset-details";
    String ASSET_TAGS_LIST = "/asset-tags-list";
    String CATEGORIES_LIST = "/categories-list";
    String GET_LICENSES = "/get-licenses/:assetId";
    String GET_ASSET_COMPONENTS = "/get-asset-components/:assetId";
    String GENERATE_DISPLAY_ID = "/generate-display-id";
    String CREATE_LICENSES = "/create-licenses";
    String UNISSUED_ASSET_NAMES = "/unissued-asset-names";
    String GET_ASSET_HISTORY = "/asset-history/:assetId";
    String EXPORT_REPORTS = "/export-reports";
    String REPORTS_GROUPED = "/reports-grouped";
    String EXPORT_ASSETS = "/export-assets";
    String EXPORT_ISSUE_LOGS = "/export-issue-logs";
    String EXPORT_RETURN_LOGS = "/export-return-logs";
}

