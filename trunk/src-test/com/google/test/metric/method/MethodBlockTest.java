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
import com.google.test.metric.method.Block.Assignment;
import com.google.test.metric.method.Block.MethodInvokation;
import com.google.test.metric.method.Block.Operation;

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

	private void assertOperations(Block block, String... operations) {
		String error = "\nExpecting: " + Arrays.toString(operations)
				+ "\nActual:" + block;
		assertEquals(error, operations.length, block.getOperations().size());
		for (int i = 0; i < operations.length; i++) {
			String expecting = operations[i];
			String actual = block.getOperations().get(i).toString();
			assertEquals(error, expecting, actual);
		}
	}

	public void testConstructor() throws Exception {
		MethodInfo method = getMethod("<init>()V", Simple.class);
		Block block = method.getBlock();
		List<Operation> operations = block.getOperations();
		assertEquals(3, operations.size());

		MethodInvokation invokeSuper = (MethodInvokation) operations.get(0);
		assertTrue(invokeSuper.getLineNumber() > 1);
		assertEquals("java.lang.Object.<init>()V", invokeSuper.toString());

		MethodInvokation invokeNew = (MethodInvokation) operations.get(1);
		assertEquals(invokeSuper.getLineNumber() + 1, invokeNew.getLineNumber());
		assertEquals("java.lang.Object.<init>()V", invokeNew.toString());

		Assignment assignment = (Assignment) operations.get(2);
		assertEquals(invokeSuper.getLineNumber() + 1, assignment
				.getLineNumber());
		assertEquals("a", assignment.getVariable().getName());
		assertEquals("<new java/lang/Object>", assignment.getValue().toString());
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
		Block preBlock = method.getBlock();
		Block tryBlock = preBlock.getNextBlocks().get(0);
		Block catchBlock = tryBlock.getNextBlocks().get(0);
		Block finallyBlock = catchBlock.getNextBlocks().get(0);
		Block postBlock = finallyBlock.getNextBlocks().get(0);

		assertOperations(preBlock, "<1> -> b");
		assertOperations(tryBlock, "<2> -> b");
		assertOperations(catchBlock, "<java.lang.Throwable> -> e", "<3> -> b");
		assertOperations(finallyBlock, "<4> -> b");
		assertOperations(postBlock, "<5> -> b");
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
		Block preBlock = method.getBlock();
		Block falseBlock = preBlock.getNextBlocks().get(0);
		Block trueBlock = preBlock.getNextBlocks().get(1);
		Block postBlock = falseBlock.getNextBlocks().get(0);
		assertSame(postBlock, trueBlock.getNextBlocks().get(0));

		assertOperations(preBlock, "<1> -> b");
		assertOperations(falseBlock, "java.lang.Object.<init>()V",
				"<new java/lang/Object> -> " + clazz.getName() + ".a");

		assertOperations(trueBlock, "<null> -> " + clazz.getName() + ".a");
		assertOperations(postBlock, "<2> -> b");
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
		Block preBlock = method.getBlock();
		assertOperations(preBlock, "<0> -> a");

		List<Block> switchBlocks = preBlock.getNextBlocks();
		assertOperations(switchBlocks.get(0), "<1> -> a");
		assertOperations(switchBlocks.get(1), "<2> -> a");
		assertOperations(switchBlocks.get(2), "<3> -> a");
		
		Block defaultBlock = switchBlocks.get(3);
		assertOperations(defaultBlock, "<4> -> a");
		
		Block postBlock = defaultBlock.getNextBlocks().get(0);
		assertOperations(postBlock, "<5> -> a");
	}

}
