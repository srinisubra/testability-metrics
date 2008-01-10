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

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

public class HumanReadablePrinter {

  private final PrintStream out;

  public HumanReadablePrinter(PrintStream out) {
    this.out = out;
  }

  public void print(ClassCost classCost, int maxDepth, int minCost) {
    out.println("\nTestability cost for " + classCost.getClassInfo());
    for (MethodCost cost : classCost.getMethods()) {
      print("  ", cost, maxDepth, minCost);
    }
  }

  public void print(String prefix, MethodCost cost, int maxDepth, int minCost) {
    Set<MethodInfo> alreadySeen = new HashSet<MethodInfo>();
    if (shouldPrint(cost, maxDepth, minCost, alreadySeen)) {
      out.print(prefix);
      out.println(cost);
      for (LineNumberCost child : cost.getOperationCosts()) {
        print("  " + prefix, child, maxDepth - 1, minCost, alreadySeen);
      }
    }
  }
  
  private void print(String prefix, LineNumberCost line, int maxDepth, 
      int minCost, Set<MethodInfo> alreadSeen) {
    MethodCost method = line.getMethodCost();
    if (shouldPrint(method, maxDepth, minCost, alreadSeen)) {
      out.print(prefix);
      out.print("line ");
      out.print(line.getLineNumber());
      out.print(": ");
      out.println(method);
      for (LineNumberCost child : method.getOperationCosts()) {
        print("  " + prefix, child, maxDepth - 1, minCost, alreadSeen);
      }
    }
  }

  private boolean shouldPrint(MethodCost method, int maxDepth, int minCost,
      Set<MethodInfo> alreadySeen) {
    if (maxDepth <= 0 || alreadySeen.contains(method.getMethod())) {
      return false;
    }
    alreadySeen.add(method.getMethod());
    long totalComplexityCost = method.getTotalComplexityCost();
    long totalGlobalCost = method.getTotalGlobalCost();
    if (totalGlobalCost < minCost && totalComplexityCost < minCost) {
      return false;
    }
    return true;
  }

}
