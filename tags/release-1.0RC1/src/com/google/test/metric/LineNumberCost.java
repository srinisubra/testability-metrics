package com.google.test.metric;


/**
 * User: jwolter Date: Nov 30, 2007
 */
public class LineNumberCost {
  private final int lineNumber;
  private final MethodCost methodCost;

  public LineNumberCost(int lineNumber, MethodCost methodCost) {
    this.lineNumber = lineNumber;
    this.methodCost = methodCost;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public MethodCost getMethodCost() {
    return methodCost;
  }

  @Override
  public String toString() {
    return methodCost.getMethod() + ":" + lineNumber;
  }

}
