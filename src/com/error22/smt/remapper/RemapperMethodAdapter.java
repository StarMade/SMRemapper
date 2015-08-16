package com.error22.smt.remapper;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.RemappingAnnotationAdapter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public class RemapperMethodAdapter extends MethodVisitor {
	private SMRemapper remapper;

	protected RemapperMethodAdapter(int access, String desc,
			MethodVisitor mv) {
		super(Opcodes.ASM5, mv);
		this.remapper = SMRemapper.INSTANCE;
	}

	@Override
	public AnnotationVisitor visitAnnotationDefault() {
		AnnotationVisitor av = mv.visitAnnotationDefault();
		return av == null ? av : new RemappingAnnotationAdapter(av, remapper);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		AnnotationVisitor av = mv.visitAnnotation(remapper.mapDesc(desc),
				visible);
		return av == null ? av : new RemappingAnnotationAdapter(av, remapper);
	}

	@Override
	public AnnotationVisitor visitParameterAnnotation(int parameter,
			String desc, boolean visible) {
		AnnotationVisitor av = mv.visitParameterAnnotation(parameter,
				remapper.mapDesc(desc), visible);
		return av == null ? av : new RemappingAnnotationAdapter(av, remapper);
	}

	@Override
	public void visitFrame(int type, int nLocal, Object[] local, int nStack,
			Object[] stack) {
		super.visitFrame(type, nLocal, remapEntries(nLocal, local), nStack,
				remapEntries(nStack, stack));
	}

	private Object[] remapEntries(int n, Object[] entries) {
		for (int i = 0; i < n; i++) {
			if (entries[i] instanceof String) {
				Object[] newEntries = new Object[n];
				if (i > 0) {
					System.arraycopy(entries, 0, newEntries, 0, i);
				}
				do {
					Object t = entries[i];
					newEntries[i++] = t instanceof String ? remapper
							.mapType((String) t) : t;
				} while (i < n);
				return newEntries;
			}
		}
		return entries;
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name,
			String desc) {
		super.visitFieldInsn(
				opcode,
				remapper.mapType(owner),
				remapper.mapFieldName(owner, name, desc,
						findAccess(true, owner, name, desc), true),
				remapper.mapDesc(desc));
	}

	private int findAccess(boolean field, String owner, String name, String desc) {
		int access = -1;
		ClassNode clazz = remapper.getClass(owner);
		if (clazz != null) {
			if (field) {
				for (FieldNode f : clazz.fields) {
					if (f.name.equals(name) && f.desc.equals(desc)) {
						access = f.access;
						break;
					}
				}
			} else {
				for (MethodNode m : clazz.methods) {
					if (m.name.equals(name) && m.desc.equals(desc)) {
						access = m.access;
						break;
					}
				}
			}
		}

		return access;
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name,
			String desc, boolean itf) {

		super.visitMethodInsn(
				opcode,
				remapper.mapType(owner),
				remapper.mapMethodName(owner, name, desc,
						findAccess(false, owner, name, desc), true),
				remapper.mapMethodDesc(desc), itf);
	}

	@Override
	public void visitInvokeDynamicInsn(String name, String desc, Handle bsm,
			Object... bsmArgs) {
		for (int i = 0; i < bsmArgs.length; i++) {
			bsmArgs[i] = remapper.mapValue(bsmArgs[i]);
		}
		super.visitInvokeDynamicInsn(
				remapper.mapInvokeDynamicMethodName(name, desc),
				remapper.mapMethodDesc(desc), (Handle) remapper.mapValue(bsm),
				bsmArgs);
	}

	@Override
	public void visitTypeInsn(int opcode, String type) {
		super.visitTypeInsn(opcode, remapper.mapType(type));
	}

	@Override
	public void visitLdcInsn(Object cst) {
		super.visitLdcInsn(remapper.mapValue(cst));
	}

	@Override
	public void visitMultiANewArrayInsn(String desc, int dims) {
		super.visitMultiANewArrayInsn(remapper.mapDesc(desc), dims);
	}

	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler,
			String type) {
		super.visitTryCatchBlock(start, end, handler, type == null ? null
				: remapper.mapType(type));
	}

	@Override
	public void visitLocalVariable(String name, String desc, String signature,
			Label start, Label end, int index) {
		super.visitLocalVariable(name, remapper.mapDesc(desc),
				remapper.mapSignature(signature, true), start, end, index);
	}
}