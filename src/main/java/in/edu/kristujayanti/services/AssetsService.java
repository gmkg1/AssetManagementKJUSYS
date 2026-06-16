package in.edu.kristujayanti.services;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Arrays;
import java.util.Date;
import java.util.ArrayList;
import java.util.regex.Pattern;
import in.edu.kristujayanti.util.PaginatedResult;

public class AssetsService {

  private static final Logger LOGGER = LoggerFactory.getLogger(AssetsService.class);
  private static final String ASSETS_COLLECTION = "assets";

  private final MongoDatabase mongoDatabase;

  private Document createFacetStage(int page, int pageSize) {

    int skip = (page - 1) * pageSize;

    return new Document("$facet",
        new Document("data",
            List.of(
                new Document("$skip", skip),
                new Document("$limit", pageSize)))
            .append("metadata",
                List.of(
                    new Document("$count", "totalRecords"))));
  }

  private long getTotalRecords(Document facetResult) {

    List<Document> metadata = facetResult.getList("metadata", Document.class);

    if (metadata == null || metadata.isEmpty()) {
      return 0;
    }

    Number count = (Number) metadata.get(0).get("totalRecords");

    return count.longValue();
  }

  public AssetsService(MongoDatabase mongoDatabase) {
    this.mongoDatabase = mongoDatabase;
    try {
      mongoDatabase.getCollection("issueto").createIndex(
          new Document("assetId", 1),
          new com.mongodb.client.model.IndexOptions()
              .unique(true)
              .partialFilterExpression(new Document("returnStatus", false)));
    } catch (Exception e) {
      LOGGER.warn("Failed to create partial unique index on issueto", e);
    }
  }

  public JsonObject createIssueAsset(JsonObject payload) {
    LOGGER.info("Creating issued asset record");

    if (payload == null) {
      throw new IllegalArgumentException("Request body is required");
    }

    String assetId = payload.getString("assetId");
    String issueDate = payload.getString("issueDate");
    String locationId = payload.getString("locationId");
    String personId = payload.getString("personId");
    String issuedToAssetId = payload.getString("issuedToAssetId");

    if (assetId == null || assetId.isBlank()) {
      throw new IllegalArgumentException("assetId is required");
    }

    boolean hasLocation = locationId != null && !locationId.isBlank();
    boolean hasAsset = issuedToAssetId != null && !issuedToAssetId.isBlank();

    if (hasLocation && hasAsset) {
      throw new IllegalArgumentException("Cannot assign to both a location and an asset");
    }

    // Prevent self-assignment
    if (hasAsset && assetId.trim().equalsIgnoreCase(issuedToAssetId.trim())) {
      throw new IllegalArgumentException("Self-assignment is not allowed");
    }

    if ((locationId == null || locationId.isBlank())
        && (personId == null || personId.isBlank())
        && (issuedToAssetId == null || issuedToAssetId.isBlank())) {
      throw new IllegalArgumentException("One of locationId, personId, or issuedToAssetId is required");
    }

    MongoCollection<Document> collection = mongoDatabase.getCollection("issueto");

    // 1. Rule validation: No asset should have multiple active outgoing assignments
    Document activeQuery = new Document("assetId", new ObjectId(assetId.trim()))
        .append("returnStatus", false);
    if (collection.find(activeQuery).first() != null) {
      throw new IllegalArgumentException("Asset is already actively assigned and must be returned first");
    }

    // 2. Rule validation: Depth-agnostic circular assignment loop prevention
    if (hasAsset) {
      ObjectId sourceObjectId = new ObjectId(assetId.trim());
      ObjectId current = new ObjectId(issuedToAssetId.trim());
      while (current != null) {
        if (current.equals(sourceObjectId)) {
          throw new IllegalArgumentException("Circular assignment loop detected");
        }
        Document parentAssoc = collection.find(new Document("assetId", current).append("returnStatus", false)).first();
        if (parentAssoc != null) {
          current = parentAssoc.getObjectId("issuedToAssetId");
        } else {
          current = null;
        }
      }
    }

    Document issueDocument = new Document("assetId", new ObjectId(assetId.trim()))
        .append("issueDate", parseIssueDate(issueDate))
        .append("locationId", toObjectIdOrNull(locationId))
        .append("personId", toObjectIdOrNull(personId))
        .append("issuedToAssetId", toObjectIdOrNull(issuedToAssetId))
        .append("returnStatus", false)
        .append("returnDate", null);

    collection.insertOne(issueDocument);

    return new JsonObject()
        .put("message", "Asset issued successfully")
        .put("issueId", objectIdToString(issueDocument.getObjectId("_id")))
        .put("assetId", assetId.trim())
        .put("issueDate", dateToString(issueDocument.getDate("issueDate")))
        .put("locationId", objectIdToString(issueDocument.getObjectId("locationId")))
        .put("personId", objectIdToString(issueDocument.getObjectId("personId")))
        .put("issuedToAssetId", objectIdToString(issueDocument.getObjectId("issuedToAssetId")));
  }

