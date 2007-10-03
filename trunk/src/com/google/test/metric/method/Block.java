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

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Stack;

import org.objectweb.asm.signature.SignatureReader;

import com.google.test.metric.FieldInfo;
import com.google.test.metric.InjectabilityContext;
import com.google.test.metric.Variable;
import com.google.test.metric.asm.ParameterCountVisitor;

public class Block {

	private static class NoopPopClosure implements PopClosure {
		public void popOperation(Block block, Variable variable, int lineNumber) {
		}
	}

	private static interface PopClosure {

		void popOperation(Block block, Variable variable, int lineNumber);
	}

	private static class Frame {

		private final int lineNumber;
		private final Variable var;

		public Frame(int lineNumber, Variable var) {
			this.lineNumber = lineNumber;
			this.var = var;
		}

		@Override
		public String toString() {
			return var + "[" + lineNumber + "]";
		}

	}

	public static abstract class Operation {

		private final int lineNumber;

		public Operation(int lineNumber) {
			this.lineNumber = lineNumber;
		}

		public int getLineNumber() {
			return lineNumber;
		}

		public abstract void applyInjectability(InjectabilityContext context);
	}

	public static class Assignment extends Operation {

		private final Variable value;
		private final Variable variable;

		public Assignment(int lineNumber, Variable dst, Variable value) {
			super(lineNumber);
			this.value = value;
			this.variable = dst;
		}

		public Variable getVariable() {
			return variable;
		}

		public Variable getValue() {
			return value;
		}

		@Override
		public String toString() {
			return value + " -> " + variable;
		}

		@Override
		public void applyInjectability(InjectabilityContext context) {
			if (context.isInjectable(value)) {
				context.setInjectable(variable);
			}
		}
	}

	public static class MethodInvokation extends Operation {

		private final String name;
		private final String owner;
		private final String desc;

		public MethodInvokation(int lineNumber, String owner, String name, String desc) {
			super(lineNumber);
			this.owner = owner;
			this.name = name;
			this.desc = desc;
		}

		public String getMethodName() {
			return owner + "." + name;
		}

		public String getName() {
			return name;
		}

		public String getOwner() {
			return owner;
		}

		@Override
		public String toString() {
			return getMethodName() + desc;
		}

		@Override
		public void applyInjectability(InjectabilityContext context) {
			
		}

	}

	private List<Block> previousBlocks = new ArrayList<Block>();
	private List<Block> nextBlocks = new ArrayList<Block>();
	private List<Operation> operations = new ArrayList<Operation>();
	private Stack<Frame> stack = new Stack<Frame>();
	private int id;

	public Block(int id) {
		this.id = id;
	}

	public void addNextBlock(Block nextBlock) {
		nextBlocks.add(nextBlock);
		nextBlock.previousBlocks.add(this);
	}

	public void addOperation(Operation operation) {
		operations.add(operation);
	}

	public List<Operation> getOperations() {
		return operations;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("[" + id + "]{\n");
		for (Operation operation : operations) {
			buf.append("   ");
			buf.append(operation);
			buf.append("\n");
		}
		if (!stack.isEmpty()) {
			buf.append("---\n");
		}
		for (Frame frame : stack) {
			buf.append("   ");
			buf.append(frame);
			buf.append("\n");
		}
		buf.append("}");
		return buf.toString();
	}

	public void push(int lineNumber, Variable var) {
		push(new Frame(lineNumber, var));
	}

	private void push(Frame frame) {
		stack.push(frame);
	}

	public void pop(int lineNumber, final Variable dst) {
		/**
		 * If we have items in our stack then just use them, but if we are in a
		 * tertiary expression then we have to use the previous blocks.
		 */
		pop(new PopClosure() {
			public void popOperation(Block block, Variable value, int lineNumber) {
				if (dst instanceof FieldInfo) {
					block.pop(); // remove this of field;
				}
				block.addOperation(new Assignment(lineNumber, dst, value));
			}
		});
	}

	public void dup(int lineNumber) {
		pop(new PopClosure() {
			public void popOperation(Block block, Variable variable,
					int lineNumber) {
				Frame frame = new Frame(lineNumber, variable);
				push(frame);
				push(frame);
			}
		});
	}

	public void invoke(int lineNumber, int opcode, String owner, String name,
			String desc) {
		int operandCount = getOperandCount(desc);
		while (operandCount-- > 0) {
			pop();
		}
		addOperation(new MethodInvokation(lineNumber, owner, name, desc));
	}

	public void pop() {
		pop(new NoopPopClosure());
	}

	private int getOperandCount(String desc) {
		ParameterCountVisitor counter = new ParameterCountVisitor();
		new SignatureReader(desc).accept(counter);
		return counter.getCount();
	}

	public void pop(PopClosure closure) {
		List<Block> blocks = stack.isEmpty() ? previousBlocks : asList(this);
		for (Block block : blocks) {
			try {
				Frame frame = block.stack.pop();
				closure.popOperation(block, frame.var, frame.lineNumber);
			} catch (EmptyStackException e) {
				throw new IllegalStateException(block.toString(), e);
			}
		}
	}

	public void compact() {
	}

	public void setId(int id) {
		this.id = id;
	}

	public List<Block> getNextBlocks() {
		return nextBlocks;
	}

	public void applyInjectability(InjectabilityContext context) {
		for (Operation operation : operations) {
			operation.applyInjectability(context);
		}
	}

}
