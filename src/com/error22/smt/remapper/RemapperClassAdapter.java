package com.error22.smt.remapper;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;

public class RemapperClassAdapter extends ClassVisitor {
	private String className;
	private SMRemapper remapper;

	protected RemapperClassAdapter(ClassVisitor cv) {
		super(Opcodes.ASM5, cv);
		remapper = SMRemapper.INSTANCE;
	}

	@Override
	public void visit(int version, int access, String name, String signature,
			String superName, String[] interfaces) {
		className = name;

		String newName = remapper.mapType(name);
		String newSignature = remapper.mapSignature(signature, false);
		String newSuperName = remapper.mapType(superName);
		String[] newInterfaces = interfaces != null ? remapper
				.mapTypes(interfaces) : null;

		super.visit(version, access, newName, newSignature, newSuperName,
				newInterfaces);
	}

	@Override
	public void visitInnerClass(String name, String outerName,
			String innerName, int access) {
		String newName = remapper.mapType(name);
		String newOuterName = outerName != null ? remapper.mapType(outerName)
				: null;
		String newInnerName = innerName != null ? newName.substring(newName
				.lastIndexOf(newName.contains("$") ? '$' : '/') + 1) : null;

		super.visitInnerClass(newName, newOuterName, newInnerName, access);
	}

	@Override
	public void visitOuterClass(String owner, String name, String desc) {
		String newOwner = remapper.mapType(owner);
		String newName = name != null ? remapper.mapMethodName(owner, name,
				desc) : null;
		String newDesc = desc != null ? remapper.mapMethodDesc(desc) : null;

		super.visitOuterClass(newOwner, newName, newDesc);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc,
			String signature, Object value) {
		String newName = remapper.mapFieldName(className, name, desc, access,
				true);
		String newDesc = remapper.mapDesc(desc);
		String newSignature = remapper.mapSignature(signature, true);
		Object newValue = remapper.mapValue(value);

		return super.visitField(access, newName, newDesc, newSignature,
				newValue);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {
		String newName = remapper.mapMethodName(className, name, desc, access,
				true);
		String newDesc = remapper.mapMethodDesc(desc);
		String newSignature = remapper.mapSignature(signature, false);
		String[] newExceptions = exceptions != null ? remapper
				.mapTypes(exceptions) : null;

		return new RemapperMethodAdapter(access, newDesc,
				super.visitMethod(access, newName, newDesc, newSignature,
						newExceptions));
	}
	
	@Override
	public AnnotationVisitor visitTypeAnnotation(int typeRef,
			TypePath typePath, String desc, boolean visible) {
		throw new RuntimeException("Not used!");
//		return super.visitTypeAnnotation(typeRef, typePath, desc, visible);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		Type t = Type.getType(desc);

		 RemapperAnnotationVisitor.log("visitAnnotation:MAIN  " + desc + "  " + visible
		 + " " + remapper.mapDesc(desc));

		if (t.getSort() == 10) {
			AnnotationVisitor av;
			av = super.visitAnnotation(remapper.mapDesc(desc), visible);
			return av == null ? null : new RemapperAnnotationVisitor(av,
					t.getInternalName(), className, visible);
		} else {
			throw new RuntimeException("Unsupported annotation desc " + desc
					+ " !");
		}

	}

	@Override
	public void visitSource(String source, String debug) {
	}
}
