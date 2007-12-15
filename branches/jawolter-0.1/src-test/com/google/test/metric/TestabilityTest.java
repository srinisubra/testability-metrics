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

import com.google.classpath.DirectoryClasspathRootTest;
import junit.framework.TestCase;
import org.kohsuke.args4j.CmdLineException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class TestabilityTest extends TestCase {

  public void testComputeCostSingleClass() throws Exception {
    String classUnderTest = com.google.test.metric.Testability.class.getName();
    Testability testability = new Testability();
    ClassCost classCost = testability.computeCost(classUnderTest);
    assertNotNull(classCost);
    assertTrue(classCost.toString().length() > 0);
  }

  public void testParseNoArgs() throws IOException {
    Testability testability = new Testability();
    StringWriter err = new StringWriter();
    try {
      testability.parseArgs(err);
      fail("Should have thrown a CmdLineException exception");
    } catch (CmdLineException expected) {
      // expecting this
    }
    assertTrue(err.toString().indexOf("No argument was given") > -1);
  }

  public void testParseClasspathAndSingleClass() throws Exception {
    Testability testability = new Testability();
    StringWriter err = new StringWriter();
    testability.parseArgs(err, "-cp", "not/default/path", "com.google.TestClass");

    assertEquals("", err.toString());
    assertEquals("not/default/path", testability.classpath);
    List<String> expectedArgs = new ArrayList<String>();
    expectedArgs.add("com.google.TestClass");
    assertNotNull(testability.packagesToAnalyzeFilter);
    assertEquals(expectedArgs, testability.packagesToAnalyzeFilter);
  }


  public void testMainWithJarFileNoClasspath() throws Exception {
    PrintStream origOut = System.out;
    PrintStream origErr = System.err;
    WatchedOutputStream tempOut = null;
    WatchedOutputStream tempErr = null;
    try {
      tempOut = new WatchedOutputStream();
      tempErr = new WatchedOutputStream();
      System.setOut(new PrintStream(tempOut));
      System.setErr(new PrintStream(tempErr));
      Testability.main("junit.runner", "-cp");
    } finally {
      System.setOut(origOut);
      System.setErr(origErr);
      assertNotNull(tempOut);
      assertNotNull(tempErr);
      assertEquals("", tempOut.getOutput());
      /* we expect the error to say something about proper usage of the
arguments. The -cp needs a value */
      assertTrue(tempErr.getOutput().length() > 0);
    }
  }


  public void testMainWithJarFile() throws Exception {
    WatchedOutputStream out = new WatchedOutputStream();
    Testability testability = new Testability();
    testability.doMain(new String[] {"", "-cp", "lib/junit.jar"},
      new PrintStream(out));
//    System.out.println(out.getOutput());
    assertTrue("output too short, expected parsing lots of files correctly",
      out.getOutput().length() > 1000);
    assertEquals(1, testability.packagesToAnalyzeFilter.size());
    assertEquals("", testability.packagesToAnalyzeFilter.get(0));
    assertTrue(testability.classpath.endsWith("lib/junit.jar"));
   }

  public void testMainWithJarFileAndClassesNotInClasspath() throws Exception {
   /* root2/ contains some classes from this project, but not all. There are
    * many references to classes that will not be in this test's -cp classpath.
    * We are testing that when the ClassRepository encounters a
    * ClassNotFoundException, it continues nicely and prints the values for
    * the classes that it _does_ find. */
    Testability.main("", "-cp",
      DirectoryClasspathRootTest.ROOT_2_CLASSES_FOR_TEST);
  }

  public void testMainWithJarFileAndFilter() throws Exception {
    PrintStream out = System.out;
    try {
//  System.setOut(new PrintStream(new IgnoreOutputStream()));
      Testability.main("junit.runner", "-cp", "lib/junit.jar");
      Testability.main("junit.swingui.ProgressBar", "-cp", "lib/junit.jar");
    } finally {
      System.setOut(out);
    }
  }

  public void testMainWithJarsAndDirectoryOfClasses() throws Exception {
    Testability.main("" /* blank will look for everything */, "-cp",
            "lib/asm-3.0.jar:lib/args4j-2.0.8.jar:out/production/testability-metrics");
  }

  public void testMainWithJarsAndDirectoryOfClassesAndFilter() throws Exception {

  }


  public static class WatchedOutputStream extends OutputStream {
    StringBuffer sb = new StringBuffer();

    @Override
    public void write(int ch) throws IOException {
      sb.append(ch);
    }

    @Override
    public void write(byte[] b) throws IOException {
      sb.append(new String(b));
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      sb.append(new String(b, off, len));
    }

    public String getOutput() {
      return sb.toString();
    }
  }
}
