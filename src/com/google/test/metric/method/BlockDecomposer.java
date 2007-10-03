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

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Label;

import com.google.test.metric.Variable;

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
 * @author mhevery
 */
public class BlockDecomposer {

	private int nextBlockId = 0;
	private Block mainBlock = newBlock();
	private Map<Label, Block> lookAheadBlocks = new HashMap<Label, Block>();
	private Block currentBlock = mainBlock;
	private int currentLineNumber;

	public Block getMainBlock() {
		return mainBlock;
	}

	public void unconditionalGoto(Label label) {
		Block lookaheadBlock = newBlock();
		lookAheadBlocks.put(label, lookaheadBlock);
		currentBlock.addNextBlock(lookaheadBlock);
		currentBlock = null;
	}

	public void conditionalGoto(Label label) {
		currentBlock.pop(); // If condition operand;
		Block nextBlock = newBlock();
		currentBlock.addNextBlock(newLookaaheadBlock(label));
		currentBlock.addNextBlock(nextBlock);
		currentBlock = nextBlock;
	}

	private Block newLookaaheadBlock(Label label) {
		Block lookaheadBlock = lookAheadBlocks.get(label);
		if (lookaheadBlock == null) {
			lookaheadBlock = new Block(-1);
			lookAheadBlocks.put(label, lookaheadBlock);
		}
		return lookaheadBlock;
	}

	public void tryCatchBlock(Label start, Label end, Label handler) {
		Block startBlock = newLookaaheadBlock(start);
		Block handlerBlock = newLookaaheadBlock(handler);
		startBlock.addNextBlock(handlerBlock);
		startBlock.addNextBlock(newLookaaheadBlock(end));
		handlerBlock.push(-1, new Constant(Throwable.class.getName()));
	}

	private Block newBlock() {
		return new Block(nextBlockId++);
	}

	public void tableSwitch(Label dflt, Label[] labels) {
		for (Label caseLabel : labels) {
			currentBlock.addNextBlock(newLookaaheadBlock(caseLabel));
		}
		currentBlock.addNextBlock(newLookaaheadBlock(dflt));
		currentBlock = null;
	}

	public void pushVariable(Variable var) {
		currentBlock.push(currentLineNumber, var);
	}

	public void popVariable(Variable variable) {
		currentBlock.pop(currentLineNumber, variable);
	}

	public void dup() {
		currentBlock.dup(currentLineNumber);
	}

	public void label(Label label) {
		if (lookAheadBlocks.containsKey(label)) {
			Block nextBlock = lookAheadBlocks.get(label);
			nextBlock.setId(nextBlockId++);
			if (currentBlock != null) {
				currentBlock.addNextBlock(nextBlock);
			}
			currentBlock = nextBlock;
		}
	}

	public void lineNumber(int line, Label label) {
		currentLineNumber = line;
	}

	public void methodInvokation(int opcode, String owner, String name,
			String desc) {
		currentBlock.invoke(currentLineNumber, opcode, owner, name, desc);
	}

	public void done() {
		mainBlock.compact();
	}

}
