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
package com.google.test.metric.method;

import java.util.Arrays;
import java.util.List;

import com.google.test.metric.ClassInfo;
import com.google.test.metric.ClassRepository;
import com.google.test.metric.MethodInfo;
import com.google.test.metric.method.op.turing.Operation;

import junit.framework.TestCase;

public class MethodBlockTest extends TestCase {

	public static class Simple {
		@SuppressWarnings("unused")
		private Object a;

		public Simple() {
			super();
			a = new Object();
		}
	}

	private void assertOperations(List<Operation> block, String... operations) {
		String error = "\nExpecting:" + Arrays.toString(operations)
				     + "\n   Actual:" + block;
		assertEquals(error, operations.length, block.size());
		for (int i = 0; i < operations.length; i++) {
			String expecting = operations[i];
			String actual = block.get(i).toString();
			assertEquals(error, expecting, actual);
		}
	}

	public void testConstructor() throws Exception {
		MethodInfo method = getMethod("<init>()V", Simple.class);
		List<Operation> operations = method.getOperations();
		assertOperations(operations, 
				"java.lang.Object.<init>()V",
				"java.lang.Object.<init>()V",
				"com.google.test.metric.method.MethodBlockTest$Simple.a <- new java.lang.Object");
	}

	public static class TryCatchFinally {
		public void method() {
			@SuppressWarnings("unused")
			int b = 1;
			try {
				b = 2;
			} catch (RuntimeException e) {
				b = 3;
			} finally {
				b = 4;
			}
			b = 5;
		}
	}

	public void testTryCatchBlock() throws Exception {
		MethodInfo method = getMethod("method()V", TryCatchFinally.class);
		List<Operation> operations = method.getOperations();

		assertOperations(operations, 
				"b <- 1", 
				// try {
				"b <- 2",
				// } catch (RuntimeException e ) {
				"e <- java.lang.Throwable", 
				"b <- 3",
				// } Finally uncaught {
				"local_3 <- java.lang.Throwable",
				"b <- 4",
				// } Finally caught {
				"b <- 4",
				// } Finally normal {
				"b <- 4",
				// }
				"b <- 5");
	}

	public static class IIF {
		@SuppressWarnings("unused")
		private Object a;

		public void method() {
			int b = 1;
			a = b > 0 ? null : new Object();
			b = 2;
		}
	}

	private MethodInfo getMethod(String methodName, Class<?> clazz) {
		ClassRepository repo = new ClassRepository();
		ClassInfo classInfo = repo.getClass(clazz);
		return classInfo.getMethod(methodName);
	}

	public void testMethodWithIIF() throws Exception {
		Class<IIF> clazz = IIF.class;
		MethodInfo method = getMethod("method()V", clazz);
		assertOperations(method.getOperations(), 
				"b <- 1",
				"java.lang.Object.<init>()V", 
				clazz.getName() + ".a <- null", 
				clazz.getName() + ".a <- new java.lang.Object", 
				"b <- 2");
	}

	public class SwitchTable {
		public void method() {
			int a = 0;
			switch (a) {
			case 1:
				a = 1;
				break;
			case 2:
				a = 2;
				break;
			case 3:
				a = 3;
				break;
			default:
				a = 4;
			}
			a = 5;
		}
	}

	public void testSwitchTable() throws Exception {
		MethodInfo method = getMethod("method()V", SwitchTable.class);
		assertOperations(method.getOperations(), 
				"a <- 0", 
				"a <- 1", 
				"a <- 2",
				"a <- 3",
				"a <- 4", 
				"a <- 5");
	}

	public static class CallMethods {
		private String text = "ABC";
		private static String staticText = "abc";

		public int length() {
			return text.length();
		}

		public static int staticLength() {
			return staticText.length();
		}
	}

	public void testCallMethodsLength() throws Exception {
		MethodInfo method = getMethod("length()I", CallMethods.class);
		assertOperations(method.getOperations(), "java.lang.String.length()I");
	}

	public void testCallMethodsStaticLength() throws Exception {
		MethodInfo method = getMethod("staticLength()I", CallMethods.class);
		assertOperations(method.getOperations(), "java.lang.String.length()I");
	}

}
