package com.error22.smt.remapper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
import org.objectweb.asm.commons.RemappingClassAdapter;
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
	private HashMap<StringPair, StringPair> fieldMap, methodMap;
	private HashMap<String, JarEntry> jarMap;
	
	public void init(File input, File output, File mapping, boolean reverse) throws Exception{
		System.out.println("Loading mappings...");

		String data = Files.toString(mapping, Charset.defaultCharset());
		if (!data.substring(data.indexOf("<") + 1, data.indexOf(">")).equals(
				"smtmap 1")) {
			System.out.println("Unsupported mapping format!");
			System.exit(0);
		}

		JsonObject mappingsJson = new GsonBuilder().create().fromJson(
				data.substring(data.indexOf(">") + 1), JsonObject.class);

		System.out.println("    Loading classes...");
		classMap = new HashMap<String, String>();
		JsonArray classesJson = mappingsJson.getAsJsonArray("classes");
		for (JsonElement elem : classesJson) {
			JsonObject obj = (JsonObject) elem;
			String oldName = obj.getAsJsonPrimitive("oldName").getAsString();
			String newName = obj.getAsJsonPrimitive("newName").getAsString();
			
			if(reverse){
				String temp = oldName;
				oldName = newName;
				newName = temp;
			}

			classMap.put(oldName, newName);
		}

		System.out.println("    Loading fields...");
		fieldMap = new HashMap<StringPair, StringPair>();
		JsonArray fieldsJson = mappingsJson.getAsJsonArray("fields");
		for (JsonElement elem : fieldsJson) {
			JsonObject obj = (JsonObject) elem;
			String oldName = obj.getAsJsonPrimitive("oldName").getAsString();
			String oldDesc = obj.getAsJsonPrimitive("oldDesc").getAsString();
			String newName = obj.getAsJsonPrimitive("newName").getAsString();
			String newDesc = obj.getAsJsonPrimitive("newDesc").getAsString();
			
			if(reverse){
				String temp = oldName;
				oldName = newName;
				newName = temp;
				
				temp = oldDesc;
				oldDesc = newDesc;
				newDesc = temp;
			}

			fieldMap.put(new StringPair(oldName, oldDesc), new StringPair(
					newName, newDesc));
		}

		System.out.println("    Loading methods...");
		methodMap = new HashMap<StringPair, StringPair>();
		JsonArray methodsJson = mappingsJson.getAsJsonArray("methods");
		for (JsonElement elem : methodsJson) {
			JsonObject obj = (JsonObject) elem;
			String oldName = obj.getAsJsonPrimitive("oldName").getAsString();
			String oldDesc = obj.getAsJsonPrimitive("oldDesc").getAsString();
			String newName = obj.getAsJsonPrimitive("newName").getAsString();
			String newDesc = obj.getAsJsonPrimitive("newDesc").getAsString();
			
			if(reverse){
				String temp = oldName;
				oldName = newName;
				newName = temp;
				
				temp = oldDesc;
				oldDesc = newDesc;
				newDesc = temp;
			}
			
			methodMap.put(new StringPair(oldName, oldDesc), new StringPair(
					newName, newDesc));
		}

		System.out
				.println("Loadded " + classMap.size() + " classes, "
						+ fieldMap.size() + " fields, " + methodMap.size()
						+ " methods");

		JarFile jar = new JarFile(input, false);
		JarOutputStream out = new JarOutputStream(new FileOutputStream(output));

		jarMap = new HashMap<String, JarEntry>();
		for (Enumeration<JarEntry> entr = jar.entries(); entr.hasMoreElements();) {
			JarEntry entry = entr.nextElement();
			String name = entry.getName();

			if (name.endsWith(".class")) {
				name = name.substring(0, name.length() - CLASS_LENGTH);
				jarMap.put(name, entry);
			} else {
				JarEntry nentry = new JarEntry(name);

				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				int n;
				byte[] b = new byte[1 << 15];
				while ((n = jar.getInputStream(entry).read(b, 0, b.length)) != -1) {
					buffer.write(b, 0, n);
				}
				buffer.flush();

				nentry.setTime(0);
				out.putNextEntry(nentry);
				out.write(buffer.toByteArray());
			}
		}

		for (Entry<String, JarEntry> e : jarMap.entrySet()) {
			ClassReader reader = new ClassReader(jar.getInputStream(e.getValue()));
			ClassNode node = new ClassNode();
	        
			RemappingClassAdapter mapper = new RemappingClassAdapter(node, this);
	        reader.accept(mapper, 0);

	        ClassWriter wr = new ClassWriter(ClassWriter.COMPUTE_MAXS);
	        node.accept(wr);
	        
	        JarEntry entry = new JarEntry(map(e.getKey()) + ".class");
	        entry.setTime(0);
			out.putNextEntry(entry);
			out.write(wr.toByteArray());

			System.out.println("Remapping "+e.getKey()+" >> "+map(e.getKey()));

		}

		jar.close();
		out.close();
	}
	
	@Override
	public String map(String typeName) {
		if(classMap.containsKey(typeName)){
			return classMap.get(typeName);
		}
		
		int index = typeName.lastIndexOf('$');
        if (index != -1)
        {
            String outer = typeName.substring(0, index);
            String mapped = map(outer);
            if  (mapped == null) return null;
            return mapped + typeName.substring(index);
        }
		
		return typeName;
	}

	public static void main(String[] args) throws Exception {
		System.out.println("SMRemapper - Version 1.0");
		System.out.println("Made for SMT by Error22");
		System.out.println();

		if (args.length != 4) {
			System.out
					.println("Usage: java -jar SMRemapper.jar {input} {output} {mapping} {reverse}");
			System.exit(0);
		}

		File input = new File(args[0]);
		File output = new File(args[1]);
		File mapping = new File(args[2]);
		boolean reverse = args[3].equalsIgnoreCase("true");

		INSTANCE = new SMRemapper();
		INSTANCE.init(input, output, mapping, reverse);
	}
}
