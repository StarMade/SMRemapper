package com.error22.smt.remapper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;

import com.google.common.io.Files;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class SMRemapper extends Remapper {
	public static final int CLASS_LENGTH = ".class".length();
	public static SMRemapper INSTANCE;
	private HashMap<String, String> classMap;
	private HashMap<String, ClassNode> classNodeMap;
	private HashMap<StringTriple, StringTriple> fieldMap, methodMap;
	private HashMap<String, JarEntry> jarMap;
	private JarFile jar;
	private boolean keepSource;

	public void init(File input, File output, File mapping, File libsFolder, boolean reverse, boolean keepSource)
			throws Exception {
		this.keepSource = keepSource;
		System.out.println("Loading mappings...");

		String data = Files.toString(mapping, Charset.defaultCharset());
		if (!data.substring(data.indexOf("<") + 1, data.indexOf(">")).equals("smtmap 1")) {
			System.out.println("Unsupported mapping format!");
			System.exit(0);
		}

		JsonObject mappingsJson = new GsonBuilder().create().fromJson(data.substring(data.indexOf(">") + 1),
				JsonObject.class);

		JsonObject info = mappingsJson.getAsJsonObject("info");
		System.out.println("    Mapping Info:");
		System.out.println("        StarMade Build : " + info.getAsJsonPrimitive("build").getAsString());
		System.out.println("        Mapping Version: " + info.getAsJsonPrimitive("version").getAsString());
		System.out.println("        Mapping Date   : " + info.getAsJsonPrimitive("date").getAsString());
		String creator = info.getAsJsonPrimitive("creator").getAsString();
		System.out.println("        Mapping Creator: " + creator + " ("
				+ (creator.equalsIgnoreCase("error22") ? "Official" : "Unofficial") + ")");
		if (!creator.equalsIgnoreCase("error22")) {
			System.out.println("        * WARNING * You are using an unofficial mapping file.");
			System.out.println("        * WARNING * This may cause issues, do not report any issues directly to SMT");
			System.out.println("        * WARNING * as it may be the mapping creators fault, ask them first!");
		}

		System.out.println("    Loading classes...");
		classMap = new HashMap<String, String>();
		JsonArray classesJson = mappingsJson.getAsJsonArray("classes");
		for (JsonElement elem : classesJson) {
			JsonObject obj = (JsonObject) elem;
			String oldName = obj.getAsJsonPrimitive("oldName").getAsString();
			String newName = obj.getAsJsonPrimitive("newName").getAsString();

			if (reverse) {
				String temp = oldName;
				oldName = newName;
				newName = temp;
			}

			classMap.put(oldName, newName);
		}

		System.out.println("    Loading fields...");
		fieldMap = new HashMap<StringTriple, StringTriple>();
		JsonArray fieldsJson = mappingsJson.getAsJsonArray("fields");
		for (JsonElement elem : fieldsJson) {
			JsonObject obj = (JsonObject) elem;
			String oldClass = obj.getAsJsonPrimitive("oldClass").getAsString();
			String oldName = obj.getAsJsonPrimitive("oldName").getAsString();
			String oldDesc = obj.getAsJsonPrimitive("oldDesc").getAsString();
			String newClass = obj.getAsJsonPrimitive("newClass").getAsString();
			String newName = obj.getAsJsonPrimitive("newName").getAsString();
			String newDesc = obj.getAsJsonPrimitive("newDesc").getAsString();

			if (reverse) {
				String temp = oldName;
				oldName = newName;
				newName = temp;

				temp = oldDesc;
				oldDesc = newDesc;
				newDesc = temp;

				temp = oldClass;
				oldClass = newClass;
				newClass = temp;
			}

			fieldMap.put(new StringTriple(oldClass, oldName, oldDesc), new StringTriple(newClass, newName, newDesc));
		}

		System.out.println("    Loading methods...");
		methodMap = new HashMap<StringTriple, StringTriple>();
		JsonArray methodsJson = mappingsJson.getAsJsonArray("methods");
		for (JsonElement elem : methodsJson) {
			JsonObject obj = (JsonObject) elem;
			String oldClass = obj.getAsJsonPrimitive("oldClass").getAsString();
			String oldName = obj.getAsJsonPrimitive("oldName").getAsString();
			String oldDesc = obj.getAsJsonPrimitive("oldDesc").getAsString();
			String newClass = obj.getAsJsonPrimitive("newClass").getAsString();
			String newName = obj.getAsJsonPrimitive("newName").getAsString();
			String newDesc = obj.getAsJsonPrimitive("newDesc").getAsString();

			if (reverse) {
				String temp = oldName;
				oldName = newName;
				newName = temp;

				temp = oldDesc;
				oldDesc = newDesc;
				newDesc = temp;

				temp = oldClass;
				oldClass = newClass;
				newClass = temp;
			}

			methodMap.put(new StringTriple(oldClass, oldName, oldDesc), new StringTriple(newClass, newName, newDesc));
		}

		System.out.println("Remapping jar with " + classMap.size() + " class mappings, " + fieldMap.size()
				+ " field mappings and " + methodMap.size() + " method mappings");

		jar = new JarFile(input, false);
		JarOutputStream out = new JarOutputStream(new FileOutputStream(output));

		jarMap = new HashMap<String, JarEntry>();
		classNodeMap = new HashMap<String, ClassNode>();
		System.out.println("    First pass...");
		File[] libs = libsFolder.listFiles();
		for (File lib : libs) {
			loadLib(lib);
		}

		for (Enumeration<JarEntry> entr = jar.entries(); entr.hasMoreElements();) {
			JarEntry entry = entr.nextElement();
			String name = entry.getName();

			if (entry.isDirectory()) {
				continue;
			}

			if (name.endsWith(".class")) {
				name = name.substring(0, name.length() - CLASS_LENGTH);
				jarMap.put(name, entry);

				ClassReader cr = new ClassReader(jar.getInputStream(entry));
				ClassNode node = new ClassNode();
				cr.accept(node, 0);

				classNodeMap.put(name, node);
			} else {
				JarEntry nentry = new JarEntry(name);
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				int n;
				byte[] b = new byte[1 << 15];
				InputStream is = jar.getInputStream(entry);
				while ((n = is.read(b, 0, b.length)) != -1) {
					buffer.write(b, 0, n);
				}
				buffer.flush();

				nentry.setTime(0);
				out.putNextEntry(nentry);
				out.write(buffer.toByteArray());
			}
		}

		System.out.println("    Second pass...");
		for (Entry<String, JarEntry> e : jarMap.entrySet()) {

			ClassReader reader = new ClassReader(jar.getInputStream(e.getValue()));
			ClassNode node = new ClassNode();

			RemapperClassAdapter mapper = new RemapperClassAdapter(node);
			reader.accept(mapper, 0);

			ClassWriter wr = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			node.accept(wr);

			JarEntry entry = new JarEntry(map(e.getKey()) + ".class");
			entry.setTime(0);
			out.putNextEntry(entry);
			out.write(wr.toByteArray());

		}

		jar.close();
		out.close();

		System.out.println("Complete!");
	}

	private void loadLib(File path) throws Exception {
		JarFile libJar = new JarFile(path, false);

		for (Enumeration<JarEntry> entr = libJar.entries(); entr.hasMoreElements();) {
			JarEntry entry = entr.nextElement();
			String name = entry.getName();

			if (entry.isDirectory()) {
				continue;
			}

			if (name.endsWith(".class")) {
				name = name.substring(0, name.length() - CLASS_LENGTH);

				ClassReader cr = new ClassReader(libJar.getInputStream(entry));
				ClassNode node = new ClassNode();
				cr.accept(node, 0);

				classNodeMap.put(name, node);
			}
		}

		libJar.close();
	}

	@Override
	public String map(String typeName) {
		if (classMap.containsKey(typeName)) {
			return classMap.get(typeName);
		}

		int index = typeName.lastIndexOf('$');
		if (index != -1) {
			String outer = typeName.substring(0, index);
			String mapped = map(outer);
			if (mapped == null)
				return null;
			return mapped + typeName.substring(index);
		}

		return typeName;
	}

	public ClassNode getClass(String clazz) {
		return classNodeMap.containsKey(clazz) ? classNodeMap.get(clazz) : null;
	}

	public String mapFieldName(String owner, String name, String desc, int access, boolean base) {
		StringTriple mapped = fieldMap.get(new StringTriple(owner, name, desc));
		ClassNode clazz = getClass(owner);

		if (mapped != null) {
			return mapped.getB();
		} else if (checkParents(access) && clazz != null) {
			if (clazz.superName != null) {
				String map = mapFieldName(clazz.superName, name, desc, access, false);
				if (map != null) {
					return map;
				}
			}

			for (String iface : clazz.interfaces) {
				String map = mapFieldName(iface, name, desc, access, false);
				if (map != null) {
					return map;
				}
			}
		}
		return base ? name : null;
	}

	public String mapMethodName(String owner, String name, String desc, int access, boolean base) {
		StringTriple mapped = methodMap.get(new StringTriple(owner, name, desc));
		ClassNode clazz = getClass(owner);

		if (mapped != null) {
			return mapped.getB();
		} else if (checkParents(access) && clazz != null) {
			if (clazz.superName != null) {
				String map = mapMethodName(clazz.superName, name, desc, access, false);
				if (map != null) {
					return map;
				}
			}

			for (String iface : clazz.interfaces) {
				String map = mapMethodName(iface, name, desc, access, false);
				if (map != null) {
					return map;
				}
			}
		}
		return base ? name : null;
	}

	private boolean checkParents(int access) {
		return access == -1 || (!Modifier.isPrivate(access) && !Modifier.isStatic(access));
	}

	@Override
	public String mapFieldName(String owner, String name, String desc) {
		return mapFieldName(owner, name, desc, 0, true);
	}

	@Override
	public String mapMethodName(String owner, String name, String desc) {
		return mapMethodName(owner, name, desc, 0, true);
	}

	public boolean shouldKeepSource() {
		return keepSource;
	}

	public static void main(String[] args) throws Exception {
		System.out.println("SMRemapper - Version 1.1");
		System.out.println("Made for SMT by Error22");
		System.out.println();

		if (args.length != 6) {
			System.out.println(
					"Usage: java -jar SMRemapper.jar {input} {output} {mapping} {libs folder} {reverse (true/false)} {keep source (true/false)}");
			System.out.println(
					"Libs Folder: The libs folder must include the rt.jar(or classes on mac) file otherwise inheritance lookup will not work correctly!");
			System.out.println(
					"Remember: You will also need the StarMade.jar(or the deobf version) in the libs folder if you are only working with a partial jar (ie only contains changed files)");
			System.exit(0);
		}

		File input = new File(args[0]);
		File output = new File(args[1]);
		File mapping = new File(args[2]);
		File libsFolder = new File(args[3]);
		boolean reverse = args[4].equalsIgnoreCase("true");
		boolean keepSource = args[5].equalsIgnoreCase("true");

		INSTANCE = new SMRemapper();
		INSTANCE.init(input, output, mapping, libsFolder, reverse, keepSource);
	}
}
