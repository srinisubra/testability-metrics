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
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.test.metric.ClassCost;


public class TextReport {

  private final PrintStream out;
  private final SortedSet<ClassCost> costs = new TreeSet<ClassCost>(new ClassCost.Comparator());
  private final int maxExcelentCost;
  private final int maxAcceptableCost;
  private int excelentCount = 0;
  private int goodCount = 0;
  private int needsWorkCount = 0;

  public TextReport(PrintStream out, int maxExcelentCost, int maxAcceptableCost) {
    this.out = out;
    this.maxExcelentCost = maxExcelentCost;
    this.maxAcceptableCost = maxAcceptableCost;
  }

  public void printSummary() {
    int total = costs.size();
    out.printf("      Analyzed classes: %5d%n", total);
    out.printf("  Excelent classes (.): %5d %5.1f%%%n", excelentCount, 100f * excelentCount / total);
    out.printf("      Good classes (=): %5d %5.1f%%%n", goodCount, 100f * goodCount / total);
    out.printf("Needs work classes (@): %5d %5.1f%%%n", needsWorkCount, 100f * needsWorkCount / total);
    PieGraph graph = new PieGraph(50, new CharMarker('.', '=', '@'));
    String chart = graph.render(excelentCount, goodCount, needsWorkCount);
    out.printf("             Breakdown: [%s]%n", chart);
  }

  public void addClassCost(ClassCost classCost) {
    long cost = classCost.getOverallCost();
    if (cost < maxExcelentCost) {
      excelentCount++;
    } else if (cost < maxAcceptableCost) {
      goodCount++;
    } else {
      needsWorkCount++;
    }
    costs.add(classCost);
  }

  public void printDistribution(int rows, int width) {
    Histogram histogram = new Histogram(width, rows, new Marker() {
      public char get(int index, float value) {
        if (value < maxExcelentCost) {
          return '.';
        } else if (value < maxAcceptableCost) {
          return '=';
        } else {
          return '@';
        }
      }
    });
    float[] values = new float[costs.size()];
    int i = 0;
    for (ClassCost cost : costs) {
      values[i++] = cost.getOverallCost();
    }
    for (String graph : histogram.graph(values)) {
      out.println(graph);
    }
  }

  public void printWorstOffenders(int worstOffenderCount) {
    out.println();
    out.println("Highest Cost");
    out.println("============");
    int i=0;
    for (ClassCost cost : costs) {
      out.println(cost);
      if (++i == worstOffenderCount) {
        break;
      }
    }
  }

}
