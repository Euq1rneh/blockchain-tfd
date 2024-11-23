#!/bin/bash

# Set directories and JAR file name
SRC_DIR="src"
BIN_DIR="bin"
JAR_DIR="dist"
JAR_NAME="streamlet.jar"

# Main class (specify as "package.ClassName" if your main class is in a package)
MAIN_CLASS="streamlet.Node"

# Function to compile Java files
compile_java() {
    # Create bin directory if it doesn't exist
    mkdir -p "$BIN_DIR"

    # Find all .java files in the src directory and compile them
    find "$SRC_DIR" -name "*.java" > sources.txt
    javac -d "$BIN_DIR" @sources.txt
    rm sources.txt

    if [ $? -eq 0 ]; then
        echo "Compilation successful."
    else
        echo "Compilation failed."
        exit 1
    fi
}

# Function to create a JAR file
create_jar() {
    if [ -n "$MAIN_CLASS" ]; then
        # Create JAR directory if it doesn't exist
        mkdir -p "$JAR_DIR"

        # Create the manifest file
        echo "Main-Class: $MAIN_CLASS" > manifest.txt

        # Package the compiled classes into a JAR file
        jar cvfm "$JAR_DIR/$JAR_NAME" manifest.txt -C "$BIN_DIR" .

        # Clean up the manifest file
        rm manifest.txt

        echo "JAR file created at $JAR_DIR/$JAR_NAME"
    else
        echo "No MAIN_CLASS specified. Skipping JAR creation."
    fi
}

# Function to run the JAR file
run_jar() {
    if [ -n "$MAIN_CLASS" ]; then
        echo "Running JAR file: $JAR_DIR/$JAR_NAME"
        java -jar "$JAR_DIR/$JAR_NAME"
    else
        echo "No MAIN_CLASS specified. Skipping execution."
    fi
}

# Function to update the start time in the config.txt file
update_start_time() {
    START_TIME=$(date -d '+1 minutes' +%H:%M:00)
    sed -i "s/start_time=.*/start_time=$START_TIME/" config.txt
}

# Function to delete log files
delete_logs() {
    find . -type f -name '*.log' -delete
}

# Main execution
echo "Starting compilation..."
update_start_time
compile_java
create_jar
delete_logs
#run_jar