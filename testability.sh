#!/bin/bash

SCRIPT_DIR=`dirname $0`
CWD="$PWD"

cd "$SCRIPT_DIR/build"
BUILD_DIR=`pwd -P`

classpath=$CLASSPATH

cd "$CWD"

for jarfile in `ls $BUILD_DIR/*.jar`; do
    classpath=$classpath:$jarfile
done

#TODO: this will not work if a -cp option used
JAR_OR_DIRECTORY="$@"

classpath=$classpath:$JAR_OR_DIRECTORY

# echo "CLASSPATH: $classpath"

java -Xmx512m -cp $classpath com.google.test.metric.Testability $JAR_OR_DIRECTORY
