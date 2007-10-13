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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.test.metric.Variable;
import com.google.test.metric.method.op.turing.Operation;

public abstract class StackOperation {
	
	protected final int lineNumber;
	
	public StackOperation(int lineNumber) {
		this.lineNumber = lineNumber;
	}

	public List<Variable> apply(List<Variable> input){
		return Collections.emptyList(); 
	}

	public int getOperatorCount(){
		return 0;
	}

	public Operation toOperation(List<Variable> input) {
		return null;
	}

	protected List<Variable> list(Variable...vars) {
		return Arrays.asList(vars);
	}
}