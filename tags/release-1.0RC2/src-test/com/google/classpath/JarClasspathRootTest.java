package com.google.classpath;

import junit.framework.TestCase;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * User: jwolter
 * Date: Dec 13, 2007
 */
public class JarClasspathRootTest extends TestCase {
  public void testCreateNewJarClasspathRootTest() throws Exception {
    File jar = new File("lib/asm-3.0.jar");
    assertTrue(jar.isFile());
    ClasspathRoot root = ClasspathRootFactory.makeClasspathRoot(jar, "");
    assertNotNull(root);
    assertTrue(root instanceof JarClasspathRoot);
  }

  public void testCreateNewJarsClasspathRootTest() throws Exception {
    String classpath = "lib/asm-3.0.jar:lib/junit.jar:lib/jarjar.jar";
    ClasspathRootGroup group = ClasspathRootFactory.makeClasspathRootGroup(classpath);
    assertNotNull(group);
    assertEquals(3, group.getGroupCount());
    ArrayList<String> packageFilter = new ArrayList<String>();
    packageFilter.add("");
    List<String> names = group.getAllContainedClassNames(packageFilter);
    assertTrue(names.contains("junit.runner.Sorter"));
    assertTrue(names.contains("junit.textui.TestRunner"));
    assertTrue(names.contains("com.tonicsystems.jarjar.ext_util.JarProcessor"));

    String wantedResource = "com/tonicsystems/jarjar/asm/ClassReader.class";
    InputStream isTest = group.getResourceAsStream(wantedResource);
    assertNotNull(isTest);
  }

}
