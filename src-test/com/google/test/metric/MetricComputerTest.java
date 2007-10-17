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

import junit.framework.TestCase;

public class MetricComputerTest extends TestCase {

	private ClassRepository repo = new ClassRepository();
	private MetricComputer computer = new MetricComputer(repo);

	public static class Medium {
		public Medium() {
			statiCost1();
			cost2();
		}

		/**
		 * I cost 1
		 */
		public static int statiCost1() {
			int i = 0;
			return i > 0 ? 1 : 2;
		}

		/**
		 * I cost 2, but I am instance method so I can be overridden. so my cost
		 * may be avoided in most cases.
		 */
		public int cost2() {
			int i = 0;
			return i > 0 ? i > 1 ? 1 : 2 : 2;
		}

		/**
		 * I am instance method hence you will have to add the cost of
		 * constructor to me. (by myself I cost 4)
		 */
		public Object testMethod4() {
			int i = 0;
			i = i > 0 ? 1 : 2;
			i = i > 0 ? 1 : 2;
			i = i > 0 ? 1 : 2;
			i = i > 0 ? 1 : 2;
			return new Object();
		}
	}

	public void testMediumCost1() throws Exception {
		MethodInfo method = repo.getClass(Medium.class).getMethod(
				"statiCost1()I");
		assertFalse(method.canOverride());
		MethodCost cost = computer.compute(Medium.class, "statiCost1()I");
		assertEquals(1l, cost.getComplexity());
	}

	/**
	 * Since cost2 is called twice, once by our test and once by constructor we
	 * don't want to add it twice. But the constructor adds 1 so total cost is
	 * 3.
	 */
	public void testMediumCost2() throws Exception {
		MethodInfo method = repo.getClass(Medium.class).getMethod("cost2()I");
		assertTrue(method.canOverride());
		MethodCost cost = computer.compute(Medium.class, "cost2()I");
		assertEquals(3l, cost.getComplexity());
	}

	/**
	 * Cost of the constructor needs to add the cost of the static method it
	 * calls as it can not be overridden but not the cost of the instance
	 * method.
	 */
	public void testMediumInit() throws Exception {
		MethodInfo method = repo.getClass(Medium.class).getMethod("<init>()V");
		assertFalse(method.canOverride());
		MethodCost cost = computer.compute(Medium.class, "<init>()V");
		assertEquals(1l, cost.getComplexity());
	}

	/**
	 * Method4 is cost of 4 by itself, but one has the add the cost of
	 * constructor since it is an instance method. The constructor is 0 but
	 * calls to methods: cost1 method is static and can not be intercepted hence
	 * it has to be added. cost2 method is instance and can be overridden hence
	 * we don't add that cost.
	 */
	public void testMediumMethod4() throws Exception {
		MethodCost cost = computer.compute(Medium.class,
				"testMethod4()Ljava/lang/Object;");
		assertEquals(5l, cost.getComplexity());
	}

	public static class Node {
		public String cost1() {
			int a = 0;
			return a == 2 ? "" : null;
		}
	}

	public static class Tree {
		private Node subTitle; // non-injectable
		public Node title = new Node(); // injectable (only after constructor)

		public Tree() {
		}

		public String titleLength() {
			return title.cost1();
		}

		public String subTitleLength() {
			return subTitle.cost1();
		}

		public String veryExpensive() {
			return "".toLowerCase();
		}
	}

	public void testTree() throws Exception {
		MethodCost cost = computer.compute(Tree.class, "<init>()V");
		assertEquals(0l, cost.getComplexity());
	}

	public void testTreeTitleLength() throws Exception {
		MethodCost cost = computer.compute(Tree.class,
				"titleLength()Ljava/lang/String;");
		assertEquals(0l, cost.getComplexity());
	}

	public void testTreeSubTitleLength() throws Exception {
		MethodCost cost = computer.compute(Tree.class,
				"subTitleLength()Ljava/lang/String;");
		assertEquals(1l, cost.getComplexity());
	}

	public void testVeryExpensive() throws Exception {
		MethodCost cost = computer.compute(Tree.class,
				"veryExpensive()Ljava/lang/String;");
		assertTrue(100l < cost.getComplexity());
	}

}
