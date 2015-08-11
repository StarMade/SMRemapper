package com.error22.smt.remapper;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * *This class is horrible and is not complete or working, this should not
 * really be included in the repo yet but it is required * 
 * Due to asm remapping issues, annotations do not get correctly renamed.
 * The main issue is that the method names are not updated and it is hard
 * to remap the names as in some cases like arrays you do not know the
 * type at the point where you want to rename the method so you can not
 * lookup the new mapping. This is the main focus of bug fixing at the
 * moment
 */
public class RemapperAnnotationVisitor extends AnnotationVisitor {
	private SMRemapper remapper;
	private String clazz;
	
	protected RemapperAnnotationVisitor(AnnotationVisitor av, String clazz) {
		super(Opcodes.ASM5, av);
		this.remapper = SMRemapper.INSTANCE;
		this.clazz = clazz;
	}
	
	@Override
	public void visit(String name, Object value) {
		if(value instanceof Type || value instanceof Handle){
			throw new UnsupportedOperationException("RemapAnnotationVisitor:visit("+clazz+") "+name+" "+value+"("+value.getClass()+")");
		}
		
		//Class3142iF
		
		if(value.getClass().isArray()){
			System.out.println("Please report this to the developer, this is not a bug but it is a very "
					+ "important as it will allow this version to be improved!");
			System.out.println("RemapAnnotationVisitor:visit("+clazz+") "+name+" "+value+"("+value.getClass()+")");
		}
		System.out.println("AnnotationVisitor:visit " + name + " "
				+ value + " " + remapper.mapValue(value) + " "
				+ value.getClass());
//		MD: obfuscated/nK/g ()V deobf/Class3386nK/method45427g ()V
		
		System.out.println("RemapAnnotationVisitor:visit("+clazz+") "+name+" "+value+"("+value.getClass()+")");
		System.out.println(" "+(clazz+"/"+name+" ()"+Type.getDescriptor(value.getClass())));
		av.visit(remapper.mapMethodName(clazz, name, "()"+Type.getDescriptor(value.getClass())), value);
	}
	
	@Override
	public void visitEnum(String name, String desc, String value) {
//		throw new UnsupportedOperationException("RemapAnnotationVisitor:visitEnum("+clazz+") "+name+" "+desc+" "+value+"("+value.getClass()+")");
		av.visitEnum(name, remapper.mapDesc(desc), value);
	}
	
	@Override
	public AnnotationVisitor visitAnnotation(String name, String desc) {
//		throw new UnsupportedOperationException("RemapAnnotationVisitor:visitAnnotation("+clazz+") "+name+" "+desc);
		AnnotationVisitor v = av.visitAnnotation(name, remapper.mapDesc(desc));
		return v == av ? this : v == null ? null : new RemapperAnnotationVisitor(v, clazz);
	}
	
	@Override
	public AnnotationVisitor visitArray(String name) {
		if(clazz.startsWith("java/lang")){
			System.out.println("RemapAnnotationVisitor:visitArray("+clazz+") '"+name+"' - Allowing as lang class");
			
			AnnotationVisitor v = av.visitArray(name);
			return v == av ? this : v == null ? null : new RemapperAnnotationVisitor(v, clazz);
		}
		throw new UnsupportedOperationException("RemapAnnotationVisitor:visitArray("+clazz+") '"+name+"' - None lang classes are not supported yet!");
	}
}