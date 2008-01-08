#!/bin/bash
JAVA_CMD="java6 -classpath bin:lib/args4j-2.0.8.jar:lib/asm-3.0.jar:lib/jarjar.jar:lib/junit.jar com.google.test.metric.Testability"
ARG="-maxPrintingDepth 2 com.google"
if [ "$1" = "wl" ]; then
  CMD="$JAVA_CMD -whitelist java.:org.objectweb $ARG"
else 
  CMD="$JAVA_CMD $ARG"
fi

echo $CMD
$CMD # | echo "lines: " `wc -l`


