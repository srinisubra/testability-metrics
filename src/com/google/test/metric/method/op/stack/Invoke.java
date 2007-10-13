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

import java.util.Collections;
import java.util.List;

import com.google.test.metric.Variable;
import com.google.test.metric.method.Constant;
import com.google.test.metric.method.op.turing.MethodInvokation;
import com.google.test.metric.method.op.turing.Operation;

public class Invoke extends StackOperation {

	private final String clazz;
	private final String name;
	private final String signature;
	private final int parameterCount;
	private final boolean isStatic;
	private final String returnType;

	public Invoke(int lineNumber, String clazz, String name, String signature,
			int parameterCount, boolean isStatic, String returnType) {
		super(lineNumber);
		this.clazz = clazz;
		this.name = name;
		this.signature = signature;
		this.parameterCount = parameterCount;
		this.isStatic = isStatic;
		this.returnType = returnType;
	}

	@Override
	public int getOperatorCount() {
		int instanceOffset = isStatic ? 0 : 1;
		return parameterCount + instanceOffset;
	}

	@Override
	public List<Variable> apply(List<Variable> input) {
		if (returnType == null) {
			return Collections.emptyList();
		} else {
			return list(new Constant("?", Object.class));
		}
	}

	@Override
	public Operation toOperation(List<Variable> input) {
		Variable methodThis = isStatic ? null: input.remove(0);
		return new MethodInvokation(lineNumber, clazz, name, signature,
				methodThis, input);
	}
	
	@Override
	public String toString() {
		return "invoke " + clazz + "." + name + signature 
			+ (returnType == null ?  "" : " : " + returnType);
	}

}
