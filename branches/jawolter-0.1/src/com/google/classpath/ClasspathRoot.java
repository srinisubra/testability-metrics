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

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * An entry on the classpath, which may be a directory or jar. Concrete classes
 * are provided for each type.
 * 
 * @author misko@google.com, pepstein@google.com, jwolter@google.com
 */
public abstract class ClasspathRoot {
  protected URLClassLoader classloader;
  protected URL url;

  public InputStream getResourceAsStream(String resourceName) {
    return classloader.getResourceAsStream(resourceName);
  }

  abstract Collection<String> getResources(String packageName);

  public Collection<String> getAllContainedClassNames() {
    List<String> classNames = new ArrayList<String>();
    try {
      buildClassNamesList(url, this, "", "", classNames, false);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
    return classNames;
  }

  @Override
  public String toString() {
    String url = this.url.toString();
    if (url.endsWith("/")) {
      url = url.substring(0, url.length() - 1);
    }
    int index = Math.max(0, url.lastIndexOf('/') + 1);
    return url.substring(index);
  }

  protected static void buildClassNamesList(URL root,
      ClasspathRoot classpathRoot, String requiredPrefix, String packageName,
      List<String> classNamesList, boolean verbose)
      throws MalformedURLException {
    for (String resource : classpathRoot.getResources(packageName)) {
      if (resource.endsWith(".class")) {
        String className = packageName + resource;
        className = className.replace(".class", "").replace('/', '.');
        if (!className.contains("$")) {
          if (validClassNameByPrefixFilter(className, requiredPrefix)) {
            if (verbose) {
              System.out.println("Found: " + className.replace("/", "."));
            }
            classNamesList.add(className);
          }
        }
      } else if (resource.endsWith(".jar")) {
        String temp = root.getPath() + packageName + resource;
        URL jarRoot = new File(temp).toURI().toURL();
        ClasspathRoot jarPath = new JarClasspathRoot(jarRoot, null);
        buildClassNamesList(jarRoot, jarPath, requiredPrefix, "",
            classNamesList, verbose);
      } else {
        buildClassNamesList(root, classpathRoot, requiredPrefix, packageName
            + resource + "/", classNamesList, verbose);
      }
    }
  }

  private static boolean validClassNameByPrefixFilter(String className,
      String requiredPrefix) {
    return className.startsWith(requiredPrefix);
  }

  protected static void parseAndAddToClasspathList(List<URL> completeClasspath,
      String classpath) {
    for (String path : classpath.split("(:|;)")) {
      try {
        URL url = new File(path).toURI().toURL();
        completeClasspath.add(url);
      } catch (MalformedURLException e) {
        throw new RuntimeException("Error parsing classpath. " + e.getMessage());
      }
    }
  }
}
