package wntiv.class_parser;

import wntiv.wasm_output.WasmModule;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.zip.ZipOutputStream;

public class JarHandler {
	private final ZipOutputStream assetsOut;
	private final Map<ClassHandler.ConstantClassInfo, ClassHandler> classes =  new HashMap<>();
	private final List<IntermediaryMethod> methods = new ArrayList<>();
	private final Map<ClassHandler.ConstantClassInfo,
						Map<ClassHandler.ConstantNameAndTypeInfo, Integer>> methodIds = new HashMap<>();
	private final Map<ClassHandler.ConstantClassInfo,
					Map<ClassHandler.ConstantNameAndTypeInfo, Integer>> staticFieldIndices = new HashMap<>();
	private final Map<ClassHandler.ConstantClassInfo,
					Map<ClassHandler.ConstantNameAndTypeInfo, ClassHandler.FieldInfo>> fieldInfo = new HashMap<>();
	private final WasmModule module = new WasmModule();
	public JarHandler(JarInputStream input) throws IOException {
		File outFile = new File("./out/assets.zip");
		assert outFile.getParentFile().mkdirs();
		if(!outFile.createNewFile()) {
			System.out.println(outFile.getPath() + " already exists, overwriting");
		}
		assetsOut = new ZipOutputStream(new FileOutputStream(outFile));
		parseJar(input, assetsOut);
	}
	private void parseJar(JarInputStream input, ZipOutputStream assetsOut) throws IOException {
		JarEntry entry;
		while(Objects.nonNull(entry = input.getNextJarEntry())) {
			if(entry.getRealName().endsWith(".class")) {
				// Java class
				ClassHandler handler = new ClassHandler(new DataInputStream(input));
				classes.put(handler.this_class, handler);
				System.out.println(handler);
				System.in.read();
			} else {
				assetsOut.putNextEntry(entry);
				input.transferTo(assetsOut);
			}
			input.closeEntry();
		}
	}
	public void addLibraryJar(JarInputStream input) throws IOException {
		parseJar(input, assetsOut); // TODO: not use same assets file?
	}
	public WasmModule transpile() {
		// Fields
		for (var entry : classes.entrySet()) {
			Map<ClassHandler.ConstantNameAndTypeInfo, ClassHandler.FieldInfo> fields = new HashMap<>();
			Map<ClassHandler.ConstantNameAndTypeInfo, Integer> fieldIds = new HashMap<>();
			for (ClassHandler.FieldInfo field : entry.getValue().fields) {
				fields.put(field.getNameAndType(), field);
				if ((field.access_flags & ClassHandler.FieldInfo.ACC_STATIC) == 0) continue;
				boolean constant = (field.access_flags & ClassHandler.FieldInfo.ACC_FINAL) != 0;
				var value = field.attributes.constantValue;
				// Create global for static
				switch (field.descriptor) {
					case "I" -> // Integer
						fieldIds.put(field.getNameAndType(),
							module.addGlobal(
								value == null ? 0 : ((ClassHandler.ConstantIntegerInfo) value.value).value(),
								!constant));
					case "F" -> // Float
						fieldIds.put(field.getNameAndType(),
							module.addGlobal(
								value == null ? 0F : ((ClassHandler.ConstantFloatInfo) value.value).value(),
								!constant));
					case "J" -> // Long
						fieldIds.put(field.getNameAndType(),
							module.addGlobal(
								value == null ? 0L : ((ClassHandler.ConstantLongInfo) value.value).value(),
								!constant));
					case "D" -> // Double
						fieldIds.put(field.getNameAndType(),
							module.addGlobal(
								value == null ? 0D : ((ClassHandler.ConstantDoubleInfo) value.value).value(),
								!constant));
					default -> throw new RuntimeException("Not Implemented :(");
				}
			}
			staticFieldIndices.put(entry.getKey(), fieldIds);
			fieldInfo.put(entry.getKey(), fields);
		}
		// Methods
		for (var entry : classes.entrySet()) {
			Map<ClassHandler.ConstantNameAndTypeInfo, Integer> classMethodIds = new HashMap<>();
			var classMethods = entry.getValue().prepareFunctions(this.module, this);
			for (var methodEntry : classMethods.entrySet()) {
				methods.add(methodEntry.getValue());
				classMethodIds.put(methodEntry.getKey(), methods.size() - 1);
			}
			methodIds.put(entry.getKey(), classMethodIds);
		}
		// Must do seperately so all methods are defined in method ordering
		for (IntermediaryMethod method : methods) {
			module.addFunction(method);
		}
		return module;
	}

	public int getFunctionIndex(ClassHandler.ConstantMethodRefInfo method) {
		return methodIds.get(method.getCls()).get(method.getSignature());
	}

	public int getGlobal(ClassHandler.ConstantFieldRefInfo field) {
		return staticFieldIndices.get(field.getCls()).get(field.getSignature());
	}
}
