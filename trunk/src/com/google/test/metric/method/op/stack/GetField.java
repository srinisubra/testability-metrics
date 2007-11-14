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
package com.google.test.metric.method.op.stack;

import com.google.test.metric.FieldInfo;
import com.google.test.metric.Variable;

import java.util.List;

public class GetField extends StackOperation {

  private final FieldInfo fieldInfo;

  public GetField(int lineNumber, FieldInfo fieldInfo) {
    super(lineNumber);
    this.fieldInfo = fieldInfo;
  }

  @Override
  public String toString() {
    return "get " + (fieldInfo.isStatic() ? "static " : "") + fieldInfo;
  }

  @Override
  public int getOperatorCount() {
    return fieldInfo.isStatic() ? 0 : 1;
  }

  @Override
  public List<Variable> apply(List<Variable> input) {
    return list(fieldInfo);
  }

}
