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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InjectabilityContext {

  private Set<MethodInfo> visitedMethods = new HashSet<MethodInfo>();
  private Set<Variable> injectables = new HashSet<Variable>();
  private Set<Variable> globals = new HashSet<Variable>();
  private Set<Variable> globalState = new HashSet<Variable>();
  private long totalCost = 0;
  private final ClassRepository classRepository;

  public InjectabilityContext(ClassRepository classRepository) {
    this.classRepository = classRepository;
  }

  public Collection<Variable> getInjectables() {
    return injectables;
  }

  public void addMethodCost(long cost) {
    totalCost += cost;
  }

  public long getTotalCost() {
    return totalCost;
  }

  public long getGlobalState() {
    return 0;
  }

  public long getGlobalMutableState() {
    return 0;
  }

  public MethodInfo getMethod(String clazzName, String methodName) {
    return classRepository.getClass(clazzName).getMethod(methodName);
  }

  public void visitMethod(MethodInfo method) {
    visitedMethods.add(method);
  }

  public boolean methodAlreadyVisited(MethodInfo method) {
    return visitedMethods.contains(method);
  }

  @Override
  public String toString() {
    return "TotalCost: " + totalCost + " " + injectables + "\nGlobalCost: "
        + globalState.size() + " " + globals;
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

  public long getGlobalLoad() {
    return globalState.size();
  }

  public void localAssginment(Variable destination, Variable source) {
    if (isInjectable(source)) {
      setInjectable(destination);
    }
    if (destination.isStatic() || isGlobal(source)) {
      setGlobal(destination);
    }
  }
  
  public void fieldAssignment(Variable fieldInstance, Variable field, Variable value) {
    localAssginment(field, value);
    if (fieldInstance == null || globals.contains(fieldInstance)) {
      globalState.add(field);
      globals.add(field);
    }
  }
  
  public void arrayAssignment(Variable array, Variable index, Variable value) {
    localAssginment(array, value);
    if (globals.contains(array)) {
      globalState.add(array);
    }
  }
  public boolean isGlobal(Variable var) {
    return var.isStatic() || globals.contains(var);
  }

  public void setGlobal(Variable var) {
    globals.add(var);
  }
  
  public boolean isInjectable(Variable var) {
    return injectables.contains(var);
  }

  public void setInjectable(Variable var) {
    injectables.add(var);
  }


}
