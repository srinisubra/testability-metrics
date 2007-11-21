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

import com.google.test.metric.asm.Visibility;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class MetricComputer {

  private final ClassRepository classRepository;

  public MetricComputer(ClassRepository classRepository) {
    this.classRepository = classRepository;
  }

  public MethodCost compute(Class<?> clazz, String methodName) {
    ClassInfo classInfo = classRepository.getClass(clazz);
    MethodInfo method = classInfo.getMethod(methodName);
    return compute(method);
  }

  public MethodCost compute(MethodInfo method) {
    InjectabilityContext context = new InjectabilityContext(classRepository);
    ClassInfo classInfo = method.getClassInfo();
    addStaticCost(classInfo, context);
    addConstructorCost(classInfo, method, context);
    addSetterInjection(classInfo, context);
    addFieldCost(classInfo, context);
    context.setInjectable(method);
    method.computeMetric(context);
    return new MethodCost(method, context.getTotalCost(), context.getGlobalLoad());
  }

  private void addSetterInjection(ClassInfo classInfo, InjectabilityContext context) {
    for (MethodInfo method : classInfo.getMethods()) {
      if (method.getName().startsWith("set")) {
        context.setInjectable(method);
        method.computeMetric(context);
      }
    }
  }

  private void addConstructorCost(ClassInfo classInfo,
      MethodInfo method, InjectabilityContext context) {
    if (!method.isStatic()) {
      MethodInfo constructor = getPrefferedConstructor(classInfo);
      if (constructor != null) {
        context.setInjectable(constructor);
        constructor.computeMetric(context);
      }
    }
  }

  private void addFieldCost(ClassInfo classInfo,
      InjectabilityContext context) {
    for (FieldInfo field : classInfo.getFields()) {
      if (!field.isPrivate()) {
        context.setInjectable(field);
      }
    }
  }

  private void addStaticCost(ClassInfo classInfo, InjectabilityContext context) {
    for (MethodInfo method : classInfo.getMethods()) {
      if (method.getName().startsWith("<clinit>")) {
        method.computeMetric(context);
      }
    }
  }

  MethodInfo getPrefferedConstructor(ClassInfo classInfo) {
    Collection<MethodInfo> methods = classInfo.getMethods();
    MethodInfo constructor = null;
    int currentArgsCount = -1;
    for (MethodInfo methodInfo : methods) {
      if (methodInfo.getVisibility() != Visibility.PRIVATE
          && methodInfo.getName().startsWith("<init>")) {
        int count = countNonPrimitiveArgs(methodInfo.getParameters());
        if (currentArgsCount < count) {
          constructor = methodInfo;
          currentArgsCount = count;
        }
      }
    }
    return constructor;
  }

  private int countNonPrimitiveArgs(List<ParameterInfo> parameters) {
    int count = 0;
    for (ParameterInfo parameter : parameters) {
      if (parameter.getType().isObject()) {
        count++;
      }
    }
    return count;
  }

  public ClassCost compute(Class<?> clazz) {
    return compute(classRepository.getClass(clazz));
  }

  public ClassCost compute(ClassInfo clazz) {
    List<MethodCost> methods = new LinkedList<MethodCost>();
    for (MethodInfo method : clazz.getMethods()) {
      methods.add(compute(method));
    }
    return new ClassCost(clazz, methods);
  }

}
