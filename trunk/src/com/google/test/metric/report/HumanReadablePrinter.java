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
package com.google.test.metric.report;

import static java.lang.System.getProperty;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.test.metric.ClassCost;
import com.google.test.metric.LineNumberCost;
import com.google.test.metric.MethodCost;

public class HumanReadablePrinter implements Report {
  public static final String NEW_LINE = getProperty("line.separator");

  private static final String DIVIDER  = "-----------------------------------------\n";
  private final PrintStream out;
  private final List<String> entryList;
  private final SortedSet<ClassCost> toPrint = new TreeSet<ClassCost>(new ClassCost.Comparator());
  private final int maxDepth;
  private final int minCost;
  private long cumulativeTCC = 0;
  private long cumulativeTGC = 0;
  private int countAnalyzed;

  public HumanReadablePrinter(PrintStream out, List<String> entryList,
      int maxDepth, int minCost) {
    this.out = out;
    this.entryList = entryList;
    this.maxDepth = maxDepth;
    this.minCost = minCost;
  }

  public void printHeader() {
    out.println(DIVIDER + "Packages/Classes To Enter: ");
    for (String entry : entryList) {
      out.println("  " + entry + "*");
    }
    out.println("Max Method Print Depth: " + maxDepth);
    out.println("Min Class Cost: " + minCost);
    out.println(DIVIDER);
  }

  public void printFooter() {
    printClassCosts();
    out.println(NEW_LINE + DIVIDER + "Summary Statistics:");
    out.println(" TCC for all classes entered: " + cumulativeTCC);
    out.println(" TGC for all classes entered: " + cumulativeTGC);
    out.println(" Average TCC for all classes entered: " +
        String.format("%.2f", ((double)cumulativeTCC) / countAnalyzed));
    out.println(" Average TGC for all classes entered: " +
        String.format("%.2f", ((double)cumulativeTGC) / countAnalyzed));

    out.println(NEW_LINE + "Key:");
    out.println(" TCC: Total Compexity Cost");
    out.println(" TGC: Total Global Cost");

    out.println(NEW_LINE + "Analyzed " + countAnalyzed +
    " classes (plus non-whitelisted external dependencies)");
  }

  public void addClassCost(ClassCost classCost) {
    toPrint.add(classCost);
  }

  public void printClassCosts() {
    for (ClassCost classCost : toPrint) {
      print(classCost);
    }
  }

  public void print(ClassCost classCost) {
    if (shouldPrint(classCost, minCost)) {
      long tcc = classCost.getTotalComplexityCost();
      long tgc = classCost.getTotalGlobalCost();
      cumulativeTCC += tcc;
      cumulativeTGC += tgc;
      out.println(NEW_LINE + "Testability cost for " + classCost.getClassName() + " [ "
          + tcc + " TCC, " + tgc + " TGC ]");
      for (MethodCost cost : classCost.getMethods()) {
        print("  ", cost, maxDepth);
      }
    }
  }

  public void print(String prefix, MethodCost cost, int maxDepth) {
    Set<String> alreadySeen = new HashSet<String>();
    if (shouldPrint(cost, maxDepth, alreadySeen)) {
      out.print(prefix);
      out.println(cost);
      for (LineNumberCost child : cost.getOperationCosts()) {
        print("  " + prefix, child, maxDepth - 1, alreadySeen);
      }
    }
  }

  private void print(String prefix, LineNumberCost line, int maxDepth,
      Set<String> alreadSeen) {
    MethodCost method = line.getMethodCost();
    if (shouldPrint(method, maxDepth, alreadSeen)) {
      out.print(prefix);
      out.print("line ");
      out.print(line.getLineNumber());
      out.print(": ");
      out.println(method);
      for (LineNumberCost child : method.getOperationCosts()) {
        print("  " + prefix, child, maxDepth - 1, alreadSeen);
      }
    }
  }

  private boolean shouldPrint(ClassCost classCost, int minCost) {
    return classCost.getHighestMethodComplexityCost() >= minCost
        || classCost.getHighestMethodGlobalCost() >= minCost;
  }

  private boolean shouldPrint(MethodCost method, int maxDepth, Set<String> alreadySeen) {
    if (maxDepth <= 0 || alreadySeen.contains(method.getMethodName())) {
      return false;
    }
    alreadySeen.add(method.getMethodName());
    long totalComplexityCost = method.getTotalComplexityCost();
    long totalGlobalCost = method.getTotalGlobalCost();
    if (totalGlobalCost < minCost && totalComplexityCost < minCost) {
      return false;
    }
    return true;
  }

}
