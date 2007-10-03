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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.test.metric.asm.Visibility;


public class ClassInfo {

	private final Map<String, MethodInfo> methods = new HashMap<String, MethodInfo>();
	private final Map<String, FieldInfo> fields = new HashMap<String, FieldInfo>();
	private final String name;

	public ClassInfo(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public MethodInfo getMethod(String methodName) {
		MethodInfo methodInfo = methods.get(methodName);
		if (methodInfo == null) {
			throw new MethodNotFoundException(this, methodName);
		}
		return methodInfo;
	}

	public void addMethod(MethodInfo methodInfo) {
		methods.put(methodInfo.getNameDesc(), methodInfo);
	}
	
	@Override
	public String toString() {
		return name;
	}

	public FieldInfo getField(String fieldName) {
		FieldInfo fieldInfo = fields.get(fieldName);
		if (fieldInfo == null) {
			throw new FieldNotFoundException(this, fieldName);
		}
		return fieldInfo;
	}

	public void addField(FieldInfo fieldInfo) {
		fields.put(fieldInfo.getName(), fieldInfo);
	}

	public Collection<MethodInfo> getMethods() {
		return methods.values();
	}

	public Collection<FieldInfo> getFields() {
		return fields.values();
	}

	public void applyInjectability(InjectabilityContext context) {
		for (MethodInfo method : methods.values()) {
			if (method.isConstructor() && method.getVisibility() != Visibility.PRIVATE) {
				method.applyInjectability(context);
			}
		}
	}

	public void seedInjectability(InjectabilityContext context) {
		for (MethodInfo method : methods.values()) {
			if (method.getVisibility() != Visibility.PRIVATE) {
				for (ParameterInfo parameter : method.getParameters()) {
					context.setInjectable(parameter);
				}
			}
		}
	}

}
