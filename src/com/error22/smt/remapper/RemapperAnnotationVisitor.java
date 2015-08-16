package com.error22.smt.remapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

/**
 * This annotation visitor partly works and has not been fully tested!
 * A good annotation to test with is Class3142iF
 */
public class RemapperAnnotationVisitor extends AnnotationVisitor {
	private SMRemapper remapper;
	private String clazz, ownerClazz;
	private boolean visible;
	private AnnotationNode node;
	private HashMap<String, Object> valueMap;

	protected RemapperAnnotationVisitor(AnnotationVisitor av, String clazz,
			String ownerClazz, boolean visible) {
		super(Opcodes.ASM5, av);
		this.remapper = SMRemapper.INSTANCE;
		this.clazz = clazz;
		this.ownerClazz = ownerClazz;
		this.visible = visible;

		ClassNode cn = remapper.getClass(ownerClazz);

		List<AnnotationNode> annotationNodes = visible ? cn.visibleAnnotations
				: cn.invisibleAnnotations;
		for (AnnotationNode n : annotationNodes) {
			if (Type.getType(n.desc).getInternalName().equals(clazz)) {
				if (node != null) {
					throw new RuntimeException(
							"Multiple annotations with the same type! " + clazz
									+ " in " + ownerClazz);
				}
				node = n;
			}
		}

		if (node == null) {
			throw new RuntimeException(
					"No annotation node found with the type! " + clazz + " in "
							+ ownerClazz);
		}

		valueMap = new HashMap<String, Object>();

		if (node.values != null) {
			if (node.values.size() % 2 != 0) {
				throw new RuntimeException("Values are not even!");
			}

			// for(Object o : node.values){
			//
			// log(" ~ "+o+" "+(o instanceof
			// ArrayList)+"  "+(o.getClass())+" "+(o.getClass().isArray()));
			// if(o instanceof ArrayList){
			// log("ARRAY");
			// for(Object a1 : (ArrayList)o){
			// log("ARRAY VALUE");
			// log(" ~~ "+a1+" "+(a1 instanceof
			// ArrayList)+"  "+(a1.getClass())+" "+(a1.getClass().isArray()));
			// for(Object a2 : (Object[]) a1){
			// log(" ~~~ "+a2);
			// }
			//
			// }
			// }
			// }

			for (int i = 0; i < node.values.size(); i += 2) {
				valueMap.put((String) node.values.get(i),
						node.values.get(i + 1));

				log(((String) node.values.get(i)) + "="
						+ node.values.get(i + 1) + "("
						+ node.values.get(i + 1).getClass() + ")");
			}
		}

	}

	@Override
	public void visit(String name, Object value) {
		if (value instanceof Type || value instanceof Handle) {
			throw new UnsupportedOperationException(
					"RemapAnnotationVisitor:visit(" + clazz + ") " + name + " "
							+ value + "(" + value.getClass() + ")");
		}

		log("RemapAnnotationVisitor:visit(" + clazz + ") "
				+ name + " = " + value + "(" + value.getClass() + ")");
		av.visit(
				remapper.mapMethodName(clazz, name,
						"()" + Type.getDescriptor(value.getClass())),
				remapper.mapValue(value));
	}

	@Override
	public void visitEnum(String name, String desc, String value) {
		log("RemapAnnotationVisitor:visitEnum(" + clazz + ") "
				+ name + " " + desc + " " + value + "(" + value.getClass()
				+ ")");
		av.visitEnum(name, remapper.mapDesc(desc), value);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String name, String desc) {
		// if(clazz.startsWith("java/lang")){
		// AnnotationVisitor v = av.visitAnnotation(name,
		// remapper.mapDesc(desc));
		// return v == av ? this : v == null ? null : new
		// RemapperAnnotationVisitor(v, clazz);
		// }

		throw new UnsupportedOperationException(
				"RemapAnnotationVisitor:visitAnnotation(" + clazz + ") " + name
						+ " " + desc);
		// AnnotationVisitor v = av.visitAnnotation(name,
		// remapper.mapDesc(desc));
		// return v == av ? this : v == null ? null : new
		// RemapperAnnotationVisitor(v, clazz);
	}

	@SuppressWarnings("unchecked")
	@Override
	public AnnotationVisitor visitArray(String name) {
		log("RemapAnnotationVisitor:visitArray(" + clazz + ") '"
				+ name + "'");
		ArrayList<Object> vals = (ArrayList<Object>) valueMap.get(name);

		String type = null;
		for (Object o : vals) {
			String valType = (String) ((Object[]) o)[0];

			if (type != null && !type.equals(valType)) {
				throw new RuntimeException("Array descriptions are different "
						+ type + " " + valType);
			}

			type = valType;
		}
		log("Remapping: " + clazz + "/" + name + " ()" + type
				+ " => " + remapper.mapMethodName(clazz, name, "()" + type));

		AnnotationVisitor v = av.visitArray(remapper.mapMethodName(clazz, name,
				"()" + type));
		return v == av ? this : v == null ? null
				: new RemapperAnnotationVisitor(v, clazz, ownerClazz, visible);
	}
	
	public static void log(String msg){
		if(DEBUG){
			System.out.println(msg);
		}
	}
	
	private static final boolean DEBUG = false;
}