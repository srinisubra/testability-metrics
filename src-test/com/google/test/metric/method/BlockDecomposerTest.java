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

import com.google.test.metric.Type;
import com.google.test.metric.method.op.stack.JSR;
import com.google.test.metric.method.op.stack.Load;
import com.google.test.metric.method.op.stack.RetSub;
import com.google.test.metric.method.op.stack.StackOperation;

import junit.framework.TestCase;

import org.objectweb.asm.Label;

import java.util.List;

public class BlockDecomposerTest extends TestCase {

  /**
	 * load 1
	 * jsr mySub
	 * load 2
	 * return
   *
	 * mySub:
	 * load 3
	 * return;
   */
  public void testJSR() throws Exception {
    BlockDecomposer decomposer = new BlockDecomposer();
    decomposer.addOp(load(1));
    Label sub = new Label();
    decomposer.jumpSubroutine(sub, 0);
    decomposer.addOp(load(2));
    decomposer.addOp(new RetSub(0));
    decomposer.label(sub);
    decomposer.addOp(load(3));
    decomposer.addOp(new RetSub(0));

    Block mainBlock = decomposer.getMainBlock();
    assertEquals(0, mainBlock.getNextBlocks().size());
    List<StackOperation> operations = mainBlock.getOperations();
    assertEquals("[load 1{int}, JSR subroutine1, load 2{int}, RETSUB]", operations.toString());
    JSR jsr = (JSR) operations.get(1);
    Block subBlock = jsr.getBlock();
    assertEquals("[load 3{int}, RETSUB]", subBlock.getOperations().toString());
  }

  private Load load(int value) {
    return new Load(0, new Constant(value, Type.INT));
  }

}