  public PaginatedResult<JsonObject> getAssets(
      String assetName,
      String assetTagName,
      String categoryId,
      String locationId,
      String statusId,
      String purchaseDateFrom,
      String purchaseDateTo,
      int page,
      int pageSize) {

    LOGGER.info("Fetching detailed assets page={} pageSize={}",
        page,
        pageSize);

    MongoCollection<Document> collection = mongoDatabase.getCollection(ASSETS_COLLECTION);

    int skip = (page - 1) * pageSize;

    List<Document> pipeline = new ArrayList<>();

    // =========================
    // JOIN ASSET TAG
    // =========================
    pipeline.add(
        new Document("$lookup",
            new Document("from", "assettags")
                .append("localField", "assetTagId")
                .append("foreignField", "_id")
                .append("as", "assetTag")));

    pipeline.add(
        new Document("$unwind", "$assetTag"));

    // =========================
    // JOIN CATEGORY
    // =========================
    pipeline.add(
        new Document("$lookup",
            new Document("from", "categories")
                .append("localField", "assetTag.categoryId")
                .append("foreignField", "_id")
                .append("as", "category")));

    pipeline.add(
        new Document("$unwind", "$category"));

    // =========================
    // JOIN CAMPUS
    // =========================
    pipeline.add(
        new Document("$lookup",
            new Document("from", "campuses")
                .append("localField", "campusId")
                .append("foreignField", "_id")
                .append("as", "campus")));

    pipeline.add(
        new Document("$unwind", "$campus"));

    // =========================
    // JOIN STATUS
    // =========================
    pipeline.add(
        new Document("$lookup",
            new Document("from", "status")
                .append("localField", "statusId")
                .append("foreignField", "_id")
                .append("as", "status")));

    pipeline.add(
        new Document("$unwind", "$status"));

    // =========================
    // JOIN LOCATION
    // =========================
    pipeline.add(
        new Document("$lookup",
            new Document("from", "locations")
                .append("localField", "locationId")
                .append("foreignField", "_id")
                .append("as", "location")));

    pipeline.add(
        new Document("$unwind", "$location"));

    // =========================
    // JOIN ISSUE TO
    // =========================
    pipeline.add(
        new Document("$lookup",
            new Document("from", "issueto")
                .append("localField", "_id")
                .append("foreignField", "assetId")
                .append("as", "issueInfo")));

    pipeline.add(
        new Document("$unwind",
            new Document("path", "$issueInfo")
                .append("preserveNullAndEmptyArrays", true)));

    // =========================
    // JOIN ISSUED LOCATION
    // =========================
    pipeline.add(
        new Document("$lookup",
            new Document("from", "locations")
                .append("localField", "issueInfo.locationId")
                .append("foreignField", "_id")
                .append("as", "issuedLocation")));

    pipeline.add(
        new Document("$unwind",
            new Document("path", "$issuedLocation")
                .append("preserveNullAndEmptyArrays", true)));

    // =========================
    // JOIN ISSUED ASSET
    // =========================
    pipeline.add(
        new Document("$lookup",
            new Document("from", "assets")
                .append("localField",
                    "issueInfo.issuedToAssetId")
                .append("foreignField", "_id")
                .append("as", "issuedAsset"))

    );

    pipeline.add(
        new Document("$unwind",
            new Document("path", "$issuedAsset")
                .append("preserveNullAndEmptyArrays", true))

    );

    List<Document> matchConditions = new ArrayList<>();

    if (assetName != null && !assetName.isBlank()) {
      matchConditions.add(new Document("assetName",
          new Document("$regex", Pattern.quote(assetName.trim()))
              .append("$options", "i")));
    }

    if (assetTagName != null && !assetTagName.isBlank()) {
      matchConditions.add(new Document("assetTag.assetTagName",
          new Document("$regex", Pattern.quote(assetTagName.trim()))
              .append("$options", "i")));
    }

    if (categoryId != null && !categoryId.isBlank()) {
      matchConditions.add(new Document("category._id", new ObjectId(categoryId)));
    }

    if (locationId != null && !locationId.isBlank()) {
      matchConditions.add(new Document("location._id", new ObjectId(locationId)));
    }

    if (statusId != null && !statusId.isBlank()) {
      matchConditions.add(new Document("status._id", new ObjectId(statusId)));
    }

    if ((purchaseDateFrom != null && !purchaseDateFrom.isBlank())
        || (purchaseDateTo != null && !purchaseDateTo.isBlank())) {
      Document purchaseDateMatch = new Document();
      if (purchaseDateFrom != null && !purchaseDateFrom.isBlank()) {
        purchaseDateMatch.append("$gte",
            java.util.Date.from(
                java.time.LocalDate.parse(purchaseDateFrom.trim())
                    .atStartOfDay(java.time.ZoneId.systemDefault())
                    .toInstant()));
      }
      if (purchaseDateTo != null && !purchaseDateTo.isBlank()) {
        purchaseDateMatch.append("$lte",
            java.util.Date.from(
                java.time.LocalDate.parse(purchaseDateTo.trim())
                    .plusDays(1)
                    .atStartOfDay(java.time.ZoneId.systemDefault())
                    .minusNanos(1)
                    .toInstant()));
      }
      matchConditions.add(new Document("purchaseDate", purchaseDateMatch));
    }

    if (!matchConditions.isEmpty()) {
      pipeline.add(new Document("$match",
          matchConditions.size() == 1
              ? matchConditions.get(0)
              : new Document("$and", matchConditions)));
    }

    // =========================
    // FACET
    // =========================
    pipeline.add(
        new Document("$facet",
            new Document("data",
                List.of(
                    new Document("$skip", skip),
                    new Document("$limit", pageSize)))
                .append("metadata",
                    List.of(
                        new Document("$count",
                            "totalRecords")))));

    Document facetResult = collection.aggregate(pipeline).first();

    List<JsonObject> result = new ArrayList<>();

    long totalRecords = 0;

    if (facetResult != null) {

      List<Document> metadata = facetResult.getList(
          "metadata",
          Document.class);

      if (metadata != null && !metadata.isEmpty()) {

        Number count = (Number) metadata.get(0)
            .get("totalRecords");

        totalRecords = count.longValue();
      }

      List<Document> data = facetResult.getList(
          "data",
          Document.class);

      if (data != null) {

        for (Document doc : data) {

          JsonObject json = new JsonObject();

          // =========================
          // BASIC DETAILS
          // =========================
          json.put("assetName",
              doc.getString("assetName"));

          json.put("model",
              ((Document) doc.get("assetTag"))
                  .getString("assetTagName"));

          json.put("category",
              ((Document) doc.get("category"))
                  .getString("categoryName"));

          json.put("status",
              ((Document) doc.get("status"))
                  .getString("statusName"));

          json.put("issuable",
              doc.getBoolean("isIssuable"));

          // =========================
          // LOCATION
          // =========================
          Document locationDoc = (Document) doc.get("location");

          if (locationDoc != null) {

            json.put("location",
                locationDoc.getString(
                    "locationName"));

          } else {

            json.put("location",
                "N/A");
          }

          // =========================
          // BLOCK NAME
          // =========================
          String blockId = doc.getString("blockId");

          Document campusDoc = (Document) doc.get("campus");

          String blockName = "N/A";

          if (campusDoc != null) {

            List<Document> blocks = (List<Document>) campusDoc.get(
                "blocks");

            if (blocks != null) {

              for (Document block : blocks) {

                if (blockId != null &&
                    blockId.equals(
                        block.getString(
                            "blockId"))) {

                  blockName = block.getString(
                      "blockName");

                  break;
                }
              }
            }
          }

          json.put("block",
              blockName);

          // =========================
          // ISSUED TO
          // =========================
          String issuedTo = "Not Issued";

          Document issueInfo = (Document) doc.get(
              "issueInfo");

          if (issueInfo != null) {

            ObjectId personId = issueInfo.getObjectId(
                "personId");

            Document issuedLocation = (Document) doc.get(
                "issuedLocation");

            Document issuedAsset = (Document) doc.get(
                "issuedAsset");

            if (issuedLocation != null) {

              issuedTo = issuedLocation.getString(
                  "locationName");

            } else if (issuedAsset != null) {

              issuedTo = issuedAsset.getString(
                  "assetName");

            } else if (personId != null) {

              issuedTo = personId.toHexString();
            }
          }

          json.put("issuedTo",
              issuedTo);

          result.add(json);
        }
      }
    }

    return new PaginatedResult<>(
        result,
        totalRecords,
        page,
        pageSize);
  }

  public JsonArray getAssetCountGroupedByCategory() {

    LOGGER.info("Fetching asset count grouped by category");

    JsonArray result = new JsonArray();

    MongoCollection<Document> collection = mongoDatabase.getCollection(ASSETS_COLLECTION);

    List<Document> pipeline = List.of(

        // JOIN assettags
        new Document("$lookup",
            new Document("from", "assettags")
                .append("localField", "assetTagId")
                .append("foreignField", "_id")
                .append("as", "assetTag")),

        // Convert array → object
        new Document("$unwind", "$assetTag"),

        // JOIN categories
        new Document("$lookup",
            new Document("from", "categories")
                .append("localField", "assetTag.categoryId")
                .append("foreignField", "_id")
                .append("as", "category")),

        // Convert array → object
        new Document("$unwind", "$category"),

        // GROUP by category
        new Document("$group",
            new Document("_id", "$category._id")
                .append("categoryName",
                    new Document("$first",
                        "$category.categoryName"))

                // Count documents
                .append("assetCount",
                    new Document("$sum", "$quantity"))));

    for (Document doc : collection.aggregate(pipeline)) {

      JsonObject json = new JsonObject()
          .put("categoryId",
              objectIdToString(doc.getObjectId("_id")))
          .put("categoryName",
              doc.getString("categoryName"))
          .put("assetCount",
              doc.getInteger("assetCount"));

      result.add(json);
    }

    return result;
  }

  public JsonArray getAssetCountGroupedByStatus() {

    LOGGER.info("Fetching asset count grouped by status");

    JsonArray result = new JsonArray();

    MongoCollection<Document> collection = mongoDatabase.getCollection(ASSETS_COLLECTION);

    List<Document> pipeline = List.of(

        // JOIN statuses
        new Document("$lookup",
            new Document("from", "status")
                .append("localField", "statusId")
                .append("foreignField", "_id")
                .append("as", "status")),

        // Convert array -> object
        new Document("$unwind", "$status"),

        // GROUP by status
        new Document("$group",
            new Document("_id", "$status._id")
                .append("statusName",
                    new Document("$first",
                        "$status.statusName"))
                .append("assetCount",
                    new Document("$sum", "$quantity"))));

    for (Document doc : collection.aggregate(pipeline)) {

      JsonObject json = new JsonObject()
          .put("statusId",
              objectIdToString(doc.getObjectId("_id")))
          .put("statusName",
              doc.getString("statusName"))
          .put("assetCount",
              doc.getInteger("assetCount"));

      result.add(json);
    }
    return result;
  }

