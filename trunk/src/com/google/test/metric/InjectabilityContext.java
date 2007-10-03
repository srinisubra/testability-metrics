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
import java.util.HashSet;
import java.util.Set;

public class InjectabilityContext {

	private Set<Variable> injectables = new HashSet<Variable>();
	
	public boolean isInjectable(Variable var) {
		return injectables.contains(var);
	}

	public void setInjectable(Variable var) {
		injectables.add(var);
	}

	public Collection<Variable> getInjectables() {
		return injectables;
	}

}
