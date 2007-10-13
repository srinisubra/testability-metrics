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
package com.google.test.metric.method.op.stack;

import java.util.List;

import com.google.test.metric.Variable;
import com.google.test.metric.method.Constant;

public class Transform extends StackOperation {

	private final int operatorCount;
	private final Constant constant;

	public Transform(int lineNumber, int popCount, Constant constant) {
		super(lineNumber);
		this.operatorCount = popCount;
		this.constant = constant;
	}
	
	@Override
	public int getOperatorCount() {
		return operatorCount;
	}
	
	@Override
	public List<Variable> apply(List<Variable> input) {
		return constant == null ? list() : list(constant);
	}
	
	@Override
	public String toString() {
		return "pop X " + operatorCount + (constant == null ? "" : " push " + constant); 
	}

}
