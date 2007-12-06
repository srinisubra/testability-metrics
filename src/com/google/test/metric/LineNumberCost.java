package com.google.test.metric;

/**
 * User: jwolter Date: Nov 30, 2007
 */
public class LineNumberCost {
  private final MethodInfo methodInfo;
  private final long cost;
  private final int lineNumber;

  public LineNumberCost(MethodInfo methodInfo, int lineNumber, long cost) {
    this.methodInfo = methodInfo;
    this.lineNumber = lineNumber;
    this.cost = cost;
  }

  public long getCost() {
    return cost;
  }

  public MethodInfo getMethod() {
    return methodInfo;
  }

  public int getLineNumber() {
    return lineNumber;
  }
}
