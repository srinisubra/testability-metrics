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

import static java.util.Collections.EMPTY_LIST;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import com.google.test.metric.asm.Visibility;

public class ClassCostTest extends TestCase {

  @SuppressWarnings("unchecked")
  private final ClassInfo A = new ClassInfo("c.g.t.A", false, null, EMPTY_LIST);

  @SuppressWarnings("unchecked")
  private final  MethodInfo method0 = new MethodInfo(A, "method0", 1, "()V",
          null, null, null, Visibility.PUBLIC, 1, EMPTY_LIST);

  @SuppressWarnings("unchecked")
  private final  MethodInfo method1 = new MethodInfo(A, "method1", 1, "()V",
          null, null, null, Visibility.PUBLIC, 2, EMPTY_LIST);

  @SuppressWarnings("unchecked")
  private final  MethodInfo method2 = new MethodInfo(A, "method2", 2, "()V",
          null, null, null, Visibility.PUBLIC, 3, EMPTY_LIST);

  private final  MethodCost methodCost0 = new MethodCost(method0);
  private final  MethodCost methodCost1 = new MethodCost(method1);
  private final  MethodCost methodCost2 = new MethodCost(method2);

  ClassInfo classInfo0 = new ClassInfo("FAKE_classInfo0", false, null, null);
  ClassInfo classInfo1 = new ClassInfo("FAKE_classInfo1", false, null, null);
  ClassInfo classInfo2 = new ClassInfo("FAKE_classInfo2", false, null, null);

  ClassCost classCost0;
  ClassCost classCost1;
  ClassCost classCost2;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    methodCost0.link();
    methodCost1.link();
    methodCost2.link();

    List<MethodCost> methodCosts0 = new ArrayList<MethodCost>();
    methodCosts0.add(methodCost0);

    List<MethodCost> methodCosts1 = new ArrayList<MethodCost>();
    methodCosts1.add(methodCost0);
    methodCosts1.add(methodCost1);

    List<MethodCost> methodCosts2 = new ArrayList<MethodCost>();
    methodCosts2.add(methodCost0);
    methodCosts2.add(methodCost1);
    methodCosts2.add(methodCost2);

    classCost0 = new ClassCost(classInfo0, methodCosts0);
    classCost1 = new ClassCost(classInfo1, methodCosts1);
    classCost2 = new ClassCost(classInfo2, methodCosts2);
  }

  public void testSumsUpTotalClassCostCorrectly() throws Exception {
    assertEquals(0, classCost0.getTotalComplexityCost());
    assertEquals(1, classCost1.getTotalComplexityCost());
    assertEquals(3, classCost2.getTotalComplexityCost());
  }

  public void testClassCostSortsByDescendingCost() throws Exception {
    List<ClassCost> classCosts = new ArrayList<ClassCost>();
    classCosts.add(classCost1);
    classCosts.add(classCost0);
    classCosts.add(classCost2);
    Collections.sort(classCosts, new ClassCost.Comparator());
    assertEquals(classCost2, classCosts.get(0));
    assertEquals(classCost1, classCosts.get(1));
    assertEquals(classCost0, classCosts.get(2));
  }

}
