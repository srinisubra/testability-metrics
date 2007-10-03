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
package com.google.test.metric;

import java.util.ArrayList;
import java.util.List;

import com.google.test.metric.asm.Visibility;
import com.google.test.metric.method.Block;

public class MethodInfo {

	private final ClassInfo classInfo;
	private final String name;
	private final List<ParameterInfo> parameters;
	private final List<LocalVariableInfo> localVariables;
	private final Block block;
	private final String desc;
	private final long cyclomaticComplexity;
	private final Visibility visibility;

	public MethodInfo(ClassInfo classInfo, String methodName, String desc,
			Block block, List<ParameterInfo> parameters,
			List<LocalVariableInfo> localVariables, Visibility visibility,
			long cylomaticComplexity) {
		this.classInfo = classInfo;
		this.name = methodName;
		this.desc = desc;
		this.block = block;
		this.parameters = parameters;
		this.localVariables = localVariables;
		this.cyclomaticComplexity = cylomaticComplexity;
		this.visibility = visibility;
	}

	public String getNameDesc() {
		return name + desc;
	}

	@Override
	public String toString() {
		return classInfo.getName() + "." + getNameDesc();
	}

	public long getNonRecursiveCyclomaticComplexity() {
		return cyclomaticComplexity;
	}

	public String getName() {
		return name;
	}

	public List<ParameterInfo> getParameters() {
		return parameters;
	}

	public List<LocalVariableInfo> getLocalVariables() {
		return localVariables;
	}

	public Block getBlock() {
		return block;
	}

	public boolean isConstructor() {
		return name.equals("<init>");
	}

	public Visibility getVisibility() {
		return visibility;
	}

	public void applyInjectability(InjectabilityContext context) {
		List<Block> blocks = new ArrayList<Block>(10);
		blocks.add(getBlock());
		while (!blocks.isEmpty()) {
			Block block = blocks.remove(0);
			block.applyInjectability(context);
			blocks.addAll(block.getNextBlocks());
		}
	}

}
