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

public class TestabilityMetric extends TestCase {

	private ClassRepository repo = new ClassRepository();
	private InjectabilityContext context = new InjectabilityContext(repo);

	private void assertCost(long cost, Class<?> clazz, String methodName) {
		ClassInfo classInfo = repo.getClass(clazz);
		MethodInfo method = classInfo.getMethod(methodName);
		if (method.canOverride()) {
			classInfo.getMethod("<init>()V").computeMetric(context);
		}
		method.computeMetric(context);
		assertEquals(cost, context.getTotalCost());
	}

	private static class Simple {
		public static void staticMethod() {
		}

		public void instanceMethod() {
		}

		public static int ifMethod() {
			int i = 0;
			if (i > 2)
				return 1;
			else
				return 2;
		}

		public static int loopMethod() {
			int i = 0;
			while (i < 10) {
				i++;
			}
			return 1;
		}
	}

	public void testSimpleInit() throws Exception {
		assertCost(0l, Simple.class, "<init>()V");
	}

	public void testSimpleStaticMethod() throws Exception {
		assertCost(0l, Simple.class, "staticMethod()V");
	}

	public void testSimpleInstanceMethod() throws Exception {
		assertCost(0l, Simple.class, "instanceMethod()V");
	}

	public void testSimpleIfMethod() throws Exception {
		assertCost(1l, Simple.class, "ifMethod()I");
	}

	public void testSimpleLoopMethod() throws Exception {
		assertCost(1l, Simple.class, "loopMethod()I");
	}

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
		 * I cost 2, but I am instance method so I can be overridden.
		 * so my cost may be avoided in most cases.
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
		MethodInfo method = repo.getClass(Medium.class).getMethod("statiCost1()I");
		assertFalse(method.canOverride());
		assertCost(1l, Medium.class, "statiCost1()I");
	}

	/**
	 * Since cost2 is called twice, once by our test and once by constructor
	 * we don't want to add it twice. But the constructor adds 1 so total cost
	 * is 3.
	 */
	public void testMediumCost2() throws Exception {
		MethodInfo method = repo.getClass(Medium.class).getMethod("cost2()I");
		assertTrue(method.canOverride());
		assertCost(3l, Medium.class, "cost2()I");
	}

	/**
	 * Cost of the constructor needs to add the cost of the static method it
	 * calls as it can not be overridden but not the cost of the instance
	 * method.
	 */
	public void testMediumInit() throws Exception {
		MethodInfo method = repo.getClass(Medium.class).getMethod("<init>()V");
		assertFalse(method.canOverride());
		assertCost(1l, Medium.class, "<init>()V");
	}
	
	/**
	 * Method4 is cost of 4 by itself, but one has the add the cost of 
	 * constructor since it is an instance method. The constructor is 0 but
	 * calls to methods: cost1 method is static and can not be intercepted
	 * hence it has to be added. cost2 method is instance and can be overridden
	 * hence we don't add that cost.
	 */
	public void testMediumMethod4() throws Exception {
		assertCost(5l, Medium.class, "testMethod4()Ljava/lang/Object;");
	}
	
	public static class Node {
	}
	public static class Tree {
		private String subTitle; // non-injectable
		public String title = "Title"; // injectable (only after constructor)
		public Tree() {
		}
		
		public int titleLength() {
			return title.length();
		}
		public int subTitleLength() {
			return subTitle.length();
		}
	}
	
	public void testTree() throws Exception {
		assertCost(0l, Tree.class, "<init>()V");
	}
	
	public void testTreeTitleLength() throws Exception {
		assertCost(10l, Tree.class, "titleLength()I");
	}

}