  public PaginatedResult<JsonObject> getIssuedAssetsDetailed(
      int page,
      int pageSize,
      String assetName,
      String category,
      String issuedTo,
      String type,
      String issueDate) {

    LOGGER.info(
        "Fetching issued assets detailed data page={} pageSize={}",
        page,
        pageSize);

    int skip = (page - 1) * pageSize;

    MongoCollection<Document> issueCollection = mongoDatabase.getCollection("issueto");

    List<Document> pipeline = new ArrayList<>();

    pipeline.add(new Document("$lookup",
        new Document("from", "assets")
            .append("localField", "assetId")
            .append("foreignField", "_id")
            .append("as", "asset")));

    pipeline.add(new Document("$unwind",
        new Document("path", "$asset")
            .append("preserveNullAndEmptyArrays", true)));

    pipeline.add(new Document("$lookup",
        new Document("from", "assettags")
            .append("localField", "asset.assetTagId")
            .append("foreignField", "_id")
            .append("as", "assetTag")));

    pipeline.add(new Document("$unwind",
        new Document("path", "$assetTag")
            .append("preserveNullAndEmptyArrays", true)));

    pipeline.add(new Document("$lookup",
        new Document("from", "categories")
            .append("localField", "assetTag.categoryId")
            .append("foreignField", "_id")
            .append("as", "categoryDoc")));

    pipeline.add(new Document("$unwind",
        new Document("path", "$categoryDoc")
            .append("preserveNullAndEmptyArrays", true)));

    pipeline.add(new Document("$lookup",
        new Document("from", "locations")
            .append("localField", "locationId")
            .append("foreignField", "_id")
            .append("as", "location")));

    pipeline.add(new Document("$unwind",
        new Document("path", "$location")
            .append("preserveNullAndEmptyArrays", true)));

    pipeline.add(new Document("$lookup",
        new Document("from", "assets")
            .append("localField", "issuedToAssetId")
            .append("foreignField", "_id")
            .append("as", "issuedAsset")));

    pipeline.add(new Document("$unwind",
        new Document("path", "$issuedAsset")
            .append("preserveNullAndEmptyArrays", true)));

    pipeline.add(new Document("$addFields",
        new Document("assetName",
            new Document("$ifNull", Arrays.asList("$asset.assetName", "")))
            .append("assetCategory",
                new Document("$ifNull", Arrays.asList("$categoryDoc.categoryName", "")))
            .append("receiverType",
                new Document("$switch",
                    new Document("branches", Arrays.asList(
                        new Document("case", new Document("$ne", Arrays.asList("$locationId", null)))
                            .append("then", "Location"),
                        new Document("case", new Document("$ne", Arrays.asList("$issuedToAssetId", null)))
                            .append("then", "Asset"),
                        new Document("case", new Document("$ne", Arrays.asList("$personId", null)))
                            .append("then", "Person")))
                        .append("default", "Unknown")))
            .append("receiverName",
                new Document("$switch",
                    new Document("branches", Arrays.asList(
                        new Document("case", new Document("$ne", Arrays.asList("$locationId", null)))
                            .append("then", new Document("$ifNull", Arrays.asList("$location.locationName", ""))),
                        new Document("case", new Document("$ne", Arrays.asList("$issuedToAssetId", null)))
                            .append("then", new Document("$ifNull", Arrays.asList("$issuedAsset.assetName", ""))),
                        new Document("case", new Document("$ne", Arrays.asList("$personId", null)))
                            .append("then", new Document("$toString", "$personId"))))
                        .append("default", "")))));

    List<Document> matchConditions = new ArrayList<>();

    if (assetName != null && !assetName.isBlank()) {
      matchConditions.add(new Document("assetName",
          new Document("$regex", Pattern.quote(assetName.trim()))
              .append("$options", "i")));
    }

    if (category != null && !category.isBlank()) {
      matchConditions.add(new Document("assetCategory",
          new Document("$regex", Pattern.quote(category.trim()))
              .append("$options", "i")));
    }

    if (issuedTo != null && !issuedTo.isBlank()) {
      matchConditions.add(new Document("receiverName",
          new Document("$regex", Pattern.quote(issuedTo.trim()))
              .append("$options", "i")));
    }

    if (type != null && !type.isBlank()) {
      matchConditions.add(new Document("receiverType", type.trim()));
    }

    if (issueDate != null && !issueDate.isBlank()) {
      Date parsedDate = java.util.Date.from(
          java.time.LocalDate.parse(issueDate.trim())
              .atStartOfDay(java.time.ZoneId.systemDefault())
              .toInstant());
      Date start = parsedDate;
      Date end = java.util.Date.from(
          java.time.LocalDate.parse(issueDate.trim())
              .plusDays(1)
              .atStartOfDay(java.time.ZoneId.systemDefault())
              .minusNanos(1)
              .toInstant());
      matchConditions.add(new Document("issueDate",
          new Document("$gte", start).append("$lte", end)));
    }

    if (!matchConditions.isEmpty()) {
      pipeline.add(new Document("$match",
          matchConditions.size() == 1
              ? matchConditions.get(0)
              : new Document("$and", matchConditions)));
    }

    pipeline.add(new Document("$facet",
        new Document("data",
            List.of(
                new Document("$skip", skip),
                new Document("$limit", pageSize)))
            .append("metadata",
                List.of(
                    new Document("$count", "totalRecords")))));

    Document facetResult = issueCollection.aggregate(pipeline).first();

    long totalRecords = 0;

    List<Document> issuedDocs = new ArrayList<>();

    if (facetResult != null) {
      List<Document> metadata = facetResult.getList("metadata", Document.class);
      if (metadata != null && !metadata.isEmpty()) {
        Number count = (Number) metadata.get(0).get("totalRecords");
        totalRecords = count.longValue();
      }
      List<Document> data = facetResult.getList("data", Document.class);
      if (data != null) {
        issuedDocs = data;
      }
    }

    List<JsonObject> result = new ArrayList<>();

    for (Document issueDoc : issuedDocs) {

      JsonObject json = new JsonObject();

      Document assetDoc = (Document) issueDoc.get("asset");
      Document categoryDoc = (Document) issueDoc.get("categoryDoc");
      Document locationDoc = (Document) issueDoc.get("location");
      Document issuedAssetDoc = (Document) issueDoc.get("issuedAsset");

      if (assetDoc != null) {
        json.put("assetName", assetDoc.getString("assetName"));
      }

      if (categoryDoc != null) {
        json.put("assetCategory", categoryDoc.getString("categoryName"));
      }

      json.put(
          "issueDate",
          dateToString(
              issueDoc.getDate("issueDate")));

      if (locationDoc != null) {
        json.put("receiverName", locationDoc.getString("locationName"));
        json.put("receiverType", "Location");
      } else if (issuedAssetDoc != null) {
        json.put("receiverName", issuedAssetDoc.getString("assetName"));
        json.put("receiverType", "Asset");
      } else if (issueDoc.get("personId") != null) {
        json.put("receiverName", objectIdToString(issueDoc.getObjectId("personId")));
        json.put("receiverType", "Person");
      }

      json.put("_id", objectIdToString(issueDoc.getObjectId("_id")));
      json.put("assetId", objectIdToString(issueDoc.getObjectId("assetId")));
      json.put("locationId", objectIdToString(issueDoc.getObjectId("locationId")));
      json.put("personId", objectIdToString(issueDoc.getObjectId("personId")));
      json.put("issuedToAssetId", objectIdToString(issueDoc.getObjectId("issuedToAssetId")));

      result.add(json);
    }

    return new PaginatedResult<>(
        result,
        totalRecords,
        page,
        pageSize);
  }

