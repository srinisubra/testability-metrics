package com.google.test.metric;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;

public class PackageWhiteList implements WhiteList {

  private final List<String> packages;

  public PackageWhiteList(String... pacakges) {
    this.packages = new ArrayList<String>(asList(pacakges));
  }

  public boolean isClassWhiteListed(String className) {
    for (String packageName : packages) {
      if (className.startsWith(packageName)) {
        return true;
      }
    }
    return false;
  }

  public void addPackage(String packageName) {
    packages.add(packageName);
  }

}
