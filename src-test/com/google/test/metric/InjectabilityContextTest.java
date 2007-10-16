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

import junit.framework.TestCase;

public class InjectabilityContextTest extends TestCase {

	public void testIsInjectable() throws Exception {
		InjectabilityContext context = new InjectabilityContext(null);
		Variable var = new Variable("", Type.ADDRESS);
		assertFalse(context.isInjectable(var));
		context.setInjectable(var);
		assertTrue(context.isInjectable(var));
		assertEquals(1, context.getInjectables().size());
		assertTrue(context.getInjectables().contains(var));
	}
	
}