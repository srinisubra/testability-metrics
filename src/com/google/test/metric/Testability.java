/*
 * Copyright 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.test.metric;

import com.google.classpath.ClasspathRoot;
import com.google.classpath.ClasspathRootFactory;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.ExampleMode;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.io.Writer;
import java.io.PrintWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Testability {

  @Option(name = "-cp", metaVar = "lib/one.jar:lib/two.jar", usage = "classpath needed for analyzing the jar or directory")
  private String classpath = "";

  @Argument(metaVar = "path/to/directory_or_jar")
  private List<String> arguments = new ArrayList<String>();

  public String getClasspath() {
    return classpath;
  }

  public List<String> getArguments() {
    return arguments;
  }

  public static void main(String... args) throws CmdLineException, IOException {
    new Testability().doMain(args);
  }

  public void doMain(String... args) throws IOException, CmdLineException {
    parseArgs(new PrintWriter(System.err), args);
    File jarOrDir = new File(arguments.get(0));
    if (classpath.length() == 0) {
      classpath = System.getProperty("java.class.path", ".");
    }
    ClasspathRoot classpathRoot = ClasspathRootFactory.makeClasspathRoot(
        jarOrDir, classpath);
    ClassRepository classRepository = new ClassRepository(classpathRoot);
    for (String className : classpathRoot.getAllContainedClassNames()) {
      ClassCost classCost = computeCost(className, classRepository);
      System.out.println("Testability cost for " + classCost + "\n");
    }
  }

  public void parseArgs(Writer err, String... args) throws IOException,
      CmdLineException {
    CmdLineParser parser = new CmdLineParser(this);
    try {
      parser.parseArgument(args);
      if (arguments.isEmpty()) {
        throw new CmdLineException("No argument was given");
      }
    } catch (CmdLineException e) {
      err.write(e.getMessage());
      err.write("\njava com.google.test.metric.Testability [options...] arguments...");
      err.write("\nExample: java path/to/dir_or_jar"
          + parser.printExample(ExampleMode.ALL) + "\n\n");
      err.flush();
      throw new CmdLineException("Exiting...");
    }
  }

  public ClassCost computeCost(String className, ClassRepository repo) {
    MetricComputer metricComputer = new MetricComputer(repo);
    ClassCost classCost = metricComputer.compute(repo.getClass(className));
    return classCost;
  }

  public ClassCost computeCost(String className) {
    return computeCost(className, new ClassRepository());
  }
}
