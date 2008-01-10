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

import static com.google.classpath.DirectoryClasspathRootTest.CLASSES_EXTERNAL_DEPS_AND_SUPERCLASSES;
import static com.google.classpath.DirectoryClasspathRootTest.CLASSES_EXTERNAL_DEPS_NO_SUPERCLASSES;
import static com.google.classpath.DirectoryClasspathRootTest.CLASS_NO_EXTERNAL_DEPS;
import static com.google.classpath.JarClasspathRootTest.ASM_JAR;
import static com.google.classpath.JarClasspathRootTest.JUNIT_JAR;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.kohsuke.args4j.CmdLineException;

import com.google.classpath.JarClasspathRootTest;

public class TestabilityTest extends AutoFieldClearTestCase {
    private WatchedOutputStream out;
    private WatchedOutputStream err;
    private Testability testability;

    @Override
    protected void setUp() {
        out = new WatchedOutputStream();
        err = new WatchedOutputStream();
        testability = new Testability(new PrintStream(out),
                new PrintStream(err));
    }

    public void testParseNoArgs() {
        try {
            testability.parseArgs();
            fail("Should have thrown a CmdLineException exception");
        } catch (CmdLineException expected) {
            // expecting this
        }
        assertTrue(err.toString().indexOf(
                "Argument \"classes/packages\" is required") > -1);
    }

    public void testParseClasspathAndSingleClass() throws Exception {
        testability
                .parseArgs("-cp", "not/default/path", "com.google.TestClass");

        assertEquals("", err.toString());
        assertEquals("not/default/path", testability.cp);
        List<String> expectedArgs = new ArrayList<String>();
        expectedArgs.add("com.google.TestClass");
        assertNotNull(testability.entryList);
        assertEquals(expectedArgs, testability.entryList);
    }

    public void testJarFileNoClasspath() throws Exception {
        testability.run("junit.runner", "-cp");
        /**
         * we expect the error to say something about proper usage of the
         * arguments. The -cp needs a value
         */
        assertTrue(out.toString().length() == 0);
        assertTrue(err.toString().length() > 0);
    }

    public void testJarFileParseSetupAndComputeGroupMetric() throws Exception {
        testability.parseSetup("", "-cp", JUNIT_JAR);
        testability.computeGroupMetric();
        assertTrue(
                "output too short, expected parsing lots of files correctly",
                out.toString().length() > 1000);
        assertEquals(1, testability.entryList.size());
        assertEquals("", testability.entryList.get(0));
        assertTrue(testability.cp.endsWith(JUNIT_JAR));
    }

    public void testClassesNotInClasspath() throws Exception {
        testability.run("", "-cp", CLASSES_EXTERNAL_DEPS_AND_SUPERCLASSES);
        assertTrue(out.toString().length() > 0);
        assertTrue(err.toString().length() > 0);
    }

    /**
     * The given classpath contains some classes from this
     * project, but not all. There are many references to classes that will
     * not be in this test's -cp classpath. This test verifies that when the
     * ClassRepository encounters a ClassNotFoundException, it continues
     * nicely and prints the values for the classes that it <em>does</em> find.
     */
    public void testIncompleteClasspath() throws Exception {
        testability.run("" /* blank will look for everything */, "-cp",
                CLASSES_EXTERNAL_DEPS_AND_SUPERCLASSES);
        assertTrue("Output was empty, some output expected", out.toString()
                .length() > 0);
        assertTrue("Error output was empty, expected error output from class not found",
                err.toString().length() > 0);
    }

    public void testJarFileAndJunitSwinguiProgressBarEntryPattern()
            throws Exception {
        testability.run("junit.swingui.ProgressBar", "-cp",
                JarClasspathRootTest.JUNIT_JAR);
        assertTrue(out.toString().length() > 0);
        assertTrue(err.toString().length() == 0);
    }

    public void testJarFileAndJunitRunnerEntryPattern() {
        testability.run("junit.runner", "-cp", JarClasspathRootTest.JUNIT_JAR);
        assertTrue(out.toString().length() > 0);
        assertTrue(err.toString().length() == 0);
        // System.out.println(out.toString());
    }

    public void testJarFileAndJunitRunnerEntryPatternAndMaxDepthZero() {
        testability.run("junit.runner", "-cp", JarClasspathRootTest.JUNIT_JAR,
                "-printDepth", "0");
        assertTrue(out.toString().length() > 0);

        Pattern noLinesPattern = Pattern.compile("^(\\s)*line",
                Pattern.MULTILINE);
        assertFalse(
                "Should not have any line matchings for printing depth of 0",
                noLinesPattern.matcher(out.toString()).find());
        assertEquals(0, err.toString().length());
    }

    public void testJarsAndDirectoryWildcardEntryPattern() throws Exception {
        testability.run("" /* blank will look for everything */, "-cp",
                ASM_JAR + ":" + JUNIT_JAR + ":" + CLASS_NO_EXTERNAL_DEPS);
        System.out.println(err.toString());
        assertTrue(out.toString().length() > 0);
        assertEquals(0, err.toString().length());
    }

    public void testJarsAndDirectoryOfClassesAndFilter() throws Exception {
        testability.run("junit.swingui.ProgressBar", "-cp", JUNIT_JAR + ":"
                + CLASS_NO_EXTERNAL_DEPS);
        assertTrue(out.toString().length() > 0);
        assertEquals(0, err.toString().length());
    }

    /**
     * Tries calculating the cost for classes that reference other classes not
     * in the classpath.
     */
    public void testForWarningWhenClassesRecurseToIncludeClassesOutOfClasspath() 
            throws Exception {
        testability.run("" /* blank will look for everything */, "-cp",
                        CLASSES_EXTERNAL_DEPS_NO_SUPERCLASSES,
                        "-printDepth", "1");
        assertTrue(out.toString().length() > 0);
        assertTrue(err.toString().length() > 0);
        assertTrue(err.toString().startsWith("WARNING: class not found: "));
    }

    /**
     * Tries calculating the cost for classes that extend from another class,
     * which does not exist in the classpath.
     */
    public void testForWarningWhenClassExtendsFromClassOutOfClasspath()
            throws Exception {
        testability.run("" /* blank will look for everything */, "-cp",
                CLASSES_EXTERNAL_DEPS_AND_SUPERCLASSES, "-printDepth",
                "1");
        assertTrue(out.toString().length() > 0);
        assertTrue(err.toString().length() > 0);
        assertTrue(err.toString().startsWith("WARNING: can not analyze class "));
    }

    public void testFilterCostOverTotalCostThreshold() throws Exception {
        testability.run("junit.runner", "-cp", JarClasspathRootTest.JUNIT_JAR);
        String baselineOutput = out.toString();
        out.clear();
        testability.run("junit.runner", "-cp", JUNIT_JAR, "-costThreshold",
                "1000");
        String throttledOutput = out.toString();
        assertTrue(throttledOutput.length() < baselineOutput.length());
        assertFalse(baselineOutput.equals(throttledOutput));
    }

    public void testOneEntryWhitelist() throws Exception {
        testability.run("junit.runner", "-cp", JUNIT_JAR);
        String baselineOutput = out.toString();
        out.clear();
        testability.run("junit.runner", "-cp", JUNIT_JAR, "-whitelist",
                "junit.framework.");
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
