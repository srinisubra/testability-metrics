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
package com.google.test.metric.asm;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureReader;

import com.google.test.metric.ClassInfo;
import com.google.test.metric.LocalVariableInfo;
import com.google.test.metric.MethodInfo;
import com.google.test.metric.ParameterInfo;
import com.google.test.metric.Variable;
import com.google.test.metric.method.BlockDecomposer;
import com.google.test.metric.method.Constant;

public class MethodVisitorBuilder extends NoopMethodVisitor {

	private final ClassInfo classInfo;
	private final String name;
	private final String desc;
	private final int parameterCount;
	private final Visibility visibility;
	private List<Variable> variables = new ArrayList<Variable>();
	private final BlockDecomposer blockDecomposer = new BlockDecomposer();
	private List<Runnable> recorder = new LinkedList<Runnable>();
	private long cyclomaticComplexity = 1;

	public MethodVisitorBuilder(ClassInfo classInfo, String name, String desc,
			String signature, String[] exceptions, Visibility visibility) {
		this.classInfo = classInfo;
		this.name = name;
		this.desc = desc;
		this.visibility = visibility;
		ParameterCountVisitor counter = new ParameterCountVisitor();
		new SignatureReader(desc).accept(counter);
		parameterCount = counter.getCount();
	}

	@Override
	public void visitJumpInsn(int opcode, final Label label) {
		if (opcode == Opcodes.GOTO) {
			recorder.add(new Runnable() {
				public void run() {
					blockDecomposer.unconditionalGoto(label);
				}
			});
		} else {
			cyclomaticComplexity++;
			recorder.add(new Runnable() {
				public void run() {
					blockDecomposer.conditionalGoto(label);
				}
			});
		}
	}

	@Override
	public void visitTryCatchBlock(final Label start, final Label end,
			final Label handler, String type) {
		if (type != null) {
			cyclomaticComplexity++;
		}
		recorder.add(new Runnable() {
			public void run() {
				blockDecomposer.tryCatchBlock(start, end, handler);
			}
		});
	}

	@Override
	public void visitTableSwitchInsn(int min, int max, final Label dflt,
			final Label[] labels) {
		for (Label label : labels) {
			if (label != dflt) {
				cyclomaticComplexity++;
			}
		}
		recorder.add(new Runnable() {
			public void run() {
				blockDecomposer.tableSwitch(dflt, labels);
			}
		});
	}

	@Override
	public void visitLookupSwitchInsn(final Label dflt, final int[] keys,
			final Label[] labels) {
		for (Label label : labels) {
			if (label != null) {
				cyclomaticComplexity++;
			}
		}
		recorder.add(new Runnable() {
			public void run() {
				blockDecomposer.tableSwitch(dflt, labels);
			}
		});
	}

	@Override
	public void visitLocalVariable(String name, String desc, String signature,
			Label start, Label end, int index) {
		Variable variable;
		if (index == 0 && name.equals("this")) {
			variable = new LocalVariableInfo(name);
		} else if (index < parameterCount + (isInstanceMethod() ? 1 : 0)) {
			variable = new ParameterInfo(name);
		} else {
			variable = new LocalVariableInfo(name);
		}
		variables.add(variable);
	}

	private boolean isInstanceMethod() {
		return variables.size() > 0
				&& variables.get(0).getName().equals("this");
	}

	@Override
	public void visitLineNumber(final int line, final Label start) {
		recorder.add(new Runnable() {
			public void run() {
				blockDecomposer.lineNumber(line, start);
			}
		});
	}

	@Override
	public void visitEnd() {
		List<ParameterInfo> parameters = new ArrayList<ParameterInfo>();
		List<LocalVariableInfo> localVariables = new ArrayList<LocalVariableInfo>();
		for (Variable variable : variables) {
			if (variable.getName().equals("this"))
				continue;
			if (variable instanceof ParameterInfo) {
				parameters.add((ParameterInfo) variable);
			} else {
				localVariables.add((LocalVariableInfo) variable);
			}
		}
		MethodInfo methodInfo = new MethodInfo(classInfo, name, desc,
				blockDecomposer.getMainBlock(), parameters, localVariables,
				visibility, cyclomaticComplexity);
		classInfo.addMethod(methodInfo);
		for (Runnable runnable : recorder) {
			runnable.run();
		}
		blockDecomposer.done();
	}

	@Override
	public void visitTypeInsn(final int opcode, final String desc) {
		recorder.add(new Runnable() {
			public void run() {
				switch (opcode) {
				case Opcodes.NEW:
				case Opcodes.NEWARRAY:
					blockDecomposer.pushVariable(new Constant("new " + desc));
					break;
				default:
					break;
				}
			}
		});
	}

	@Override
	public void visitVarInsn(final int opcode, final int var) {
		switch (opcode) {
		case Opcodes.ILOAD:
		case Opcodes.LLOAD:
		case Opcodes.FLOAD:
		case Opcodes.DLOAD:
		case Opcodes.ALOAD:
			recorder.add(new Runnable() {
				public void run() {
					blockDecomposer.pushVariable(variable(var));
				}
			});
			break;

		case Opcodes.ISTORE:
		case Opcodes.LSTORE:
		case Opcodes.FSTORE:
		case Opcodes.DSTORE:
		case Opcodes.ASTORE:
			recorder.add(new Runnable() {
				public void run() {
					blockDecomposer.popVariable(variable(var));
				}
			});
			break;

		}
	}

