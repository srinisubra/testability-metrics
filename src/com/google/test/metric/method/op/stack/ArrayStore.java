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

import com.google.test.metric.Type;
import com.google.test.metric.Variable;
import com.google.test.metric.method.op.turing.Assignment;
import com.google.test.metric.method.op.turing.Operation;

public class ArrayStore extends StackOperation {

	private final Type type;

	public ArrayStore(int lineNumber, Type type) {
		super(lineNumber);
		this.type = type;
	}

	@Override
	public int getOperatorCount() {
		return 2 + (type.isDouble() ? 2 : 1);
	}

	@Override
	public Operation toOperation(List<Variable> input) {
		if (!input.get(0).getType().isObject())
			throw new IllegalStateException();
		return new Assignment(lineNumber, input.get(0), input.get(2));
	}

	@Override
	public String toString() {
		return "arraystore";
	}

}
