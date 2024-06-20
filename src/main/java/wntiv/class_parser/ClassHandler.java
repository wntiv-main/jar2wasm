package wntiv.class_parser;


import jdk.jfr.Unsigned;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ClassHandler {
	private static String hexByte(byte value) {
		String[] alphabet = "0123456789ABCDEF".split("");
		int unsigned = Byte.toUnsignedInt(value);
		return alphabet[unsigned/16] + alphabet[unsigned%16];
	}
	public final byte[] magic;
	public final @Unsigned short minor_version;
	public final @Unsigned short major_version;
	public final ConstantPool constant_pool;
	static class AccessFlags {
		public static final @Unsigned short ACC_PUBLIC = 0x0001; // Declared public; may be accessed from outside its package.
		public static final @Unsigned short ACC_FINAL = 0x0010; // Declared final; no subclasses allowed.
		public static final @Unsigned short ACC_SUPER = 0x0020; // Treat superclass methods specially when invoked by the invokespecial instruction.
		public static final @Unsigned short ACC_INTERFACE = 0x0200; // Is an interface, not a class.
		public static final @Unsigned short ACC_ABSTRACT = 0x0400; // Declared abstract; must not be instantiated.
		public static final @Unsigned short ACC_SYNTHETIC = 0x1000; // Declared synthetic; not present in the source code.
		public static final @Unsigned short ACC_ANNOTATION = 0x2000; // Declared as an annotation type.
		public static final @Unsigned short ACC_ENUM = 0x4000; // Declared as an enum type.
		public static String toEnumString(short value) {
			List<String> present = new ArrayList<>();
			if((value & ACC_PUBLIC) != 0) present.add("ACC_PUBLIC");
			if((value & ACC_FINAL) != 0) present.add("ACC_FINAL");
			if((value & ACC_SUPER) != 0) present.add("ACC_SUPER");
			if((value & ACC_INTERFACE) != 0) present.add("ACC_INTERFACE");
			if((value & ACC_ABSTRACT) != 0) present.add("ACC_ABSTRACT");
			if((value & ACC_SYNTHETIC) != 0) present.add("ACC_SYNTHETIC");
			if((value & ACC_ANNOTATION) != 0) present.add("ACC_ANNOTATION");
			if((value & ACC_ENUM) != 0) present.add("ACC_ENUM");
			return String.join(" | ", present);
		}

		public static String toReadableString(short value) {
			String result = "";
			if((value & ACC_PUBLIC) != 0) result += "public ";
			if((value & ACC_FINAL) != 0) result += "final ";
			if((value & ACC_SUPER) != 0) result += "super ";
			if((value & ACC_INTERFACE) != 0) result += "interface ";
			if((value & ACC_ABSTRACT) != 0) result += "abstract ";
			if((value & ACC_SYNTHETIC) != 0) result += "synthetic ";
			if((value & ACC_ANNOTATION) != 0) result += "annotation ";
			if((value & ACC_ENUM) != 0) result += "enum ";
			return result;
		}
	}
	public final @Unsigned short access_flags;
	public final ConstantClassInfo this_class;
	public final @Nullable ConstantClassInfo super_class;
	public final List<ConstantClassInfo> interfaces = new ArrayList<>();
	public final List<FieldInfo> fields = new ArrayList<>();
	public final List<MethodInfo> methods = new ArrayList<>();
	public final Attributes attributes;

	public ClassHandler(DataInputStream in) throws IOException {
		magic = in.readNBytes(4);
		minor_version = in.readShort();
		major_version = in.readShort();
		int constant_pool_count = in.readUnsignedShort();
		constant_pool = new ConstantPool(in, constant_pool_count);
		access_flags = in.readShort();
		if(!(constant_pool.get(in.readUnsignedShort()) instanceof ConstantClassInfo this_cls))
			throw new RuntimeException("Invalid this_class");
		this_class = this_cls;
		int super_class_index = in.readUnsignedShort();
		if (super_class_index == 0) {
			super_class = null;
		} else if(constant_pool.get(super_class_index) instanceof ConstantClassInfo super_cls) {
			super_class = super_cls;
		} else throw new RuntimeException("Invalid super_class");
		int interfaces_count = in.readUnsignedShort();
		while (interfaces_count --> 0) {
			if(!(constant_pool.get(in.readUnsignedShort()) instanceof ConstantClassInfo interface_cls))
				throw new RuntimeException("Invalid interface");
			interfaces.add(interface_cls);
		}
		int fields_count = in.readUnsignedShort();
		while(fields_count --> 0) {
			fields.add(FieldInfo.readFrom(in, constant_pool));
		}
		int methods_count = in.readUnsignedShort();
		while(methods_count --> 0) {
			methods.add(MethodInfo.readFrom(in, constant_pool));
		}
		attributes = new Attributes(in, constant_pool);
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("ClassHandler {\n\tmagic: 0x");
		for (byte b : magic) {
			result.append(hexByte(b));
		}
		result.append("\n\tversion: ");
		result.append(major_version);
		result.append(".");
		result.append(minor_version);
//		result.append("\n\tconstant_pool: ");
//		result.append(constant_pool.toString().replace("\n", "\n\t"));
		result.append("\n\taccess_flags: ");
		result.append(AccessFlags.toEnumString(access_flags));
		result.append("\n\tthis_class: ");
		result.append(this_class.toString());
		result.append("\n\tsuper_class: ");
		result.append(super_class != null ? super_class.toString(): "null");
		result.append("\n\tinterfaces: ");
		result.append("[\n\t\t").append(String.join(",\n\t\t",
				interfaces.stream().map(x -> x.toString().replace("\n", "\n\t")).toList())).append("\n\t]");
		result.append("\n\tfields: ");
		result.append("[\n\t\t").append(String.join(",\n\t\t",
				fields.stream().map(x -> x.toString().replace("\n", "\n\t")).toList())).append("\n\t]");
		result.append("\n\tmethods: ");
		result.append("[\n\t\t").append(String.join(",\n\t\t",
				methods.stream().map(x -> x.toString().replace("\n", "\n\t")).toList())).append("\n\t]");
		result.append("\n\tattributes: ");
		result.append(attributes.toString().replace("\n", "\n\t"));
		result.append("\n}");
		return result.toString().replaceAll("\\[[\r\n\t ]*]", "[]");
	}

	private static class ConstantPool {
		private final DataInputStream data;
		private final List<ConstantPoolItem> pool = new ArrayList<>();

		ConstantPool(DataInputStream in, int len) throws IOException {
			data = in;
			while(pool.size() < len - 1) {
				ConstantPoolItem.readFrom(data, this);
			}
		}

		public ConstantPoolItem await(int index) throws IOException {
			while(pool.size() < index) {
				ConstantPoolItem.readFrom(data, this);
			}
			return get(index);
		}
		public ConstantPoolItem get(int index) {
			return pool.get(index - 1);
		}

		public void add(ConstantPoolItem item) {
			pool.add(item);
		}

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			for (ConstantPoolItem elem : pool) {
				if(result.isEmpty()) result.append("[");
				else result.append(",");
				result.append("\n\t");
				result.append(elem);
			}
			result.append("\n]");
			return result.toString();
		}
	}

	static class Attributes {
		private final Map<String, byte[]> unknownAttributes = new HashMap<>();
		private final List<String> availableAttributes = new ArrayList<>();
		public @Nullable ConstantValueAttribute constantValue = null;
		public @Nullable CodeAttribute code = null;
		public @Nullable StackMapTableAttribute stackMapTable = null;
		public @Nullable ExceptionsAttribute exceptions = null;
		public @Nullable InnerClassesAttribute innerClasses = null;
		public @Nullable EnclosingMethodAttribute enclosingMethod = null;
		public boolean synthetic = false;
		public @Nullable SignatureAttribute signature = null;
		public @Nullable SourceFileAttribute sourceFile = null;
		public @Nullable LineNumberTableAttribute lineNumberTable = null;
		public @Nullable LocalVariableTableAttribute localVariableTable = null;
		public @Nullable LocalVariableTypeTableAttribute localVariableTypeTable = null;
		public boolean deprecated = false;
		public @Nullable RuntimeVisibleAnnotationsAttribute runtimeVisibleAnnotations = null;
		public @Nullable RuntimeInvisibleAnnotationsAttribute runtimeInvisibleAnnotations = null;
		public @Nullable RuntimeVisibleParameterAnnotationsAttribute runtimeVisibleParameterAnnotations = null;
		public @Nullable RuntimeInvisibleParameterAnnotationsAttribute runtimeInvisibleParameterAnnotations = null;
		public @Nullable AnnotationDefaultAttribute annotationDefault = null;
		public @Nullable BootstrapMethodsAttribute bootstrapMethods = null;
		Attributes(DataInputStream in, ConstantPool constantPool) throws IOException {
			int attributes_count = in.readUnsignedShort();
			while(attributes_count --> 0) {
				if(!(constantPool.get(in.readUnsignedShort()) instanceof ConstantUtf8Info attr_name))
					throw new RuntimeException("Invalid attribute name");
				availableAttributes.add(attr_name.value);
				long length = Integer.toUnsignedLong(in.readInt());
				switch (attr_name.value) {
					case ConstantValueAttribute.ATTR_NAME -> constantValue = new ConstantValueAttribute(in, constantPool, length);
					case CodeAttribute.ATTR_NAME -> code = new CodeAttribute(in, constantPool, length);
					case StackMapTableAttribute.ATTR_NAME -> stackMapTable = new StackMapTableAttribute(in, constantPool, length);
					case ExceptionsAttribute.ATTR_NAME -> exceptions = new ExceptionsAttribute(in, constantPool, length);
					case InnerClassesAttribute.ATTR_NAME -> innerClasses = new InnerClassesAttribute(in, constantPool, length);
					case EnclosingMethodAttribute.ATTR_NAME -> enclosingMethod = new EnclosingMethodAttribute(in, constantPool, length);
					case "Synthetic" -> synthetic = true;
					case SignatureAttribute.ATTR_NAME -> signature = new SignatureAttribute(in, constantPool, length);
					case SourceFileAttribute.ATTR_NAME -> sourceFile = new SourceFileAttribute(in, constantPool, length);
					// TODO: SourceDebugExtension
					case LineNumberTableAttribute.ATTR_NAME -> lineNumberTable = new LineNumberTableAttribute(in, constantPool, length);
					case LocalVariableTableAttribute.ATTR_NAME -> localVariableTable = new LocalVariableTableAttribute(in, constantPool, length);
					case LocalVariableTypeTableAttribute.ATTR_NAME -> localVariableTypeTable = new LocalVariableTypeTableAttribute(in, constantPool, length);
					case "Deprecated" -> deprecated = true;
					case RuntimeVisibleAnnotationsAttribute.ATTR_NAME -> runtimeVisibleAnnotations = new RuntimeVisibleAnnotationsAttribute(in, constantPool, length);
					case RuntimeInvisibleAnnotationsAttribute.ATTR_NAME -> runtimeInvisibleAnnotations = new RuntimeInvisibleAnnotationsAttribute(in, constantPool, length);
					case RuntimeVisibleParameterAnnotationsAttribute.ATTR_NAME -> runtimeVisibleParameterAnnotations = new RuntimeVisibleParameterAnnotationsAttribute(in, constantPool, length);
					case RuntimeInvisibleParameterAnnotationsAttribute.ATTR_NAME -> runtimeInvisibleParameterAnnotations = new RuntimeInvisibleParameterAnnotationsAttribute(in, constantPool, length);
					case AnnotationDefaultAttribute.ATTR_NAME -> annotationDefault = new AnnotationDefaultAttribute(in, constantPool, length);
					case BootstrapMethodsAttribute.ATTR_NAME -> bootstrapMethods = new BootstrapMethodsAttribute(in, constantPool, length);
					default -> unknownAttributes.put(attr_name.value, in.readNBytes((int) length)); // TODO: dangerous!!
				}
			}
		}

		private Object getAttributeValue(String name) {
			return switch (name) {
				case ConstantValueAttribute.ATTR_NAME -> constantValue;
				case CodeAttribute.ATTR_NAME -> code;
				case StackMapTableAttribute.ATTR_NAME -> stackMapTable;
				case ExceptionsAttribute.ATTR_NAME -> exceptions;
				case InnerClassesAttribute.ATTR_NAME -> innerClasses;
				case EnclosingMethodAttribute.ATTR_NAME -> enclosingMethod;
				case "Synthetic" -> synthetic;
				case SignatureAttribute.ATTR_NAME -> signature;
				case SourceFileAttribute.ATTR_NAME -> sourceFile;
				case LineNumberTableAttribute.ATTR_NAME -> lineNumberTable;
				case LocalVariableTableAttribute.ATTR_NAME -> localVariableTable;
				case LocalVariableTypeTableAttribute.ATTR_NAME -> localVariableTypeTable;
				case "Deprecated" -> deprecated = true;
				case RuntimeVisibleAnnotationsAttribute.ATTR_NAME -> runtimeVisibleAnnotations;
				case RuntimeInvisibleAnnotationsAttribute.ATTR_NAME -> runtimeInvisibleAnnotations;
				case RuntimeVisibleParameterAnnotationsAttribute.ATTR_NAME -> runtimeVisibleParameterAnnotations;
				case RuntimeInvisibleParameterAnnotationsAttribute.ATTR_NAME -> runtimeInvisibleParameterAnnotations;
				case AnnotationDefaultAttribute.ATTR_NAME -> annotationDefault;
				case BootstrapMethodsAttribute.ATTR_NAME -> bootstrapMethods;
				default -> unknownAttributes.get(name); // TODO: dangerous!!
			};
		}

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			for (String attribute : availableAttributes) {
				if (result.isEmpty()) result.append("{\n\t");
				else result.append(",\n\t");
				result.append(attribute);
				result.append(": ");
				result.append(getAttributeValue(attribute).toString().replace("\n", "\n\t"));
			}
			if (result.isEmpty()) result.append("{}");
			else result.append("\n}");
			return result.toString();
		}

		interface AttributeValue {
			@Override
			String toString();
			// Maybe reinstate this as public static factory method?
//			AttributeValue(DataInputStream in, ConstantPool constantPool, long length) throws IOException {}
		}
		static class ConstantValueAttribute implements AttributeValue {
			public static final String ATTR_NAME = "ConstantValue";
			public final ConstantPoolItem value;
			ConstantValueAttribute(DataInputStream in, ConstantPool constantPool, long length) throws IOException {
				assert length == Short.BYTES;
				value = constantPool.get(in.readUnsignedShort());
				assert value instanceof ConstantStringInfo
					|| value instanceof ConstantIntegerInfo
					|| value instanceof ConstantFloatInfo
					|| value instanceof ConstantLongInfo
					|| value instanceof ConstantDoubleInfo;
			}

			@Override
			public String toString() {
				return value.toString();
			}
		}
		static class CodeAttribute implements AttributeValue {
			public static final String ATTR_NAME = "Code";
			public final @Unsigned short max_stack;
			public final @Unsigned short max_locals;
			public final byte[] code;
			static class ExceptionHandle {
				public final @Unsigned short start_pc;
				public final @Unsigned short end_pc;
				public final @Unsigned short handler_pc;
				public final @Nullable ConstantClassInfo catch_type;
				ExceptionHandle(DataInputStream in, ConstantPool constantPool) throws IOException {
					start_pc = in.readShort();
					end_pc = in.readShort();
					handler_pc = in.readShort();
					int catch_type_index = in.readUnsignedShort();
					if (catch_type_index == 0) {
						catch_type = null;
					} else if (constantPool.get(catch_type_index) instanceof ConstantClassInfo cls) {
						catch_type = cls;
					} else throw new RuntimeException("Invalid exception class");
				}

				@Override
				public String toString() {
					return "@" + start_pc + " - @" + end_pc +
							" --(" + catch_type + ")-> @" + handler_pc;
				}
			}
			public final List<ExceptionHandle> exception_table;
			public final Attributes attributes;
			public final ConstantPool pool;
			CodeAttribute(DataInputStream in, ConstantPool constantPool, long length) throws IOException {
				max_stack = in.readShort();
				max_locals = in.readShort();
				// TODO: read longer byte[]
				code = in.readNBytes((int) Integer.toUnsignedLong(in.readInt()));
				int exception_table_length = in.readUnsignedShort();
				exception_table = new ArrayList<>(exception_table_length);
				while (exception_table_length --> 0) {
					exception_table.add(new ExceptionHandle(in, constantPool));
				}
				attributes = new Attributes(in, constantPool);
				pool = constantPool;
//				byte[] x;
//				new ArrayList<Byte>(x).stream().map();
//				new ByteArrayInputStream(x)
			}
			private static final String[] ARRAY_TYPES = new String[]{"BOOLEAN", "CHAR", "FLOAT", "DOUBLE", "BYTE", "SHORT", "INT", "LONG"};
			@Override
			public String toString() {
				StringBuilder result = new StringBuilder();
				result.append("{\n\tmax_stack: ");
				result.append(max_stack);
				result.append("\n\tmax_locals: ");
				result.append(max_locals);
				result.append("\n\tcode:");
				var iter_base = new ByteArrayInputStream(code) {
					public int getPos() {
						return pos;
					}
				};
				DataInputStream iter = new DataInputStream(iter_base);
				try {
					int opcode, instructionPointer;
					while (iter.available() > 0) {
						result.append("\n\t\t");
						result.append(instructionPointer = iter_base.getPos()).append(" ");
						switch (opcode = iter.readUnsignedByte()) {
							case 0x00 /* nop */ -> result.append("NOP");
							case 0x01 /* aconst_null */ -> result.append("ACONST_NULL");
							case 0x02 /* iconst_m1 */ -> result.append("ICONST_m1");
							case 0x03 /* iconst_0 */ -> result.append("ICONST_0");
							case 0x04 /* iconst_1 */ -> result.append("ICONST_1");
							case 0x05 /* iconst_2 */ -> result.append("ICONST_2");
							case 0x06 /* iconst_3 */ -> result.append("ICONST_3");
							case 0x07 /* iconst_4 */ -> result.append("ICONST_4");
							case 0x08 /* iconst_5 */ -> result.append("ICONST_5");
							case 0x09 /* lconst_0 */ -> result.append("LCONST_0");
							case 0x0a /* lconst_1 */ -> result.append("LCONST_1");
							case 0x0b /* fconst_0 */ -> result.append("FCONST_0");
							case 0x0c /* fconst_1 */ -> result.append("FCONST_1");
							case 0x0d /* fconst_2 */ -> result.append("FCONST_2");
							case 0x0e /* dconst_0 */ -> result.append("DCONST_0");
							case 0x0f /* dconst_1 */ -> result.append("DCONST_1");
							case 0x10 /* bipush */ -> result.append("BIPUSH ").append(iter.readByte());
							case 0x11 /* sipush */ -> result.append("SIPUSH ").append(iter.readShort());
							case 0x12 /* ldc */ -> result.append("LDC ").append(pool.get(iter.readUnsignedByte()));
							case 0x13 /* ldc_w */ -> result.append("LDC_W ").append(pool.get(iter.readUnsignedShort()));
							case 0x14 /* ldc2_w */ -> result.append("LDC2_W ").append(pool.get(iter.readUnsignedShort()));
							case 0x15 /* iload */ -> result.append("ILOAD ").append(iter.readUnsignedByte());
							case 0x16 /* lload */ -> result.append("LLOAD ").append(iter.readUnsignedByte());
							case 0x17 /* fload */ -> result.append("FLOAD ").append(iter.readUnsignedByte());
							case 0x18 /* dload */ -> result.append("DLOAD ").append(iter.readUnsignedByte());
							case 0x19 /* aload */ -> result.append("ALOAD ").append(iter.readUnsignedByte());
							case 0x1a, 0x1b, 0x1c, 0x1d /* iload_<n> */ -> result.append("ILOAD_").append(opcode - 0x1a);
							case 0x1e, 0x1f, 0x20, 0x21 /* lload_<n> */ -> result.append("LLOAD_").append(opcode - 0x1e);
							case 0x22, 0x23, 0x24, 0x25 /* fload_<n> */ -> result.append("FLOAD_").append(opcode - 0x22);
							case 0x26, 0x27, 0x28, 0x29 /* dload_<n> */ -> result.append("DLOAD_").append(opcode - 0x26);
							case 0x2a, 0x2b, 0x2c, 0x2d /* aload_<n> */ -> result.append("ALOAD_").append(opcode - 0x2a);
							case 0x2e /* iaload */ -> result.append("IALOAD");
							case 0x2f /* laload */ -> result.append("LALOAD");
							case 0x30 /* faload */ -> result.append("FALOAD");
							case 0x31 /* daload */ -> result.append("DALOAD");
							case 0x32 /* aaload */ -> result.append("AALOAD");
							case 0x33 /* baload */ -> result.append("BALOAD");
							case 0x34 /* caload */ -> result.append("CALOAD");
							case 0x35 /* saload */ -> result.append("SALOAD");
							case 0x36 /* istore */ -> result.append("ISTORE ").append(iter.readUnsignedByte());
							case 0x37 /* lstore */ -> result.append("LSTORE ").append(iter.readUnsignedByte());
							case 0x38 /* fstore */ -> result.append("FSTORE ").append(iter.readUnsignedByte());
							case 0x39 /* dstore */ -> result.append("DSTORE ").append(iter.readUnsignedByte());
							case 0x3a /* astore */ -> result.append("ASTORE ").append(iter.readUnsignedByte());
							case 0x3b, 0x3c, 0x3d, 0x3e /* istore_<n> */ -> result.append("ISTORE_").append(opcode - 0x3b);
							case 0x3f, 0x40, 0x41, 0x42 /* lstore_<n> */ -> result.append("LSTORE_").append(opcode - 0x3f);
							case 0x43, 0x44, 0x45, 0x46 /* fstore_<n> */ -> result.append("FSTORE_").append(opcode - 0x43);
							case 0x47, 0x48, 0x49, 0x4a /* dstore_<n> */ -> result.append("DSTORE_").append(opcode - 0x47);
							case 0x4b, 0x4c, 0x4d, 0x4e /* astore_<n> */ -> result.append("ASTORE_").append(opcode - 0x4b);
							case 0x4f /* iastore */ -> result.append("IASTORE");
							case 0x50 /* lastore */ -> result.append("LASTORE");
							case 0x51 /* fastore */ -> result.append("FASTORE");
							case 0x52 /* dastore */ -> result.append("DASTORE");
							case 0x53 /* aastore */ -> result.append("AASTORE");
							case 0x54 /* bastore */ -> result.append("BASTORE");
							case 0x55 /* castore */ -> result.append("CASTORE");
							case 0x56 /* sastore */ -> result.append("SASTORE");
							case 0x57 /* pop */ -> result.append("POP");
							case 0x58 /* pop2 */ -> result.append("POP2");
							case 0x59 /* dup */ -> result.append("DUP");
							case 0x5a /* dup_x1 */ -> result.append("DUP_x1");
							case 0x5b /* dup_x2 */ -> result.append("DUP_x2");
							case 0x5c /* dup2 */ -> result.append("DUP2");
							case 0x5d /* dup2_x1 */ -> result.append("DUP2_x1");
							case 0x5e /* dup2_x2 */ -> result.append("DUP2_x2");
							case 0x5f /* swap */ -> result.append("SWAP");
							case 0x60 /* iadd */ -> result.append("IADD");
							case 0x61 /* ladd */ -> result.append("LADD");
							case 0x62 /* fadd */ -> result.append("FADD");
							case 0x63 /* dadd */ -> result.append("DADD");
							case 0x64 /* isub */ -> result.append("ISUB");
							case 0x65 /* lsub */ -> result.append("LSUB");
							case 0x66 /* fsub */ -> result.append("FSUB");
							case 0x67 /* dsub */ -> result.append("DSUB");
							case 0x68 /* imul */ -> result.append("IMUL");
							case 0x69 /* lmul */ -> result.append("LMUL");
							case 0x6a /* fmul */ -> result.append("FMUL");
							case 0x6b /* dmul */ -> result.append("DMUL");
							case 0x6c /* idiv */ -> result.append("IDIV");
							case 0x6d /* ldiv */ -> result.append("LDIV");
							case 0x6e /* fdiv */ -> result.append("FDIV");
							case 0x6f /* ddiv */ -> result.append("DDIV");
							case 0x70 /* irem */ -> result.append("IREM");
							case 0x71 /* lrem */ -> result.append("LREM");
							case 0x72 /* frem */ -> result.append("FREM");
							case 0x73 /* drem */ -> result.append("DREM");
							case 0x74 /* ineg */ -> result.append("INEG");
							case 0x75 /* lneg */ -> result.append("LNEG");
							case 0x76 /* fneg */ -> result.append("FNEG");
							case 0x77 /* dneg */ -> result.append("DNEG");
							case 0x78 /* ishl */ -> result.append("ISHL");
							case 0x79 /* lshl */ -> result.append("LSHL");
							case 0x7a /* ishr */ -> result.append("ISHR");
							case 0x7b /* lshr */ -> result.append("LSHR");
							case 0x7c /* iushr */ -> result.append("IUSHR");
							case 0x7d /* lushr */ -> result.append("LUSHR");
							case 0x7e /* iand */ -> result.append("IAND");
							case 0x7f /* land */ -> result.append("LAND");
							case 0x80 /* ior */ -> result.append("IOR");
							case 0x81 /* lor */ -> result.append("LOR");
							case 0x82 /* ixor */ -> result.append("IXOR");
							case 0x83 /* lxor */ -> result.append("LXOR");
							case 0x84 /* iinc */ -> result.append("IINC ").append(iter.readUnsignedByte())
									.append(" ").append(iter.readByte());
							case 0x85 /* i2l */ -> result.append("I2L");
							case 0x86 /* i2f */ -> result.append("I2F");
							case 0x87 /* i2d */ -> result.append("I2D");
							case 0x88 /* l2i */ -> result.append("L2I");
							case 0x89 /* l2f */ -> result.append("L2F");
							case 0x8a /* l2d */ -> result.append("L2D");
							case 0x8b /* f2i */ -> result.append("F2I");
							case 0x8c /* f2l */ -> result.append("F2L");
							case 0x8d /* f2d */ -> result.append("F2D");
							case 0x8e /* d2i */ -> result.append("D2I");
							case 0x8f /* d2l */ -> result.append("D2L");
							case 0x90 /* d2f */ -> result.append("D2F");
							case 0x91 /* i2b */ -> result.append("I2B");
							case 0x92 /* i2c */ -> result.append("I2C");
							case 0x93 /* i2s */ -> result.append("I2S");
							case 0x94 /* lcmp */ -> result.append("LCMP");
							case 0x95 /* fcmpl */ -> result.append("FCMPL");
							case 0x96 /* fcmpg */ -> result.append("FCMPG");
							case 0x97 /* dcmpl */ -> result.append("DCMPL");
							case 0x98 /* dcmpg */ -> result.append("DCMPG");
							case 0x99 /* ifeq */ -> result.append("IFEQ ").append(instructionPointer + iter.readShort());
							case 0x9a /* ifne */ -> result.append("IFNE ").append(instructionPointer + iter.readShort());
							case 0x9b /* iflt */ -> result.append("IFLT ").append(instructionPointer + iter.readShort());
							case 0x9c /* ifge */ -> result.append("IFGE ").append(instructionPointer + iter.readShort());
							case 0x9d /* ifgt */ -> result.append("IFGT ").append(instructionPointer + iter.readShort());
							case 0x9e /* ifle */ -> result.append("IFLE ").append(instructionPointer + iter.readShort());
							case 0x9f /* if_icmpeq */ -> result.append("IF_ICMPEQ ").append(instructionPointer + iter.readShort());
							case 0xa0 /* if_icmpne */ -> result.append("IF_ICMPNE ").append(instructionPointer + iter.readShort());
							case 0xa1 /* if_icmplt */ -> result.append("IF_ICMPLT ").append(instructionPointer + iter.readShort());
							case 0xa2 /* if_icmpge */ -> result.append("IF_ICMPGE ").append(instructionPointer + iter.readShort());
							case 0xa3 /* if_icmpgt */ -> result.append("IF_ICMPGT ").append(instructionPointer + iter.readShort());
							case 0xa4 /* if_icmple */ -> result.append("IF_ICMPLE ").append(instructionPointer + iter.readShort());
							case 0xa5 /* if_acmpeq */ -> result.append("IF_ACMPEQ ").append(instructionPointer + iter.readShort());
							case 0xa6 /* if_acmpne */ -> result.append("IF_ACMPNE ").append(instructionPointer + iter.readShort());
							case 0xa7 /* goto */ -> result.append("GOTO ").append(instructionPointer + iter.readShort());
							case 0xa8 /* jsr */ -> result.append("JSR ").append(instructionPointer + iter.readShort());
							case 0xa9 /* ret */ -> result.append("RET ").append(iter.readUnsignedByte());
							case 0xaa /* tableswitch */ -> {
								result.append("TABLESWITCH {");
								// Magic ;) dont question it
								iter.skipNBytes((-iter_base.getPos()) & 0b11);
								result.append("\n\tdefault: ");
								result.append(iter.readInt());
								int low = iter.readInt();
								int high = iter.readInt();
								int numPairs = high - low + 1;
								while (numPairs-- > 0) {
									if (numPairs == 0 || numPairs == high - low) {
										result.append(",\n\tcase 0x");
										String hexString = Integer.toUnsignedString(high - numPairs, 16);
										while (hexString.length() < 2 * Integer.BYTES) hexString = '0' + hexString;
										result.append(hexString);
										result.append(": ");
									} else result.append(",\n\t                 ");
									result.append(instructionPointer + iter.readInt());
								}
								result.append("\n}");
							}
							case 0xab /* lookupswitch */ -> {
								result.append("LOOKUPSWITCH {");
								// Magic ;) dont question it
								iter.skipNBytes((-iter_base.getPos()) & 0b11);
								result.append("\n\tdefault: ");
								result.append(iter.readInt());
								int numPairs = iter.readInt();
								while (numPairs-- > 0) {
									result.append(",\n\tcase 0x");
									String hexString = Integer.toUnsignedString(iter.readInt(), 16);
									while (hexString.length() < 2 * Integer.BYTES) hexString = '0' + hexString;
									result.append(hexString);
									result.append(": ");
									result.append(iter.readInt());
								}
								result.append("\n}");
							}
							case 0xac /* ireturn */ -> result.append("IRETURN");
							case 0xad /* lreturn */ -> result.append("LRETURN");
							case 0xae /* freturn */ -> result.append("FRETURN");
							case 0xaf /* dreturn */ -> result.append("DRETURN");
							case 0xb0 /* areturn */ -> result.append("ARETURN");
							case 0xb1 /* return */ -> result.append("RETURN");
							case 0xb2 /* getstatic */ -> result.append("GETSTATIC ").append(pool.get(iter.readUnsignedShort()));
							case 0xb3 /* putstatic */ -> result.append("PUT_STATIC ").append(pool.get(iter.readUnsignedShort()));
							case 0xb4 /* getfield */ -> result.append("GETFIELD ").append(pool.get(iter.readUnsignedShort()));
							case 0xb5 /* putfield */ -> result.append("PUT_FIELD ").append(pool.get(iter.readUnsignedShort()));
							case 0xb6 /* invokevirtual */ -> result.append("INVOKE_VIRTUAL ").append(pool.get(iter.readUnsignedShort()));
							case 0xb7 /* invokespecial */ -> result.append("INVOKE_SPECIAL ").append(pool.get(iter.readUnsignedShort()));
							case 0xb8 /* invokestatic */ -> result.append("INVOKE_STATIC ").append(pool.get(iter.readUnsignedShort()));
							case 0xb9 /* invokeinterface */ -> {
								result.append("INVOKE_INTERFACE ").append(pool.get(iter.readUnsignedShort()))
										.append(" ").append(iter.readUnsignedByte());
								assert iter.readByte() == 0;
							}
							case 0xba /* invokedynamic */ -> {
								result.append("INVOKE_DYNAMIC ").append(pool.get(iter.readUnsignedShort()));
								assert iter.readShort() == 0;
							}
							case 0xbb /* new */ -> result.append("NEW ").append(pool.get(iter.readUnsignedShort()));
							case 0xbc /* newarray */ -> result.append("NEW_ARRAY ").append(ARRAY_TYPES[iter.readByte() - 4]);
							case 0xbd /* anewarray */ -> result.append("ANEWARRAY ").append(pool.get(iter.readUnsignedShort()));
							case 0xbe /* arraylength */ -> result.append("ARRAYLENGTH");
							case 0xbf /* athrow*/ -> result.append("ATHROW");
							case 0xc0 /* checkcast */ -> result.append("CHECKCAST ").append(pool.get(iter.readUnsignedShort()));
							case 0xc1 /* instanceof */ -> result.append("INSTANCEOF ").append(pool.get(iter.readUnsignedShort()));
							case 0xc2 /* monitorenter */ -> result.append("MONITORENTER");
							case 0xc3 /* monitorexit */ -> result.append("MONITOREXIT");
							case 0xc5 /* multianewarray */ -> result.append("MULTIANEWARRAY ").append(pool.get(iter.readUnsignedShort()))
									.append(" ").append(iter.readUnsignedByte());
							case 0xc6 /* ifnull */ -> result.append("IFNULL ").append(instructionPointer + iter.readShort());
							case 0xc7 /* ifnonnull */ -> result.append("IFNONNULL ").append(instructionPointer + iter.readShort());
							case 0xc8 /* goto_w */ -> result.append("GOTO_W ").append(instructionPointer + iter.readInt());
							case 0xc9 /* jsr_w */ -> result.append("JSR_W ").append(instructionPointer + iter.readInt());
							case 0xc4 /* wide */ -> {
								result.append("WIDE ");
								switch (opcode = iter.readUnsignedByte()) {
									case 0x15 /* iload */ -> result.append("ILOAD ").append(iter.readUnsignedShort());
									case 0x16 /* lload */ -> result.append("LLOAD ").append(iter.readUnsignedShort());
									case 0x17 /* fload */ -> result.append("FLOAD ").append(iter.readUnsignedShort());
									case 0x18 /* dload */ -> result.append("DLOAD ").append(iter.readUnsignedShort());
									case 0x19 /* aload */ -> result.append("ALOAD ").append(iter.readUnsignedShort());
									case 0x36 /* istore */ -> result.append("ISTORE ").append(iter.readUnsignedShort());
									case 0x37 /* lstore */ -> result.append("LSTORE ").append(iter.readUnsignedShort());
									case 0x38 /* fstore */ -> result.append("FSTORE ").append(iter.readUnsignedShort());
									case 0x39 /* dstore */ -> result.append("DSTORE ").append(iter.readUnsignedShort());
									case 0x3a /* astore */ -> result.append("ASTORE ").append(iter.readUnsignedShort());
									case 0x84 /* iinc */ -> result.append("IINC ").append(iter.readUnsignedShort())
											.append(" ").append(iter.readShort());
									case 0xa9 /* ret */ -> result.append("RET ").append(iter.readUnsignedShort());
									default -> result.append("UNKNOWN");
								}
							}
							default -> result.append("UNKNOWN");
						}
					}
				} catch (IOException ignored) {
					// Shouldn't happen
				}
				result.append("\n\texception_table: ");
				result.append(exception_table.toString()
						.replaceFirst("^\\[", "[\n\t")
						.replaceFirst("\\]$", "\n]")
						.replace("[\n\t\n]", "[]")
						.replace(", ", ",\n\t")
						.replace("\n", "\n\t"));
				result.append("\n\tattributes: ");
				result.append(attributes.toString().replace("\n", "\n\t"));
				result.append("\n}");
				return result.toString();
			}
		}
		static class StackMapTableAttribute extends ArrayList<StackMapTableAttribute.StackMapFrame> implements AttributeValue {
			public static final String ATTR_NAME = "StackMapTable";
			StackMapTableAttribute(DataInputStream in, ConstantPool constantPool, long length) throws IOException {
				int num_entries = in.readUnsignedShort();
				ensureCapacity(num_entries); // Wish this could be super()
				List<VerificationTypeInfo> previousLocals = List.of();
				while (num_entries --> 0) {
					StackMapFrame frame = new StackMapFrame(in, constantPool, previousLocals);
					previousLocals = frame.locals;
					add(frame);
				}
			}
			@Override
			public String toString() {
				return "[\n\t" + String.join(",\n\t", stream().map(x -> x.toString().replace("\n", "\n\t")).toList()) + "\n]";
			}
			public static class VerificationTypeInfo {
				public static final @Unsigned byte ITEM_Top = 0;
				public static final @Unsigned byte ITEM_Integer = 1;
				public static final @Unsigned byte ITEM_Float = 2;
				public static final @Unsigned byte ITEM_Long = 4;
				public static final @Unsigned byte ITEM_Double = 3;
				public static final @Unsigned byte ITEM_Null = 5;
				public static final @Unsigned byte ITEM_UninitializedThis = 6;
				public static final @Unsigned byte ITEM_Object = 7;
				public static final @Unsigned byte ITEM_Uninitialized = 8;
				public final @Unsigned byte tag;
				public final @Nullable ConstantClassInfo type;
				public final @Unsigned short offset;
				VerificationTypeInfo(DataInputStream in, ConstantPool constantPool) throws IOException {
					tag = in.readByte();
					if (tag == ITEM_Object) {
						if(!(constantPool.get(in.readUnsignedShort()) instanceof ConstantClassInfo cls))
							throw new RuntimeException("Invalid object type");
						type = cls;
					} else type = null;
					if (tag ==  ITEM_Uninitialized) {
						offset = in.readShort();
					} else offset = -1;
				}

				@Override
				public String toString() {
					return switch (tag) {
						case ITEM_Top -> "Top";
						case ITEM_Integer -> "Integer";
						case ITEM_Float -> "Float";
						case ITEM_Double -> "Double";
						case ITEM_Long -> "Long";
						case ITEM_Null -> "Null";
						case ITEM_UninitializedThis -> "UninitializedThis";
						case ITEM_Object -> "Object(" + type + ")";
						case ITEM_Uninitialized -> "Uninitialized(" + offset + ")";
						default -> throw new IllegalStateException("Unexpected VerificationType value: " + tag);
					};
				}
			}
			public static class StackMapFrame {
				public final @Unsigned byte frame_type;
				public final @Unsigned short offset_delta;
				public final List<VerificationTypeInfo> locals;
				public final List<VerificationTypeInfo> stack;
				public StackMapFrame(DataInputStream in, ConstantPool constantPool,
				                     List<VerificationTypeInfo> previousLocals) throws IOException {
					// TYPES:                       offset_delta     locals            stack
					// 0-63:            same_frame: (ty)             prev              [0]
					// 64-127: same_locals_1_stack: (ty - 64)        prev              [1]
					// 128-246: RESERVED
					// 247: same_locals_1_stack_ex: offset_delta     prev              [1]
					// 248-250:         chop_frame: offset_delta     prev - (251 - ty) [0]
					// 251:          same_frame_ex: offset_delta     prev              [0]
					// 252-254:       append_frame: offset_delta     prev + [ty - 251] [0]
					// 255:             full_frame: offset_delta <n> locals[n]     <m> [m]
					frame_type = in.readByte();
					if (frame_type >= 0 /* 0 - 127 */) {
						offset_delta = (short) (frame_type % 64);
					} else {
						offset_delta = in.readShort();
					}
					if (frame_type == (byte) 255) {
						int number_of_locals = in.readUnsignedShort();
						locals = new ArrayList<>(number_of_locals);
						while (number_of_locals --> 0) {
							locals.add(new VerificationTypeInfo(in, constantPool));
						}
					} else if ((byte) 247 < frame_type && frame_type < (byte) 251) {
						List<VerificationTypeInfo> l;
						try {
							l = previousLocals.subList(0, previousLocals.size() - (byte) 251 + frame_type);
						} catch (Exception ignored) {l = new ArrayList<>();}
						locals = l;
					} else if ((byte) 251 < frame_type && frame_type < (byte) 255) {
						int number_of_locals = frame_type - (byte) 251;
						locals = new ArrayList<>(previousLocals);
						while (number_of_locals --> 0) {
							locals.add(new VerificationTypeInfo(in, constantPool));
						}
					} else {
						locals = List.copyOf(previousLocals);
					}
					if (frame_type == (byte) 255) {
						int number_of_stack_items = in.readUnsignedShort();
						stack = new ArrayList<>(number_of_stack_items);
						while (number_of_stack_items --> 0) {
							stack.add(new VerificationTypeInfo(in, constantPool));
						}
					} else if(frame_type >= 64 /* 64 - 127 */ || frame_type == (byte) 247) {
						stack = List.of(new VerificationTypeInfo(in, constantPool));
					} else {
						stack = new ArrayList<>();
					}
				}

				@Override
				public String toString() {
					return "StackMapFrame {" + "\n\tframe_type: " + frame_type +
							"\n\toffset_delta: " + offset_delta +
							"\n\tlocals: " + locals +
							"\n\tstack: " + stack +
							"\n}";
				}
			}
		}
		static class ExceptionsAttribute extends ArrayList<ConstantClassInfo> implements AttributeValue {
			public static final String ATTR_NAME = "Exceptions";
			ExceptionsAttribute(DataInputStream in, ConstantPool constantPool, long length) throws IOException {
				int numEntries = in.readUnsignedShort();
				assert numEntries == length * Short.BYTES;
				while (numEntries --> 0) {
					if(!(constantPool.get(in.readUnsignedShort()) instanceof ConstantClassInfo cls))
						throw new RuntimeException("Invalid exception class");
					add(cls);
				}
			}
		}
		static class InnerClassesAttribute extends ArrayList<InnerClassesAttribute.InnerClass> implements AttributeValue {
			public static final String ATTR_NAME = "InnerClasses";
			static class InnerClass {
				// Flags here appear to be same as AccessFlags class
//				public static final @Unsigned short ACC_PUBLIC = 0x0001; // Marked or implicitly public in source.
//				public static final @Unsigned short ACC_PRIVATE = 0x0002; // Marked private in source.
//				public static final @Unsigned short ACC_PROTECTED = 0x0004; // Marked protected in source.
//				public static final @Unsigned short ACC_STATIC = 0x0008; // Marked or implicitly static in source.
//				public static final @Unsigned short ACC_FINAL = 0x0010; // Marked final in source.
//				public static final @Unsigned short ACC_INTERFACE = 0x0200; // Was an interface in source.
//				public static final @Unsigned short ACC_ABSTRACT = 0x0400; // Marked or implicitly abstract in source.
//				public static final @Unsigned short ACC_SYNTHETIC = 0x1000; // Declared synthetic; not present in the source code.
//				public static final @Unsigned short ACC_ANNOTATION = 0x2000; // Declared as an annotation type.
//				public static final @Unsigned short ACC_ENUM = 0x4000; // Declared as an enum type.
				public final ConstantClassInfo innerClass;
				public final @Nullable ConstantClassInfo outerClass;
				public final @Nullable String innerName;
				public final @Unsigned short access_flags;

				InnerClass(DataInputStream in, ConstantPool constantPool) throws IOException {
					if(!(constantPool.get(in.readUnsignedShort()) instanceof ConstantClassInfo inner_class))
						throw new RuntimeException("Invalid inner class");
					innerClass = inner_class;

					int outer_class_index = in.readUnsignedShort();
					if(outer_class_index == 0) {
						outerClass = null;
					} else if(constantPool.get(outer_class_index) instanceof ConstantClassInfo outer_class) {
						outerClass = outer_class;
					} else throw new RuntimeException("Invalid outer class");

					int inner_name_index = in.readUnsignedShort();
					if(inner_name_index == 0) {
						innerName = null;
					} else if(constantPool.get(inner_name_index) instanceof ConstantUtf8Info name) {
						innerName = name.value;
					} else throw new RuntimeException("Invalid inner class name");

					access_flags = in.readShort();
				}

				@Override
				public String toString() {
					StringBuilder result = new StringBuilder();
					if (outerClass != null) {
						result.append(outerClass.toString());
						result.append(" > ");
					}
					result.append(AccessFlags.toReadableString(access_flags));
					result.append(innerClass.toString());
					if (innerName != null) {
						result.append(": ");
						result.append(innerName);
					}
					return result.toString();
				}
			}
			InnerClassesAttribute(DataInputStream in, ConstantPool constantPool, long length) throws IOException {
				int num_entries = in.readUnsignedShort();
				assert length == Short.BYTES + num_entries * (Short.BYTES * 4);
				while (num_entries --> 0) {
					add(new InnerClass(in, constantPool));
				}
			}

			@Override
			public String toString() {
				return super.toString()
					.replaceFirst("^\\[", "[\n\t")
					.replaceFirst("\\]$", "\n]")
					.replace("[\n\t\n]", "[]")
					.replace(", ", ",\n\t");
			}
		}
		static class EnclosingMethodAttribute implements AttributeValue {
			public static final String ATTR_NAME = "EnclosingMethod";
			public final ConstantClassInfo enclosingClass;
			public final @Nullable ConstantNameAndTypeInfo enclosingMethod;
			EnclosingMethodAttribute(DataInputStream in, ConstantPool constantPool, long length) throws IOException {
				assert length == Short.BYTES * 2;
				if(!(constantPool.get(in.readUnsignedShort()) instanceof ConstantClassInfo cls))
					throw new RuntimeException("Invalid enclosing class");
				enclosingClass = cls;

				int method_index = in.readUnsignedShort();
				if(method_index == 0) {
					enclosingMethod = null;
				} else if(constantPool.get(method_index) instanceof ConstantNameAndTypeInfo outer_class) {
					enclosingMethod = outer_class;
				} else throw new RuntimeException("Invalid enclosing method");
			}

			@Override
			public String toString() {
				StringBuilder result = new StringBuilder();
				result.append(enclosingClass);
				if (enclosingMethod != null) {
					result.append("::");
					result.append(enclosingMethod);
				}
				return result.toString();
			}
		}
		static class SignatureAttribute implements AttributeValue {
			public static final String ATTR_NAME = "Signature";
			public final String value;
			SignatureAttribute(DataInputStream in, ConstantPool constantPool, long length) throws IOException {
				assert length == Short.BYTES;
				if(!(constantPool.get(in.readUnsignedShort()) instanceof ConstantUtf8Info signature))
					throw new RuntimeException("Invalid enclosing class");
				value = signature.value;
			}

			@Override
			public String toString() {
				return value;
			}
		}
		static class SourceFileAttribute implements AttributeValue {
			public static final String ATTR_NAME = "SourceFile";
			public final String value;
			SourceFileAttribute(DataInputStream in, ConstantPool constantPool, long length) throws IOException {
				assert length == Short.BYTES;
				if(!(constantPool.get(in.readUnsignedShort()) instanceof ConstantUtf8Info source_file))
					throw new RuntimeException("Invalid enclosing class");
				value = source_file.value;
			}

			@Override
			public String toString() {
				return value;
			}
		}
		static class LineNumberTableAttribute extends ArrayList<LineNumberTableAttribute.LineNumber> implements AttributeValue {
			public static final String ATTR_NAME = "LineNumberTable";
			LineNumberTableAttribute(DataInputStream in, ConstantPool constantPool, long length) throws IOException {
				int line_number_table_length = in.readUnsignedShort();
				ensureCapacity(line_number_table_length);
				assert length == Short.BYTES + line_number_table_length + (2 * Short.BYTES);
				while (line_number_table_length --> 0) {
					add(new LineNumber(in, constantPool));
				}
			}
			static class LineNumber {
				public final @Unsigned short start_pc;
				public final @Unsigned short line_number;
				LineNumber(DataInputStream in, ConstantPool constantPool) throws IOException {
					start_pc = in.readShort();
					line_number = in.readShort();
				}
				@Override
				public String toString() {
					return start_pc + ": " + line_number;
				}
			}
		}
		static class LocalVariableTableAttribute extends ArrayList<LocalVariableTableAttribute.LocalVariable> implements AttributeValue {
			public static final String ATTR_NAME = "LocalVariableTable";
			LocalVariableTableAttribute(DataInputStream in, ConstantPool constantPool, long length) throws IOException {
				int local_variable_table_length = in.readUnsignedShort();
				ensureCapacity(local_variable_table_length);
				assert length == Short.BYTES + local_variable_table_length + (5 * Short.BYTES);
				while (local_variable_table_length --> 0) {
					add(new LocalVariable(in, constantPool));
				}
			}

			@Override
			public String toString() {
				return super.toString()
						.replaceFirst("^\\[", "[\n\t")
						.replaceFirst("\\]$", "\n]")
						.replace("[\n\t\n]", "[]")
						.replace(", ", ",\n\t");
			}

			static class LocalVariable {
				public final @Unsigned short start_pc;
				public final @Unsigned short length;
				public final String name;
				public final String descriptor;
				public final @Unsigned short index;
				LocalVariable(DataInputStream in, ConstantPool constantPool) throws IOException {
					start_pc = in.readShort();
					length = in.readShort();
					if (!(constantPool.get(in.readUnsignedShort()) instanceof ConstantUtf8Info name_str))
						throw new RuntimeException("Invalid local variable name");
					if (!(constantPool.get(in.readUnsignedShort()) instanceof ConstantUtf8Info descriptor_str))
						throw new RuntimeException("Invalid local variable descriptor");
					name = name_str.value;
					descriptor = descriptor_str.value;
					index = in.readShort();
				}
				@Override
				public String toString() {
					return name + ": " + descriptor
							+ " @[" + index + "] (" + start_pc + " - " + (start_pc + index - 1) + ")";
				}
			}
		}
		static class LocalVariableTypeTableAttribute extends ArrayList<LocalVariableTypeTableAttribute.LocalVariableType> implements AttributeValue {
			public static final String ATTR_NAME = "LocalVariableTypeTable";
			LocalVariableTypeTableAttribute(DataInputStream in, ConstantPool constantPool, long length) throws IOException {
				int local_variable_table_length = in.readUnsignedShort();
				ensureCapacity(local_variable_table_length);
				assert length == Short.BYTES + local_variable_table_length + (5 * Short.BYTES);
				while (local_variable_table_length --> 0) {
					add(new LocalVariableType(in, constantPool));
				}
			}

			@Override
			public String toString() {
				return super.toString()
						.replaceFirst("^\\[", "[\n\t")
						.replaceFirst("\\]$", "\n]")
						.replace("[\n\t\n]", "[]")
						.replace(", ", ",\n\t");
			}

			static class LocalVariableType {
				public final @Unsigned short start_pc;
				public final @Unsigned short length;
				public final String name;
				public final String signature;
				public final @Unsigned short index;
				LocalVariableType(DataInputStream in, ConstantPool constantPool) throws IOException {
					start_pc = in.readShort();
					length = in.readShort();
					if (!(constantPool.get(in.readUnsignedShort()) instanceof ConstantUtf8Info name_str))
						throw new RuntimeException("Invalid local variable name");
					if (!(constantPool.get(in.readUnsignedShort()) instanceof ConstantUtf8Info signature_str))
						throw new RuntimeException("Invalid local variable signature");
					name = name_str.value;
					signature = signature_str.value;
					index = in.readShort();
				}
				@Override
				public String toString() {
					return name + ": " + signature
							+ " @[" + index + "] (" + start_pc + " - " + (start_pc + index - 1) + ")";
				}
			}
		}
		static abstract class ElementValue {
			public static final char BYTE = 'B';
			public static final char CHAR = 'C';
			public static final char DOUBLE = 'D';
			public static final char FLOAT = 'F';
			public static final char INTEGER = 'I';
			public static final char LONG = 'J';
			public static final char SHORT = 'S';
			public static final char BOOLEAN = 'Z';
			public static final char STRING = 's';
			public static final char ENUM = 'e';
			public static final char CLASS = 'c';
			public static final char ANNOTATION = '@';
			public static final char ARRAY = '[';
			public final char tag;
			protected ElementValue(char type) {
				tag = type;
			}
			public static ElementValue read(DataInputStream in, ConstantPool constantPool) throws IOException {
				char tag = (char) in.readByte();
				return switch (tag) {
					case BYTE, CHAR, DOUBLE, FLOAT, INTEGER, LONG, SHORT, BOOLEAN, STRING
						-> new ConstantValue(tag, in, constantPool);
					case ENUM -> new EnumValue(in, constantPool);
					case CLASS -> new ClassValue(in, constantPool);
					case ANNOTATION -> new AnnotationValue(in, constantPool);
					case ARRAY -> new ArrayValue(in, constantPool);
					default -> throw new IllegalStateException("Unexpected element value tag: " + tag);
				};
			}

			@Override
			public abstract String toString();
			public static class ConstantValue extends ElementValue {
				public final ConstantPoolItem value;
				ConstantValue(char tag, DataInputStream in, ConstantPool constantPool) throws IOException {
					super(tag);
					value = constantPool.get(in.readUnsignedShort());
					assert value instanceof ConstantIntegerInfo
						|| value instanceof ConstantFloatInfo
						|| value instanceof ConstantLongInfo
						|| value instanceof ConstantDoubleInfo
						|| value instanceof ConstantStringInfo;
				}

				@Override
				public String toString() {
					return value.toString();
				}
			}
			public static class EnumValue extends ElementValue {
				public final String type_name;
				public final String value_name;
				EnumValue(DataInputStream in, ConstantPool constantPool) throws IOException {
					super(ENUM);
					if (!(constantPool.get(in.readUnsignedShort()) instanceof ConstantUtf8Info cls))
						throw new RuntimeException("Invalid enum classname");
					if (!(constantPool.get(in.readUnsignedShort()) instanceof ConstantUtf8Info val))
						throw new RuntimeException("Invalid enum const name");
					type_name = cls.value;
					value_name = val.value;
				}

				@Override
				public String toString() {
					return type_name + "::" + value_name;
				}
			}
			public static class ClassValue extends ElementValue {
				public final String class_name;
				ClassValue(DataInputStream in, ConstantPool constantPool) throws IOException {
					super(CLASS);
					if (!(constantPool.get(in.readUnsignedShort()) instanceof ConstantUtf8Info cls))
						throw new RuntimeException("Invalid classname");
					class_name = cls.value;
				}

				@Override
				public String toString() {
					return "Class<" + class_name + ">";
				}
			}
			public static class AnnotationValue extends ElementValue {
				public final Annotation value;
				AnnotationValue(DataInputStream in, ConstantPool constantPool) throws IOException {
					super(ANNOTATION);
					value = new Annotation(in, constantPool);
				}

				@Override
				public String toString() {
					return value.toString();
				}
			}
			public static class ArrayValue extends ElementValue {
				public final List<ElementValue> values;
				ArrayValue(DataInputStream in, ConstantPool constantPool) throws IOException {
					super(ARRAY);
					int length = in.readUnsignedShort();
					values = new ArrayList<>(length);
					while (length --> 0)  {
						values.add(ElementValue.read(in, constantPool));
					}
				}

				@Override
				public String toString() {
					return values.toString();
				}
			}
		}
		static class Annotation {
			public final String type;
			public final Map<String, ElementValue> element_values = new HashMap<>();
			Annotation(DataInputStream in, ConstantPool constantPool) throws IOException {
				if (!(constantPool.get(in.readUnsignedShort()) instanceof ConstantUtf8Info type_desc))
					throw new RuntimeException("Invalid annotation field type");
				type = type_desc.value;
				int length = in.readUnsignedShort();
				while (length --> 0) {
					if (!(constantPool.get(in.readUnsignedShort()) instanceof ConstantUtf8Info element_name))
						throw new RuntimeException("Invalid annotation element name");
					element_values.put(element_name.value, ElementValue.read(in, constantPool));
				}
			}

			@Override
			public String toString() {
				StringBuilder result = new StringBuilder("@" + type);
				if (!element_values.isEmpty()) {
					result.append("(");
					for (var entry : element_values.entrySet()) {
						result.append(entry.getKey());
						result.append("=");
						result.append(entry.getValue().toString());
						result.append(", ");
					}
					result.append("\b\b)");
				}
				return result.toString();
			}
		}
		static class RuntimeVisibleAnnotationsAttribute extends ArrayList<Annotation> implements AttributeValue {
			public static final String ATTR_NAME = "RuntimeVisibleAnnotations";
			RuntimeVisibleAnnotationsAttribute(DataInputStream in, ConstantPool constantPool, long length) throws IOException {
				int num_attributes = in.readUnsignedShort();
				ensureCapacity(num_attributes);
				while (num_attributes --> 0) {
					add(new Annotation(in, constantPool));
				}
			}
		}
		static class RuntimeInvisibleAnnotationsAttribute extends ArrayList<Annotation> implements AttributeValue {
			public static final String ATTR_NAME = "RuntimeInvisibleAnnotations";
			RuntimeInvisibleAnnotationsAttribute(DataInputStream in, ConstantPool constantPool, long length) throws IOException {
				int num_attributes = in.readUnsignedShort();
				ensureCapacity(num_attributes);
				while (num_attributes --> 0) {
					add(new Annotation(in, constantPool));
				}
			}
		}
		static class ParameterAnnotations extends ArrayList<Annotation> {
			ParameterAnnotations(DataInputStream in, ConstantPool constantPool) throws IOException {
				int length = in.readUnsignedShort();
				ensureCapacity(length);
				while (length --> 0) {
					add(new Annotation(in, constantPool));
				}
			}
		}
		static class RuntimeVisibleParameterAnnotationsAttribute extends ArrayList<ParameterAnnotations> implements AttributeValue {
			public static final String ATTR_NAME = "RuntimeVisibleParameterAnnotations";
			RuntimeVisibleParameterAnnotationsAttribute(DataInputStream in, ConstantPool constantPool, long length) throws IOException {
				int num_parameters = in.readUnsignedByte();
				ensureCapacity(num_parameters);
				while (num_parameters --> 0) {
					add(new ParameterAnnotations(in, constantPool));
				}
			}
		}
		static class RuntimeInvisibleParameterAnnotationsAttribute extends ArrayList<ParameterAnnotations> implements AttributeValue {
			public static final String ATTR_NAME = "RuntimeInvisibleParameterAnnotations";
			RuntimeInvisibleParameterAnnotationsAttribute(DataInputStream in, ConstantPool constantPool, long length) throws IOException {
				int num_parameters = in.readUnsignedByte();
				ensureCapacity(num_parameters);
				while (num_parameters --> 0) {
					add(new ParameterAnnotations(in, constantPool));
				}
			}
		}
		static class AnnotationDefaultAttribute implements AttributeValue {
			public static final String ATTR_NAME = "AnnotationDefault";
			public final ElementValue value;
			AnnotationDefaultAttribute(DataInputStream in, ConstantPool constantPool, long length) throws IOException {
				value = ElementValue.read(in, constantPool);
			}

			@Override
			public String toString() {
				return value.toString();
			}
		}
		static class BootstrapMethodsAttribute extends ArrayList<BootstrapMethodsAttribute.BootstrapMethod> implements AttributeValue {
			public static final String ATTR_NAME = "BootstrapMethods";
			BootstrapMethodsAttribute(DataInputStream in, ConstantPool constantPool, long length) throws IOException {
				int num_bootstrap_methods = in.readUnsignedShort();
				ensureCapacity(num_bootstrap_methods);
				while (num_bootstrap_methods --> 0) {
					add(new BootstrapMethod(in, constantPool));
				}
			}

			@Override
			public String toString() {
				return "[\n\t" + String.join(",\n\t", stream().map(x -> x.toString().replace("\n", "\n\t")).toList()) + "\n]";
			}
			static class BootstrapMethod {
				public final ConstantMethodHandleInfo bootstrap_method;
				public final List<ConstantPoolItem> bootstrap_arguments;
				BootstrapMethod(DataInputStream in, ConstantPool constantPool) throws IOException {
					if (!(constantPool.get(in.readUnsignedShort()) instanceof ConstantMethodHandleInfo method_handle))
						throw new RuntimeException("Invalid bootstrap method ref");
					bootstrap_method = method_handle;
					int length = in.readUnsignedShort();
					bootstrap_arguments = new ArrayList<>(length);
					while (length --> 0) {
						ConstantPoolItem bootstrap_arg = constantPool.get(in.readUnsignedShort());
						assert bootstrap_arg instanceof ConstantIntegerInfo
							|| bootstrap_arg instanceof ConstantFloatInfo
							|| bootstrap_arg instanceof ConstantLongInfo
							|| bootstrap_arg instanceof ConstantDoubleInfo
							|| bootstrap_arg instanceof ConstantStringInfo;
						bootstrap_arguments.add(bootstrap_arg);
					}
				}

				@Override
				public String toString() {
					return bootstrap_method.toString() + "(" + bootstrap_arguments.toString().substring(1) + "\b)";
				}
			}
		}
	}
	static class MethodInfo {
		public static @Unsigned short ACC_PUBLIC = 0x0001; // Declared public; may be accessed from outside its package.
		public static @Unsigned short ACC_PRIVATE = 0x0002; // Declared private; accessible only within the defining class.
		public static @Unsigned short ACC_PROTECTED = 0x0004; // Declared protected; may be accessed within subclasses.
		public static @Unsigned short ACC_STATIC = 0x0008; // Declared static.
		public static @Unsigned short ACC_FINAL = 0x0010; // Declared final; must not be overridden (§5.4.5).
		public static @Unsigned short ACC_SYNCHRONIZED = 0x0020; // Declared synchronized; invocation is wrapped by a monitor use.
		public static @Unsigned short ACC_BRIDGE = 0x0040; // A bridge method, generated by the compiler.
		public static @Unsigned short ACC_VARARGS = 0x0080; // Declared with variable number of arguments.
		public static @Unsigned short ACC_NATIVE = 0x0100; // Declared native; implemented in a language other than Java.
		public static @Unsigned short ACC_ABSTRACT = 0x0400; // Declared abstract; no implementation is provided.
		public static @Unsigned short ACC_STRICT = 0x0800; // Declared strictfp; floating-point mode is FP-strict.
		public static @Unsigned short ACC_SYNTHETIC = 0x1000; // Declared synthetic; not present in the source code.
		final @Unsigned short access_flags;
		final String name;
		final String descriptor;
		final Attributes attributes;

		private MethodInfo(DataInputStream in, ConstantPool constantPool) throws IOException {
			access_flags = in.readShort();
			if(!(constantPool.get(in.readUnsignedShort()) instanceof ConstantUtf8Info method_name))
				throw new RuntimeException("Invalid method name");
			if(!(constantPool.get(in.readUnsignedShort()) instanceof ConstantUtf8Info method_desc))
				throw new RuntimeException("Invalid method descriptor");
			name = method_name.value;
			descriptor = method_desc.value;
			attributes = new Attributes(in, constantPool);
		}

		public static MethodInfo readFrom(DataInputStream in, ConstantPool constantPool) throws IOException {
			return new MethodInfo(in, constantPool);
		}

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			if((access_flags & ACC_PUBLIC) != 0) result.append("public ");
			if((access_flags & ACC_PRIVATE) != 0) result.append("private ");
			if((access_flags & ACC_PROTECTED) != 0) result.append("protected ");
			if((access_flags & ACC_STATIC) != 0) result.append("static ");
			if((access_flags & ACC_FINAL) != 0) result.append("final ");
			if((access_flags & ACC_SYNCHRONIZED) != 0) result.append("synchronized ");
			if((access_flags & ACC_BRIDGE) != 0) result.append("[bridge] ");
			if((access_flags & ACC_VARARGS) != 0) result.append("[varargs] ");
			if((access_flags & ACC_NATIVE) != 0) result.append("native ");
			if((access_flags & ACC_ABSTRACT) != 0) result.append("abstract ");
			if((access_flags & ACC_STRICT) != 0) result.append("strict ");
			if((access_flags & ACC_SYNTHETIC) != 0) result.append("synthetic ");
			result.append(name);
			result.append(": ");
			result.append(descriptor);
			result.append(" ");
			result.append(attributes.toString().replace("\n", "\n\t"));
			return result.toString();
		}
	}

	static class FieldInfo {
		public static final @Unsigned short ACC_PUBLIC = 0x0001; // Declared public; may be accessed from outside its package.
		public static final @Unsigned short ACC_PRIVATE = 0x0002; // Declared private; usable only within the defining class.
		public static final @Unsigned short ACC_PROTECTED = 0x0004; // Declared protected; may be accessed within subclasses.
		public static final @Unsigned short ACC_STATIC = 0x0008; // Declared static.
		public static final @Unsigned short ACC_FINAL = 0x0010; // Declared final; never directly assigned to after object construction (JLS §17.5).
		public static final @Unsigned short ACC_VOLATILE = 0x0040; // Declared volatile; cannot be cached.
		public static final @Unsigned short ACC_TRANSIENT = 0x0080; // Declared transient; not written or read by a persistent object manager.
		public static final @Unsigned short ACC_SYNTHETIC = 0x1000; // Declared synthetic; not present in the source code.
		public static final @Unsigned short ACC_ENUM = 0x4000; // Declared as an element of an enum.
		final @Unsigned short access_flags;
		final String name;
		final String descriptor;
		final Attributes attributes;
		private FieldInfo(DataInputStream in, ConstantPool constantPool) throws IOException {
			access_flags = in.readShort();

			if(!(constantPool.get(in.readUnsignedShort()) instanceof ConstantUtf8Info field_name))
				throw new RuntimeException("Invalid field name");
			if(!(constantPool.get(in.readUnsignedShort()) instanceof ConstantUtf8Info field_desc))
				throw new RuntimeException("Invalid field descriptor");
			name = field_name.value;
			descriptor = field_desc.value;
			attributes = new Attributes(in, constantPool);
		}

		public static FieldInfo readFrom(DataInputStream in, ConstantPool constantPool) throws IOException {
			return new FieldInfo(in, constantPool);
		}

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			if((access_flags & ACC_PUBLIC) != 0) result.append("public ");
			if((access_flags & ACC_PRIVATE) != 0) result.append("private ");
			if((access_flags & ACC_PROTECTED) != 0) result.append("protected ");
			if((access_flags & ACC_STATIC) != 0) result.append("static ");
			if((access_flags & ACC_FINAL) != 0) result.append("final ");
			if((access_flags & ACC_VOLATILE) != 0) result.append("volatile ");
			if((access_flags & ACC_TRANSIENT) != 0) result.append("transient ");
			if((access_flags & ACC_SYNTHETIC) != 0) result.append("synthetic ");
			if((access_flags & ACC_ENUM) != 0) result.append("[enum] ");
			result.append(name);
			result.append(": ");
			result.append(descriptor);
			result.append(" ");
			result.append(attributes.toString().replace("\n", "\n\t"));
			return result.toString();
		}
	}

	static abstract class ConstantPoolItem {
		protected ConstantPoolItem(ConstantPool pool) {
			pool.add(this);
		}

		@Override
		public abstract String toString();

		static void readFrom(DataInputStream in, ConstantPool constantPool) throws IOException {
			switch (in.readByte()) {
				case ConstantClassInfo.TYPE -> new ConstantClassInfo(in, constantPool);
				case ConstantFieldRefInfo.TYPE -> new ConstantFieldRefInfo(in, constantPool);
				case ConstantMethodRefInfo.TYPE -> new ConstantMethodRefInfo(in, constantPool);
				case ConstantInterfaceMethodRefInfo.TYPE -> new ConstantInterfaceMethodRefInfo(in, constantPool);
				case ConstantStringInfo.TYPE -> new ConstantStringInfo(in, constantPool);
				case ConstantIntegerInfo.TYPE -> new ConstantIntegerInfo(in, constantPool);
				case ConstantFloatInfo.TYPE -> new ConstantFloatInfo(in, constantPool);
				case ConstantLongInfo.TYPE -> new ConstantLongInfo(in, constantPool);
				case ConstantDoubleInfo.TYPE -> new ConstantDoubleInfo(in, constantPool);
				case ConstantNameAndTypeInfo.TYPE -> new ConstantNameAndTypeInfo(in, constantPool);
				case ConstantUtf8Info.TYPE -> new ConstantUtf8Info(in, constantPool);
				case ConstantMethodHandleInfo.TYPE -> new ConstantMethodHandleInfo(in, constantPool);
				case ConstantMethodTypeInfo.TYPE -> new ConstantMethodTypeInfo(in, constantPool);
				case ConstantInvokeDynamicInfo.TYPE -> new ConstantInvokeDynamicInfo(in, constantPool);
				default -> throw new IllegalStateException("Unexpected value");
			}
		}
	}

	public static class ConstantClassInfo extends ConstantPoolItem {
		static final byte TYPE = 7;
		public final String name;

		protected ConstantClassInfo(DataInputStream in, ConstantPool constantPool) throws IOException {
			super(constantPool);
			if(!(constantPool.await(in.readUnsignedShort()) instanceof ConstantUtf8Info cls_name))
				throw new RuntimeException("Invalid class_name");
			name = cls_name.value;
		}

		@Override
		public String toString() {
			return "Class<" + name + ">";
		}
	}
	static class ConstantMemberRefInfo extends ConstantPoolItem {
		final ConstantClassInfo cls;
		final ConstantNameAndTypeInfo signature;

		protected ConstantMemberRefInfo(DataInputStream in, ConstantPool constantPool) throws IOException {
			super(constantPool);
			int class_index = in.readUnsignedShort();
			int name_and_type_index = in.readUnsignedShort();
			if(!(constantPool.await(class_index) instanceof ConstantClassInfo cls_info))
				throw new RuntimeException("Invalid class");
			if(!(constantPool.await(name_and_type_index) instanceof ConstantNameAndTypeInfo name_type))
				throw new RuntimeException("Invalid signature");
			cls = cls_info;
			signature = name_type;
		}

		@Override
		public String toString() {
			return cls.toString() + "::" + signature.toString();
		}
	}
	static class ConstantFieldRefInfo extends ConstantMemberRefInfo {
		static final byte TYPE = 9;

		protected ConstantFieldRefInfo(DataInputStream in, ConstantPool constantPool) throws IOException {
			super(in, constantPool);
		}
	}
	static class ConstantMethodRefInfo extends ConstantMemberRefInfo {
		static final byte TYPE = 10;

		protected ConstantMethodRefInfo(DataInputStream in, ConstantPool constantPool) throws IOException {
			super(in, constantPool);
		}
	}
	static class ConstantInterfaceMethodRefInfo extends ConstantMemberRefInfo {
		static final byte TYPE = 11;

		protected ConstantInterfaceMethodRefInfo(DataInputStream in, ConstantPool constantPool) throws IOException {
			super(in, constantPool);
		}
	}
	static class ConstantStringInfo extends ConstantPoolItem {
		static final byte TYPE = 8;
		final String value;

		protected ConstantStringInfo(DataInputStream in, ConstantPool constantPool) throws IOException {
			super(constantPool);
			if(!(constantPool.await(in.readUnsignedShort()) instanceof ConstantUtf8Info str))
				throw new RuntimeException("Invalid string value");
			value = str.value;
		}

		@Override
		public String toString() {
			return "String(\"" + value + "\")";
		}
	}
	static class ConstantIntegerInfo extends ConstantPoolItem {
		static final byte TYPE = 3;
		final int value;

		protected ConstantIntegerInfo(DataInputStream in, ConstantPool constantPool) throws IOException {
			super(constantPool);
			value = in.readInt();
		}

		@Override
		public String toString() {
			return Integer.toString(value);
		}
	}
	static class ConstantFloatInfo extends ConstantPoolItem {
		static final byte TYPE = 4;
		final float value;

		protected ConstantFloatInfo(DataInputStream in, ConstantPool constantPool) throws IOException {
			super(constantPool);
			value = in.readFloat();
		}

		@Override
		public String toString() {
			return Float.toString(value);
		}
	}
	static class ConstantLongInfo extends ConstantPoolItem {
		static final byte TYPE = 5;
		final long value;

		protected ConstantLongInfo(DataInputStream in, ConstantPool constantPool) throws IOException {
			super(constantPool);
			constantPool.add(this);
			value = in.readLong();
		}

		@Override
		public String toString() {
			return Long.toString(value);
		}
	}
	static class ConstantDoubleInfo extends ConstantPoolItem {
		static final byte TYPE = 6;
		final double value;

		protected ConstantDoubleInfo(DataInputStream in, ConstantPool constantPool) throws IOException {
			super(constantPool);
			constantPool.add(this);
			value = in.readDouble();
		}

		@Override
		public String toString() {
			return Double.toString(value);
		}
	}
	static class ConstantNameAndTypeInfo extends ConstantPoolItem {
		static final byte TYPE = 12;
		final String name;
		final String descriptor;

		protected ConstantNameAndTypeInfo(DataInputStream in, ConstantPool constantPool) throws IOException {
			super(constantPool);
			int name_index = in.readUnsignedShort();
			int type_index = in.readUnsignedShort();
			if(!(constantPool.await(name_index) instanceof ConstantUtf8Info val_name))
				throw new RuntimeException("Invalid value name");
			if(!(constantPool.await(type_index) instanceof ConstantUtf8Info val_type))
				throw new RuntimeException("Invalid value descriptor");
			name = val_name.value;
			descriptor = val_type.value;
		}

		@Override
		public String toString() {
			return name + ": " + descriptor;
		}
	}
	static class ConstantUtf8Info extends ConstantPoolItem {
		static final byte TYPE = 1;
		final @Unsigned String value;

		protected ConstantUtf8Info(DataInputStream in, ConstantPool constantPool) throws IOException {
			super(constantPool);
			int length = in.readUnsignedShort();
			// TODO: fucking java uses weird "modified" UTF8
			// see: https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.4.7
			value = new String(in.readNBytes(length), StandardCharsets.UTF_8);
		}

		@Override
		public String toString() {
			return '"' + value + '"';
		}
	}
	static class ConstantMethodHandleInfo extends ConstantPoolItem {
		static final byte REF_getField = (byte)1; // getfield C.f:T
		static final byte REF_getStatic = (byte)2; // getstatic C.f:T
		static final byte REF_putField = (byte)3; // putfield C.f:T
		static final byte REF_putStatic = (byte)4; // putstatic C.f:T
		static final byte REF_invokeVirtual = (byte)5; // invokevirtual C.m:(A*)T
		static final byte REF_invokeStatic = (byte)6; // invokestatic C.m:(A*)T
		static final byte REF_invokeSpecial = (byte)7; // invokespecial C.m:(A*)T
		static final byte REF_newInvokeSpecial = (byte)8; // new C; dup; invokespecial C.<init>:(A*)void
		static final byte REF_invokeInterface = (byte)9; // invokeinterface C.m:(A*)T
		static final byte TYPE = 15;
		final @Unsigned byte /* ReferenceType */ reference_type;
		final ConstantPoolItem reference;

		protected ConstantMethodHandleInfo(DataInputStream in, ConstantPool constantPool) throws IOException {
			super(constantPool);
			reference_type = in.readByte();
			ConstantPoolItem ref = constantPool.await(in.readUnsignedShort());
			if(!switch (reference_type) {
				case REF_getField, REF_getStatic, REF_putField, REF_putStatic
						-> ref instanceof ConstantFieldRefInfo;
				case REF_invokeVirtual, REF_invokeStatic, REF_invokeSpecial, REF_newInvokeSpecial
						-> ref instanceof ConstantMethodRefInfo || /* apparently, undocumented */ ref instanceof ConstantInterfaceMethodRefInfo;
				case REF_invokeInterface
						-> ref instanceof ConstantInterfaceMethodRefInfo;
				default -> throw new RuntimeException("Unexpected reference type: " + reference_type);})
				throw new RuntimeException("Invalid reference for type " + reference_type);
			reference = ref;
		}

		@Override
		public String toString() {
			return "&(" + switch (reference_type) {
				case REF_getField -> "getField";
				case REF_getStatic -> "getStatic";
				case REF_putField -> "putField";
				case REF_putStatic -> "putStatic";
				case REF_invokeVirtual -> "invokeVirtual";
				case REF_invokeStatic -> "invokeStatic";
				case REF_invokeSpecial -> "invokeSpecial";
				case REF_newInvokeSpecial -> "newInvokeSpecial";
				case REF_invokeInterface -> "invokeInterface";
				default -> "???";
			} + ")" + reference.toString();
		}
	}
	static class ConstantMethodTypeInfo extends ConstantPoolItem {
		static final byte TYPE = 16;
		final String descriptor;

		protected ConstantMethodTypeInfo(DataInputStream in, ConstantPool constantPool) throws IOException {
			super(constantPool);
			if(!(constantPool.await(in.readUnsignedShort()) instanceof ConstantUtf8Info desc))
				throw new RuntimeException("Invalid descriptor");
			descriptor = desc.value;
		}

		@Override
		public String toString() {
			return descriptor;
		}
	}
	static class ConstantInvokeDynamicInfo extends ConstantPoolItem {
		static final byte TYPE = 18;
		final @Unsigned short bootstrap_method_attr_index;
		final ConstantNameAndTypeInfo signature;

		protected ConstantInvokeDynamicInfo(DataInputStream in, ConstantPool constantPool) throws IOException {
			super(constantPool);
			bootstrap_method_attr_index = in.readShort();
			if(!(constantPool.await(in.readUnsignedShort()) instanceof ConstantNameAndTypeInfo name_type))
				throw new RuntimeException("Invalid signature");
			signature = name_type;
		}

		@Override
		public String toString() {
			return signature + " @" + bootstrap_method_attr_index;
		}
	}
}
