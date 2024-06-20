package wntiv;

import wntiv.class_parser.ClassHandler;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.zip.ZipOutputStream;

public class JarHandler {
	private final ZipOutputStream assetsOut;
	private final Map<String, ClassHandler> classes =  new HashMap<>();
	JarHandler(JarInputStream input) throws IOException {
		File outFile = new File("./out/assets.zip");
		assert outFile.getParentFile().mkdirs();
		if(!outFile.createNewFile()) {
			System.out.println(outFile.getPath() + " already exists, overwriting");
		}
		assetsOut = new ZipOutputStream(new FileOutputStream(outFile));
		JarEntry entry;
		while(Objects.nonNull(entry = input.getNextJarEntry())) {
			if(entry.getRealName().endsWith(".class")) {
				// Java class
				ClassHandler handler = new ClassHandler(new DataInputStream(input));
				classes.put(handler.this_class.name, handler);
//				System.out.println(handler);
			} else {
				assetsOut.putNextEntry(entry);
				input.transferTo(assetsOut);
			}
			input.closeEntry();
		}
		assetsOut.finish();
		assetsOut.close();

	}
}
