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

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureReader;

import com.google.test.metric.ClassInfo;
import com.google.test.metric.ClassRepository;
import com.google.test.metric.FieldInfo;
import com.google.test.metric.LocalVariableInfo;
import com.google.test.metric.MethodInfo;
import com.google.test.metric.ParameterInfo;
import com.google.test.metric.Variable;
import com.google.test.metric.method.BlockDecomposer;
import com.google.test.metric.method.Constant;
import com.google.test.metric.method.op.stack.ArrayLoad;
import com.google.test.metric.method.op.stack.ArrayStore;
import com.google.test.metric.method.op.stack.Duplicate;
import com.google.test.metric.method.op.stack.Duplicate2;
import com.google.test.metric.method.op.stack.GetField;
import com.google.test.metric.method.op.stack.Invoke;
import com.google.test.metric.method.op.stack.Load;
import com.google.test.metric.method.op.stack.MonitorEnter;
import com.google.test.metric.method.op.stack.MonitorExit;
import com.google.test.metric.method.op.stack.Pop;
import com.google.test.metric.method.op.stack.PutField;
import com.google.test.metric.method.op.stack.Return;
import com.google.test.metric.method.op.stack.Store;
import com.google.test.metric.method.op.stack.Swap;
import com.google.test.metric.method.op.stack.Throw;
import com.google.test.metric.method.op.stack.Transform;

public class MethodVisitorBuilder implements MethodVisitor {

	private final ClassInfo classInfo;
	private final String name;
	private final String desc;
	private final int parameterCount;
	private final Visibility visibility;
	private final List<Variable> variables = new ArrayList<Variable>();
	private final BlockDecomposer block = new BlockDecomposer();
	private final List<Runnable> recorder = new LinkedList<Runnable>();
	private final boolean isStatic;
	private final ClassRepository repository;

	private long cyclomaticComplexity = 1;
	private Variable methodThis;
	private int lineNumber;

	public MethodVisitorBuilder(ClassRepository repository,
			ClassInfo classInfo, String name, String desc, String signature,
			String[] exceptions, boolean isStatic, Visibility visibility) {
		this.repository = repository;
		this.classInfo = classInfo;
		this.name = name;
		this.desc = desc;
		this.isStatic = isStatic;
		this.visibility = visibility;
		ParameterCountVisitor counter = new ParameterCountVisitor();
		new SignatureReader(desc).accept(counter);
		parameterCount = counter.getParameterCount();
	}

	public void visitJumpInsn(final int opcode, final Label label) {
		if (opcode == Opcodes.GOTO) {
			recorder.add(new Runnable() {
				public void run() {
					block.unconditionalGoto(label);
				}
			});
		} else {
			cyclomaticComplexity++;
			recorder.add(new Runnable() {
				public void run() {
					boolean singleOperand = opcode == Opcodes.IFEQ
							|| opcode == Opcodes.IFNE || opcode == Opcodes.IFLT
							|| opcode == Opcodes.IFGE || opcode == Opcodes.IFGT
							|| opcode == Opcodes.IFLE
							|| opcode == Opcodes.IFNONNULL
							|| opcode == Opcodes.IFNULL;
					int operatorCount = singleOperand ? 1 : 2;
					block.addOp(new Transform(lineNumber, operatorCount, null));
					block.conditionalGoto(label);
				}
			});
		}
	}

	public void visitTryCatchBlock(final Label start, final Label end,
			final Label handler, String type) {
		if (type != null) {
			cyclomaticComplexity++;
		}
		recorder.add(new Runnable() {
			public void run() {
				block.tryCatchBlock(start, end, handler);
			}
		});
	}

	public void visitTableSwitchInsn(int min, int max, final Label dflt,
			final Label[] labels) {
		for (Label label : labels) {
			if (label != dflt) {
				cyclomaticComplexity++;
			}
		}
		recorder.add(new Runnable() {
			public void run() {
				block.addOp(new Pop(lineNumber, 0));
				block.tableSwitch(dflt, labels);
			}
		});
	}

	public void visitLookupSwitchInsn(final Label dflt, final int[] keys,
			final Label[] labels) {
		for (Label label : labels) {
			if (label != null) {
				cyclomaticComplexity++;
			}
		}
		recorder.add(new Runnable() {
			public void run() {
				block.addOp(new Pop(lineNumber, 0));
				block.tableSwitch(dflt, labels);
			}
		});
	}

