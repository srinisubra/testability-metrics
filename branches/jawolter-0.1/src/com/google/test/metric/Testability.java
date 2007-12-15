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
import java.io.PrintWriter;
import java.io.Writer;
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
          "Matches any class starting with these." +
          "\nEx. com.example.analyze.these com.google.and.these.packages " +
          "com.google.or.AClass", required = true)
  protected List<String> packagesToAnalyzeFilter = new ArrayList<String>();

  public static void main(String... args) throws IOException {
    try {
      new Testability().doMain(args, System.err);
    } catch (CmdLineException ignored) { }
  }

  public void doMain(String[] args, PrintStream err) throws IOException, CmdLineException {
    parseArgs(new PrintWriter(err), args);
    System.out.println(packagesToAnalyzeFilter.get(0));
    if (classpath.length() == 0) {
      classpath = System.getProperty("java.class.path", ".");
    }
    ClasspathRootGroup classpathRootGroup =
      ClasspathRootFactory.makeClasspathRootGroup(classpath);
    ClassRepository classRepository = new ClassRepository(classpathRootGroup);
    List<String> allContainedClassNames =
      classpathRootGroup.getAllContainedClassNames(packagesToAnalyzeFilter);
    for (String className : allContainedClassNames) {
      ClassCost classCost = computeCost(className, classRepository);
      System.out.println("Testability cost for " + classCost + "\n");
    }
    System.out.println("Analyzed " + allContainedClassNames.size() +
      " classes (plus how ever many of their external dependencies)");
  }

  public void parseArgs(Writer err, String... args) throws IOException,
      CmdLineException {
    CmdLineParser parser = new CmdLineParser(this);
    try {
      parser.parseArgument(args);
      if (packagesToAnalyzeFilter.isEmpty()) {
        throw new CmdLineException("No argument was given");
      } else {
        for (int i = 0; i < packagesToAnalyzeFilter.size(); i++) {
          packagesToAnalyzeFilter.set(i, packagesToAnalyzeFilter.get(i).replaceAll("/", "."));
        }
      }
    } catch (CmdLineException e) {
      err.write(e.getMessage());
      err.write("\njava com.google.test.metric.Testability" +
        " -cp classpath packages.to.analyze");
      err.write("\nExample: java -cp lib/foo.jar com.foo.model.Device\n" +
        "Example: java -cp lib/foo.jar:classes com.foo.model.subpackages foo.AClass\n");
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