  public PaginatedResult<JsonObject> getAssetStatusSummary(
      int page,
      int pageSize) {

    LOGGER.info(
        "Fetching asset status summary page={} pageSize={}",
        page,
        pageSize);

    int skip = (page - 1) * pageSize;

    MongoCollection<Document> collection = mongoDatabase.getCollection("assets");

    List<Document> pipeline = new ArrayList<>();

    pipeline.add(new Document("$lookup",
        new Document("from", "assettags")
            .append("localField", "assetTagId")
            .append("foreignField", "_id")
            .append("as", "assetTag")));

    pipeline.add(new Document("$unwind", "$assetTag"));

    pipeline.add(new Document("$lookup",
        new Document("from", "categories")
            .append("localField", "assetTag.categoryId")
            .append("foreignField", "_id")
            .append("as", "category")));

    pipeline.add(new Document("$unwind", "$category"));

    pipeline.add(new Document("$lookup",
        new Document("from", "status")
            .append("localField", "statusId")
            .append("foreignField", "_id")
            .append("as", "status")));

    pipeline.add(new Document("$unwind", "$status"));

    pipeline.add(new Document("$group",
        new Document("_id",
            new Document("assetTagId", "$assetTag._id")
                .append("assetTagName",
                    "$assetTag.assetTagName")
                .append("categoryName",
                    "$category.categoryName"))

            .append("totalAssets",
                new Document("$sum", "$quantity"))

            .append("ready",
                new Document("$sum",
                    new Document("$cond",
                        List.of(
                            new Document("$eq",
                                List.of(
                                    "$status.statusName",
                                    "Ready to Deploy")),
                            "$quantity",
                            0))))

            .append("deployed",
                new Document("$sum",
                    new Document("$cond",
                        List.of(
                            new Document("$eq",
                                List.of(
                                    "$status.statusName",
                                    "Deployed")),
                            "$quantity",
                            0))))

            .append("deadStock",
                new Document("$sum",
                    new Document("$cond",
                        List.of(
                            new Document("$eq",
                                List.of(
                                    "$status.statusName",
                                    "Dead Stock")),
                            "$quantity",
                            0))))

            .append("underMaintenance",
                new Document("$sum",
                    new Document("$cond",
                        List.of(
                            new Document("$eq",
                                List.of(
                                    "$status.statusName",
                                    "Under Maintenance")),
                            "$quantity",
                            0))))

            .append("damaged",
                new Document("$sum",
                    new Document("$cond",
                        List.of(
                            new Document("$eq",
                                List.of(
                                    "$status.statusName",
                                    "Damaged")),
                            "$quantity",
                            0))))));

    pipeline.add(
        new Document("$facet",
            new Document("metadata",
                List.of(
                    new Document("$count",
                        "totalRecords")))
                .append("data",
                    List.of(
                        new Document("$skip", skip),
                        new Document("$limit",
                            pageSize)))));

    Document facetResult = collection.aggregate(pipeline)
        .first();

    List<JsonObject> result = new ArrayList<>();

    long totalRecords = 0;

    if (facetResult != null) {

      List<Document> metadata = (List<Document>) facetResult.get(
          "metadata");

      if (!metadata.isEmpty()) {

        totalRecords = metadata.get(0)
            .getInteger(
                "totalRecords",
                0);
      }

      List<Document> data = (List<Document>) facetResult.get(
          "data");

      for (Document doc : data) {

        Document id = (Document) doc.get("_id");

        result.add(
            new JsonObject()
                .put(
                    "assetTagName",
                    id.getString(
                        "assetTagName"))
                .put(
                    "category",
                    id.getString(
                        "categoryName"))
                .put(
                    "totalAssets",
                    doc.getInteger(
                        "totalAssets",
                        0))
                .put(
                    "ready",
                    doc.getInteger(
                        "ready",
                        0))
                .put(
                    "deployed",
                    doc.getInteger(
                        "deployed",
                        0))
                .put(
                    "deadStock",
                    doc.getInteger(
                        "deadStock",
                        0))
                .put(
                    "underMaintenance",
                    doc.getInteger(
                        "underMaintenance",
                        0))
                .put(
                    "damaged",
                    doc.getInteger(
                        "damaged",
                        0)));
      }
    }

    return new PaginatedResult<>(
        result,
        totalRecords,
        page,
        pageSize);
  }

  public PaginatedResult<JsonObject> getAssetsByCategory(
      String categoryId,
      String assetName,
      int page,
      int pageSize) {

    LOGGER.info(
        "Fetching assets for category={} page={} pageSize={}",
        categoryId,
        page,
        pageSize);

    int skip = (page - 1) * pageSize;

    MongoCollection<Document> collection = mongoDatabase.getCollection("assets");

    List<Document> pipeline = new ArrayList<>();

    pipeline.add(new Document("$lookup",
        new Document("from", "assettags")
            .append("localField", "assetTagId")
            .append("foreignField", "_id")
            .append("as", "assetTag")));

    pipeline.add(new Document("$unwind", "$assetTag"));

    pipeline.add(new Document("$lookup",
        new Document("from", "categories")
            .append("localField", "assetTag.categoryId")
            .append("foreignField", "_id")
            .append("as", "category")));

    pipeline.add(new Document("$unwind", "$category"));

    pipeline.add(new Document("$match",
        new Document("category._id",
            new ObjectId(categoryId))));

    pipeline.add(new Document("$lookup",
        new Document("from", "status")
            .append("localField", "statusId")
            .append("foreignField", "_id")
            .append("as", "status")));

    pipeline.add(new Document("$unwind", "$status"));

    pipeline.add(new Document("$lookup",
        new Document("from", "locations")
            .append("localField", "locationId")
            .append("foreignField", "_id")
            .append("as", "location")));

    pipeline.add(new Document("$unwind",
        new Document("path", "$location")
            .append("preserveNullAndEmptyArrays", true)));

    pipeline.add(new Document("$project",
        new Document("assetName", 1)
            .append("assetSerialNumber", 1)
            .append("purchaseDate", 1)
            .append("purchaseCost", 1)
            .append("quantity", 1)
            .append("isIssuable", 1)
            .append("assetTagName",
                "$assetTag.assetTagName")
            .append("categoryName",
                "$category.categoryName")
            .append("statusName",
                "$status.statusName")
            .append("locationName",
                "$location.locationName")));

    pipeline.add(
        new Document("$facet",
            new Document("metadata",
                List.of(
                    new Document("$count",
                        "totalRecords")))
                .append("data",
                    List.of(
                        new Document("$skip", skip),
                        new Document("$limit", pageSize)))));

    Document facetResult = collection.aggregate(pipeline)
        .first();

    List<JsonObject> result = new ArrayList<>();

    long totalRecords = 0;

    if (facetResult != null) {

      List<Document> metadata = (List<Document>) facetResult.get("metadata");

      if (!metadata.isEmpty()) {

        totalRecords = metadata.get(0)
            .getInteger(
                "totalRecords",
                0);
      }

      List<Document> data = (List<Document>) facetResult.get("data");

      for (Document doc : data) {

        result.add(
            new JsonObject()
                .put("_id",
                    objectIdToString(
                        doc.getObjectId("_id")))
                .put("assetName",
                    doc.getString("assetName"))
                .put("assetSerialNumber",
                    doc.getString("assetSerialNumber"))
                .put("assetTagName",
                    doc.getString("assetTagName"))
                .put("category",
                    doc.getString("categoryName"))
                .put("status",
                    doc.getString("statusName"))
                .put("location",
                    doc.getString("locationName"))
                .put("purchaseDate",
                    dateToString(
                        doc.getDate("purchaseDate")))
                .put("purchaseCost",
                    doc.getInteger("purchaseCost"))
                .put("quantity",
                    doc.getInteger("quantity"))
                .put("isIssuable",
                    doc.getBoolean("isIssuable")));
      }
    }

    return new PaginatedResult<>(
        result,
        totalRecords,
        page,
        pageSize);
  }

