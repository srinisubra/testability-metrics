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

import static com.google.classpath.DirectoryClasspathRootTest.ROOT_1_CLASS_FOR_TEST;
import static com.google.classpath.DirectoryClasspathRootTest.ROOT_CLASSES_EXTERNAL_DEPS_FOR_TEST;
import static com.google.classpath.JarClasspathRootTest.ASM_JAR;
import static com.google.classpath.JarClasspathRootTest.JUNIT_JAR;

import com.google.classpath.JarClasspathRootTest;

import org.kohsuke.args4j.CmdLineException;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class TestabilityTest extends AutoFieldClearTestCase {
  private WatchedOutputStream out;
  private WatchedOutputStream err;
  private Testability testability;

  @Override
  protected void setUp() {
    out = new WatchedOutputStream();
    err = new WatchedOutputStream();
    testability = new Testability(new PrintStream(out), new PrintStream(err));
  }

  public void testComputeCostSingleClass() throws Exception {
    String classUnderTest = com.google.test.metric.Testability.class.getName();
    ClassCost classCost = testability.computeCost(classUnderTest);
    assertNotNull(classCost);
    assertTrue(classCost.toString().length() > 0);
  }

  public void testParseNoArgs() {
    try {
      testability.parseArgs();
      fail("Should have thrown a CmdLineException exception");
    } catch (CmdLineException expected) {
      // expecting this
    }
    assertTrue(err.toString().indexOf("Argument \"classes/packages\" is required") > -1);
  }

  public void testParseClasspathAndSingleClass() throws Exception {
    testability.parseArgs("-cp", "not/default/path", "com.google.TestClass");

    assertEquals("", err.toString());
    assertEquals("not/default/path", testability.classpath);
    List<String> expectedArgs = new ArrayList<String>();
    expectedArgs.add("com.google.TestClass");
    assertNotNull(testability.entryList);
    assertEquals(expectedArgs, testability.entryList);
  }


  public void testJarFileNoClasspath() throws Exception {
    testability.run("junit.runner", "-cp");
    /** we expect the error to say something about proper usage of the arguments.
     * The -cp needs a value */
    assertTrue(out.toString().length() == 0);
    assertTrue(err.toString().length() > 0);
  }


  public void testJarFileParseSetupAndComputeGroupMetric() throws Exception {
    testability.parseSetup("", "-cp", JUNIT_JAR);
    testability.computeGroupMetric();
    assertTrue("output too short, expected parsing lots of files correctly",
        out.toString().length() > 1000);
    assertEquals(1, testability.entryList.size());
    assertEquals("", testability.entryList.get(0));
    assertTrue(testability.classpath.endsWith(JUNIT_JAR));
  }

  public void testClassesNotInClasspath() throws Exception {
    testability.run("", "-cp", ROOT_CLASSES_EXTERNAL_DEPS_FOR_TEST);
    assertTrue(out.toString().length() > 0);
    assertTrue(err.toString().length() > 0);
  }

  public void testIncompleteClasspath() throws Exception {
    /* ROOT_CLASSES_EXTERNAL_DEPS_FOR_TEST contains some classes from this 
     * project, but not all. There are many references to classes that will 
     * not be in this test's -cp classpath.
     * We are testing that when the ClassRepository encounters a
     * ClassNotFoundException, it continues nicely and prints the values for
     * the classes that it _does_ find. */
    testability.run("" /* blank will look for everything */, "-cp", 
        ROOT_CLASSES_EXTERNAL_DEPS_FOR_TEST);
    assertTrue("Output was empty, some output expected", out.toString().length() > 0);
    assertTrue("Error output was empty, expected error output from class not found",
        err.toString().length() > 0);
  }
  
  public void testJarFileAndJunitSwinguiProgressBarEntryPattern() throws Exception {
    testability.run("junit.swingui.ProgressBar", "-cp", JarClasspathRootTest.JUNIT_JAR);
    assertTrue(out.toString().length() > 0);
    assertTrue(err.toString().length() == 0);
  }

  public void testJarFileAndJunitRunnerEntryPattern() {
    testability.run("junit.runner", "-cp", JarClasspathRootTest.JUNIT_JAR);
    assertTrue(out.toString().length() > 0);
    assertTrue(err.toString().length() == 0);
//    System.out.println(out.toString());
  }

  public void testJarFileAndJunitRunnerEntryPatternAndMaxDepthTwo() {
    testability.run("junit.runner", "-cp", JarClasspathRootTest.JUNIT_JAR, "-maxPrintingDepth", "2");
    assertTrue(out.toString().length() > 0);

    Pattern sixSpacesThenLinePattern = Pattern.compile("^(\\s){6}line", Pattern.MULTILINE);
    assertTrue("Expected 6 leading spaces spaces for maxPrintingDepth=2",
        sixSpacesThenLinePattern.matcher(out.toString()).find());

    Pattern over7SpacesThenLinePattern = Pattern.compile("^(\\s){7,}line", Pattern.MULTILINE);
    assertFalse("Should not have had more than 2 + 2*2 = 6 leading spaces for maxPrintingDepth=2",
        over7SpacesThenLinePattern.matcher(out.toString()).find());
    assertTrue(err.toString().length() == 0);
  }

  public void testJarFileAndJunitRunnerEntryPatternAndMaxDepthZero() {
    testability.run("junit.runner", "-cp", JarClasspathRootTest.JUNIT_JAR, "-maxPrintingDepth", "0");
    assertTrue(out.toString().length() > 0);

    Pattern noLinesPattern = Pattern.compile("^(\\s)*line", Pattern.MULTILINE);
    assertFalse("Should not have any line matchings for printing depth of 0",
        noLinesPattern.matcher(out.toString()).find());
    assertEquals(0, err.toString().length());
  }

  public void testJarsAndDirectoryWildcardEntryPattern() throws Exception {
    testability.run("" /* blank will look for everything */, "-cp",
        ASM_JAR + ":" + JUNIT_JAR + ":" + ROOT_1_CLASS_FOR_TEST);
    System.out.println(err.toString());
    assertTrue(out.toString().length() > 0);
    assertEquals(0, err.toString().length());
  }


  public void testJarsAndDirectoryOfClassesAndFilter() throws Exception {
    testability.run("junit.swingui.ProgressBar", "-cp",
        JUNIT_JAR + ":" + ROOT_1_CLASS_FOR_TEST);
    assertTrue(out.toString().length() > 0);
    assertEquals(0, err.toString().length());
  }

  public void testForWarningWhenClassesRecurseToIncludeClassesOutOfClasspath() throws Exception {
    //TODO jwolter 1-8-08 this fails because the ROOT_CLASSES_EXTERNAL_DEPS_FOR_TEST root 
    // extends from classes that are out of the classpath. This causes a different error
    // than when I have a member variable out of the classpath. I need to create a 
    // source dir that encapsulates the exact tests I want to test on all of these test cases.
    // ALSO, creating custom jars with the interdependencies would speed up these tests.
    // SO 2 birds with 1 stone. 1. less 'mysterous' classes-for-test/ and 2. faster tests
    
    testability.run("" /* blank will look for everything */, 
        "-cp", ROOT_CLASSES_EXTERNAL_DEPS_FOR_TEST, "-maxPrintingDepth", "5");
    assertTrue("Output was empty, some output expected", out.toString().length() > 0);
    assertTrue("Error output was empty, expected error output from class not found",
        err.toString().length() > 0);
    System.out.println("--------");
    System.out.println(err.toString());
    System.out.println("--------");    
    assertTrue(err.toString().indexOf("WARNING: class not found: ") > -1);
  }

  public void testForWarningWhenClassExtendsFromClassOutOfClasspath() throws Exception {
    testability.computeCost("ThisClassDoesNotExist");
    assertEquals(0, out.toString().length());    
    assertTrue(err.toString().length() > 0);
    System.out.println("--------");
    System.out.println(err.toString());
    System.out.println("--------");    
    assertTrue(err.toString().startsWith("WARNING: can not analyze class 'ThisClassDoesNotExist"));
  }

  public void testFilterCostOverTotalCostThreshold() throws Exception {
    testability.run("junit.runner", "-cp", JarClasspathRootTest.JUNIT_JAR);
    int baselineLength = out.toString().length();
    String baselineOutput = out.toString();
    out.clear();
    testability.run("junit.runner", "-cp", JUNIT_JAR, "-costThreshold", "1000");
    int throttledLength = out.toString().length();
    String throttledOutput = out.toString();
    assertTrue(throttledOutput.length() < baselineOutput.length());
    assertFalse(baselineOutput.equals(throttledOutput));
  }
  
  public void testOneEntryWhitelist() throws Exception {
    testability.run("junit.runner", "-cp", JUNIT_JAR);
    String baselineOutput = out.toString();
    out.clear();
    testability.run("junit.runner", "-cp", JUNIT_JAR, 
        "-whitelist", "java.lang");
    String throttledOutput = out.toString();
    assertTrue(throttledOutput.length() < baselineOutput.length());
    assertFalse(baselineOutput.equals(throttledOutput));
  }

  public static class WatchedOutputStream extends OutputStream {
    StringBuffer sb = new StringBuffer(5000);

    @Override
    public void write(int ch) {
      sb.append(ch);
    }

    @Override
    public void write(byte[] b) {
      sb.append(new String(b));
    }

    @Override
    public void write(byte[] b, int off, int len) {
      sb.append(new String(b, off, len));
    }

    @Override
    public String toString() {
      return sb.toString();
    }
    
    public void clear() {
      sb = new StringBuffer(5000);
    }

  }
}
