#!/bin/bash

# setup.sh - Initialize a new project from Kjusys Microservice Base

echo "---------------------------------------------------------"
echo "   Initialize New Project from Kjusys Microservice Base"
echo "---------------------------------------------------------"

# Helper function for cross-platform sed in-place editing
run_sed() {
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "$@"
    else
        sed -i "$@"
    fi
}

echo "Note: Project Name must be lowercase with dashes only (e.g., user-service)"
read -p "Enter Project Name: " inputName

# Validation: Check if inputName contains only lowercase letters and dashes
if ! [[ "$inputName" =~ ^[a-z-]+$ ]]; then
    echo "Error: Project Name must contain only lowercase letters and dashes."
    exit 1
fi

if [ -z "$inputName" ]; then
    echo "Error: Project Name is required."
    exit 1
fi

# 1. Processed Project Name (SentenceCase with dash removed)
# e.g., user-service -> UserService
processedProjectName=$(echo "$inputName" | awk -F- '{for(i=1;i<=NF;i++)sub(/./,toupper(substr($i,1,1)),$i)}1' OFS="")

# 2. Artifact ID
# e.g., KJUsys-UserService
artifactId="KJUsys-$processedProjectName"

# 3. Microservice App Info
# e.g., user-service-app-info
appInfoKey="${inputName}-app-info"

# 4. Log File Name Base
# e.g., kjusys-user-service
logNameBase="kjusys-$inputName"

echo "---------------------------------------------------------"
echo "Generatred Configuration:"
echo "Artifact ID:       $artifactId"
echo "Project Name:      $processedProjectName"
echo "App Info Key:      $appInfoKey"
echo "Log File Base:     $logNameBase"
echo "---------------------------------------------------------"

read -p "Proceed with these values? (y/n): " confirm
if [ "$confirm" != "y" ]; then
    echo "Aborted."
    exit 0
fi

echo "Initializing project..."

# 1. Update pom.xml
if [ -f "pom.xml" ]; then
    # Replace artifactId
    run_sed "s/kjusys-microservice-base/$artifactId/g" pom.xml
    echo "Updated pom.xml"
else
    echo "Warning: pom.xml not found."
fi

# 2. Update config/log4j2.xml
if [ -f "config/log4j2.xml" ]; then
    # Replace kjusys-microservice-base with kjusys-<inputName>
    run_sed "s/kjusys-microservice-base/$logNameBase/g" config/log4j2.xml
    echo "Updated config/log4j2.xml"
else
    echo "Warning: config/log4j2.xml not found."
fi

# 3. Update README.md
if [ -f "README.md" ]; then
    read -p "Update README.md with project specific details? (y/n): " confirmReadme
    if [ "$confirmReadme" == "y" ]; then
        # 1. Remove Content (Must be done before replacements to preserve patterns)
        
        # Update Description
        run_sed "s/This is the \*\*KJUsys Microservice Base\*\*.*/This is **$processedProjectName** part of the KJUsys Microservices./g" README.md

        # Remove Setup Instructions (Initialize Project section)
        run_sed '/### 1. Initialize Project (First Time Only)/{N;N;N;N;d;}' README.md

        # Remove the Note at the bottom
        run_sed '/\*\*Note\*\*: If you have run `setup.sh`/d' README.md
        run_sed '/---/d' README.md

        # 2. Update Content
        
        # Update Title
        run_sed "s/KJUsys Microservice Base/$processedProjectName/g" README.md
        
        # Update API Name
        upperInputName=$(echo "$inputName" | tr '[:lower:]' '[:upper:]' | tr '-' ' ')
        run_sed "s/KJUSYS MICROSERVICE API/$upperInputName API/g" README.md
        
        # Update App Info Config Key
        run_sed "s/microservice-app-info/$appInfoKey/g" README.md

        # Update JAR Name in Run Command
        run_sed "s/kjusys-microservice-base-1.0-SNAPSHOT-fat.jar/$artifactId-1.0-SNAPSHOT-fat.jar/g" README.md

        # Update Context URL in API Endpoints
        run_sed "s|/kjusys-api/microservice-base|/kjusys-api/$inputName|g" README.md
    
        # Renumber subsequent sections
        run_sed 's/### 2. Build the Project/### 1. Build the Project/g' README.md
        run_sed 's/### 3. Run the Application/### 2. Run the Application/g' README.md
        
        echo "Updated README.md"
    else
        echo "Skipping README.md update."
    fi
else
    echo "Warning: README.md not found."
fi

# 4. Update MicroserviceStarter.java
starterFile="src/main/java/in/edu/kristujayanti/MicroserviceStarter.java"
if [ -f "$starterFile" ]; then
    run_sed "s/microservice-app-info/$appInfoKey/g" "$starterFile"
    echo "Updated app-info key in MicroserviceStarter.java"
else
    echo "Warning: MicroserviceStarter.java not found."
fi

# 5. Update ContextRoutingURLName.java
contextFile="src/main/java/in/edu/kristujayanti/constants/ContextRoutingURLName.java"
if [ -f "$contextFile" ]; then
    # Escape slashes for sed or use different delimiter
    run_sed "s|/kjusys-api/microservice-base|/kjusys-api/$inputName|g" "$contextFile"
    echo "Updated Context URL in ContextRoutingURLName.java"
else
    echo "Warning: ContextRoutingURLName.java not found."
fi

# 6. Update Run Configuration
runConfigFile=".idea/runConfigurations/MicroserviceStarter.xml"
if [ -f "$runConfigFile" ]; then
    run_sed "s/kjusys-microservice-base/$artifactId/g" "$runConfigFile"
    
    # Update Run Configuration Name
    run_sed "s/name=\"MicroserviceStarter\"/name=\"$processedProjectName\"/g" "$runConfigFile"
    
    echo "Updated Run Configuration module name"
else
    echo "Warning: Run Configuration not found."
fi

echo "---------------------------------------------------------"
echo "Initialization Complete!"
echo "New Artifact ID: $artifactId"
echo "Logs will be written to: log/${logNameBase}.log"
echo "---------------------------------------------------------"
echo "---------------------------------------------------------"
echo "You can now delete this script: rm setup.sh"
echo "Removing .git directory to start fresh..."
rm -rf .git

echo "Adding setup.sh to .gitignore..."
echo "" >> .gitignore
echo "setup.sh" >> .gitignore

# 7. Rename Project Directory
currentDirName=$(basename "$PWD")
newDirName="${artifactId}-Backend"

if [ "$currentDirName" != "$newDirName" ]; then
    echo "---------------------------------------------------------"
    read -p "Rename project directory to $newDirName? (y/n): " confirmRename
    if [ "$confirmRename" == "y" ]; then
        cd ..
        # Check if destination exists
        if [ -d "$newDirName" ]; then
            echo "Error: Directory $newDirName already exists."
        else
            mv "$currentDirName" "$newDirName"
            echo "Project directory renamed to: $newDirName"
            echo "---------------------------------------------------------"
            echo "IMPORTANT: Your terminal is still inside the old directory path."
            echo "Please run the following command to switch to the new directory:"
            echo "cd ../$newDirName"
            
            # Delete the setup script
            rm "$newDirName/setup.sh"
            echo "setup.sh has been deleted from the new directory."
        fi
    else
        echo "Skipping directory rename."
    fi
fi
