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

import static java.lang.Integer.MAX_VALUE;
import static java.util.Collections.EMPTY_LIST;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import com.google.test.metric.asm.Visibility;

public class HumanReadablePrinterTest extends AutoFieldClearTestCase {

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

  @SuppressWarnings("unchecked")
  private final  MethodInfo method3 = new MethodInfo(A, "method3", 2, "()V",
          null, null, null, Visibility.PUBLIC, 4, EMPTY_LIST);

  private final  MethodCost methodCost0 = new MethodCost(method0);
  private final  MethodCost methodCost1 = new MethodCost(method1);
  private final  MethodCost methodCost2 = new MethodCost(method2);
  private final  MethodCost methodCost3 = new MethodCost(method3);
  private ByteArrayOutputStream out;
  private HumanReadablePrinter printer;

  @Override
  public void setUp() {
    out = new ByteArrayOutputStream();
    printer =
        new HumanReadablePrinter(new PrintStream(out), null, MAX_VALUE, 0);
  }

  public void testSimpleCost() throws Exception {
    MethodCost costOnlyMethod1 = new MethodCost(method1);
    costOnlyMethod1.addGlobalCost(0, null);
    costOnlyMethod1.link();
    printer.print("", costOnlyMethod1, Integer.MAX_VALUE, 0);
    assertEquals("c.g.t.A.method1()V [1, 1 / 1, 1]\n", out.toString());
  }

  public void test2DeepPrintAll() throws Exception {
    methodCost2.addMethodCost(81, new MethodCost(method1));
    methodCost2.link();
    printer.print("", methodCost2, MAX_VALUE, 0);
    assertEquals("c.g.t.A.method2()V [2, 0 / 3, 0]\n" +
        "  line 81: c.g.t.A.method1()V [1, 0 / 1, 0]\n", out.toString());
  }

  public void test3DeepPrintAll() throws Exception {
    methodCost2.addMethodCost(8, methodCost1);
    methodCost3.addMethodCost(2, methodCost2);
    methodCost3.link();
    printer.print("", methodCost3, MAX_VALUE, 0);
    assertEquals("c.g.t.A.method3()V [3, 0 / 6, 0]\n" +
        "  line 2: c.g.t.A.method2()V [2, 0 / 3, 0]\n" +
        "    line 8: c.g.t.A.method1()V [1, 0 / 1, 0]\n", out.toString());
  }

  public void test2DeepSupress0Cost() throws Exception {
    methodCost1.addMethodCost(8, methodCost0);
    methodCost1.addMethodCost(13, methodCost3);
    methodCost1.link();
    printer.print("", methodCost1, MAX_VALUE, 1);
    assertEquals("c.g.t.A.method1()V [1, 0 / 4, 0]\n" +
    		"  line 13: c.g.t.A.method3()V [3, 0 / 3, 0]\n", out.toString());
  }

  public void test3DeepPrint2Deep() throws Exception {
    methodCost3.addMethodCost(2, methodCost2);
    methodCost2.addMethodCost(2, methodCost1);
    methodCost3.link();
    printer.print("", methodCost3, 2, 0);
    assertEquals("c.g.t.A.method3()V [3, 0 / 6, 0]\n"
      + "  line 2: c.g.t.A.method2()V [2, 0 / 3, 0]\n", out.toString());
  }

  public void testSupressAllWhenMinCostIs4() throws Exception {
    methodCost2.addMethodCost(81, new MethodCost(method1));
    methodCost2.link();
    printer.print("", methodCost2, MAX_VALUE, 4);
    assertEquals("", out.toString());
  }

  public void testSupressPartialWhenMinCostIs2() throws Exception {
    methodCost2.addMethodCost(81, new MethodCost(method1));
    methodCost2.link();
    printer.print("", methodCost2, Integer.MAX_VALUE, 2);
    assertEquals("c.g.t.A.method2()V [2, 0 / 3, 0]\n", out.toString());
  }

