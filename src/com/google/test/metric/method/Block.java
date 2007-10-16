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

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.List;

import com.google.test.metric.method.op.stack.Return;
import com.google.test.metric.method.op.stack.StackOperation;

public class Block implements StackOperations {

	private final String id;
	private final List<Block> previousBlocks = new ArrayList<Block>();
	private List<StackOperation> operations = new ArrayList<StackOperation>();
	private List<Block> nextBlocks = new ArrayList<Block>();
	private boolean isTerminal = false;

	public Block(String id) {
		this.id = id;
	}

	public void addNextBlock(Block nextBlock) {
		if (!nextBlocks.contains(nextBlock)) {
			nextBlocks.add(nextBlock);
			nextBlock.previousBlocks.add(this);
		}
	}

	public void addOp(StackOperation operation) {
		operations.add(operation);
		if (operation instanceof Return) {
			// Return statement must be last one. Freeze the block!
			isTerminal = true;
			nextBlocks = emptyList();
			operations = unmodifiableList(operations);
		}
	}

	public List<StackOperation> getOperations() {
		return operations;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("Block[");
		buf.append(id);
		String sep = " -> ";
		for (Block next : nextBlocks) {
			buf.append(sep);
			buf.append(next.id);
			sep = ", ";
		}
		buf.append("]{\n");
		for (StackOperation operation : operations) {
			buf.append("  ");
			buf.append(operation);
			buf.append("\n");
		}
		buf.append("}");
		return buf.toString();
	}

	public List<Block> getNextBlocks() {
		return unmodifiableList(nextBlocks);
	}

	public boolean isTerminal() {
		return isTerminal ;
	}

	public String getId() {
		return id;
	}

}
