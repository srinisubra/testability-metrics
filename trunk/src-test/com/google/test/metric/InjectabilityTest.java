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

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

public class InjectabilityTest extends TestCase {

	private InjectabilityVerifier verifier = new InjectabilityVerifier();
	private InjectabilityContext context = new InjectabilityContext();
	private ClassRepository repo = new ClassRepository();

	private void assertInjectable(Class<?> clazz) {
		ClassInfo classInfo = repo.getClass(clazz);
		classInfo.seedInjectability(context);
		classInfo.applyInjectability(context);
		verifier.assertInjectable(classInfo, context);
	}

	public void testInitializeInjectabilityForEmptyClass() throws Exception {
		ClassInfo classInfo = new ClassInfo("MyClass");
		InjectabilityContext context = new InjectabilityContext();
		classInfo.applyInjectability(context);
		assertEquals(0, context.getInjectables().size());
	}

	public static class InjectabilityExampleClass {

		private Object a_NI = new Object();
		private Object b_NI;
		private Object c_I;
		private Object d_I = new Object();
		protected Object e_I = new Object();
		Object f_I = new Object();
		public Object g_I = new Object();
		public final Object staticG_NI = new Object();

		public InjectabilityExampleClass(Object paramC_I) {
			Object local_NI = new Object();
			Object localC_I = paramC_I;
			b_NI = local_NI;
			this.c_I = localC_I;
		}

		public void setD(Object paramD_I) {
			this.d_I = paramD_I;
		}

		public static void setE(Object paramE_I) {
		}

		@Override
		public String toString() {
			return "" + a_NI + b_NI + c_I + d_I;
		}

		void immediateIf(Object param1_I) {
			@SuppressWarnings("unused")
			Object x_I = param1_I == null ? null : param1_I;
			@SuppressWarnings("unused")
			Object y_I = param1_I == null ? param1_I : null;
		}
	}

	public void XtestInjectabilityExampleClass() throws Exception {
		ClassInfo classInfo = repo.getClass(InjectabilityExampleClass.class);
		verifier.assertInjectable(classInfo, context);
	}

	private static class EmptyClass {
	}

	public void testEmptyClass() throws Exception {
		ClassInfo classInfo = repo.getClass(EmptyClass.class);
		verifier.assertInjectable(classInfo, context);
	}

	private static class SingleArgConstructor {
		public SingleArgConstructor(Object a_I) {
		}

		SingleArgConstructor(Long b_I) {
		}

		protected SingleArgConstructor(Boolean c_I) {
		}

		@SuppressWarnings("unused")
		private SingleArgConstructor(String d_NI) {
		}

		@SuppressWarnings("unused")
		private void methodA(String e_NI, String e2_NI) {
		}

		public void methodB(String f_I, String f2_I) {
		}
	}

	public void testSingleArgConstructor() throws Exception {
		assertInjectable(SingleArgConstructor.class);
	}

	private class VerifyVerifier {
		public void methodA(Object iShouldBeInjectableButIamMisslabled_NI) {
		}
	}

	public void testVerifierWorks() throws Exception {
		ClassInfo classInfo = repo.getClass(VerifyVerifier.class);
		classInfo.seedInjectability(context);
		try {
			verifier.assertInjectable(classInfo, context);
			fail();
		} catch (AssertionFailedError e) {
		}
	}

	private static class InjectableConstructor {
		@SuppressWarnings("unused")
		private final Object aField_I;

		public InjectableConstructor(Object a_I) {
			this.aField_I = a_I;
		}
	}

	public void testInjectableConstructor() throws Exception {
		assertInjectable(InjectableConstructor.class);
	}

	private static class InjectableConstructorCalls {
		@SuppressWarnings("unused")
		private final Object aField_I;
		@SuppressWarnings("unused")
		private final Object bField_NI;

		private InjectableConstructorCalls(Object a_I, Object b_NI) {
			aField_I = a_I;
			bField_NI = b_NI;
		}

		public InjectableConstructorCalls(Object a_I) {
			this(a_I, new Object());
		}
	}

	public void testInjectableConstructorCalls() throws Exception {
		assertInjectable(InjectableConstructorCalls.class);
	}

}
