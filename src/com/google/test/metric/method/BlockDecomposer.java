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

import static com.google.test.metric.Type.fromClass;
import static com.google.test.metric.Type.fromJava;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Label;

import com.google.test.metric.Type;
import com.google.test.metric.method.op.stack.Load;
import com.google.test.metric.method.op.stack.StackOperation;
import com.google.test.metric.method.op.turing.Operation;

/**
 * This method shows how this class decomposes the bytecodes into the blocks
 * 
 * <pre>
 * public void methodWithIIf() {
 * 	int b = 1;
 * 	a = b &gt; 0 ? null : new Object();
 * 	b = 2;
 * }
 * </pre>
 * 
 * is decomposed into these bytecodes:
 * 
 * <pre>
 *   public void methodWithIIf();
 *   0  iconst_1                      Block0
 *   1  istore_1 [b]                  Block0  1 -&gt; b
 *   2  aload_0 [this]                Block0
 *   3  iload_1 [b]                   Block0
 *   4  ifle 11                       Block0
 * -----------------------------------------------
 *   7  aconst_null                   Block1  null -&gt; a
 *   8  goto 18                       Block1
 * -----------------------------------------------
 *  11  new java.lang.Object [3]      Block2  new ObjecT() -&gt; a
 *  14  dup                           Block2
 *                                    Block2
 *  15  invokespecial java.lang.Object() [10]
 * -----------------------------------------------
 *                                    Block3
 *  18  putfield com.google.test.metric.method.MethodBlockTest$MethodBlocks.a : java.lang.Object [12]
 *  21  iconst_2                      Block3
 *  22  istore_1 [b]                  Block3 2-&gt; b
 *  23  return
 * </pre>
 * 
 * <b>NOTE:</b>
 * <ul>
 * <li> Even thought the " -> a" is common and it is technically in block 3 it
 * needs to be back propagated to block 1 and 2; </li>
 * In other words the assignment belongs to the block from which the L
 * </ul>
 * 
 * <b>How it works:</b>
 * <ul>
 * <li>Ever time you see a conditional IF then create two blocks (1)
 * destination of if as well as (2) next instruction block</li>
 * <li>Non-conditional IF ends the current block and starts a new one.</li>
 * <li>Seeing a label means that we must have a jump to it so we must create a
 * new block. This means that Labels cause a creation a of new block always.
 * Even if we have a forward jump.
 * <li>
 * </ul>
 * 
 * @author misko@google.com <Misko Hevery>
 */
public class BlockDecomposer implements StackOperations {

	private int nextBlockId = 0;
	private final List<Block> blocksInOrder = new LinkedList<Block>();
	private final Block mainBlock = newBlock("");
	private final Map<Label, Block> lookAheadBlocks = new HashMap<Label, Block>();
	private Block currentBlock;

	public BlockDecomposer() {
		setCurrentBlock(mainBlock);
	}

	private void setCurrentBlock(Block block) {
		if (blocksInOrder.contains(block)) {
			throw new IllegalStateException();
		}
		if (block != null) {
			blocksInOrder.add(block);
		}
		currentBlock = block;
	}

	public void unconditionalGoto(Label label) {
		Block lookaheadBlock = newLookAheadBlock("goto_", label);
		currentBlock.addNextBlock(lookaheadBlock);
		setCurrentBlock(null);
	}

	public void conditionalGoto(Label label) {
		Block falseBlock = newBlock("if_false_");
		Block trueBlock = newLookAheadBlock("if_true_", label);
		currentBlock.addNextBlock(falseBlock);
		currentBlock.addNextBlock(trueBlock);
		setCurrentBlock(falseBlock);
	}

	private Block newBlock(String prefix) {
		return new Block("" + (prefix + nextBlockId++));
	}

	private Block newLookAheadBlock(String prefix, Label label) {
		Block lookaheadBlock = lookAheadBlocks.get(label);
		if (lookaheadBlock == null) {
			lookaheadBlock = newBlock(prefix);
			lookAheadBlocks.put(label, lookaheadBlock);
		}
		return lookaheadBlock;
	}

	public void tryCatchBlock(Label start, Label end, Label handler,
			String eType) {
		Block startBlock = newLookAheadBlock("try_", start);
		newLookAheadBlock("try_end_", end);
		Block handlerBlock = newLookAheadBlock("handle_" + eType + "_", handler);
		startBlock.addNextBlock(handlerBlock);
		if (handlerBlock.getOperations().size() == 0) {
			Type type = eType == null ? fromClass(Throwable.class)
					: fromJava(eType);
			handlerBlock.addOp(new Load(-1, new Constant("?", type)));
		}
	}

	public void tableSwitch(Label dflt, Label[] labels) {
		for (Label caseLabel : labels) {
			currentBlock.addNextBlock(newLookAheadBlock("case_", caseLabel));
		}
		currentBlock.addNextBlock(newLookAheadBlock("dflt_", dflt));
		setCurrentBlock(null);
	}

	public void label(Label label) {
		if (lookAheadBlocks.containsKey(label)) {
			Block nextBlock = lookAheadBlocks.get(label);
			if (currentBlock != null && !currentBlock.isTerminal()) {
				currentBlock.addNextBlock(nextBlock);
			}
			setCurrentBlock(nextBlock);
		} else {
			if (currentBlock == null) {
				setCurrentBlock(newBlock(""));
				lookAheadBlocks.put(label, currentBlock);
			} else if (currentBlock.getOperations().size() == 0) {
				lookAheadBlocks.put(label, currentBlock);
			} else {
				Block nextBlock = newBlock("");
				if (!currentBlock.isTerminal()) {
					currentBlock.addNextBlock(nextBlock);
				}
				setCurrentBlock(nextBlock);
				lookAheadBlocks.put(label, currentBlock);
			}
		}
	}

	public void done() {
	}

	public List<Operation> getOperations() {
		return new Stack2Turing(mainBlock).translate();
	}

	public void addOp(StackOperation operation) {
		currentBlock.addOp(operation);
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		List<Block> processed = new LinkedList<Block>();
		for (Block block : blocksInOrder) {
			processed.add(block);
			buf.append(block);
			buf.append("\n");
		}
		return buf.toString();
	}

}
