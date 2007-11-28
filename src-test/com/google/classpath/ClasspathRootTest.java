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
package com.google.classpath;

import static com.google.classpath.ClasspathRoot.parseAndAddToClasspathList;

import junit.framework.TestCase;

import java.net.URL;
import java.util.List;
import java.util.ArrayList;

public class ClasspathRootTest extends TestCase {
  public void testParseAndAddToClasspathList() throws Exception {
    List<URL> completeClasspath = new ArrayList<URL>();

    String classpath = "test-one.jar;";
    parseAndAddToClasspathList(completeClasspath, classpath);
    assertStringEndsWith("test-one.jar", completeClasspath.get(0).toString());

    completeClasspath.clear();
    classpath = "lib/one.jar:lib/two.jar;three.jar";
    parseAndAddToClasspathList(completeClasspath, classpath);

    assertStringEndsWith("lib/one.jar", completeClasspath.get(0).toString());
    assertStringEndsWith("lib/two.jar", completeClasspath.get(1).toString());
    assertStringEndsWith("three.jar", completeClasspath.get(2).toString());
  }

  private void assertStringEndsWith(String expectedEnding, String fullToTest) {
    assertEquals(expectedEnding, getEndOfString(fullToTest, expectedEnding.length()));
  }

  private String getEndOfString(String string, int characters) {
    return string.substring(string.length() - characters, string.length());
  }
}
