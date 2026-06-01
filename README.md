# AssetManagementApi

## Description
This is **AssetManagementApi** part of the KJUsys Microservices.

## Prerequisites
Ensure the following are installed and running:
*   **Java 17+**
*   **Redis** (for configuration)
*   **MongoDB** (database)

## Configuration
The application retrieves its configuration from a **Redis** server.

### Redis Configuration Key
*   **Key**: `app-config-values-development` (or `production`)
*   **Field**: `asset-management-api-app-info`

### Setting up Configuration
Run the following Redis command to set the initial configuration:

```bash
hset app-config-values-development asset-management-api-app-info '{"port":xxxx,"api_name":"ASSET MANAGEMENT API API", "vertx_file_buffer_path": "/tmp/vertx-file-buffer", "root_directory_path": "/tmp/file-uploads"}'
```

*   `port`: The HTTP port for the service.
*   `api_name`: Descriptive name for the API (used in logs/responses).
*   `vertx_file_buffer_path`: Temp path for Vert.x file uploads.
*   `root_directory_path`: Directory to store uploaded files.

## Setup & Running


### 1. Build the Project
```bash
mvn clean package -DskipTests
```

### 2. Run the Application
Use the command below to start the service. Ensure Redis is running locally or update the address.

```bash
java -jar target/KJUsys-AssetManagementApi-1.0-SNAPSHOT-fat.jar \
  -config_server_address 127.0.0.1:6379 \
  -environment development
```

*   Replace `kjusys-microservice-base` with your new artifact ID if initialized.

## API Endpoints

### Health Check
*   **URL**: `/kjusys-api/asset-management-api/health`
*   **Method**: `GET`
*   **Description**: Returns the status of the microservice.

