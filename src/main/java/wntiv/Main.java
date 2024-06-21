package wntiv;

import wntiv.class_parser.JarHandler;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.jar.JarInputStream;

public class Main {
	public static void main(String[] args) {
		String jarLocation = null;

		Iterator<String> arg_stack = Arrays.stream(args).iterator();
		String arg;
		while (arg_stack.hasNext())
			switch (arg = arg_stack.next()) {
				case "-h", "--help" -> System.out.println("help goes here");
				default -> jarLocation = arg;
			}
		if (jarLocation == null) {
			// User error
			System.out.println("error goes here");
			System.exit(1);
		}
		try {
			System.out.println(jarLocation);
			FileInputStream f = new FileInputStream(jarLocation);
			JarInputStream j = new JarInputStream(f);
			JarHandler handler = new JarHandler(j);
		} catch (FileNotFoundException e) {
			// User error
			throw new RuntimeException(e);
		} catch (IOException e) {
			// IDK what happened
			throw new RuntimeException(e);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
