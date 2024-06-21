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
	private final Map<ClassHandler.ConstantClassInfo, Map<String, IntermediaryMethod>> methods = new HashMap<>();
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
		WasmModule module = new WasmModule();
		for (var entry : classes.entrySet()) {
			methods.put(entry.getKey(), entry.getValue().prepareFunctions());
		}
		for (var entry : methods.entrySet()) {
			for (var method : entry.getValue().entrySet()) {
				var functionIndex = module.addFunction(method.getValue());
				// entry.getValue().resolveDependencies(methods);
			}
		}
		return module;
	}
}
