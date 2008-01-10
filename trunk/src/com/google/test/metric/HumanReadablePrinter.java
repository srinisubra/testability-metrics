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

public class HumanReadablePrinter {

  private final PrintStream out;

  public HumanReadablePrinter(PrintStream out) {
    this.out = out;
  }

  public void print(String prefix, MethodCost cost, int maxDepth, int minCost) {
    out.print(prefix);
    out.println(cost);
    for (LineNumberCost child : cost.getOperationCosts()) {
      print("  " + prefix, child, maxDepth - 1, minCost);
    }
  }

  private void print(String prefix, LineNumberCost line, int maxDepth, 
      int minCost) {
    if (maxDepth <= 0) {
      return;
    }
    MethodCost method = line.getMethodCost();
    if (method.getTotalComplexityCost()== 0 && method.getTotalGlobalCost()==0) {
      return;
    }
    out.print(prefix);
    out.print("line ");
    out.print(line.getLineNumber());
    out.print(": ");
    out.println(method);
    for (LineNumberCost child : method.getOperationCosts()) {
      print("  " + prefix, child, maxDepth - 1, minCost);
    }
  }

  public void print(ClassCost classCost, int maxDepth, int minCost) {
    out.println("Testability cost for " + classCost.getClassInfo() + "\n");
    for (MethodCost cost : classCost.getMethods()) {
      print("", cost, maxDepth, minCost);
    }
  }

}
