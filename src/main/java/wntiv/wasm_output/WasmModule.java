package wntiv.wasm_output;

import jdk.jfr.Unsigned;
import wntiv.wasm_output.types.GlobalType;
import wntiv.wasm_output.types.IndexType;
import wntiv.wasm_output.types.MemoryType;
import wntiv.wasm_output.types.TableType;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
//	final File jsOut = new File("out.js");
//		File wasmOutFile = new File("out.wasm");

public class WasmModule {
	public static final byte[] WASM_MAGIC = {0x00, 'a', 's', 'm'};
	public static final byte[] WASM_VERSION = {0x01, 0x00, 0x00, 0x00};
	private final DataSection data = new DataSection();
	private final CodeSection code = new CodeSection();
	public void write(DataOutputStream target) throws IOException {
		target.write(WASM_MAGIC);
		target.write(WASM_VERSION);
		{
			// Handle data count section specially
			target.writeByte(Section.DATA_COUNT_SECTION);
			ByteArrayOutputStream contentBuf = new ByteArrayOutputStream();
			Util.writeVarUInt(new DataOutputStream(contentBuf), data.sectionElements.size());
			Util.writeVarUInt(target, contentBuf.size());
			contentBuf.writeTo(target);
		}
		writeSection(target, code);
		writeSection(target, data);
	}
	private static void writeSection(DataOutputStream target, Section section) throws IOException {
		ByteArrayOutputStream sectionData = new ByteArrayOutputStream();
		section.write(new DataOutputStream(sectionData));
		if (sectionData.size() > 0) {
			target.writeByte(section.sectionType());
			Util.writeVarUInt(target, sectionData.size());
			sectionData.writeTo(target);
		}
	}

	static abstract class Section implements Writable {
		public static final @Unsigned byte CUSTOM_SECTION = 0;
		public static final @Unsigned byte TYPE_SECTION = 1;
		public static final @Unsigned byte IMPORT_SECTION = 2;
		public static final @Unsigned byte FUNCTION_SECTION = 3;
		public static final @Unsigned byte TABLE_SECTION = 4;
		public static final @Unsigned byte MEMORY_SECTION = 5;
		public static final @Unsigned byte GLOBAL_SECTION = 6;
		public static final @Unsigned byte EXPORT_SECTION = 7;
		public static final @Unsigned byte START_SECTION = 8;
		public static final @Unsigned byte ELEMENT_SECTION = 9;
		public static final @Unsigned byte CODE_SECTION = 10;
		public static final @Unsigned byte DATA_SECTION = 11;
		public static final @Unsigned byte DATA_COUNT_SECTION = 12;
		abstract byte sectionType();
	}
	static abstract class CollectionSection<T extends Writable> extends Section implements Writable {
		protected final List<T> sectionElements = new ArrayList<>();
		@Override
		public void write(DataOutputStream target) throws IOException {
			Util.writeVarUInt(target, sectionElements.size());
			for (T elem : sectionElements) {
				elem.write(target);
			}
		}
	}
	static class TypeSection extends CollectionSection<TypeSection.FunctionType> {
		@Override
		byte sectionType() {
			return TYPE_SECTION;
		}
		static class FunctionType implements Writable {
			public static final @Unsigned byte I32_TYPE = 0x7F;
			public static final @Unsigned byte I64_TYPE = 0x7E;
			public static final @Unsigned byte F32_TYPE = 0x7D;
			public static final @Unsigned byte F64_TYPE = 0x7C;
			public static final @Unsigned byte VECTOR_TYPE = 0x7B;
			public static final @Unsigned byte FUNCTION_REF_TYPE = 0x70;
			public static final @Unsigned byte EXTERNAL_REF_TYPE = 0x6F;
			private final List<Byte> arguments = new ArrayList<>();
			private final List<Byte> results = new ArrayList<>();
			@Override
			public void write(DataOutputStream target) throws IOException {
				target.writeByte(0x60); // Function type
				Util.writeVarUInt(target, arguments.size());
				for (byte arg_type : arguments) target.writeByte(arg_type);
				Util.writeVarUInt(target, results.size());
				for (byte result_type : results) target.writeByte(result_type);
			}
		}
	}
	static class ImportSection extends CollectionSection<ImportSection.Import> {
		@Override
		byte sectionType() {
			return IMPORT_SECTION;
		}
		static class ImportDescriptor implements Writable {
			protected static @Unsigned byte TYPE_INDEX = 0x00;
			protected static @Unsigned byte TABLE_TYPE = 0x01;
			protected static @Unsigned byte MEMORY_TYPE = 0x02;
			protected static @Unsigned byte GLOBAL_TYPE = 0x03;
			private final @Unsigned byte type;
			private final Writable value;

			ImportDescriptor(IndexType typeIndex) {
				type = TYPE_INDEX;
				value = typeIndex;
			}
			ImportDescriptor(TableType table) {
				type = TABLE_TYPE;
				value = table;
			}
			ImportDescriptor(MemoryType memory) {
				type = MEMORY_TYPE;
				value = memory;
			}
			ImportDescriptor(GlobalType global) {
				type = GLOBAL_TYPE;
				value = global;
			}

			@Override
			public void write(DataOutputStream target) throws IOException {
				target.writeByte(type);
				value.write(target);
			}
		}
		static class Import implements Writable {
			public final String moduleName;
			public final String importName;
			public final ImportDescriptor importType;
			Import(String module, String symbol, ImportDescriptor type) {
				moduleName = module;
				importName = symbol;
				importType = type;
			}
			@Override
			public void write(DataOutputStream target) throws IOException {
				Util.writeName(target, moduleName);
				Util.writeName(target, importName);
				importType.write(target);
			}
		}
	}
	static class DataSection extends CollectionSection<DataSection.DataSegment> {
		@Override
		byte sectionType() {
			return DATA_SECTION;
		}

		static class DataSegment implements Writable {
			public static final @Unsigned int PASSIVE_BIT = 0x01;
			public static final @Unsigned int EXPLICIT_MEMORY_INDEX_BIT = 0x02;
			final boolean passive;
			final int memoryIndex;
			final byte[] offsetExpression;
			private final ByteArrayOutputStream internalData = new ByteArrayOutputStream();
			final DataOutputStream data = new DataOutputStream(internalData);
			DataSegment() {
				passive = true;
				memoryIndex = 0;
				offsetExpression = new byte[]{};
			}
			DataSegment(byte[] offsetExpression) {
				this(0, offsetExpression);
			}
			DataSegment(int memoryIndex, byte[] offsetExpression) {
				passive = false;
				this.memoryIndex = memoryIndex;
				this.offsetExpression = offsetExpression;
			}
			@Override
			public void write(DataOutputStream target) throws IOException {
				int flags = 0;
				if (passive) flags |= PASSIVE_BIT;
				if (memoryIndex != 0) flags |= EXPLICIT_MEMORY_INDEX_BIT;
				Util.writeVarUInt(target, flags);
				if (memoryIndex != 0) {
					Util.writeVarUInt(target, memoryIndex);
				}
				if (!passive) {
					target.write(offsetExpression);
					target.writeByte(0x0B); // End of expression
				}
				Util.writeVarUInt(target, internalData.size());
				internalData.writeTo(target);
			}
		}
	}
	static class CodeSection extends Section {
		@Override
		public void write(DataOutputStream target) throws IOException {
			// TODO:
		}

		@Override
		byte sectionType() {
			return CODE_SECTION;
		}
	}
}
