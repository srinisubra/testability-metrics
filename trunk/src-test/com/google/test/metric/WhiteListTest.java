package com.google.test.metric;

import junit.framework.TestCase;

public class WhiteListTest extends TestCase {

  public void testPositiveHitInWhiteList() throws Exception {
    WhiteList whiteList = new PackageWhiteList("java.");
    assertTrue(whiteList.isClassWhiteListed("java.lang.String"));
    assertFalse(whiteList.isClassWhiteListed("com.company.Class"));
  }
  
}
