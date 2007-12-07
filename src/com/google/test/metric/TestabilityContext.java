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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestabilityContext {

  private Set<Variable> injectables = new HashSet<Variable>();
  private Set<Variable> statics = new HashSet<Variable>();
  private final ClassRepository classRepository;
  private final Map<MethodInfo, MethodCost> methodCosts = new HashMap<MethodInfo, MethodCost>();

  public TestabilityContext(ClassRepository classRepository) {
    this.classRepository = classRepository;
  }

  public MethodInfo getMethod(String clazzName, String methodName) {
    return classRepository.getClass(clazzName).getMethod(methodName);
  }

  public boolean methodAlreadyVisited(MethodInfo method) {
    return methodCosts.containsKey(method);
  }

  public void recordMethodCall(MethodInfo fromMethod, int fromLineNumber,
      MethodInfo toMethod) {
    MethodCost from = getMethodCost(fromMethod);
    MethodCost to = getMethodCost(toMethod);
    from.addMethodCost(fromLineNumber, to);
    toMethod.computeMetric(this);
  }

  public MethodCost getMethodCost(MethodInfo method) {
    MethodCost methodCost = methodCosts.get(method);
    if (methodCost == null) {
      methodCost = new MethodCost(method);
      methodCosts.put(method, methodCost);
    }
    return methodCost;
  }

  public void implicitCost(MethodInfo from, MethodInfo to) {
    int line = to.getStartingLineNumber();
    getMethodCost(from).addMethodCost(line, getMethodCost(to));
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("MethodCost:");
    for (MethodCost cost : methodCosts.values()) {
      buf.append("\n");
      cost.toString("   ", buf, new HashSet<MethodCost>());
    }
    buf.append("\nInjectables:");
    for (Variable var : injectables) {
      buf.append("\n   ");
      buf.append(var);
    }
    buf.append("\nGlobals:");
    for (Variable var : statics) {
      buf.append("\n   ");
      buf.append(var);
    }
    return buf.toString();
  }

  public void setInjectable(List<? extends Variable> parameters) {
    for (Variable variable : parameters) {
      setInjectable(variable);
    }
  }

  public void setInjectable(MethodInfo method) {
    if (method.getMethodThis() != null) {
      setInjectable(method.getMethodThis());
    }
    setInjectable(method.getParameters());
  }

  public void localAssginment(Variable destination, Variable source) {
    if (isInjectable(source)) {
      setInjectable(destination);
    }
    if (destination.isStatic() || isGlobal(source)) {
      setGlobal(destination);
    }
  }

  public void fieldAssignment(Variable fieldInstance, Variable field,
      Variable value, MethodInfo inMethod, int lineNumber) {
    localAssginment(field, value);
    if (fieldInstance == null || statics.contains(fieldInstance)) {
      getMethodCost(inMethod).addGlobalCost(lineNumber, fieldInstance);
      statics.add(field);
    }
  }

  public void arrayAssignment(Variable array, Variable index, Variable value, MethodInfo inMethod, int lineNumber) {
    localAssginment(array, value);
    if (statics.contains(array)) {
      getMethodCost(inMethod).addGlobalCost(lineNumber, array);
    }
  }
  public boolean isGlobal(Variable var) {
    return var.isStatic() || statics.contains(var);
  }

  public void setGlobal(Variable var) {
    statics.add(var);
  }

  public boolean isInjectable(Variable var) {
    return injectables.contains(var);
  }

  public void setInjectable(Variable var) {
    injectables.add(var);
  }

}
