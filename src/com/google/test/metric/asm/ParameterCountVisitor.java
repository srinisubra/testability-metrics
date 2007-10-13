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
package com.google.test.metric.asm;

import org.objectweb.asm.signature.SignatureVisitor;


public class ParameterCountVisitor extends NoopSignatureVisitor {

	public class ReturnTypeVisitor extends NoopSignatureVisitor {
		@Override
		public SignatureVisitor visitArrayType() {
			returnType = Object[].class;
			return new NoopSignatureVisitor();
		}
		
		@Override
		public void visitBaseType(char descriptor) {
			switch (descriptor) {
			case 'V':
				break;
			case 'B':
				returnType = byte.class;
				break;
			case 'S':
				returnType = short.class;
				break;
			case 'I':
				returnType = int.class;
				break;
			case 'Z':
				returnType = boolean.class;
				break;
			case 'C':
				returnType = char.class;
				break;
			case 'J':
				returnType = long.class;
				break;
			case 'D':
				returnType = double.class;
				break;
			case 'F':
				returnType = float.class;
				break;
			default:
				throw new IllegalStateException("Unexpected type: " + descriptor);
			}
		}
		
		@Override
		public void visitClassType(String name) {
			returnType = Object.class;
		}
		
	}

	private int count = 0;
	private Class<?> returnType;

	@Override
	public SignatureVisitor visitArrayType() {
		count++;
		return new NoopSignatureVisitor();
	}
	
	@Override
	public void visitBaseType(char descriptor) {
		count++;
	}
	
	@Override
	public void visitClassType(String name) {
		count++;
	}
	
	@Override
	public SignatureVisitor visitParameterType() {
		return this;
	}
	
	public int getParameterCount() {
		return count;
	}
	
	@Override
	public SignatureVisitor visitReturnType() {
		return new ReturnTypeVisitor();
	}
	
	public Class<?> getReturnType() {
		return returnType;
	}

}
