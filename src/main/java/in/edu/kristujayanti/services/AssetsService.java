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
import java.util.Date;
import java.util.ArrayList;
public class AssetsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssetsService.class);
    private static final String ASSETS_COLLECTION = "assets";

    private final MongoDatabase mongoDatabase;

    public AssetsService(MongoDatabase mongoDatabase) {
        this.mongoDatabase = mongoDatabase;
    }

    public JsonArray getAssets() {

        LOGGER.info("Fetching detailed assets");

        JsonArray result = new JsonArray();

        MongoCollection<Document> collection =
                mongoDatabase.getCollection(ASSETS_COLLECTION);

        List<Document> pipeline = List.of(

                // =========================
                // JOIN ASSET TAG
                // =========================
                new Document("$lookup",
                        new Document("from", "assettags")
                                .append("localField", "assetTagId")
                                .append("foreignField", "_id")
                                .append("as", "assetTag")
                ),

                new Document("$unwind", "$assetTag"),

                // =========================
                // JOIN CATEGORY
                // =========================
                new Document("$lookup",
                        new Document("from", "categories")
                                .append("localField", "assetTag.categoryId")
                                .append("foreignField", "_id")
                                .append("as", "category")
                ),

                new Document("$unwind", "$category"),

                // =========================
                // JOIN CAMPUS
                // =========================
                new Document("$lookup",
                        new Document("from", "campuses")
                                .append("localField", "campusId")
                                .append("foreignField", "_id")
                                .append("as", "campus")
                ),

                new Document("$unwind", "$campus"),

                // =========================
                // JOIN STATUS
                // =========================
                new Document("$lookup",
                        new Document("from", "status")
                                .append("localField", "statusId")
                                .append("foreignField", "_id")
                                .append("as", "status")
                ),

                new Document("$unwind", "$status"),

                // =========================
                // JOIN LOCATION
                // =========================
                new Document("$lookup",
                        new Document("from", "locations")
                                .append("localField", "locationId")
                                .append("foreignField", "_id")
                                .append("as", "location")
                ),

                new Document("$unwind", "$location"),

                // =========================
                // JOIN ISSUE TO
                // =========================
                new Document("$lookup",
                        new Document("from", "issueto")
                                .append("localField", "_id")
                                .append("foreignField", "assetId")
                                .append("as", "issueInfo")
                ),

                // Keep even if not issued
                new Document("$unwind",
                        new Document("path", "$issueInfo")
                                .append("preserveNullAndEmptyArrays", true)
                ),

                // =========================
                // JOIN ISSUED LOCATION
                // =========================
                new Document("$lookup",
                        new Document("from", "locations")
                                .append("localField", "issueInfo.locationId")
                                .append("foreignField", "_id")
                                .append("as", "issuedLocation")
                ),

                new Document("$unwind",
                        new Document("path", "$issuedLocation")
                                .append("preserveNullAndEmptyArrays", true)
                ),

                // =========================
                // JOIN ISSUED ASSET
                // =========================
                new Document("$lookup",
                        new Document("from", "assets")
                                .append("localField", "issueInfo.issuedToAssetId")
                                .append("foreignField", "_id")
                                .append("as", "issuedAsset")
                ),

                new Document("$unwind",
                        new Document("path", "$issuedAsset")
                                .append("preserveNullAndEmptyArrays", true)
                )
        );

        for (Document doc : collection.aggregate(pipeline)) {

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
            Document locationDoc =
                    (Document) doc.get("location");

            if (locationDoc != null) {

                json.put("location",
                        locationDoc.getString("locationName"));

            } else {

                json.put("location", "N/A");
            }

            // =========================
            // BLOCK NAME
            // =========================
            String blockId = doc.getString("blockId");

            Document campusDoc =
                    (Document) doc.get("campus");

            String blockName = "N/A";

            if (campusDoc != null) {

                List<Document> blocks =
                        (List<Document>) campusDoc.get("blocks");

                if (blocks != null) {

                    for (Document block : blocks) {

                        if (blockId.equals(block.getString("blockId"))) {

                            blockName = block.getString("blockName");
                            break;
                        }
                    }
                }
            }

            json.put("block", blockName);

            // =========================
            // ISSUED TO
            // =========================
            String issuedTo = "Not Issued";

            Document issueInfo =
                    (Document) doc.get("issueInfo");

            if (issueInfo != null) {

                ObjectId personId =
                        issueInfo.getObjectId("personId");

                Document issuedLocation =
                        (Document) doc.get("issuedLocation");

                Document issuedAsset =
                        (Document) doc.get("issuedAsset");

                if (issuedLocation != null) {

                    issuedTo =
                            issuedLocation.getString("locationName");

                } else if (issuedAsset != null) {

                    issuedTo =
                            issuedAsset.getString("assetName");

                } else if (personId != null) {

                    issuedTo =
                            personId.toHexString();
                }
            }

            json.put("issuedTo", issuedTo);

            result.add(json);
        }

        return result;
    }
    public JsonArray getAssetCountGroupedByCategory() {

        LOGGER.info("Fetching asset count grouped by category");

        JsonArray result = new JsonArray();

        MongoCollection<Document> collection =
                mongoDatabase.getCollection(ASSETS_COLLECTION);

        List<Document> pipeline = List.of(

                // JOIN assettags
                new Document("$lookup",
                        new Document("from", "assettags")
                                .append("localField", "assetTagId")
                                .append("foreignField", "_id")
                                .append("as", "assetTag")
                ),

                // Convert array → object
                new Document("$unwind", "$assetTag"),

                // JOIN categories
                new Document("$lookup",
                        new Document("from", "categories")
                                .append("localField", "assetTag.categoryId")
                                .append("foreignField", "_id")
                                .append("as", "category")
                ),

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
                                        new Document("$sum", "$quantity"))
                )
        );

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

    public JsonArray getIssuedAssetsDetailed() {

        LOGGER.info("Fetching issued assets detailed data");

        JsonArray result = new JsonArray();

        MongoCollection<Document> issueCollection =
                mongoDatabase.getCollection("issueto");

        MongoCollection<Document> assetsCollection =
                mongoDatabase.getCollection("assets");

        MongoCollection<Document> locationsCollection =
                mongoDatabase.getCollection("locations");

        MongoCollection<Document> assetTagsCollection =
                mongoDatabase.getCollection("assettags");

        MongoCollection<Document> categoriesCollection =
                mongoDatabase.getCollection("categories");

        List<Document> issuedDocs =
                issueCollection.find().into(new ArrayList<>());

        for (Document issueDoc : issuedDocs) {

            JsonObject json = new JsonObject();

            ObjectId assetId = issueDoc.getObjectId("assetId");

            // FETCH ASSET
            Document assetDoc = assetsCollection.find(
                    new Document("_id", assetId)
            ).first();

            if (assetDoc != null) {

                json.put("assetName",
                        assetDoc.getString("assetName"));

                // FETCH ASSET TAG
                ObjectId assetTagId =
                        assetDoc.getObjectId("assetTagId");

                Document assetTagDoc = assetTagsCollection.find(
                        new Document("_id", assetTagId)
                ).first();

                if (assetTagDoc != null) {

                    // FETCH CATEGORY
                    ObjectId categoryId =
                            assetTagDoc.getObjectId("categoryId");

                    Document categoryDoc = categoriesCollection.find(
                            new Document("_id", categoryId)
                    ).first();

                    if (categoryDoc != null) {

                        json.put("assetCategory",
                                categoryDoc.getString("categoryName"));
                    }
                }
            }

            // ISSUE DATE
            json.put("issueDate",
                    dateToString(issueDoc.getDate("issueDate")));

            // RECEIVER LOGIC
            ObjectId locationId =
                    issueDoc.getObjectId("locationId");

            ObjectId issuedToAssetId =
                    issueDoc.getObjectId("issuedToAssetId");

            ObjectId personId =
                    issueDoc.getObjectId("personId");

            // IF LOCATION
            if (locationId != null) {

                Document locationDoc = locationsCollection.find(
                        new Document("_id", locationId)
                ).first();

                if (locationDoc != null) {

                    json.put("receiverName",
                            locationDoc.getString("locationName"));

                    json.put("receiverType", "Location");
                }
            }

            // IF ASSET
            else if (issuedToAssetId != null) {

                Document issuedAssetDoc = assetsCollection.find(
                        new Document("_id", issuedToAssetId)
                ).first();

                if (issuedAssetDoc != null) {

                    json.put("receiverName",
                            issuedAssetDoc.getString("assetName"));

                    json.put("receiverType", "Asset");
                }
            }

            // IF PERSON
            else if (personId != null) {

                json.put("receiverName",
                        objectIdToString(personId));

                json.put("receiverType", "Person");
            }

            result.add(json);
        }

        return result;
    }
  public JsonArray getAssetStatusSummary() {

    JsonArray result = new JsonArray();

    MongoCollection<Document> collection =
      mongoDatabase.getCollection("assets");

    List<Document> pipeline = List.of(

      // Join Asset Tags
      new Document("$lookup",
        new Document("from", "assettags")
          .append("localField", "assetTagId")
          .append("foreignField", "_id")
          .append("as", "assetTag")),

      new Document("$unwind", "$assetTag"),

      // Join Categories
      new Document("$lookup",
        new Document("from", "categories")
          .append("localField", "assetTag.categoryId")
          .append("foreignField", "_id")
          .append("as", "category")),

      new Document("$unwind", "$category"),

      // Join Status
      new Document("$lookup",
        new Document("from", "status")
          .append("localField", "statusId")
          .append("foreignField", "_id")
          .append("as", "status")),

      new Document("$unwind", "$status"),

      // Group by Asset Tag
      new Document("$group",
        new Document("_id",
          new Document("assetTagId", "$assetTag._id")
            .append("assetTagName", "$assetTag.assetTagName")
            .append("categoryName", "$category.categoryName"))

          .append("totalAssets",
            new Document("$sum", "$quantity"))

          .append("ready",
            new Document("$sum",
              new Document("$cond",
                List.of(
                  new Document("$eq",
                    List.of("$status.statusName",
                      "Ready to Deploy")),
                  "$quantity",
                  0))))

          .append("deployed",
            new Document("$sum",
              new Document("$cond",
                List.of(
                  new Document("$eq",
                    List.of("$status.statusName",
                      "Deployed")),
                  "$quantity",
                  0))))

          .append("deadStock",
            new Document("$sum",
              new Document("$cond",
                List.of(
                  new Document("$eq",
                    List.of("$status.statusName",
                      "Dead Stock")),
                  "$quantity",
                  0))))

          .append("underMaintenance",
            new Document("$sum",
              new Document("$cond",
                List.of(
                  new Document("$eq",
                    List.of("$status.statusName",
                      "Under Maintenance")),
                  "$quantity",
                  0))))

          .append("damaged",
            new Document("$sum",
              new Document("$cond",
                List.of(
                  new Document("$eq",
                    List.of("$status.statusName",
                      "Damaged")),
                  "$quantity",
                  0))))
      )
    );

    for (Document doc : collection.aggregate(pipeline)) {

      Document id = (Document) doc.get("_id");

      JsonObject json = new JsonObject()
        .put("assetTagName", id.getString("assetTagName"))
        .put("category", id.getString("categoryName"))
        .put("totalAssets", doc.getInteger("totalAssets", 0))
        .put("ready", doc.getInteger("ready", 0))
        .put("deployed", doc.getInteger("deployed", 0))
        .put("deadStock", doc.getInteger("deadStock", 0))
        .put("underMaintenance", doc.getInteger("underMaintenance", 0))
        .put("damaged", doc.getInteger("damaged", 0));

      result.add(json);
    }

    return result;
  }
  public JsonArray getReturnLogs() {

    LOGGER.info("Fetching return logs");

    JsonArray result = new JsonArray();

    MongoCollection<Document> returnCollection =
      mongoDatabase.getCollection("returnto");

    List<Document> pipeline = new ArrayList<>();

    // =========================
    // JOIN ASSETS
    // =========================
    pipeline.add(new Document("$lookup",
      new Document("from", "assets")
        .append("localField", "assetId")
        .append("foreignField", "_id")
        .append("as", "asset")
    ));

    pipeline.add(new Document("$unwind",
      new Document("path", "$asset")
        .append("preserveNullAndEmptyArrays", true)
    ));

    // =========================
    // JOIN ASSET TAGS
    // =========================
    pipeline.add(new Document("$lookup",
      new Document("from", "assettags")
        .append("localField", "asset.assetTagId")
        .append("foreignField", "_id")
        .append("as", "assetTag")
    ));

    pipeline.add(new Document("$unwind",
      new Document("path", "$assetTag")
        .append("preserveNullAndEmptyArrays", true)
    ));

    // =========================
    // JOIN CATEGORY
    // =========================
    pipeline.add(new Document("$lookup",
      new Document("from", "categories")
        .append("localField", "assetTag.categoryId")
        .append("foreignField", "_id")
        .append("as", "category")
    ));

    pipeline.add(new Document("$unwind",
      new Document("path", "$category")
        .append("preserveNullAndEmptyArrays", true)
    ));

    // =========================
    // JOIN LOCATION
    // =========================
    pipeline.add(new Document("$lookup",
      new Document("from", "locations")
        .append("localField", "locationId")
        .append("foreignField", "_id")
        .append("as", "location")
    ));

    pipeline.add(new Document("$unwind",
      new Document("path", "$location")
        .append("preserveNullAndEmptyArrays", true)
    ));

    // =========================
    // JOIN RETURNED ASSET
    // =========================
    pipeline.add(new Document("$lookup",
      new Document("from", "assets")
        .append("localField", "returnedToAssetId")
        .append("foreignField", "_id")
        .append("as", "returnedAsset")
    ));

    pipeline.add(new Document("$unwind",
      new Document("path", "$returnedAsset")
        .append("preserveNullAndEmptyArrays", true)
    ));

    // =========================
    // BASIC PROJECT (NO LOGIC HERE)
    // =========================
    pipeline.add(new Document("$project",
      new Document("assetName", "$asset.assetName")
        .append("category", "$category.categoryName")
        .append("total", "$asset.quantity")
        .append("returnDate", "$returnDate")
        .append("locationName", "$location.locationName")
        .append("returnedAssetName", "$returnedAsset.assetName")
        .append("personId", "$personId")
        .append("locationId", "$locationId")
        .append("returnedToAssetId", "$returnedToAssetId")
    ));

    // =========================
    // EXECUTION + LOGIC IN JAVA
    // =========================
    for (Document doc : returnCollection.aggregate(pipeline)) {

      JsonObject json = new JsonObject();

      json.put("assetName", doc.getString("assetName"));
      json.put("category", doc.getString("category"));
      json.put("total", doc.getInteger("total", 0));

      json.put("returnDate",
        doc.getDate("returnDate") != null
          ? dateToString(doc.getDate("returnDate"))
          : null
      );

      String issuedFor = "Unknown";
      String returnType = "Unknown";

      if (doc.get("locationId") != null) {

        issuedFor = doc.getString("locationName");
        returnType = "Location";

      } else if (doc.get("personId") != null) {

        issuedFor = doc.getObjectId("personId").toHexString();
        returnType = "Person";

      } else if (doc.get("returnedToAssetId") != null) {

        issuedFor = doc.getString("returnedAssetName");
        returnType = "Asset";
      }

      json.put("issuedFor", issuedFor);
      json.put("returnType", returnType);

      result.add(json);
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
                .put("locationId", objectIdToString(asset.getObjectId("locationId")));
    }

    private String objectIdToString(ObjectId objectId) {
        return objectId == null ? null : objectId.toHexString();
    }

    private String dateToString(Date date) {
        return date == null ? null : date.toInstant().toString();
    }
}
