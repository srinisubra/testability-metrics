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

public enum Type {
	VOID("void", 'V', Void.class),
	BYTE("byte", 'B', Byte.class),
	SHORT("short", 'S', Short.class),
	INT("int", 'I', Integer.class),
	BOOLEAN("boolean", 'Z', Boolean.class),
	CHAR("char", 'C', Character.class),
	LONG("long", 'J', Long.class),
	DOUBLE("double", 'D', Double.class),
	FLOAT("float", 'F', Float.class),
	ADDRESS("object", 'L', Object.class);
	
	private final String name;
	private final char code;
	private final Class<?> javaClass;

	Type(String name, char code, Class<?> javaClass) {
		this.name = name;
		this.code = code;
		this.javaClass = javaClass;
	}

	@Override
	public String toString() {
		return name;
	}
	
	public char getCode() {
		return code;
	}
	
	public boolean isDouble() {
		return this == DOUBLE || this == LONG;
	}
	
	public static Type fromCode(String code) {
		if (code.startsWith("[")) {
			return Type.ADDRESS;
		}
		return fromCode(code.charAt(0));
	}
	
	public static Type fromCode(char code) {
		for (Type type : Type.values()) {
			if (type.getCode() == code) {
				return type;
			}
		}
		throw new IllegalArgumentException("Unknown type: " + code);
	}

	public static Type fromClass(Class<?> clazz) {
		for (Type type : Type.values()) {
			if (type.javaClass == clazz) {
				return type;
			}
		}
		return ADDRESS;
	}

}