	public void visitLocalVariable(String name, String desc, String signature,
			Label start, Label end, int index) {
		Variable variable;
		if (index == 0 && name.equals("this")) {
			variable = methodThis = new LocalVariableInfo(name);
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

	public void visitLineNumber(final int line, final Label start) {
		recorder.add(new Runnable() {
			public void run() {
				lineNumber = line;
			}
		});
	}

	public void visitEnd() {
		List<ParameterInfo> parameters = new ArrayList<ParameterInfo>();
		List<LocalVariableInfo> localVariables = new ArrayList<LocalVariableInfo>();
		for (Variable variable : variables) {
			if (variable.getName().equals("this")) {
				localVariables.add((LocalVariableInfo) variable);
			} else if (variable instanceof ParameterInfo) {
				parameters.add((ParameterInfo) variable);
			} else {
				localVariables.add((LocalVariableInfo) variable);
			}
		}
		for (Runnable runnable : recorder) {
			runnable.run();
		}
		block.done();
		try {
			MethodInfo methodInfo = new MethodInfo(classInfo, name, desc,
					isStatic, methodThis, parameters, localVariables,
					visibility, cyclomaticComplexity, block.getOperations());
			classInfo.addMethod(methodInfo);
		} catch (IllegalStateException e) {
			throw new IllegalStateException("Error in " + classInfo + "."
					+ name + desc, e);
		}
	}

	public void visitTypeInsn(final int opcode, final String desc) {
		final String clazz = desc.replace('/', '.');
		recorder.add(new Runnable() {
			public void run() {
				Constant constant;
				switch (opcode) {
				case Opcodes.NEW:
					constant = new Constant("new " + clazz, Object.class);
					break;
				case Opcodes.NEWARRAY:
				case Opcodes.ANEWARRAY:
					constant = new Constant("new " + clazz, Object[].class);
					break;
				case Opcodes.INSTANCEOF:
					block.addOp(new Transform(lineNumber, 1, new Constant("?",
							boolean.class)));
					return;
				case Opcodes.CHECKCAST:
					return;
				default:
					throw new UnsupportedOperationException("" + opcode);
				}
				block.addOp(new Load(lineNumber, constant));
			}
		});
	}

	public void visitVarInsn(final int opcode, final int var) {
		switch (opcode) {
		case Opcodes.ILOAD:
		case Opcodes.LLOAD:
		case Opcodes.FLOAD:
		case Opcodes.DLOAD:
		case Opcodes.ALOAD:
			recorder.add(new Runnable() {
				public void run() {
					block.addOp(new Load(lineNumber, variable(var)));
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
					block.addOp(new Store(lineNumber, variable(var)));
				}
			});
			break;

		}
	}

	private Variable variable(int var) {
		if (variables.size() > var) {
			return variables.get(var);
		} else if (var == 0 && !isStatic) {
			variables.add(new Constant("this", Object.class));
			return variable(var);
		} else {
			variables.add(new LocalVariableInfo(("local_" + variables.size())));
			return variable(var);
		}
	}

	public void visitLabel(final Label label) {
		recorder.add(new Runnable() {
			public void run() {
				block.label(label);
			}
		});
	}

	public void visitLdcInsn(final Object cst) {
		recorder.add(new Runnable() {
			public void run() {
				block.addOp(new Load(lineNumber,
						new Constant(cst, Object.class)));
			}
		});
	}

	public void visitInsn(final int opcode) {
		switch (opcode) {
		case Opcodes.ACONST_NULL:
			recorder.add(new Runnable() {
				public void run() {
					block.addOp(new Load(lineNumber, new Constant(null,
							Object.class)));
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
			recordConstant(opcode - Opcodes.ICONST_M1 - 1, int.class);
			break;
		case Opcodes.LCONST_0:
		case Opcodes.LCONST_1:
			recordConstant(opcode - Opcodes.LCONST_0, long.class);
			break;
		case Opcodes.FCONST_0:
		case Opcodes.FCONST_1:
		case Opcodes.FCONST_2:
			recordConstant(opcode - Opcodes.FCONST_0, float.class);
			break;
		case Opcodes.DCONST_0:
		case Opcodes.DCONST_1:
			recordConstant(opcode - Opcodes.DCONST_0, double.class);
			break;
		case Opcodes.IALOAD:
		case Opcodes.LALOAD:
		case Opcodes.FALOAD:
		case Opcodes.DALOAD:
		case Opcodes.AALOAD:
		case Opcodes.BALOAD:
		case Opcodes.CALOAD:
		case Opcodes.SALOAD:
			recordArrayLoad();
			break;
		case Opcodes.IASTORE:
		case Opcodes.LASTORE:
		case Opcodes.FASTORE:
		case Opcodes.DASTORE:
		case Opcodes.AASTORE:
		case Opcodes.BASTORE:
		case Opcodes.CASTORE:
		case Opcodes.SASTORE:
			recordArrayStore();
			break;
		case Opcodes.POP:
		case Opcodes.POP2:
			recorder.add(new Runnable() {
					public void run() {
						block.addOp(new Pop(lineNumber, opcode - Opcodes.POP));
					}
				});
			break;
		case Opcodes.DUP:
		case Opcodes.DUP_X1:
		case Opcodes.DUP_X2:
			recorder.add(new Runnable() {
				public void run() {
					block.addOp(new Duplicate(lineNumber, opcode - Opcodes.DUP));
				}
			});
			break;
		case Opcodes.DUP2:
		case Opcodes.DUP2_X1:
		case Opcodes.DUP2_X2:
			recorder.add(new Runnable() {
				public void run() {
					block.addOp(new Duplicate2(lineNumber, opcode - Opcodes.DUP2));
				}
			});
			break;
		case Opcodes.SWAP:
			recorder.add(new Runnable() {
				public void run() {
					block.addOp(new Swap(lineNumber));
				}
			});
			break;
		case Opcodes.IRETURN:
		case Opcodes.LRETURN:
		case Opcodes.FRETURN:
		case Opcodes.DRETURN:
		case Opcodes.ARETURN:
			recorder.add(new Runnable() {
				public void run() {
					block.addOp(new Return(lineNumber));
				}
			});
			break;
		case Opcodes.ATHROW:
			recorder.add(new Runnable() {
				public void run() {
					block.addOp(new Throw(lineNumber));
				}
			});
			break;
		case Opcodes.RETURN:
			break;
		case Opcodes.LCMP:
			recordPopPush(2, long.class);
			break;
		case Opcodes.FCMPL:
		case Opcodes.FCMPG:
			recordPopPush(2, float.class);
			break;
		case Opcodes.DCMPL:
		case Opcodes.DCMPG:
			recordPopPush(2, double.class);
			break;
		case Opcodes.LSHL:
		case Opcodes.LSHR:
		case Opcodes.LUSHR:
		case Opcodes.LADD:
		case Opcodes.LSUB:
		case Opcodes.LDIV:
		case Opcodes.LREM:
		case Opcodes.LAND:
		case Opcodes.LOR:
		case Opcodes.LXOR:
		case Opcodes.LMUL:
			recordPopPush(2, long.class);
			break;
		case Opcodes.FADD:
		case Opcodes.FSUB:
		case Opcodes.FMUL:
		case Opcodes.FREM:
		case Opcodes.FDIV:
			recordPopPush(2, float.class);
			break;
		case Opcodes.ISHL:
		case Opcodes.ISHR:
		case Opcodes.IUSHR:
		case Opcodes.IADD:
		case Opcodes.ISUB:
		case Opcodes.IMUL:
		case Opcodes.IDIV:
		case Opcodes.IREM:
		case Opcodes.IAND:
		case Opcodes.IOR:
		case Opcodes.IXOR:
			recordPopPush(2, int.class);
			break;
		case Opcodes.DSUB:
		case Opcodes.DADD:
		case Opcodes.DMUL:
		case Opcodes.DDIV:
		case Opcodes.DREM:
			recordPopPush(2, double.class);
			break;
		case Opcodes.L2I:
		case Opcodes.L2F:
		case Opcodes.L2D:
		case Opcodes.LNEG:
			recordPopPush(1, long.class);
			break;
		case Opcodes.F2I:
		case Opcodes.F2L:
		case Opcodes.FNEG:
		case Opcodes.F2D:
			recordPopPush(1, float.class);
			break;
		case Opcodes.D2I:
		case Opcodes.D2L:
		case Opcodes.D2F:
		case Opcodes.DNEG:
			recordPopPush(1, double.class);
			break;
		case Opcodes.I2L:
		case Opcodes.I2F:
		case Opcodes.I2D:
		case Opcodes.I2B:
		case Opcodes.I2C:
		case Opcodes.I2S:
		case Opcodes.INEG:
			recordPopPush(1, int.class);
			break;
		case Opcodes.ARRAYLENGTH:
			recordPopPush(1, int.class);
			break;
		case Opcodes.MONITORENTER:
			recorder.add(new Runnable() {
				public void run() {
					block.addOp(new MonitorEnter(lineNumber));
				}
			});
			break;
		case Opcodes.MONITOREXIT:
			recorder.add(new Runnable() {
				public void run() {
					block.addOp(new MonitorExit(lineNumber));
				}
			});
			break;
		}
	}

	private void recordArrayLoad() {
		recorder.add(new Runnable() {
			public void run() {
				block.addOp(new ArrayLoad(lineNumber));
			}
		});
	}

	private void recordArrayStore() {
		recorder.add(new Runnable() {
			public void run() {
				block.addOp(new ArrayStore(lineNumber));
			}
		});
	}

	private void recordConstant(final int constant, final Class<?> type) {
		recorder.add(new Runnable() {
			public void run() {
				block.addOp(new Load(lineNumber, new Constant(constant, type)));
			}
		});
	}

	private void recordPopPush(final int popCount, final Class<?> type) {
		recorder.add(new Runnable() {
			public void run() {
				block.addOp(new Transform(lineNumber, popCount, new Constant(
						"?", type)));
			}
		});
	}

	public void visitFieldInsn(final int opcode, final String owner,
			final String name, final String desc) {
		switch (opcode) {
		case Opcodes.PUTSTATIC:
		case Opcodes.PUTFIELD:
			recorder.add(new Runnable() {
				public void run() {
					block.addOp(new PutField(lineNumber, classInfo
							.getField(name)));
				}
			});
			break;
		case Opcodes.GETSTATIC:
		case Opcodes.GETFIELD:
			recorder.add(new Runnable() {
				public void run() {
					FieldInfo field = repository.getClass(owner).getField(name);
					block.addOp(new GetField(lineNumber, field));
				}

			});
			break;
		}
	}

	public void visitMethodInsn(final int opcode, final String clazz,
			final String name, final String desc) {
		SignatureReader signatureReader = new SignatureReader(desc);
		ParameterCountVisitor counter = new ParameterCountVisitor();
		signatureReader.accept(counter);
		final int paramCount = counter.getParameterCount();
		final Class<?> returnType = counter.getReturnType();
		recorder.add(new Runnable() {
			public void run() {
				block.addOp(new Invoke(lineNumber, clazz.replace('/', '.'),
						name, desc, paramCount, opcode == Opcodes.INVOKESTATIC,
						returnType == null ? null : returnType.getName()));
			}
		});
	}

	public AnnotationVisitor visitAnnotation(String arg0, boolean arg1) {
		return null;
	}

	public AnnotationVisitor visitAnnotationDefault() {
		return null;
	}

	public void visitAttribute(Attribute arg0) {
	}

	public void visitCode() {
	}

	public void visitFrame(int arg0, int arg1, Object[] arg2, int arg3,
			Object[] arg4) {
		throw new UnsupportedOperationException();
	}

	public void visitIincInsn(int arg0, int arg1) {
	}

	public void visitIntInsn(int opcode, int operand) {
		switch (opcode) {
		case Opcodes.NEWARRAY:
			recordPopPush(1, Object[].class);
			break;
		case Opcodes.BIPUSH:
			recordConstant(operand, byte.class);
			break;
		case Opcodes.SIPUSH:
			recordConstant(operand, short.class);
			break;
		default:
			throw new UnsupportedOperationException("Unexpected opcode: "
					+ opcode);
		}
	}

	public void visitMaxs(int arg0, int arg1) {
	}

	public void visitMultiANewArrayInsn(String arg0, int arg1) {
		throw new UnsupportedOperationException();
	}

	public AnnotationVisitor visitParameterAnnotation(int arg0, String arg1,
			boolean arg2) {
		return null;
	}
}
