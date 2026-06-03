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
            new Document("$limit", pageSize)
          ))
          .append("metadata",
            List.of(
              new Document("$count", "totalRecords")
            )));
    }

    private long getTotalRecords(Document facetResult) {

      List<Document> metadata =
        facetResult.getList("metadata", Document.class);

      if (metadata == null || metadata.isEmpty()) {
        return 0;
      }

      Number count =
        (Number) metadata.get(0).get("totalRecords");

      return count.longValue();
    }

    public AssetsService(MongoDatabase mongoDatabase) {
        this.mongoDatabase = mongoDatabase;
    }



      public PaginatedResult<JsonObject> getAssets(
      int page,
      int pageSize) {

      LOGGER.info("Fetching detailed assets page={} pageSize={}",
        page,
        pageSize);

      MongoCollection<Document> collection =
        mongoDatabase.getCollection(ASSETS_COLLECTION);

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
            .append("as", "assetTag"))
      );

      pipeline.add(
        new Document("$unwind", "$assetTag")
      );

      // =========================
      // JOIN CATEGORY
      // =========================
      pipeline.add(
        new Document("$lookup",
          new Document("from", "categories")
            .append("localField", "assetTag.categoryId")
            .append("foreignField", "_id")
            .append("as", "category"))
      );

      pipeline.add(
        new Document("$unwind", "$category")
      );

      // =========================
      // JOIN CAMPUS
      // =========================
      pipeline.add(
        new Document("$lookup",
          new Document("from", "campuses")
            .append("localField", "campusId")
            .append("foreignField", "_id")
            .append("as", "campus"))
      );

      pipeline.add(
        new Document("$unwind", "$campus")
      );

      // =========================
      // JOIN STATUS
      // =========================
      pipeline.add(
        new Document("$lookup",
          new Document("from", "status")
            .append("localField", "statusId")
            .append("foreignField", "_id")
            .append("as", "status"))
      );

      pipeline.add(
        new Document("$unwind", "$status")
      );

      // =========================
      // JOIN LOCATION
      // =========================
      pipeline.add(
        new Document("$lookup",
          new Document("from", "locations")
            .append("localField", "locationId")
            .append("foreignField", "_id")
            .append("as", "location"))
      );

      pipeline.add(
        new Document("$unwind", "$location")
      );

      // =========================
      // JOIN ISSUE TO
      // =========================
      pipeline.add(
        new Document("$lookup",
          new Document("from", "issueto")
            .append("localField", "_id")
            .append("foreignField", "assetId")
            .append("as", "issueInfo"))
      );

      pipeline.add(
        new Document("$unwind",
          new Document("path", "$issueInfo")
            .append("preserveNullAndEmptyArrays", true))
      );

      // =========================
      // JOIN ISSUED LOCATION
      // =========================
      pipeline.add(
        new Document("$lookup",
          new Document("from", "locations")
            .append("localField", "issueInfo.locationId")
            .append("foreignField", "_id")
            .append("as", "issuedLocation"))
      );

      pipeline.add(
        new Document("$unwind",
          new Document("path", "$issuedLocation")
            .append("preserveNullAndEmptyArrays", true))
      );

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

      // =========================
      // FACET
      // =========================
      pipeline.add(
        new Document("$facet",
          new Document("data",
            List.of(
              new Document("$skip", skip),
              new Document("$limit", pageSize)
            ))
            .append("metadata",
              List.of(
                new Document("$count",
                  "totalRecords")
              )))
      );

      Document facetResult =
        collection.aggregate(pipeline).first();

      List<JsonObject> result =
        new ArrayList<>();

      long totalRecords = 0;

      if (facetResult != null) {

        List<Document> metadata =
          facetResult.getList(
            "metadata",
            Document.class);

        if (metadata != null && !metadata.isEmpty()) {

          Number count =
            (Number) metadata.get(0)
              .get("totalRecords");

          totalRecords =
            count.longValue();
        }

        List<Document> data =
          facetResult.getList(
            "data",
            Document.class);

        if (data != null) {

          for (Document doc : data) {

            JsonObject json =
              new JsonObject();

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
                locationDoc.getString(
                  "locationName"));

            } else {

              json.put("location",
                "N/A");
            }

            // =========================
            // BLOCK NAME
            // =========================
            String blockId =
              doc.getString("blockId");

            Document campusDoc =
              (Document) doc.get("campus");

            String blockName =
              "N/A";

            if (campusDoc != null) {

              List<Document> blocks =
                (List<Document>)
                  campusDoc.get(
                    "blocks");

              if (blocks != null) {

                for (Document block : blocks) {

                  if (blockId != null &&
                    blockId.equals(
                      block.getString(
                        "blockId"))) {

                    blockName =
                      block.getString(
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
            String issuedTo =
              "Not Issued";

            Document issueInfo =
              (Document) doc.get(
                "issueInfo");

            if (issueInfo != null) {

              ObjectId personId =
                issueInfo.getObjectId(
                  "personId");

              Document issuedLocation =
                (Document) doc.get(
                  "issuedLocation");

              Document issuedAsset =
                (Document) doc.get(
                  "issuedAsset");

              if (issuedLocation != null) {

                issuedTo =
                  issuedLocation.getString(
                    "locationName");

              } else if (issuedAsset != null) {

                issuedTo =
                  issuedAsset.getString(
                    "assetName");

              } else if (personId != null) {

                issuedTo =
                  personId.toHexString();
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

  public PaginatedResult<JsonObject> getIssuedAssetsDetailed(
    int page,
    int pageSize) {

    LOGGER.info(
      "Fetching issued assets detailed data page={} pageSize={}",
      page,
      pageSize);

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

    int skip = (page - 1) * pageSize;

    long totalRecords =
      issueCollection.countDocuments();

    List<Document> issuedDocs =
      issueCollection.find()
        .skip(skip)
        .limit(pageSize)
        .into(new ArrayList<>());

    List<JsonObject> result =
      new ArrayList<>();

    for (Document issueDoc : issuedDocs) {

      JsonObject json = new JsonObject();

      ObjectId assetId =
        issueDoc.getObjectId("assetId");

      Document assetDoc =
        assetsCollection.find(
            new Document("_id", assetId))
          .first();

      if (assetDoc != null) {

        json.put(
          "assetName",
          assetDoc.getString("assetName"));

        ObjectId assetTagId =
          assetDoc.getObjectId("assetTagId");

        Document assetTagDoc =
          assetTagsCollection.find(
              new Document("_id",
                assetTagId))
            .first();

        if (assetTagDoc != null) {

          ObjectId categoryId =
            assetTagDoc.getObjectId(
              "categoryId");

          Document categoryDoc =
            categoriesCollection.find(
                new Document("_id",
                  categoryId))
              .first();

          if (categoryDoc != null) {

            json.put(
              "assetCategory",
              categoryDoc.getString(
                "categoryName"));
          }
        }
      }

      json.put(
        "issueDate",
        dateToString(
          issueDoc.getDate("issueDate")));

      ObjectId locationId =
        issueDoc.getObjectId("locationId");

      ObjectId issuedToAssetId =
        issueDoc.getObjectId("issuedToAssetId");

      ObjectId personId =
        issueDoc.getObjectId("personId");

      if (locationId != null) {

        Document locationDoc =
          locationsCollection.find(
              new Document("_id",
                locationId))
            .first();

        if (locationDoc != null) {

          json.put(
            "receiverName",
            locationDoc.getString(
              "locationName"));

          json.put(
            "receiverType",
            "Location");
        }
      }

      else if (issuedToAssetId != null) {

        Document issuedAssetDoc =
          assetsCollection.find(
              new Document("_id",
                issuedToAssetId))
            .first();

        if (issuedAssetDoc != null) {

          json.put(
            "receiverName",
            issuedAssetDoc.getString(
              "assetName"));

          json.put(
            "receiverType",
            "Asset");
        }
      }

      else if (personId != null) {

        json.put(
          "receiverName",
          objectIdToString(personId));

        json.put(
          "receiverType",
          "Person");
      }

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

    MongoCollection<Document> collection =
      mongoDatabase.getCollection("assets");

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
                0))))
    ));

    pipeline.add(
      new Document("$facet",
        new Document("metadata",
          List.of(
            new Document("$count",
              "totalRecords")
          ))
          .append("data",
            List.of(
              new Document("$skip", skip),
              new Document("$limit",
                pageSize)
            ))
      )
    );

    Document facetResult =
      collection.aggregate(pipeline)
        .first();

    List<JsonObject> result =
      new ArrayList<>();

    long totalRecords = 0;

    if (facetResult != null) {

      List<Document> metadata =
        (List<Document>) facetResult.get(
          "metadata");

      if (!metadata.isEmpty()) {

        totalRecords =
          metadata.get(0)
            .getInteger(
              "totalRecords",
              0);
      }

      List<Document> data =
        (List<Document>) facetResult.get(
          "data");

      for (Document doc : data) {

        Document id =
          (Document) doc.get("_id");

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
                0))
        );
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
    int page,
    int pageSize) {

    LOGGER.info(
      "Fetching assets for category={} page={} pageSize={}",
      categoryId,
      page,
      pageSize);

    int skip = (page - 1) * pageSize;

    MongoCollection<Document> collection =
      mongoDatabase.getCollection("assets");

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
              "totalRecords")
          ))
          .append("data",
            List.of(
              new Document("$skip", skip),
              new Document("$limit", pageSize)
            ))
      )
    );

    Document facetResult =
      collection.aggregate(pipeline)
        .first();

    List<JsonObject> result =
      new ArrayList<>();

    long totalRecords = 0;

    if (facetResult != null) {

      List<Document> metadata =
        (List<Document>) facetResult.get("metadata");

      if (!metadata.isEmpty()) {

        totalRecords =
          metadata.get(0)
            .getInteger(
              "totalRecords",
              0);
      }

      List<Document> data =
        (List<Document>) facetResult.get("data");

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
              doc.getBoolean("isIssuable"))
        );
      }
    }

    return new PaginatedResult<>(
      result,
      totalRecords,
      page,
      pageSize);
  }


  //Facet  - skip , limit and then paginated Result
  public PaginatedResult<JsonObject> getReturnLogs(
    int page,
    int pageSize) {

    LOGGER.info(
      "Fetching return logs page={} pageSize={}",
      page,
      pageSize);

    int skip = (page - 1) * pageSize;

    MongoCollection<Document> returnCollection =
      mongoDatabase.getCollection("returnto");

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

    pipeline.add(new Document("$project",
      new Document("assetName", "$asset.assetName")
        .append("category", "$category.categoryName")
        .append("total", "$asset.quantity")
        .append("returnDate", "$returnDate")
        .append("locationName", "$location.locationName")
        .append("returnedAssetName", "$returnedAsset.assetName")
        .append("personId", "$personId")
        .append("locationId", "$locationId")
        .append("returnedToAssetId", "$returnedToAssetId")));

    pipeline.add(
      new Document("$facet",
        new Document("metadata",
          List.of(
            new Document("$count",
              "totalRecords")
          ))
          .append("data",
            List.of(
              new Document("$skip", skip),
              new Document("$limit", pageSize)
            ))
      )
    );

    Document facetResult =
      returnCollection.aggregate(pipeline)
        .first();

    long totalRecords = 0;

    List<JsonObject> result =
      new ArrayList<>();

    if (facetResult != null) {

      List<Document> metadata =
        (List<Document>) facetResult.get("metadata");

      if (!metadata.isEmpty()) {

        totalRecords =
          metadata.get(0)
            .getInteger("totalRecords", 0);
      }

      List<Document> data =
        (List<Document>) facetResult.get("data");

      for (Document doc : data) {

        JsonObject json = new JsonObject();

        json.put(
          "assetName",
          doc.getString("assetName"));

        json.put(
          "category",
          doc.getString("category"));

        json.put(
          "total",
          doc.getInteger("total", 0));

        json.put(
          "returnDate",
          doc.getDate("returnDate") != null
            ? dateToString(
            doc.getDate("returnDate"))
            : null
        );

        String issuedFor = "Unknown";
        String returnType = "Unknown";

        if (doc.get("locationId") != null) {

          issuedFor =
            doc.getString("locationName");

          returnType =
            "Location";

        } else if (doc.get("personId") != null) {

          issuedFor =
            doc.getObjectId("personId")
              .toHexString();

          returnType =
            "Person";

        } else if (doc.get("returnedToAssetId") != null) {

          issuedFor =
            doc.getString(
              "returnedAssetName");

          returnType =
            "Asset";
        }

        json.put("issuedFor", issuedFor);
        json.put("returnType", returnType);

        result.add(json);
      }
    }

    return new PaginatedResult<>(
      result,
      totalRecords,
      page,
      pageSize);
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
