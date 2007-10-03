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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassReader;

import com.google.test.metric.asm.ClassInfoBuilderVisitor;

public class ClassRepository {

	private final Map<String, ClassInfo> classes = new HashMap<String, ClassInfo>();
	
	public ClassInfo getClass(Class<?> clazz) {
		return getClass(clazz.getName());
	}

	public ClassInfo getClass(String clazzName) {
		ClassInfo classInfo = classes.get(clazzName);
		if (classInfo == null) {
			classInfo = parseClass(inputStreamForClass(clazzName));
			classes.put(clazzName, classInfo);
		}
		return classInfo;
	}

	private InputStream inputStreamForClass(String clazzName) {
		String resource = clazzName.replace(".", "/") + ".class";
		InputStream classBytes = ClassLoader.getSystemResourceAsStream(resource);
		if (classBytes == null) {
			throw new ClassNotFoundException(clazzName);
		}
		return classBytes;
	}

	private ClassInfo parseClass(InputStream classBytes) {
		try {
			ClassReader classReader = new ClassReader(classBytes);
			ClassInfoBuilderVisitor visitor = new ClassInfoBuilderVisitor();
			classReader.accept(visitor, 0);
			return visitor.getClassInfo();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
