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
  private final long implicitCost;
  private long globalLoad;

  public MethodCost(MethodInfo method, long implicitCost) {
    this.method = method;
    this.implicitCost = implicitCost;
  }

  public long getComplexity() {
    return getComplexity(new HashSet<MethodCost>());
  }

  public long getComplexity(Set<MethodCost> alreadySeen) {
    if (alreadySeen.contains(this)) {
      return 0;
    }
    alreadySeen.add(this);
    long sum = implicitCost;
    for (LineNumberCost lineNumberCost : lineNumberCosts) {
      sum += lineNumberCost.getMethodCost().getComplexity(alreadySeen);
    }
    return sum;
  }

  public String getNameDesc() {
    return method.getNameDesc();
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    toString("", buf, new HashSet<MethodCost>());
    return buf.toString();
  }

  public void toString(String prefix, StringBuilder buf,
      Set<MethodCost> alreadySeen) {
    buf.append(method.getNameDesc());
    long cost = getComplexity(new HashSet<MethodCost>(alreadySeen));
    buf.append("[" + implicitCost + "/" + cost + "]");
    if (alreadySeen.contains(this)) {
      return;
    }
    alreadySeen.add(this);
    if (cost > 0) {
      for (LineNumberCost line : lineNumberCosts) {
        buf.append("\n");
        buf.append(prefix + "  line ");
        buf.append(line.getLineNumber());
        buf.append(": ");
        line.getMethodCost().toString(prefix + "  ", buf, alreadySeen);
      }
    }
  }
  public long getGlobal() {
    return globalLoad;
  }

  public List<LineNumberCost> getOperationCosts() {
    return lineNumberCosts;
  }

  public MethodInfo getMethod() {
    return method;
  }

  public void addMethodCost(int fromLineNumber, MethodCost to) {
    lineNumberCosts.add(new LineNumberCost(fromLineNumber, to));
  }

}
