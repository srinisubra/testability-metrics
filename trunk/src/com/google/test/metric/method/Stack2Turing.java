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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.google.test.metric.Variable;
import com.google.test.metric.collection.KeyedMultiStack;
import com.google.test.metric.collection.PopClosure;
import com.google.test.metric.method.op.stack.StackOperation;
import com.google.test.metric.method.op.turing.Operation;

public class Stack2Turing {

	private final Block rootBlock;
	private final LinkedList<Operation> operations = new LinkedList<Operation>();
	public KeyedMultiStack<Block, Variable> stack = new KeyedMultiStack<Block, Variable>();

	public Stack2Turing(Block block) {
		this.rootBlock = block;
	}

	public List<Operation> translate() {
		List<Block> blocks = new LinkedList<Block>();
		List<Block> processed = new LinkedList<Block>();
		stack.init(rootBlock);
		blocks.add(rootBlock);
		while (!blocks.isEmpty()) {
			Block block = blocks.remove(0);
			processed.add(block);
			for (StackOperation operation : block.getOperations()) {
				translateStackOperation(block, operation);
			}
			List<Block> nextBlocks = new LinkedList<Block>(block
					.getNextBlocks());
			nextBlocks.removeAll(processed); // Don't visit already visited
			// blocks
			if (nextBlocks.size() > 0) {
				stack.split(block, nextBlocks);
			}
			blocks.addAll(nextBlocks);
			blocks.removeAll(processed);
		}
		// It appears that when exceptions are involved a method might have
		// paths where stacks are not emptied. So we can't assert this.
		// Verdict is still out.
		// stack.assertEmpty();
		return operations;
	}

	private void translateStackOperation(Block block,
			final StackOperation operation) {
		int consumes = operation.getOperatorCount();
		stack.pop(block, consumes, new PopClosure<Block, Variable>() {
			@Override
			public void pop(Block key, List<Variable> input) {
				List<Variable> variables = operation.apply(input);
				assertValid(variables);
				for (Variable output : variables) {
					stack.push(key, output);
				}
				Operation turingOp = operation.toOperation(input);
				if (turingOp != null) {
					operations.add(turingOp);
				}
			}
		});
	}

	protected void assertValid(List<Variable> variables) {
		Iterator<Variable> iter = variables.iterator();
		while (iter.hasNext()) {
			final Variable variable = iter.next();
			if (variable.getType().isDouble()) {
				Variable varNext = iter.hasNext() ? iter.next() : null;
				if (variable != varNext) {
					throw new IllegalStateException("Variable list '"
							+ variables + "' contanins variable '" + variable
							+ "' which is a double but the next "
							+ "variable in the list is not a duplicate.");
				}
			}
		}
	}

}
