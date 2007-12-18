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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class MethodCost {

  private final MethodInfo method;
  private final List<LineNumberCost> lineNumberCosts = new LinkedList<LineNumberCost>();
  private final List<GlobalStateCost> globalStateCosts = new LinkedList<GlobalStateCost>();
  private long totalGlobalCost = -1;
  private long totalComplexityCost = -1;

  public MethodCost(MethodInfo method) {
    this.method = method;
  }

  public long getTotalComplexityCost() {
    assertLinked();
    return totalComplexityCost;
  }

  public long getTotalGlobalCost() {
    assertLinked();
    return totalGlobalCost;
  }

  private void assertLinked() {
    if (!isLinked()) {
      throw new IllegalStateException("Need to link first.");
    }
  }

  private boolean isLinked() {
    return totalComplexityCost >= 0 && totalGlobalCost >= 0;
  }

  public void link() {
    if (!isLinked()) {
      totalGlobalCost = 0;
      totalComplexityCost = 0;
      for (LineNumberCost lineNumberCost : lineNumberCosts) {
        MethodCost childCost = lineNumberCost.getMethodCost();
        childCost.link();
        totalGlobalCost += childCost.getTotalGlobalCost();
        totalComplexityCost += childCost.getTotalComplexityCost();
      }
      totalComplexityCost += getComplexityCost();
      totalGlobalCost += getGlobalCost();
    }
  }

  private long getComplexityCost() {
    return method.getTestCost();
  }

  private int getGlobalCost() {
    return globalStateCosts.size();
  }

  public void toString(String prefix, StringBuilder buf,
      Set<MethodCost> alreadySeen) {
    buf.append(method.getClassInfo().getName() + " ");
    buf.append(method.getNameDesc());
    buf.append("[" + getComplexityCost() + ", " + getGlobalCost() + " / "
        + totalComplexityCost + ", " + totalGlobalCost + "]");
    if (alreadySeen.contains(this)) {
      return;
    }
    alreadySeen.add(this);
    for (LineNumberCost line : lineNumberCosts) {
      MethodCost childCost = line.getMethodCost();
      if (childCost.totalGlobalCost > 0 || childCost.totalComplexityCost > 0) {
        buf.append("\n");
        buf.append(prefix + "  line ");
        buf.append(line.getLineNumber());
        buf.append(": ");
        childCost.toString(prefix + "  ", buf, alreadySeen);
      }
    }
  }

  public List<LineNumberCost> getOperationCosts() {
    return lineNumberCosts;
  }

  public MethodInfo getMethod() {
    return method;
  }

  public void addMethodCost(int lineNumber, MethodCost to) {
    lineNumberCosts.add(new LineNumberCost(lineNumber, to));
  }

  public void addGlobalCost(int lineNumber, Variable variable) {
    globalStateCosts.add(new GlobalStateCost(lineNumber, variable));
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    toString("", buf, new HashSet<MethodCost>());
    return buf.toString();
  }

}
