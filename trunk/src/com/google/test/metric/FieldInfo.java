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

public class FieldInfo extends Variable {

	private final ClassInfo classInfo;
	private final boolean isStatic;
	private final boolean isPrivate;

	public FieldInfo(ClassInfo classInfo, String name, boolean isStatic,
			boolean isPrivate) {
		super(name);
		this.classInfo = classInfo;
		this.isStatic = isStatic;
		this.isPrivate = isPrivate;
	}

	@Override
	public String toString() {
		return classInfo + "." + getName();
	}

	public boolean isStatic() {
		return isStatic;
	}

	public boolean isPrivate() {
		return isPrivate;
	}
}
