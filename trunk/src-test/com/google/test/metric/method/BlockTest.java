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

import java.util.List;

import junit.framework.TestCase;

import com.google.test.metric.FieldInfo;
import com.google.test.metric.method.op.stack.GetField;
import com.google.test.metric.method.op.stack.Invoke;
import com.google.test.metric.method.op.stack.Load;
import com.google.test.metric.method.op.stack.PutField;
import com.google.test.metric.method.op.turing.MethodInvokation;
import com.google.test.metric.method.op.turing.Operation;

public class BlockTest extends TestCase {

	public void testBlockToString() throws Exception {
		Block block = new Block("1");
		assertEquals("Block[1]{\n}", block.toString());
		
		block.addOp(new Load(-1, 1));
		assertEquals("Block[1]{\n  push 1\n}", block.toString());
	}
	
	public void testVariableStaticAssignment() throws Exception {
		Block block = new Block("1");
		block.addOp(new Load(-1, 1));
		block.addOp(new PutField(-1, new FieldInfo(null, "abc", true)));
		
		List<Operation> operations = new Stack2Turing(block).translate();
		assertEquals("[null.abc <- 1]", operations.toString());
	}
	
	public void testVariableAssignment() throws Exception {
		Block block = new Block("1");
		block.addOp(new Load(-1, null)); // this
		block.addOp(new Load(-1, 1));
		block.addOp(new PutField(-1, new FieldInfo(null, "abc", false)));
		
		List<Operation> operations = new Stack2Turing(block).translate();
		assertEquals("[null.abc <- 1]", operations.toString());
	}
	
	public void testGetField() throws Exception {
		Block block = new Block("1");
		block.addOp(new GetField(-1, new FieldInfo(null, "src", true)));
		block.addOp(new PutField(-1, new FieldInfo(null, "dst", true)));
		
		List<Operation> operations = new Stack2Turing(block).translate();
		assertEquals("[null.dst <- null.src]", operations.toString());
	}
	
	public void testMethodInvocation() throws Exception {
		Block block = new Block("1");
		block.addOp(new Load(-1, "methodThis")); // this
		block.addOp(new GetField(-1, new FieldInfo(null, "p1", true)));
		block.addOp(new GetField(-1, new FieldInfo(null, "p2", true)));
		block.addOp(new Invoke(-1, null, "methodA", "(II)I", 2, false, "returnType"));
		block.addOp(new PutField(-1, new FieldInfo(null, "dst", true)));
		
		List<Operation> operations = new Stack2Turing(block).translate();
		assertEquals("[null.methodA(II)I, null.dst <- ?]", operations.toString());
	}
	
	public void testDiamondBlockArrangment() throws Exception {
		Block root = new Block("root");
		Block branchA = new Block("branchA");
		Block branchB = new Block("branchB");
		Block joined = new Block("joined");
		root.addNextBlock(branchA);
		root.addNextBlock(branchB);
		branchA.addNextBlock(joined);
		branchB.addNextBlock(joined);
		
		root.addOp(new Load(-1, "this"));
		root.addOp(new Load(-1, "root"));
		branchA.addOp(new Load(-1, "A"));
		branchB.addOp(new Load(-1, "B"));
		joined.addOp(new Load(-1, "joined"));
		joined.addOp(new Invoke(-1, null, "m", "(III)V", 3, false, null));
		
		List<Operation> operations = new Stack2Turing(root).translate();
		assertEquals(2, operations.size());
		MethodInvokation m1 = (MethodInvokation) operations.get(0);
		MethodInvokation m2 = (MethodInvokation) operations.get(1);
		
		assertEquals("[root, B, joined]", m1.getParameters().toString());
		assertEquals("this", m1.getMethodThis().toString());
		assertEquals("[root, A, joined]", m2.getParameters().toString());
		assertEquals("this", m2.getMethodThis().toString());
	}

}
