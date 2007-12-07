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
import java.util.List;

public class ClassCost {

  private final List<MethodCost> methods;
  private final ClassInfo classInfo;

  public ClassCost(ClassInfo classInfo, List<MethodCost> methods) {
    this.classInfo = classInfo;
    this.methods = methods;
  }

  public MethodCost getMethodCost(String methodName) {
    for (MethodCost cost : methods) {
      if (cost.getNameDesc().equals(methodName)) {
        return cost;
      }
    }
    throw new IllegalArgumentException("Method '" + methodName
        + "' does not exist.");
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    toString(buf);
    return buf.toString();
  }

  public void toString(StringBuilder buf) {
    buf.append(classInfo.toString());
    for (MethodCost cost : methods) {
      buf.append("\n  ");
      cost.toString("  ", buf, new HashSet<MethodCost>());
    }
  }

}
