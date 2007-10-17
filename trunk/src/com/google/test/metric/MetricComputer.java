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

public class MetricComputer {

	private final ClassRepository classRepository;

	public MetricComputer(ClassRepository classRepository) {
		this.classRepository = classRepository;
	}

	public MethodCost compute(Class<?> clazz, String methodName) {
		ClassInfo classInfo = classRepository.getClass(clazz);
		MethodInfo method = classInfo.getMethod(methodName);
		return compute(classInfo, method);
	}

	private MethodCost compute(ClassInfo classInfo, MethodInfo method) {
		InjectabilityContext context = new InjectabilityContext(classRepository);
		if (method.canOverride()) {
			getPrefferedConstructor(classInfo, context).computeMetric(context);
		}
		for (FieldInfo field : classInfo.getFields()) {
			if (!field.isPrivate()) {
				context.setInjectable(field);
			}
		}
		context.setInjectable(method);
		method.computeMetric(context);
		return new MethodCost(method, context.getTotalCost());
	}

	private MethodInfo getPrefferedConstructor(ClassInfo classInfo,
			InjectabilityContext context) {
		MethodInfo constructor = classInfo.getMethod("<init>()V");
		context.setInjectable(constructor);
		return constructor;
	}

}
