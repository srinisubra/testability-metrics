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
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

import com.google.test.metric.ClassCost;


public class TextReport {

  private final PrintStream out;
  private final List<ClassCost> excelent = new LinkedList<ClassCost>();
  private final List<ClassCost> good = new LinkedList<ClassCost>();
  private final List<ClassCost> needsWork = new LinkedList<ClassCost>();
  private final int maxExcelentCost;
  private final int maxAcceptableCost;

  public TextReport(PrintStream out, int maxExcelentCost, int maxAcceptableCost) {
    this.out = out;
    this.maxExcelentCost = maxExcelentCost;
    this.maxAcceptableCost = maxAcceptableCost;
  }

  public void printSummary() {
    int needsWork = this.needsWork.size();
    int good = this.good.size();
    int excelent = this.excelent.size();
    int total = excelent + good + needsWork;
    out.printf("      Analyzed classes: %5d%n", total);
    out.printf("  Excelent classes (.): %5d %5.1f%%%n", excelent, 100f * excelent / total);
    out.printf("      Good classes (=): %5d %5.1f%%%n", good, 100f * good / total);
    out.printf("Needs work classes (@): %5d %5.1f%%%n", needsWork, 100f * needsWork / total);
    PieGraph graph = new PieGraph(50, '.', '=', '@');
    String chart = graph.render(excelent, good, needsWork);
    out.printf("             Breakdown: [%s]%n", chart);
  }

  public void addExcelent(ClassCost classCost) {
    excelent.add(classCost);
  }

  public void addGood(ClassCost classCost) {
    good.add(classCost);
  }

  public void addNeedsWork(ClassCost classCost) {
    needsWork.add(classCost);
  }

  public void addClassCost(ClassCost classCost) {
    throw new UnsupportedOperationException();
  }

  public void printExcelentDistribution(int rows) {
    out.printf("Excelent Cost Distribution%n");
    out.printf("==========================%n");
    printDistribution(excelent, rows, 0, maxExcelentCost, '.');
  }

  public void printGoodDistribution(int rows) {
    out.printf("Good Cost Distribution%n");
    out.printf("======================%n");
    printDistribution(excelent, rows, maxExcelentCost, maxAcceptableCost, '=');
  }

  public void printNeedsWorkDistribution(int rows) {
    out.printf("Needs Work Cost Distribution%n");
    out.printf("============================%n");
    printDistribution(excelent, rows, maxAcceptableCost, -1, '#');
  }

  public void printDistribution(List<ClassCost> costs, int rows, int min, int max, char marker) {
    Histogram histogram = new Histogram(50, rows, marker);
    histogram.setMin(min);
    histogram.setMax(max);
    float[] values = new float[costs.size()];
    int i = 0;
    for (ClassCost cost : costs) {
      values[i++] = cost.getOverallCost();
    }
    for (String graph : histogram.graph(values)) {
      out.println(graph);
    }
  }

}