	private Variable variable(int var) {
		if (variables.size() > var) {
			return variables.get(var);
		} else {
			variables.add(new LocalVariableInfo("local_" + variables.size()));
			return variable(var);
		}
	}

	@Override
	public void visitLabel(final Label label) {
		recorder.add(new Runnable() {
			public void run() {
				blockDecomposer.label(label);
			}
		});
	}

	@Override
	public void visitInsn(final int opcode) {
		switch (opcode) {
		case Opcodes.ACONST_NULL:
			recorder.add(new Runnable() {
				public void run() {
					blockDecomposer.pushVariable(new Constant(null));
				}
			});
			break;
		case Opcodes.ICONST_M1:
		case Opcodes.ICONST_0:
		case Opcodes.ICONST_1:
		case Opcodes.ICONST_2:
		case Opcodes.ICONST_3:
		case Opcodes.ICONST_4:
		case Opcodes.ICONST_5:
			recorder.add(new Runnable() {
				public void run() {
					blockDecomposer.pushVariable(new Constant(opcode
							- Opcodes.ICONST_M1 - 1));
				}
			});
			break;
		case Opcodes.LCONST_0:
		case Opcodes.LCONST_1:
		case Opcodes.FCONST_0:
		case Opcodes.FCONST_1:
		case Opcodes.FCONST_2:
		case Opcodes.DCONST_0:
		case Opcodes.DCONST_1:
		case Opcodes.IALOAD:
		case Opcodes.LALOAD:
		case Opcodes.FALOAD:
		case Opcodes.DALOAD:
		case Opcodes.AALOAD:
		case Opcodes.BALOAD:
		case Opcodes.CALOAD:
		case Opcodes.SALOAD:
		case Opcodes.IASTORE:
		case Opcodes.LASTORE:
		case Opcodes.FASTORE:
		case Opcodes.DASTORE:
		case Opcodes.AASTORE:
		case Opcodes.BASTORE:
		case Opcodes.CASTORE:
		case Opcodes.SASTORE:
		case Opcodes.POP:
		case Opcodes.POP2:
			break;
		case Opcodes.DUP:
			recorder.add(new Runnable() {
				public void run() {
					blockDecomposer.dup();
				}
			});
			break;
		case Opcodes.DUP_X1:
		case Opcodes.DUP_X2:
		case Opcodes.DUP2:
		case Opcodes.DUP2_X1:
		case Opcodes.DUP2_X2:
		case Opcodes.SWAP:
		case Opcodes.IADD:
		case Opcodes.LADD:
		case Opcodes.FADD:
		case Opcodes.DADD:
		case Opcodes.ISUB:
		case Opcodes.LSUB:
		case Opcodes.FSUB:
		case Opcodes.DSUB:
		case Opcodes.IMUL:
		case Opcodes.LMUL:
		case Opcodes.FMUL:
		case Opcodes.DMUL:
		case Opcodes.IDIV:
		case Opcodes.LDIV:
		case Opcodes.FDIV:
		case Opcodes.DDIV:
		case Opcodes.IREM:
		case Opcodes.LREM:
		case Opcodes.FREM:
		case Opcodes.DREM:
		case Opcodes.INEG:
		case Opcodes.LNEG:
		case Opcodes.FNEG:
		case Opcodes.DNEG:
		case Opcodes.ISHL:
		case Opcodes.LSHL:
		case Opcodes.ISHR:
		case Opcodes.LSHR:
		case Opcodes.IUSHR:
		case Opcodes.LUSHR:
		case Opcodes.IAND:
		case Opcodes.LAND:
		case Opcodes.IOR:
		case Opcodes.LOR:
		case Opcodes.IXOR:
		case Opcodes.LXOR:
		case Opcodes.I2L:
		case Opcodes.I2F:
		case Opcodes.I2D:
		case Opcodes.L2I:
		case Opcodes.L2F:
		case Opcodes.L2D:
		case Opcodes.F2I:
		case Opcodes.F2L:
		case Opcodes.F2D:
		case Opcodes.D2I:
		case Opcodes.D2L:
		case Opcodes.D2F:
		case Opcodes.I2B:
		case Opcodes.I2C:
		case Opcodes.I2S:
		case Opcodes.LCMP:
		case Opcodes.FCMPL:
		case Opcodes.FCMPG:
		case Opcodes.DCMPL:
		case Opcodes.DCMPG:
		case Opcodes.IRETURN:
		case Opcodes.LRETURN:
		case Opcodes.FRETURN:
		case Opcodes.DRETURN:
		case Opcodes.ARETURN:
		case Opcodes.RETURN:
		case Opcodes.ARRAYLENGTH:
		case Opcodes.ATHROW:
		case Opcodes.MONITORENTER:
		case Opcodes.MONITOREXIT:
		}
	}

	@Override
	public void visitFieldInsn(final int opcode, final String owner,
			final String name, final String desc) {
		switch (opcode) {
		case Opcodes.GETSTATIC:
		case Opcodes.PUTSTATIC:
		case Opcodes.GETFIELD:
		case Opcodes.PUTFIELD:
			recorder.add(new Runnable() {
				public void run() {
					blockDecomposer.popVariable(classInfo.getField(name));
				}
			});
			break;
		}
	}

	@Override
	public void visitMethodInsn(final int opcode, final String owner,
			final String name, final String desc) {
		recorder.add(new Runnable() {
			public void run() {
				blockDecomposer.methodInvokation(opcode, owner
						.replace('/', '.'), name, desc);
			}
		});
	}
}