  public void testSecondLevelRecursive() throws Exception {
    methodCost3.addMethodCost(1, methodCost2);
    methodCost2.addMethodCost(2, methodCost2);
    methodCost3.link();
    printer.print("", methodCost3, 10, 0);
    assertEquals("c.g.t.A.method3()V [3, 0 / 5, 0]\n"
      + "  line 1: c.g.t.A.method2()V [2, 0 / 2, 0]\n", out.toString());
  }

  public void testAddOneClassCostThenPrintIt() throws Exception {
    ClassInfo classInfo0 = new ClassInfo("FAKE_classInfo0", false, null, null);
    ClassCost classCost0 = new ClassCost(classInfo0, new ArrayList<MethodCost>());
    printer.addClassCostToPrint(classCost0);
    printer.printClassCosts();
    assertEquals("\nTestability cost for FAKE_classInfo0 [ 0 TCC, 0 TGC ]\n",
        out.toString());
  }

  public void testAddSeveralClassCostsAndPrintThem() throws Exception {
    ClassInfo classInfo0 = new ClassInfo("FAKE_classInfo0", false, null, null);
    ClassInfo classInfo1 = new ClassInfo("FAKE_classInfo1", false, null, null);
    ClassInfo classInfo2 = new ClassInfo("FAKE_classInfo2", false, null, null);
    ClassCost classCost0 = new ClassCost(classInfo0, new ArrayList<MethodCost>());
    ClassCost classCost1 = new ClassCost(classInfo1, new ArrayList<MethodCost>());
    ClassCost classCost2 = new ClassCost(classInfo2, new ArrayList<MethodCost>());
    printer.addClassCostToPrint(classCost0);
    printer.addClassCostToPrint(classCost1);
    printer.addClassCostToPrint(classCost2);
    printer.printClassCosts();
    assertEquals("\nTestability cost for FAKE_classInfo0 [ 0 TCC, 0 TGC ]\n" +
        "\nTestability cost for FAKE_classInfo1 [ 0 TCC, 0 TGC ]\n" +
        "\nTestability cost for FAKE_classInfo2 [ 0 TCC, 0 TGC ]\n",
        out.toString());
  }

  public void testAddSeveralClassCostsAndPrintThemInDescendingCostOrder()
      throws Exception {
    ClassInfo classInfo0 = new ClassInfo("FAKE_classInfo0", false, null, null);
    ClassInfo classInfo1 = new ClassInfo("FAKE_classInfo1", false, null, null);
    ClassInfo classInfo2 = new ClassInfo("FAKE_classInfo2", false, null, null);
    List<MethodCost> methodCosts1 = new ArrayList<MethodCost>();
    methodCosts1.add(methodCost1);
    methodCost1.link();
    List<MethodCost> methodCosts2 = new ArrayList<MethodCost>();
    methodCosts2.add(methodCost2);
    methodCost2.link();
    ClassCost classCost0 = new ClassCost(classInfo0, new ArrayList<MethodCost>());
    ClassCost classCost1 = new ClassCost(classInfo1, methodCosts1);
    ClassCost classCost2 = new ClassCost(classInfo2, methodCosts2);
    printer.addClassCostToPrint(classCost0);
    printer.addClassCostToPrint(classCost1);
    printer.addClassCostToPrint(classCost2);
    printer.printClassCosts();
    assertEquals("\nTestability cost for FAKE_classInfo2 [ 2 TCC, 0 TGC ]\n" +
    		"  c.g.t.A.method2()V [2, 0 / 2, 0]\n" +
        "\nTestability cost for FAKE_classInfo1 [ 1 TCC, 0 TGC ]\n" +
        "  c.g.t.A.method1()V [1, 0 / 1, 0]\n" +
        "\nTestability cost for FAKE_classInfo0 [ 0 TCC, 0 TGC ]\n",
        out.toString());
  }

}