  public JsonArray getDistinctLocations() {
    JsonArray result = new JsonArray();
    MongoCollection<Document> collection = mongoDatabase.getCollection("locations");
    for (Document doc : collection.find()) {
      result.add(new JsonObject()
          .put("locationId", objectIdToString(doc.getObjectId("_id")))
          .put("locationName", doc.getString("locationName")));
    }
    return result;
  }

  public JsonArray getDistinctStatuses() {
    JsonArray result = new JsonArray();
    MongoCollection<Document> collection = mongoDatabase.getCollection("status");
    for (Document doc : collection.find()) {
      result.add(new JsonObject()
          .put("statusId", objectIdToString(doc.getObjectId("_id")))
          .put("statusName", doc.getString("statusName")));
    }
    return result;
  }

  // Facet - skip , limit and then paginated Result
  public PaginatedResult<JsonObject> getReturnLogs(
      int page,
      int pageSize,
      String name,
      String classification,
      String total,
      String returnType,
      String returnTo,
      String returnDate) {

    LOGGER.info(
        "Fetching return logs page={} pageSize={}",
        page,
        pageSize);

    int skip = (page - 1) * pageSize;

    MongoCollection<Document> returnCollection = mongoDatabase.getCollection("returnto");

    List<Document> pipeline = new ArrayList<>();

    pipeline.add(new Document("$lookup",
        new Document("from", "assets")
            .append("localField", "assetId")
            .append("foreignField", "_id")
            .append("as", "asset")));

    pipeline.add(new Document("$unwind",
        new Document("path", "$asset")
            .append("preserveNullAndEmptyArrays", true)));

    pipeline.add(new Document("$lookup",
        new Document("from", "assettags")
            .append("localField", "asset.assetTagId")
            .append("foreignField", "_id")
            .append("as", "assetTag")));

    pipeline.add(new Document("$unwind",
        new Document("path", "$assetTag")
            .append("preserveNullAndEmptyArrays", true)));

    pipeline.add(new Document("$lookup",
        new Document("from", "categories")
            .append("localField", "assetTag.categoryId")
            .append("foreignField", "_id")
            .append("as", "category")));

    pipeline.add(new Document("$unwind",
        new Document("path", "$category")
            .append("preserveNullAndEmptyArrays", true)));

    pipeline.add(new Document("$lookup",
        new Document("from", "locations")
            .append("localField", "locationId")
            .append("foreignField", "_id")
            .append("as", "location")));

    pipeline.add(new Document("$unwind",
        new Document("path", "$location")
            .append("preserveNullAndEmptyArrays", true)));

    pipeline.add(new Document("$lookup",
        new Document("from", "assets")
            .append("localField", "returnedToAssetId")
            .append("foreignField", "_id")
            .append("as", "returnedAsset")));

    pipeline.add(new Document("$unwind",
        new Document("path", "$returnedAsset")
            .append("preserveNullAndEmptyArrays", true)));

    pipeline.add(new Document("$addFields",
        new Document("name", new Document("$ifNull", Arrays.asList("$asset.assetName", "")))
            .append("classification", new Document("$ifNull", Arrays.asList("$category.categoryName", "")))
            .append("total", new Document("$ifNull", Arrays.asList("$asset.quantity", 0)))
            .append("returnType", new Document("$switch",
                new Document("branches", Arrays.asList(
                    new Document("case", new Document("$ne", Arrays.asList("$locationId", null))).append("then",
                        "Location"),
                    new Document("case", new Document("$ne", Arrays.asList("$returnedToAssetId", null))).append("then",
                        "Asset"),
                    new Document("case", new Document("$ne", Arrays.asList("$personId", null))).append("then",
                        "Person")))
                    .append("default", "Unknown")))
            .append("returnTo", new Document("$switch",
                new Document("branches", Arrays.asList(
                    new Document("case", new Document("$ne", Arrays.asList("$locationId", null)))
                        .append("then", new Document("$ifNull", Arrays.asList("$location.locationName", ""))),
                    new Document("case", new Document("$ne", Arrays.asList("$returnedToAssetId", null)))
                        .append("then", new Document("$ifNull", Arrays.asList("$returnedAsset.assetName", ""))),
                    new Document("case", new Document("$ne", Arrays.asList("$personId", null)))
                        .append("then", new Document("$toString", "$personId"))))
                    .append("default", "")))
            .append("returnDateText", new Document("$dateToString",
                new Document("format", "%Y-%m-%d")
                    .append("date", "$returnDate")))));

    List<Document> matchConditions = new ArrayList<>();

    if (name != null && !name.isBlank()) {
      matchConditions.add(new Document("name",
          new Document("$regex", Pattern.quote(name.trim()))
              .append("$options", "i")));
    }
    if (classification != null && !classification.isBlank()) {
      matchConditions.add(new Document("classification",
          new Document("$regex", Pattern.quote(classification.trim()))
              .append("$options", "i")));
    }
    if (total != null && !total.isBlank()) {
      try {
        matchConditions.add(new Document("total", Integer.parseInt(total.trim())));
      } catch (NumberFormatException ignored) {
      }
    }
    if (returnType != null && !returnType.isBlank()) {
      matchConditions.add(new Document("returnType", returnType.trim()));
    }
    if (returnTo != null && !returnTo.isBlank()) {
      matchConditions.add(new Document("returnTo",
          new Document("$regex", Pattern.quote(returnTo.trim()))
              .append("$options", "i")));
    }
    if (returnDate != null && !returnDate.isBlank()) {
      matchConditions.add(new Document("returnDateText",
          new Document("$regex", Pattern.quote(returnDate.trim()))
              .append("$options", "i")));
    }
    if (!matchConditions.isEmpty()) {
      pipeline.add(new Document("$match",
          matchConditions.size() == 1 ? matchConditions.get(0) : new Document("$and", matchConditions)));
    }

    pipeline.add(
        new Document("$facet",
            new Document("metadata",
                List.of(
                    new Document("$count",
                        "totalRecords")))
                .append("data",
                    List.of(
                        new Document("$skip", skip),
                        new Document("$limit", pageSize)))));

    Document facetResult = returnCollection.aggregate(pipeline)
        .first();

    long totalRecords = 0;

    List<JsonObject> result = new ArrayList<>();

    if (facetResult != null) {

      List<Document> metadata = (List<Document>) facetResult.get("metadata");

      if (!metadata.isEmpty()) {

        totalRecords = metadata.get(0)
            .getInteger("totalRecords", 0);
      }

      List<Document> data = (List<Document>) facetResult.get("data");

      for (Document doc : data) {

        result.add(
            new JsonObject()
                .put("name", doc.getString("name"))
                .put("classification", doc.getString("classification"))
                .put("total", doc.getInteger("total", 0))
                .put("returnType", doc.getString("returnType"))
                .put("returnTo", doc.getString("returnTo"))
                .put("returnDate", doc.getDate("returnDate") != null
                    ? dateToString(doc.getDate("returnDate"))
                    : null));
      }
    }

    return new PaginatedResult<>(
        result,
        totalRecords,
        page,
        pageSize);
  }

  public JsonArray searchAssets(String query) {
    LOGGER.info("Searching assets with query: {}", query);
    JsonArray result = new JsonArray();
    MongoCollection<Document> collection = mongoDatabase.getCollection(ASSETS_COLLECTION);

    List<Document> pipeline = new ArrayList<>();

    List<Document> matchConditions = new ArrayList<>();
    if (query != null && !query.isBlank()) {
      matchConditions.add(new Document("assetName",
          new Document("$regex", Pattern.quote(query.trim()))
              .append("$options", "i")));
    }

    if (!matchConditions.isEmpty()) {
      pipeline.add(new Document("$match",
          matchConditions.size() == 1
              ? matchConditions.get(0)
              : new Document("$and", matchConditions)));
    }

    // Join assettags to retrieve the tag/model label
    pipeline.add(new Document("$lookup",
        new Document("from", "assettags")
            .append("localField", "assetTagId")
            .append("foreignField", "_id")
            .append("as", "assetTag")));

    pipeline.add(new Document("$unwind",
        new Document("path", "$assetTag")
            .append("preserveNullAndEmptyArrays", true)));

    pipeline.add(new Document("$limit", 20));

    for (Document doc : collection.aggregate(pipeline)) {
      Document assetTag = (Document) doc.get("assetTag");
      String tagLabel = assetTag != null ? assetTag.getString("assetTagName") : "";
      result.add(new JsonObject()
          .put("_id", objectIdToString(doc.getObjectId("_id")))
          .put("assetName", doc.getString("assetName"))
          .put("assetTagName", tagLabel)
          .put("isIssuable", doc.getBoolean("isIssuable")));
    }
    return result;
  }

