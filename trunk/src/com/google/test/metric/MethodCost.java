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

import java.util.LinkedList;
import java.util.List;

public class MethodCost {

  private final String methodName;
  private final long cyclomaticCost;
  private final int lineNumber;
  private final List<LineNumberCost> operationCosts = new LinkedList<LineNumberCost>();
  private final List<GlobalStateCost> globalStateCosts = new LinkedList<GlobalStateCost>();
  private long totalGlobalCost = -1;
  private long totalComplexityCost = -1;
  private long overallCost;

  public MethodCost(String methodName, int lineNumber, long cyclomaticCost) {
    this.methodName = methodName;
    this.lineNumber = lineNumber;
    this.cyclomaticCost = cyclomaticCost;
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

  public void link(CostModel costModel) {
    if (!isLinked()) {
      totalGlobalCost = 0;
      totalComplexityCost = 0;
      for (LineNumberCost operationCost : operationCosts) {
        MethodCost childCost = operationCost.getMethodCost();
        childCost.link(costModel);
        totalGlobalCost += childCost.getTotalGlobalCost();
        totalComplexityCost += childCost.getTotalComplexityCost();
      }
      totalGlobalCost += getGlobalCost();
      totalComplexityCost += getCyclomaticCost();
      overallCost = costModel.computeMethod(getTotalComplexityCost(), getTotalGlobalCost());
    }
  }

  public int getGlobalCost() {
    return globalStateCosts.size();
  }

  public List<LineNumberCost> getOperationCosts() {
    return operationCosts;
  }

  public String getMethodName() {
    return methodName;
  }

  public void addMethodCost(int lineNumber, MethodCost to) {
    operationCosts.add(new LineNumberCost(lineNumber, to));
  }

  public void addGlobalCost(int lineNumber, Variable variable) {
    globalStateCosts.add(new GlobalStateCost(lineNumber, variable));
  }

  @Override
  public String toString() {
      return getMethodName() + toCostsString();
  }

  public String toCostsString() {
    return " [" + getCyclomaticCost() + ", " + getGlobalCost() + " / "
    + getTotalComplexityCost() + ", " + getTotalGlobalCost() + "]";
  }

  public long getCyclomaticCost() {
    return cyclomaticCost;
  }

  public int getMethodLineNumber() {
    return lineNumber;
  }

  public long getOverallCost() {
    return overallCost;
  }

}
