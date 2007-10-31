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
package com.google.test.metric.x;

import org.objectweb.asm.ClassReader;

import junit.framework.TestCase;

import com.google.test.metric.ClassCost;
import com.google.test.metric.ClassRepository;
import com.google.test.metric.MethodCost;
import com.google.test.metric.MetricComputer;

public class SelfTest extends TestCase {

	ClassRepository repo = new ClassRepository();
	MetricComputer computer = new MetricComputer(repo);

	public void testMethodCost() throws Exception {
		System.out.println(computer.compute(MethodCost.class));
	}

	public void testClassCost() throws Exception {
		System.out.println(computer.compute(ClassCost.class));
	}

	public void testClassRepository() throws Exception {
		System.out.println(computer.compute(ClassRepository.class));
	}

	public void testClassReader() throws Exception {
		System.out.println(computer.compute(ClassReader.class));
	}

}
