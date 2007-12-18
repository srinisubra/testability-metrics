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

import junit.framework.TestCase;

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.InputStream;

public class DirectoryClasspathRootTest extends TestCase {
  /**
   * Directories to be used for testing that contains class files, for testing.
   * These are included in subversion so that any checkout will have a consistent
   * environment for testing.
   */
  public static final String ROOT_1_CLASSES_FOR_TEST = "classes-for-test/root1";
  public static final String ROOT_2_CLASSES_FOR_TEST = "classes-for-test/root2";

  public void testCreateNewDirectoryClasspathRoot() throws Exception {
    File dir = new File(ROOT_1_CLASSES_FOR_TEST);
    assertTrue(dir.isDirectory());
    ClasspathRoot root = ClasspathRootFactory.makeClasspathRoot(dir, "");
    assertNotNull(root);
    assertTrue(root instanceof DirectoryClasspathRoot);
  }

   public void testCreateNewJarsClasspathRootTest() throws Exception {
    final String cp = ROOT_1_CLASSES_FOR_TEST + ":" + ROOT_2_CLASSES_FOR_TEST;
    ClasspathRootGroup group = ClasspathRootFactory.makeClasspathRootGroup(cp);
    assertNotNull(group);
    assertEquals(2, group.getGroupCount());
    ArrayList<String> packageFilter = new ArrayList<String>();
    packageFilter.add("");
    List<String> names = group.getAllContainedClassNames(packageFilter);
    assertTrue(names.contains("com.google.classpath.ColonDelimitedStringParser"));
    assertTrue(names.contains("com.google.test.metric.AutoFieldClearTestCase"));
    assertTrue(names.contains("com.google.test.metric.ClassInfoTest"));
    assertTrue(names.contains("com.google.test.metric.x.SelfTest"));

    String wantedResource = "com/google/test/metric/x/SelfTest.class";
    InputStream is = group.getResourceAsStream(wantedResource);
    assertNotNull(is);
  }

}