  private JsonObject toJson(Document asset) {
    return new JsonObject()
        .put("_id", objectIdToString(asset.getObjectId("_id")))
        .put("purchaseCost", asset.getInteger("purchaseCost"))
        .put("purchaseDate", dateToString(asset.getDate("purchaseDate")))
        .put("assetSerialNumber", asset.getString("assetSerialNumber"))
        .put("isIssuable", asset.getBoolean("isIssuable"))
        .put("assetName", asset.getString("assetName"))
        .put("assetTagId", objectIdToString(asset.getObjectId("assetTagId")))
        .put("quantity", asset.getInteger("quantity"))
        .put("unitOfMeasureId", objectIdToString(asset.getObjectId("unitOfMeasureId")))
        .put("campusId", objectIdToString(asset.getObjectId("campusId")))
        .put("blockId", asset.getString("blockId"))
        .put("statusId", objectIdToString(asset.getObjectId("statusId")))
        .put("locationId", objectIdToString(asset.getObjectId("locationId")));
  }

  private String objectIdToString(ObjectId objectId) {
    return objectId == null ? null : objectId.toHexString();
  }

  private String dateToString(Date date) {
    return date == null ? null : date.toInstant().toString();
  }

  private ObjectId toObjectIdOrNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return new ObjectId(value.trim());
  }

  private Date parseIssueDate(String value) {
    if (value == null || value.isBlank()) {
      return new Date();
    }
    return Date.from(java.time.LocalDate.parse(value.trim())
        .atStartOfDay(java.time.ZoneId.systemDefault())
        .toInstant());
  }

  public JsonObject createAsset(JsonObject payload) {
    LOGGER.info("Creating new asset record");
    if (payload == null) {
      throw new IllegalArgumentException("Request body is required");
    }

    String assetName = payload.getString("assetName");
    String assetTagId = payload.getString("assetTagId");
    String statusId = payload.getString("statusId");
    String defaultLocation = payload.getString("defaultLocation");
    String serial = payload.getString("serial");
    String purchaseCost = payload.getString("purchaseCost");
    String purchaseDate = payload.getString("purchaseDate");
    boolean isReturnable = payload.getBoolean("isReturnable", true);

    if (assetName == null || assetName.isBlank()) {
      throw new IllegalArgumentException("assetName is required");
    }
    if (assetTagId == null || assetTagId.isBlank()) {
      throw new IllegalArgumentException("assetTagId is required");
    }
    if (statusId == null || statusId.isBlank()) {
      throw new IllegalArgumentException("statusId is required");
    }

    ObjectId campusId = null;
    try {
      MongoCollection<Document> campusColl = mongoDatabase.getCollection("campuses");
      Document firstCampus = campusColl.find().first();
      if (firstCampus != null) {
        campusId = firstCampus.getObjectId("_id");
      } else {
        campusId = new ObjectId("6a101a50df52662f6e441530");
      }
    } catch (Exception e) {
      campusId = new ObjectId("6a101a50df52662f6e441530");
    }

    ObjectId unitOfMeasureId = new ObjectId("6a1533d11fcf30c131441533");

    int costVal = 0;
    if (purchaseCost != null && !purchaseCost.isBlank()) {
      try {
        costVal = Integer.parseInt(purchaseCost.trim());
      } catch (NumberFormatException ignored) {
      }
    }

    Date dateVal = new Date();
    if (purchaseDate != null && !purchaseDate.isBlank()) {
      try {
        dateVal = Date.from(java.time.LocalDate.parse(purchaseDate.trim())
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant());
      } catch (Exception ignored) {
      }
    }

    Document assetDocument = new Document("assetName", assetName.trim())
        .append("assetTagId", new ObjectId(assetTagId.trim()))
        .append("statusId", new ObjectId(statusId.trim()))
        .append("locationId", toObjectIdOrNull(defaultLocation))
        .append("assetSerialNumber", serial != null ? serial.trim() : "")
        .append("purchaseCost", costVal)
        .append("purchaseDate", dateVal)
        .append("isIssuable", isReturnable)
        .append("quantity", 1)
        .append("campusId", campusId)
        .append("blockId", "B002")
        .append("unitOfMeasureId", unitOfMeasureId);

    MongoCollection<Document> collection = mongoDatabase.getCollection(ASSETS_COLLECTION);
    collection.insertOne(assetDocument);

    return new JsonObject()
        .put("message", "Asset created successfully")
        .put("_id", objectIdToString(assetDocument.getObjectId("_id")));
  }

  public JsonObject createAssetTag(JsonObject payload) {
    LOGGER.info("Creating new asset tag");
    if (payload == null) {
      throw new IllegalArgumentException("Request body is required");
    }

    String assetTagName = payload.getString("assetTagName");
    if (assetTagName == null || assetTagName.isBlank()) {
      assetTagName = payload.getString("assetTypeName");
    }

    String categoryId = payload.getString("category");

    if (assetTagName == null || assetTagName.isBlank()) {
      throw new IllegalArgumentException("assetTagName is required");
    }
    if (categoryId == null || categoryId.isBlank()) {
      throw new IllegalArgumentException("category (categoryId) is required");
    }

    ObjectId classificationId = null;
    try {
      MongoCollection<Document> classColl = mongoDatabase.getCollection("classifications");
      Document firstClass = classColl.find().first();
      if (firstClass != null) {
        classificationId = firstClass.getObjectId("_id");
      } else {
        classificationId = new ObjectId("6a155f01b2f64aac2344152e");
      }
    } catch (Exception e) {
      classificationId = new ObjectId("6a155f01b2f64aac2344152e");
    }

    Document tagDocument = new Document("assetTagName", assetTagName.trim())
        .append("categoryId", new ObjectId(categoryId.trim()))
        .append("classificationId", classificationId);

    MongoCollection<Document> collection = mongoDatabase.getCollection("assettags");
    collection.insertOne(tagDocument);

    return new JsonObject()
        .put("message", "Asset tag created successfully")
        .put("_id", objectIdToString(tagDocument.getObjectId("_id")));
  }

  public JsonObject editAsset(JsonObject payload) {
    LOGGER.info("Editing asset record");
    if (payload == null) {
      throw new IllegalArgumentException("Request body is required");
    }

    String id = payload.getString("_id");
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("_id is required");
    }

    String assetName = payload.getString("assetName");
    String assetTagId = payload.getString("assetTagId");
    String statusId = payload.getString("statusId");
    String defaultLocation = payload.getString("defaultLocation");
    String serial = payload.getString("serial");
    String purchaseCost = payload.getString("purchaseCost");
    String purchaseDate = payload.getString("purchaseDate");
    boolean isReturnable = payload.getBoolean("isReturnable", true);

    Document updateFields = new Document();
    if (assetName != null)
      updateFields.append("assetName", assetName.trim());
    if (assetTagId != null)
      updateFields.append("assetTagId", new ObjectId(assetTagId.trim()));
    if (statusId != null)
      updateFields.append("statusId", new ObjectId(statusId.trim()));
    if (defaultLocation != null)
      updateFields.append("locationId", toObjectIdOrNull(defaultLocation));
    if (serial != null)
      updateFields.append("assetSerialNumber", serial.trim());
    updateFields.append("isIssuable", isReturnable);

    if (purchaseCost != null) {
      int costVal = 0;
      try {
        costVal = Integer.parseInt(purchaseCost.trim());
      } catch (NumberFormatException ignored) {
      }
      updateFields.append("purchaseCost", costVal);
    }

    if (purchaseDate != null) {
      Date dateVal = new Date();
      try {
        dateVal = Date.from(java.time.LocalDate.parse(purchaseDate.trim())
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant());
      } catch (Exception ignored) {
      }
      updateFields.append("purchaseDate", dateVal);
    }

    MongoCollection<Document> collection = mongoDatabase.getCollection(ASSETS_COLLECTION);
    collection.updateOne(new Document("_id", new ObjectId(id.trim())), new Document("$set", updateFields));

    return new JsonObject().put("message", "Asset updated successfully");
  }

  public JsonObject getAssetById(String id) {
    LOGGER.info("Fetching asset details for id: {}", id);
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("id is required");
    }

    MongoCollection<Document> collection = mongoDatabase.getCollection(ASSETS_COLLECTION);
    Document doc = collection.find(new Document("_id", new ObjectId(id.trim()))).first();
    if (doc == null) {
      return null;
    }
    return toJson(doc);
  }

  public JsonArray getDistinctAssetTags() {
    LOGGER.info("Fetching all asset tags");
    JsonArray result = new JsonArray();
    MongoCollection<Document> collection = mongoDatabase.getCollection("assettags");
    for (Document doc : collection.find()) {
      result.add(new JsonObject()
          .put("id", objectIdToString(doc.getObjectId("_id")))
          .put("assetTagName", doc.getString("assetTagName")));
    }
    return result;
  }

  public JsonObject createReturnAsset(JsonObject payload) {
    LOGGER.info("Creating returned asset record");

    if (payload == null) {
      throw new IllegalArgumentException("Request body is required");
    }

    String assetId = payload.getString("assetId");
    String issuetoId = payload.getString("issuetoId");
    String returnDate = payload.getString("returnDate");
    String locationId = payload.getString("locationId");
    String personId = payload.getString("personId");
    String returnedToAssetId = payload.getString("returnedToAssetId");
    String notes = payload.getString("notes");

    if (assetId == null || assetId.isBlank()) {
      throw new IllegalArgumentException("assetId is required");
    }
    if (issuetoId == null || issuetoId.isBlank()) {
      throw new IllegalArgumentException("issuetoId is required");
    }

    if (!ObjectId.isValid(issuetoId.trim())) {
      throw new IllegalArgumentException("Invalid issuetoId");
    }

    MongoCollection<Document> issuetoColl = mongoDatabase.getCollection("issueto");
    ObjectId issueObjectId = new ObjectId(issuetoId.trim());

    Document activeIssue = issuetoColl.find(new Document("_id", issueObjectId).append("returnStatus", false)).first();
    if (activeIssue == null) {
      throw new IllegalArgumentException("No active assignment record found with the given issuetoId");
    }

    Date parsedReturnDate = parseIssueDate(returnDate);

    // Update active status fields in the target issueto record
    issuetoColl.updateOne(
        new Document("_id", issueObjectId),
        new Document("$set", new Document("returnStatus", true)
            .append("returnDate", parsedReturnDate)));

    Document returnDocument = new Document("assetId", new ObjectId(assetId.trim()))
        .append("issuetoId", issueObjectId)
        .append("returnDate", parsedReturnDate)
        .append("locationId", toObjectIdOrNull(locationId))
        .append("personId", toObjectIdOrNull(personId))
        .append("returnedToAssetId", toObjectIdOrNull(returnedToAssetId));

    if (notes != null) {
      returnDocument.append("notes", notes.trim());
    }

    MongoCollection<Document> collection = mongoDatabase.getCollection("returnto");

    collection.insertOne(returnDocument);

    return new JsonObject()
        .put("message", "Asset returned successfully")
        .put("returnId", objectIdToString(returnDocument.getObjectId("_id")))
        .put("assetId", assetId.trim())
        .put("issuetoId", issuetoId.trim())
        .put("returnDate", dateToString(returnDocument.getDate("returnDate")))
        .put("locationId", objectIdToString(returnDocument.getObjectId("locationId")))
        .put("personId", objectIdToString(returnDocument.getObjectId("personId")))
        .put("returnedToAssetId", objectIdToString(returnDocument.getObjectId("returnedToAssetId")));
  }

  public JsonArray getDistinctCategories() {
    LOGGER.info("Fetching all categories");
    JsonArray result = new JsonArray();
    MongoCollection<Document> collection = mongoDatabase.getCollection("categories");
    for (Document doc : collection.find()) {
      result.add(new JsonObject()
          .put("categoryId", objectIdToString(doc.getObjectId("_id")))
          .put("categoryName", doc.getString("categoryName")));
    }
    return result;
  }

  public JsonObject getLicensesAndWarrantyByAssetId(String assetId) {
    LOGGER.info("Fetching licenses and warranty for assetId: {}", assetId);
    if (assetId == null || assetId.isBlank()) {
      throw new IllegalArgumentException("assetId is required");
    }

    if (!ObjectId.isValid(assetId.trim())) {
      throw new IllegalArgumentException("Invalid assetId");
    }

    ObjectId assetObjectId = new ObjectId(assetId.trim());

    JsonArray licensesArray = new JsonArray();
    MongoCollection<Document> licensesCollection = mongoDatabase.getCollection("licenses");
    for (Document doc : licensesCollection.find(new Document("assetId", assetObjectId))) {
      licensesArray.add(new JsonObject()
          .put("licenseName", doc.getString("licenseName"))
          .put("licenseKey", doc.getString("licenseKey"))
          .put("expiryDate", formatDateOnly(doc.getDate("expiryDate"))));
    }

    JsonArray warrantyArray = new JsonArray();
    MongoCollection<Document> warrantyCollection = mongoDatabase.getCollection("warranty");
    for (Document doc : warrantyCollection.find(new Document("assetId", assetObjectId))) {
      Boolean activeStatus = doc.getBoolean("ActiveStatus");
      String status = (activeStatus != null && activeStatus) ? "Active" : "Inactive";

      warrantyArray.add(new JsonObject()
          .put("provider", doc.getString("provider"))
          .put("referenceId", doc.getString("referenceId"))
          .put("startDate", formatDateOnly(doc.getDate("startDate")))
          .put("endDate", formatDateOnly(doc.getDate("endDate")))
          .put("status", status));
    }

    return new JsonObject()
        .put("licenses", licensesArray)
        .put("warranty", warrantyArray);
  }

    /**
     * Retrieves component assets assigned to the given parent asset.
     * For each issueto record where issuedToAssetId equals the selected assetId and returnStatus is false,
     * it fetches the child asset details from the assets collection.
     *
     * @param assetId the parent asset ID (as a string)
     * @return JsonObject containing a "components" JsonArray
     */
    public JsonObject getAssetComponents(String assetId) {
        LOGGER.info("Fetching components for assetId: {}", assetId);
        if (assetId == null || assetId.isBlank()) {
            throw new IllegalArgumentException("assetId is required");
        }
        if (!ObjectId.isValid(assetId.trim())) {
            throw new IllegalArgumentException("Invalid assetId");
        }
        ObjectId parentId = new ObjectId(assetId.trim());
        MongoCollection<Document> issuetoColl = mongoDatabase.getCollection("issueto");
        Document filter = new Document("issuedToAssetId", parentId).append("returnStatus", false);
        JsonArray components = new JsonArray();
        for (Document doc : issuetoColl.find(filter)) {
            ObjectId childId = doc.getObjectId("assetId");
            if (childId != null) {
                MongoCollection<Document> assetsColl = mongoDatabase.getCollection("assets");
                Document childAsset = assetsColl.find(new Document("_id", childId)).first();
                if (childAsset != null) {
                    JsonObject comp = new JsonObject()
                        .put("assetId", childAsset.getObjectId("_id").toHexString())
                        .put("assetName", childAsset.getString("assetName"));
                    components.add(comp);
                }
            }
        }
        return new JsonObject().put("components", components);
    }

  private String formatDateOnly(Date date) {
    if (date == null) {
      return null;
    }
    return date.toInstant()
        .atZone(java.time.ZoneOffset.UTC)
        .toLocalDate()
        .toString();
  }

  /**
   * Generates a display ID for an Asset based on tag and location IDs.
   * Format: {AssetTagDisplayId}-{AssetSequence}-{LocationDisplayId}
   */
  public String generateAssetDisplayId(String assetTagId, String locationId) {
      if (assetTagId == null || assetTagId.isBlank()) {
          throw new IllegalArgumentException("assetTagId is required");
      }
      if (locationId == null || locationId.isBlank()) {
          throw new IllegalArgumentException("locationId is required");
      }

      LOGGER.info("Generating Asset displayId for assetTagId={}, locationId={}", assetTagId, locationId);

      // 1. Fetch assettags document
      MongoCollection<Document> assetTagsColl = mongoDatabase.getCollection("assettags");
      Document assetTag = assetTagsColl.find(new Document("_id", new ObjectId(assetTagId.trim()))).first();
      if (assetTag == null) {
          throw new IllegalArgumentException("Asset tag not found for ID: " + assetTagId);
      }

      // 2. Read assettags.displayId
      String assetTagDisplayId = assetTag.getString("displayId");
      if (assetTagDisplayId == null || assetTagDisplayId.isBlank()) {
          throw new IllegalArgumentException("Asset tag displayId is missing for ID: " + assetTagId);
      }

      // 3. Fetch locations document
      MongoCollection<Document> locationsColl = mongoDatabase.getCollection("locations");
      Document location = locationsColl.find(new Document("_id", new ObjectId(locationId.trim()))).first();
      if (location == null) {
          throw new IllegalArgumentException("Location not found for ID: " + locationId);
      }

      // 4. Read locations.displayId
      String locationDisplayId = location.getString("displayId");
      if (locationDisplayId == null || locationDisplayId.isBlank()) {
          throw new IllegalArgumentException("Location displayId is missing for ID: " + locationId);
      }

      // 5. Generate next AssetSequence (incrementing the counter)
      int nextSeq = getNextSequence(assetTagDisplayId.trim());
      String sequenceStr = String.format("%03d", nextSeq);

      // 6. Construct final Asset displayId
      String finalDisplayId = assetTagDisplayId.trim() + "-" + sequenceStr + "-" + locationDisplayId.trim();

      LOGGER.info("Generated Asset displayId: {}", finalDisplayId);
      return finalDisplayId;
  }

  /**
   * Atomically generates/increments the next sequence value for the sequence key.
   */
  private int getNextSequence(String sequenceKey) {
      MongoCollection<Document> seqColl = mongoDatabase.getCollection("displayid_sequences");
      Document query = new Document("_id", sequenceKey);
      Document update = new Document("$inc", new Document("nextValue", 1));
      com.mongodb.client.model.FindOneAndUpdateOptions options = new com.mongodb.client.model.FindOneAndUpdateOptions()
              .upsert(true)
              .returnDocument(com.mongodb.client.model.ReturnDocument.AFTER);

      Document result = seqColl.findOneAndUpdate(query, update, options);
      if (result == null) {
          throw new RuntimeException("Failed to update sequence for key: " + sequenceKey);
      }
      return result.getInteger("nextValue");
  }

  public JsonObject saveLicensesAndWarranty(JsonObject payload) {
    LOGGER.info("Saving licenses and warranty record");
    if (payload == null) {
      throw new IllegalArgumentException("Request body is required");
    }

    String assetId = payload.getString("assetId");
    validateAssetId(assetId);

    boolean hasLicense = payload.getString("licenseName") != null && !payload.getString("licenseName").isBlank();
    boolean hasWarranty = payload.getString("provider") != null && !payload.getString("provider").isBlank();

    if (!hasLicense && !hasWarranty) {
      throw new IllegalArgumentException("At least licenseName (for license) or provider (for warranty) must be specified");
    }

    JsonObject responseResult = new JsonObject().put("message", "Licenses and warranty details saved successfully");
    ObjectId assetObjectId = new ObjectId(assetId.trim());

    if (hasLicense) {
      String licenseName = payload.getString("licenseName");
      String licenseKey = payload.getString("licenseKey");
      String expiryDateStr = payload.getString("expiryDate");

      if (licenseKey == null || licenseKey.isBlank()) {
        throw new IllegalArgumentException("licenseKey is required when saving license");
      }

      Date expiryDate = (expiryDateStr != null && !expiryDateStr.isBlank()) ? parseLocalDate(expiryDateStr) : null;

      Document licenseDocument = new Document("assetId", assetObjectId)
          .append("licenseName", licenseName.trim())
          .append("licenseKey", licenseKey.trim())
          .append("expiryDate", expiryDate);

      MongoCollection<Document> licensesCollection = mongoDatabase.getCollection("licenses");
      licensesCollection.insertOne(licenseDocument);
      responseResult.put("licenseId", objectIdToString(licenseDocument.getObjectId("_id")));
    }

    if (hasWarranty) {
      String provider = payload.getString("provider");
      String startDateStr = payload.getString("startDate");
      String endDateStr = payload.getString("endDate");
      
      String referenceId = payload.getString("referenceId");
      
      Boolean activeStatus = null;
      if (payload.containsKey("ActiveStatus")) {
        Object activeStatusVal = payload.getValue("ActiveStatus");
        if (activeStatusVal instanceof Boolean) {
          activeStatus = (Boolean) activeStatusVal;
        } else if (activeStatusVal instanceof String) {
          activeStatus = "Active".equalsIgnoreCase(((String) activeStatusVal).trim()) || "true".equalsIgnoreCase(((String) activeStatusVal).trim());
        }
      }

      if (referenceId == null || referenceId.isBlank()) {
        throw new IllegalArgumentException("referenceId is required when saving warranty");
      }
      if (activeStatus == null) {
        throw new IllegalArgumentException("ActiveStatus is required when saving warranty");
      }
      if (startDateStr == null || startDateStr.isBlank()) {
        throw new IllegalArgumentException("startDate is required when saving warranty");
      }
      if (endDateStr == null || endDateStr.isBlank()) {
        throw new IllegalArgumentException("endDate is required when saving warranty");
      }

      Date startDate = parseLocalDate(startDateStr);
      Date endDate = parseLocalDate(endDateStr);

      if (startDate != null && endDate != null && endDate.before(startDate)) {
        throw new IllegalArgumentException("endDate cannot be before startDate");
      }

      Document warrantyDocument = new Document("assetId", assetObjectId)
          .append("provider", provider.trim())
          .append("startDate", startDate)
          .append("endDate", endDate)
          .append("referenceId", referenceId.trim())
          .append("ActiveStatus", activeStatus);

      MongoCollection<Document> warrantyCollection = mongoDatabase.getCollection("warranty");
      warrantyCollection.insertOne(warrantyDocument);
      responseResult.put("warrantyId", objectIdToString(warrantyDocument.getObjectId("_id")));
    }

    return responseResult;
  }

  private void validateAssetId(String assetId) {
    if (assetId == null || assetId.isBlank()) {
      throw new IllegalArgumentException("assetId is required");
    }
    if (!ObjectId.isValid(assetId.trim())) {
      throw new IllegalArgumentException("Invalid assetId");
    }
    MongoCollection<Document> assetsColl = mongoDatabase.getCollection(ASSETS_COLLECTION);
    Document asset = assetsColl.find(new Document("_id", new ObjectId(assetId.trim()))).first();
    if (asset == null) {
      throw new IllegalArgumentException("Asset not found with ID: " + assetId);
    }
  }

  private Date parseLocalDate(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return java.util.Date.from(java.time.LocalDate.parse(value.trim())
          .atStartOfDay(java.time.ZoneId.systemDefault())
          .toInstant());
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid date format: '" + value + "'. Expected format: YYYY-MM-DD");
    }
  }
}
