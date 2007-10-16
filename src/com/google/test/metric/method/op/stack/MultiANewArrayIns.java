package com.google.test.metric.method.op.stack;

import java.util.List;

import com.google.test.metric.Type;
import com.google.test.metric.Variable;
import com.google.test.metric.method.Constant;

public class MultiANewArrayIns extends StackOperation {

	private final String clazz;
	private final int dims;

	public MultiANewArrayIns(int lineNumber, String clazz, int dims) {
		super(lineNumber);
		this.clazz = clazz;
		this.dims = dims;
	}

	@Override
	public int getOperatorCount() {
		return 1;
	}

	@Override
	public List<Variable> apply(List<Variable> input) {
		return list(new Constant(getType(), Type.ADDRESS));
	}

	private String getType() {
		String type = clazz;
		for (int i = 0; i < dims; i++) {
			type += "[]";
		}
		return type;
	}

	@Override
	public String toString() {
		return "MultiANewArray dims=" + dims;
	}

}
