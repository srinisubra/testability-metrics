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

import com.google.classpath.ClasspathRootFactory;
import com.google.classpath.ClasspathRootGroup;
import org.kohsuke.args4j.*;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class Testability {

  @Option(name = "-cp", metaVar = "classpath",
          usage = "colon delimited classpath to analyze (jars or directories)" +
          "\nEx. lib/one.jar:lib/two.jar")
  protected String classpath = "";

/* not currently implemented
  @Option(name = "-whitelist", metaVar ="com.foo.one:com.foo.two",
          usage = "colon delimited whitelisted packages that will not " +
                  "count against you. Includes matching all subpackages.")
  private String whitelist = "";*/

  @Argument(metaVar = "classes/packages",
          usage = "Classes or packages to analyze. " +
          "Matches any class starting with these.\n" +
          "Ex. com.example.analyze.these com.google.and.these.packages " +
          "com.google.or.AClass", required = true)
  protected List<String> entryList = new ArrayList<String>();
  private final PrintStream out;
  private final PrintStream err;
  protected ClasspathRootGroup classpathRootGroup;

  public Testability(PrintStream out, PrintStream err) {
    this.out = out;
    this.err = err;
  }

  public static void main(String... args) throws IOException {
    Testability testability = new Testability(System.out, System.err);
    testability.run(args);
  }

  public void run(String... args) throws IOException {
    try {
      parseSetup(args);
      computeGroupMetric();
    } catch (CmdLineException ignored) { }
  }

  public void parseSetup(String... args) throws IOException, CmdLineException {
    parseArgs(args);
    out.println(entryList.get(0));
    if (classpath.length() == 0) {
      classpath = System.getProperty("java.class.path", ".");
    }
    classpathRootGroup = ClasspathRootFactory.makeClasspathRootGroup(classpath);
  }

  public void parseArgs(String... args) throws IOException,
      CmdLineException {
    CmdLineParser parser = new CmdLineParser(this);
    try {
      parser.parseArgument(args);
      if (entryList.isEmpty()) {
        throw new CmdLineException("No argument was given");
      } else {
        for (int i = 0; i < entryList.size(); i++) {
          entryList.set(i, entryList.get(i).replaceAll("/", "."));
        }
      }
    } catch (CmdLineException e) {
      err.println(e.getMessage());
      err.println("\njava com.google.test.metric.Testability" +
        " -cp classpath packages.to.analyze");
      err.println("\nExample: java -cp lib/foo.jar com.foo.model.Device\n" +
        "Example: java -cp lib/foo.jar:classes com.foo.model.subpackages foo.AClass\n");
      throw new CmdLineException("Exiting...");
    }
  }

  public void computeGroupMetric() throws IOException, CmdLineException {
    ClassRepository classRepository = new ClassRepository(classpathRootGroup);
    List<String> allContainedClassNames =
        classpathRootGroup.getAllContainedClassNames(entryList);
    for (String className : allContainedClassNames) {
      ClassCost classCost = computeCost(className, classRepository);
      out.println("Testability cost for " + classCost + "\n");
    }
    out.println("Analyzed " + allContainedClassNames.size() +
        " classes (plus how ever many of their external dependencies)");
  }

  public ClassCost computeCost(String className, ClassRepository repo) {
    MetricComputer metricComputer = new MetricComputer(repo, err);
    ClassCost classCost = null;
    try {
      classCost = metricComputer.compute(repo.getClass(className));
    } catch (ClassNotFoundException e) {
      err.println("WARNING: can not analyze class '" + className + "' since class '"
          + e.getClassName() + "' was not found.");
    }
    return classCost;
  }

  public ClassCost computeCost(String className) {
    return computeCost(className, new ClassRepository());
  }
}
