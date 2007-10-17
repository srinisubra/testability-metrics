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

import static com.google.test.metric.asm.SignatureParser.parse;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.google.test.metric.ClassInfo;
import com.google.test.metric.ClassRepository;
import com.google.test.metric.FieldInfo;
import com.google.test.metric.LocalVariableInfo;
import com.google.test.metric.MethodInfo;
import com.google.test.metric.ParameterInfo;
import com.google.test.metric.Type;
import com.google.test.metric.Variable;
import com.google.test.metric.method.BlockDecomposer;
import com.google.test.metric.method.Constant;
import com.google.test.metric.method.op.stack.ArrayLoad;
import com.google.test.metric.method.op.stack.ArrayStore;
import com.google.test.metric.method.op.stack.Convert;
import com.google.test.metric.method.op.stack.Duplicate;
import com.google.test.metric.method.op.stack.Duplicate2;
import com.google.test.metric.method.op.stack.GetField;
import com.google.test.metric.method.op.stack.Invoke;
import com.google.test.metric.method.op.stack.Load;
import com.google.test.metric.method.op.stack.MonitorEnter;
import com.google.test.metric.method.op.stack.MonitorExit;
import com.google.test.metric.method.op.stack.MultiANewArrayIns;
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
		parameterCount = parse(desc).getParameters().size();
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
					switch (opcode) {
					case Opcodes.IFEQ:
						if1("IFEQ");
						break;
					case Opcodes.IFNE:
						if1("IFNE");
						break;
					case Opcodes.IFLT:
						if1("IFLT");
						break;
					case Opcodes.IFGE:
						if1("IFGE");
						break;
					case Opcodes.IFGT:
						if1("IFGT");
						break;
					case Opcodes.IFLE:
						if1("IFLE");
						break;
					case Opcodes.IFNONNULL:
						if1("IFNONNULL");
						break;
					case Opcodes.IFNULL:
						if1("IFNULL");
						break;
					case Opcodes.IF_ACMPEQ:
						if2("IF_ACMPEQ");
						break;
					case Opcodes.IF_ACMPNE:
						if2("IF_ACMPNE");
						break;
					case Opcodes.IF_ICMPEQ:
						if2("IF_ICMPEQ");
						break;
					case Opcodes.IF_ICMPGE:
						if2("IF_ICMPGE");
						break;
					case Opcodes.IF_ICMPGT:
						if2("IF_ICMPGT");
						break;
					case Opcodes.IF_ICMPLE:
						if2("IF_ICMPLE");
						break;
					case Opcodes.IF_ICMPLT:
						if2("IF_ICMPLT");
						break;
					case Opcodes.IF_ICMPNE:
						if2("IF_ICMPNE");
						break;
					default:
						throw new UnsupportedOperationException("" + opcode);
					}
					block.conditionalGoto(label);
				}

				private void if1(String name) {
					block.addOp(new Transform(lineNumber, name, Type.INT, null,
							null));
				}

				private void if2(String name) {
					block.addOp(new Transform(lineNumber, name, Type.INT,
							Type.INT, null));
				}
			});
		}
	}

	public void visitTryCatchBlock(final Label start, final Label end,
			final Label handler, final String type) {
		if (type != null) {
			cyclomaticComplexity++;
		}
		recorder.add(new Runnable() {
			public void run() {
				block.tryCatchBlock(start, end, handler, type);
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
				block.addOp(new Pop(lineNumber, 1));
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
				block.addOp(new Pop(lineNumber, 1));
				block.tableSwitch(dflt, labels);
			}
		});
	}

	public void visitLocalVariable(String name, String desc, String signature,
			Label start, Label end, int index) {
		Type type = Type.fromCode(desc);
		Variable variable;
		if (index == 0 && !isStatic) {
			variable = methodThis = new LocalVariableInfo(name, type);
		} else if (index < parameterCount + (isInstanceMethod() ? 1 : 0)) {
			variable = new ParameterInfo(name, type);
		} else {
			variable = new LocalVariableInfo(name, type);
		}
		variables.add(variable);
		if (variable.getType().isDouble()) {
			variables.add(variable);
		}
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
			if (variable == null) {
			} else if (variable.getName().equals("this")) {
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
				switch (opcode) {
				case Opcodes.NEW:
					block.addOp(new Load(lineNumber, new Constant("new "
							+ clazz, Type.ADDRESS)));
					break;
				case Opcodes.NEWARRAY:
				case Opcodes.ANEWARRAY:
					block.addOp(new Transform(lineNumber, "newarray", Type.INT,
							null, Type.ADDRESS));
					break;
				case Opcodes.INSTANCEOF:
					block.addOp(new Transform(lineNumber, "instanceof",
							Type.ADDRESS, null, Type.INT));
					break;
				case Opcodes.CHECKCAST:
					block.addOp(new Transform(lineNumber, "checkcast",
							Type.ADDRESS, null, Type.ADDRESS));
					break;
				default:
					throw new UnsupportedOperationException("" + opcode);
				}
			}
		});
	}

	public void visitVarInsn(final int opcode, final int var) {
		switch (opcode) {
		case Opcodes.ILOAD:
			load(var, Type.INT);
			break;
		case Opcodes.LLOAD:
			load(var, Type.LONG);
			break;
		case Opcodes.FLOAD:
			load(var, Type.FLOAT);
			break;
		case Opcodes.DLOAD:
			load(var, Type.DOUBLE);
			break;
		case Opcodes.ALOAD:
			load(var, Type.ADDRESS);
			break;

		case Opcodes.ISTORE:
			store(var, Type.INT);
			break;
		case Opcodes.LSTORE:
			store(var, Type.LONG);
			break;
		case Opcodes.FSTORE:
			store(var, Type.FLOAT);
			break;
		case Opcodes.DSTORE:
			store(var, Type.DOUBLE);
			break;
		case Opcodes.ASTORE:
			store(var, Type.ADDRESS);
			break;

		}
	}

	private void store(final int var, final Type type) {
		recorder.add(new Runnable() {
			public void run() {
				block.addOp(new Store(lineNumber, variable(var, type)));
			}
		});
	}

	private void load(final int var, final Type type) {
		recorder.add(new Runnable() {
			public void run() {
				block.addOp(new Load(lineNumber, variable(var, type)));
			}
		});
	}

	private Variable variable(int varIndex, Type type) {
		Variable variable = variables.get(varIndex);
		if (variable == null) {
			if (varIndex == 0 && !isStatic) {
				variable = new LocalVariableInfo("this", Type.ADDRESS);
				variables.set(varIndex, variable);
			} else {
				variable = new LocalVariableInfo("local_" + varIndex, type);
				variables.set(varIndex, variable);
				if (type.isDouble()) {
					LocalVariableInfo var2 = new LocalVariableInfo("local2_"
							+ varIndex, type);
					variables.set(varIndex + 1, var2);
				}
			}
		}
		if (variable.getType() != type) {
			// Apparently the compiler reuses local variables and it is possible
			// that the types change. So if types change we have to drop
			// the variable and try again.
			variables.set(varIndex, null);
			return variable(varIndex, type);
		}
		return variable;
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
				block.addOp(new Load(lineNumber, new Constant(cst, Type
						.fromClass(cst.getClass()))));
			}
		});
	}

	public void visitInsn(final int opcode) {
		switch (opcode) {
		case Opcodes.ACONST_NULL:
			recorder.add(new Runnable() {
				public void run() {
					block.addOp(new Load(lineNumber, new Constant(null,
							Type.ADDRESS)));
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
			loadConstant(opcode - Opcodes.ICONST_M1 - 1, Type.INT);
			break;
		case Opcodes.LCONST_0:
		case Opcodes.LCONST_1:
			loadConstant(opcode - Opcodes.LCONST_0, Type.LONG);
			break;
		case Opcodes.FCONST_0:
		case Opcodes.FCONST_1:
		case Opcodes.FCONST_2:
			loadConstant(opcode - Opcodes.FCONST_0, Type.FLOAT);
			break;
		case Opcodes.DCONST_0:
		case Opcodes.DCONST_1:
			loadConstant(opcode - Opcodes.DCONST_0, Type.DOUBLE);
			break;
		case Opcodes.IALOAD:
			recordArrayLoad(Type.INT);
			break;
		case Opcodes.LALOAD:
			recordArrayLoad(Type.LONG);
			break;
		case Opcodes.FALOAD:
			recordArrayLoad(Type.FLOAT);
			break;
		case Opcodes.DALOAD:
			recordArrayLoad(Type.DOUBLE);
			break;
		case Opcodes.AALOAD:
			recordArrayLoad(Type.ADDRESS);
			break;
		case Opcodes.BALOAD:
			recordArrayLoad(Type.BYTE);
			break;
		case Opcodes.CALOAD:
			recordArrayLoad(Type.CHAR);
			break;
		case Opcodes.SALOAD:
			recordArrayLoad(Type.SHORT);
			break;

		case Opcodes.IASTORE:
			recordArrayStore(Type.INT);
			break;
		case Opcodes.LASTORE:
			recordArrayStore(Type.LONG);
			break;
		case Opcodes.FASTORE:
			recordArrayStore(Type.FLOAT);
			break;
		case Opcodes.DASTORE:
			recordArrayStore(Type.DOUBLE);
			break;
		case Opcodes.AASTORE:
			recordArrayStore(Type.ADDRESS);
			break;
		case Opcodes.BASTORE:
			recordArrayStore(Type.BYTE);
			break;
		case Opcodes.CASTORE:
			recordArrayStore(Type.CHAR);
			break;
		case Opcodes.SASTORE:
			recordArrayStore(Type.SHORT);
			break;
		case Opcodes.POP:
		case Opcodes.POP2:
			recorder.add(new Runnable() {
				public void run() {
					block.addOp(new Pop(lineNumber, opcode - Opcodes.POP + 1));
				}
			});
			break;
		case Opcodes.DUP:
		case Opcodes.DUP_X1:
		case Opcodes.DUP_X2:
			recorder.add(new Runnable() {
				public void run() {
					int offset = opcode - Opcodes.DUP;
					block.addOp(new Duplicate(lineNumber, offset));
				}
			});
			break;
		case Opcodes.DUP2:
		case Opcodes.DUP2_X1:
		case Opcodes.DUP2_X2:
			recorder.add(new Runnable() {
				public void run() {
					block.addOp(new Duplicate2(lineNumber, opcode
							- Opcodes.DUP2));
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
			_return(Type.INT);
			break;
		case Opcodes.FRETURN:
			_return(Type.FLOAT);
			break;
		case Opcodes.ARETURN:
			_return(Type.ADDRESS);
			break;
		case Opcodes.LRETURN:
			_return(Type.LONG);
			break;
		case Opcodes.DRETURN:
			_return(Type.DOUBLE);
			break;
		case Opcodes.ATHROW:
			recorder.add(new Runnable() {
				public void run() {
					block.addOp(new Throw(lineNumber));
				}
			});
			break;
		case Opcodes.RETURN:
			_return(Type.VOID);
			break;
		case Opcodes.LCMP:
			operation("cmp", Type.LONG, Type.LONG, Type.INT);
			break;
		case Opcodes.FCMPL:
			operation("cmpl", Type.FLOAT, Type.FLOAT, Type.INT);
			break;
		case Opcodes.FCMPG:
			operation("cmpg", Type.FLOAT, Type.FLOAT, Type.INT);
			break;
		case Opcodes.DCMPL:
			operation("cmpl", Type.DOUBLE, Type.DOUBLE, Type.INT);
			break;
		case Opcodes.DCMPG:
			operation("cmpg", Type.DOUBLE, Type.DOUBLE, Type.INT);
			break;
		case Opcodes.LSHL:
			operation("shl", Type.LONG, Type.INT, Type.LONG);
			break;
		case Opcodes.LSHR:
			operation("shr", Type.LONG, Type.INT, Type.LONG);
			break;
		case Opcodes.LUSHR:
			operation("ushr", Type.LONG, Type.INT, Type.LONG);
			break;
		case Opcodes.LADD:
			operation("add", Type.LONG, Type.LONG, Type.LONG);
			break;
		case Opcodes.LSUB:
			operation("sub", Type.LONG, Type.LONG, Type.LONG);
			break;
		case Opcodes.LDIV:
			operation("div", Type.LONG, Type.LONG, Type.LONG);
			break;
		case Opcodes.LREM:
			operation("rem", Type.LONG, Type.LONG, Type.LONG);
			break;
		case Opcodes.LAND:
			operation("and", Type.LONG, Type.LONG, Type.LONG);
			break;
		case Opcodes.LOR:
			operation("or", Type.LONG, Type.LONG, Type.LONG);
			break;
		case Opcodes.LXOR:
			operation("xor", Type.LONG, Type.LONG, Type.LONG);
			break;
		case Opcodes.LMUL:
			operation("mul", Type.LONG, Type.LONG, Type.LONG);
			break;
		case Opcodes.FADD:
			operation("add", Type.FLOAT, Type.FLOAT, Type.FLOAT);
			break;
		case Opcodes.FSUB:
			operation("sub", Type.FLOAT, Type.FLOAT, Type.FLOAT);
			break;
		case Opcodes.FMUL:
			operation("mul", Type.FLOAT, Type.FLOAT, Type.FLOAT);
			break;
		case Opcodes.FREM:
			operation("rem", Type.FLOAT, Type.FLOAT, Type.FLOAT);
			break;
		case Opcodes.FDIV:
			operation("div", Type.FLOAT, Type.FLOAT, Type.FLOAT);
			break;
		case Opcodes.ISHL:
			operation("shl", Type.INT, Type.INT, Type.INT);
			break;
		case Opcodes.ISHR:
			operation("shr", Type.INT, Type.INT, Type.INT);
			break;
		case Opcodes.IUSHR:
			operation("ushr", Type.INT, Type.INT, Type.INT);
			break;
		case Opcodes.IADD:
			operation("add", Type.INT, Type.INT, Type.INT);
			break;
		case Opcodes.ISUB:
			operation("sub", Type.INT, Type.INT, Type.INT);
			break;
		case Opcodes.IMUL:
			operation("mul", Type.INT, Type.INT, Type.INT);
			break;
		case Opcodes.IDIV:
			operation("div", Type.INT, Type.INT, Type.INT);
			break;
		case Opcodes.IREM:
			operation("rem", Type.INT, Type.INT, Type.INT);
			break;
		case Opcodes.IAND:
			operation("and", Type.INT, Type.INT, Type.INT);
			break;
		case Opcodes.IOR:
			operation("or", Type.INT, Type.INT, Type.INT);
			break;
		case Opcodes.IXOR:
			operation("xor", Type.INT, Type.INT, Type.INT);
			break;
		case Opcodes.DSUB:
			operation("sub", Type.DOUBLE, Type.DOUBLE, Type.DOUBLE);
			break;
		case Opcodes.DADD:
			operation("add", Type.DOUBLE, Type.DOUBLE, Type.DOUBLE);
			break;
		case Opcodes.DMUL:
			operation("mul", Type.DOUBLE, Type.DOUBLE, Type.DOUBLE);
			break;
		case Opcodes.DDIV:
			operation("div", Type.DOUBLE, Type.DOUBLE, Type.DOUBLE);
			break;
		case Opcodes.DREM:
			operation("rem", Type.DOUBLE, Type.DOUBLE, Type.DOUBLE);
			break;
		case Opcodes.L2I:
			convert(Type.LONG, Type.INT);
			break;
		case Opcodes.L2F:
			convert(Type.LONG, Type.FLOAT);
			break;
		case Opcodes.L2D:
			convert(Type.LONG, Type.DOUBLE);
			break;
		case Opcodes.LNEG:
			operation("neg", Type.LONG, null, Type.LONG);
			break;
		case Opcodes.F2I:
			convert(Type.FLOAT, Type.INT);
			break;
		case Opcodes.F2L:
			convert(Type.FLOAT, Type.LONG);
			break;
		case Opcodes.FNEG:
			operation("neg", Type.FLOAT, null, Type.FLOAT);
			break;
		case Opcodes.F2D:
			convert(Type.FLOAT, Type.DOUBLE);
			break;
		case Opcodes.D2I:
			convert(Type.DOUBLE, Type.INT);
			break;
		case Opcodes.D2L:
			convert(Type.DOUBLE, Type.LONG);
			break;
		case Opcodes.D2F:
			convert(Type.DOUBLE, Type.FLOAT);
			break;
		case Opcodes.DNEG:
			operation("neg", Type.DOUBLE, null, Type.DOUBLE);
			break;
		case Opcodes.I2L:
			convert(Type.INT, Type.LONG);
			break;
		case Opcodes.I2F:
			convert(Type.INT, Type.FLOAT);
			break;
		case Opcodes.I2D:
			convert(Type.INT, Type.DOUBLE);
			break;
		case Opcodes.I2B:
			convert(Type.INT, Type.BYTE);
			break;
		case Opcodes.I2C:
			convert(Type.INT, Type.CHAR);
			break;
		case Opcodes.I2S:
			convert(Type.INT, Type.SHORT);
			break;
		case Opcodes.INEG:
			operation("neg", Type.INT, null, Type.INT);
			break;
		case Opcodes.ARRAYLENGTH:
			operation("arraylength", Type.ADDRESS, null, Type.INT);
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

	private void operation(final String operation, final Type op1,
			final Type op2, final Type result) {
		recorder.add(new Runnable() {
			public void run() {
				block.addOp(new Transform(lineNumber, operation, op1, op2,
						result));
			}
		});
	}

	private void convert(final Type from, final Type to) {
		recorder.add(new Runnable() {
			public void run() {
				block.addOp(new Convert(lineNumber, from, to));
			}
		});
	}

	private void _return(final Type type) {
		recorder.add(new Runnable() {
			public void run() {
				block.addOp(new Return(lineNumber, type));
			}
		});
	}

	private void recordArrayLoad(final Type type) {
		recorder.add(new Runnable() {
			public void run() {
				block.addOp(new ArrayLoad(lineNumber, type));
			}
		});
	}

	private void recordArrayStore(final Type type) {
		recorder.add(new Runnable() {
			public void run() {
				block.addOp(new ArrayStore(lineNumber, type));
			}
		});
	}

	private void loadConstant(final int constant, final Type type) {
		recorder.add(new Runnable() {
			public void run() {
				block.addOp(new Load(lineNumber, new Constant(constant, type)));
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
					FieldInfo field = repository.getClass(owner).getField(name);
					block.addOp(new PutField(lineNumber, field));
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
		SignatureParser signature = parse(desc);
		final List<Type> params = signature.getParameters();
		final Type returnType = signature.getReturnType();
		recorder.add(new Runnable() {
			public void run() {
				block.addOp(new Invoke(lineNumber, clazz.replace('/', '.'),
						name, desc, params, opcode == Opcodes.INVOKESTATIC,
						returnType));
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
			newArray(operand);
			break;
		case Opcodes.BIPUSH:
			loadConstant(operand, Type.INT);
			break;
		case Opcodes.SIPUSH:
			loadConstant(operand, Type.INT);
			break;
		default:
			throw new UnsupportedOperationException("Unexpected opcode: "
					+ opcode);
		}
	}

	private void newArray(final int operand) {
		recorder.add(new Runnable() {
			public void run() {
				block.addOp(new Transform(lineNumber, "newarray", Type.INT,
						null, Type.ADDRESS));
			}
		});
	}

	public void visitMaxs(int maxStack, int maxLocals) {
		while (variables.size() <= maxLocals) {
			variables.add(null);
		}
	}

	public void visitMultiANewArrayInsn(final String clazz, final int dims) {
		recorder.add(new Runnable() {
			public void run() {
				block.addOp(new MultiANewArrayIns(lineNumber, clazz, dims));
			}
		});
	}

	public AnnotationVisitor visitParameterAnnotation(int arg0, String arg1,
			boolean arg2) {
		return null;
	}

	@Override
	public String toString() {
		return classInfo + "." + name + desc + "\n" + block;
	}
}
