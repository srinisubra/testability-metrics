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
package com.google.test.metric.collection;

import static java.util.Arrays.asList;

import java.util.List;

import junit.framework.TestCase;

public class KeyedMultiStackTest extends TestCase {

	private final class LoggingClosure extends PopClosure<String, Integer> {
		public void pop(String key, List<Integer> value) {
			log += value;
		}
	}

	KeyedMultiStack<String, Integer> stack = new KeyedMultiStack<String, Integer>(
			"");
	String log = "";

	public void testBasicOperationsOnSingleDimension() throws Exception {
		stack.push("", 0);
		stack.pop("", 1, new PopClosure<String, Integer>() {
			public void pop(String key, List<Integer> value) {
				assertEquals("", key);
				assertEquals(1, value.size());
				assertEquals(new Integer(0), value.get(0));
				log += value.get(0);
			}
		});
		assertEquals("0", log);
	}

	public void testPushPushPopOnSplit() throws Exception {
		stack.push("", 0);
		stack.split("", asList("a", "b"));
		stack.push("a", 1);
		stack.push("b", 2);
		stack.pop("a", 2, new LoggingClosure());
		stack.pop("b", 2, new LoggingClosure());
		assertEquals("[0, 1][0, 2]", log);
	}

	public void testPushSplitPushJoinPOP() throws Exception {
		stack.push("", 0);
		stack.split("", asList("a", "b"));
		stack.push("a", 1);
		stack.push("b", 2);
		stack.join(asList("a", "b"), "c");
		stack.push("c", 3);
		stack.pop("c", 3, new LoggingClosure());
		assertEquals("[0, 1, 3][0, 2, 3]", log);
	}
	
	public void testSplitAndJoinShouldCollapsMultipleStacksIfTheyAreOfSameContent() throws Exception {
		stack.push("", 0);
		stack.split("", asList("a", "b"));
		stack.join(asList("a", "b"), "");
		stack.push("", 1);
		stack.pop("", 2, new LoggingClosure());
		assertEquals("[0, 1]", log);
	}
	
	public void testConcurentPushInPopClosure() throws Exception {
		stack.push("", 0);
		stack.push("", 1);
		stack.pop("", 1, new PopClosure<String, Integer>(){
			@Override
			public void pop(String key, Integer value) {
				stack.push(key, value + 10);
			}
		});
		stack.pop("", 2, new LoggingClosure());
		assertEquals("[0, 11]", log);
	}
	
	public void testPopTooMuch() throws Exception {
		try {
			stack.pop("", 1, new LoggingClosure());
			fail();
		} catch (KeyedMultiStack.StackUnderflowException e) {
		}
	}

	public void testUnknownKey() throws Exception {
		try {
			stack.push("X", 0);
			fail();
		} catch (KeyedMultiStack.KeyNotFoundException e) {
		}
	}

	public void testSplitUnknwonNamespace() throws Exception {
		try {
			stack.split("X", asList("A", "B"));
			fail();
		} catch (KeyedMultiStack.KeyNotFoundException e) {
		}
	}

	public void testJoinUnknownNamespace() throws Exception {
		try {
			stack.join(asList("B", "C"), "A");
			fail();
		} catch (KeyedMultiStack.KeyNotFoundException e) {
		}
	}

	public void testUnevenJoin() throws Exception {
		stack.split("", asList("a", "b"));
		stack.push("a", 0);
		try {
			stack.join(asList("a", "b"), "c");
			fail();
		} catch (IllegalStateException e) {
		}
	}
	
	public void testJoinThroughSlipt() throws Exception {
		stack.push("", 0);
		stack.split("", asList("a","b"));
		stack.push("a", 1);
		stack.push("b", 2);
		stack.split("a", asList("join"));
		stack.split("b", asList("join"));
		stack.pop("join", 2, new LoggingClosure());
		assertEquals("[0, 2][0, 1]", log);
	}

}
