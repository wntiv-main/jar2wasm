package wntiv.wasm_output;

import jdk.jfr.Unsigned;
import org.jetbrains.annotations.Nullable;

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
			writeVarUInt(new DataOutputStream(contentBuf), data.dataSegments.size());
			writeVarUInt(target, contentBuf.size());
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
			writeVarUInt(target, sectionData.size());
			sectionData.writeTo(target);
		}
	}
	public static void writeVarInt(DataOutputStream to, long value) throws IOException {
		// Using SLEB128
		do {
			to.writeByte((int) (value & 0x7F) | ((value >>= 7) != 0 && value != -1 ? 0x80 : 0));
		} while (value != 0 && value != -1);
	}
	public static void writeVarUInt(DataOutputStream to, long value) throws IOException {
		// Using ULEB128
		do {
			to.writeByte((int) (value & 0x7F) | ((value >>>= 7) != 0 ? 0x80 : 0));
		} while (value != 0);
	}

	static abstract class Section {
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
		abstract void write(DataOutputStream target) throws IOException;
	}
	static class TypeSection extends Section {
		private final List<FunctionType> types = new ArrayList<>();
		@Override
		void write(DataOutputStream target) throws IOException {
			writeVarUInt(target, types.size());
			for (FunctionType type : types) {
				type.write(target);
			}
		}
		@Override
		byte sectionType() {
			return TYPE_SECTION;
		}
		static class FunctionType {
			public static final @Unsigned byte I32_TYPE = 0x7F;
			public static final @Unsigned byte I64_TYPE = 0x7E;
			public static final @Unsigned byte F32_TYPE = 0x7D;
			public static final @Unsigned byte F64_TYPE = 0x7C;
			public static final @Unsigned byte VECTOR_TYPE = 0x7B;
			public static final @Unsigned byte FUNCTION_REF_TYPE = 0x70;
			public static final @Unsigned byte EXTERNAL_REF_TYPE = 0x6F;
			private final List<Byte> arguments = new ArrayList<>();
			private final List<Byte> results = new ArrayList<>();
			void write(DataOutputStream target) throws IOException {
				target.writeByte(0x60); // Function type
				writeVarUInt(target, arguments.size());
				for (byte arg_type : arguments) target.writeByte(arg_type);
				writeVarUInt(target, results.size());
				for (byte result_type : results) target.writeByte(result_type);
			}
		}
	}
	static class DataSection extends Section {
		List<DataSegment> dataSegments = new ArrayList<>();

		@Override
		void write(DataOutputStream target) throws IOException {
			writeVarUInt(target, dataSegments.size());
			for (DataSegment segment : dataSegments) {
				segment.write(target);
			}
		}

		@Override
		byte sectionType() {
			return DATA_SECTION;
		}

		static class DataSegment {
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

			void write(DataOutputStream target) throws IOException {
				int flags = 0;
				if (passive) flags |= PASSIVE_BIT;
				if (memoryIndex != 0) flags |= EXPLICIT_MEMORY_INDEX_BIT;
				writeVarUInt(target, flags);
				if (memoryIndex != 0) {
					writeVarUInt(target, memoryIndex);
				}
				if (!passive) {
					target.write(offsetExpression);
					target.writeByte(0x0B); // End of expression
				}
				writeVarUInt(target, internalData.size());
				internalData.writeTo(target);
			}
		}
	}
	static class CodeSection extends Section {
		@Override
		void write(DataOutputStream target) throws IOException {
			// TODO:
		}

		@Override
		byte sectionType() {
			return CODE_SECTION;
		}
	}
}
