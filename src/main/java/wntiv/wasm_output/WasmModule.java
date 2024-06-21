package wntiv.wasm_output;

import jdk.jfr.Unsigned;
import org.jetbrains.annotations.Nullable;
import wntiv.wasm_output.types.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class WasmModule {
	public static final byte[] WASM_MAGIC = {0x00, 'a', 's', 'm'};
	public static final byte[] WASM_VERSION = {0x01, 0x00, 0x00, 0x00};
	private final TypeSection types = new TypeSection();
	private final ImportSection imports = new ImportSection();
	private final FunctionSection function_types = new FunctionSection();
	private final TableSection tables = new TableSection();
	private final MemorySection memory = new MemorySection();
	private final GlobalSection globals = new GlobalSection();
	private final ExportSection exports = new ExportSection();
	private @Nullable StartSection start = null;
	private final ElementSection elements = new ElementSection();
	private final DataSection data = new DataSection();
	private final CodeSection code = new CodeSection();

	public IndexType addFunction(WasmFunction functionSpec) {
		return new IndexType(code.add(functionSpec));
	}
//	public void export(IndexType function) ...?

	public void write(DataOutputStream target) throws IOException {
		target.write(WASM_MAGIC);
		target.write(WASM_VERSION);
		writeSection(target, types);
		writeSection(target, imports);
		writeSection(target, function_types);
		writeSection(target, tables);
		writeSection(target, memory);
		writeSection(target, globals);
		writeSection(target, exports);
		if (start != null) writeSection(target, start);
		writeSection(target, elements);
		{
			// Handle data count section specially
			target.writeByte(Section.DATA_COUNT_SECTION);
			ByteArrayOutputStream contentBuf = new ByteArrayOutputStream();
			Util.writeVarUInt(new DataOutputStream(contentBuf), data.size());
			Util.writeVarUInt(target, contentBuf.size());
			contentBuf.writeTo(target);
		}
		writeSection(target, code);
		writeSection(target, data);
	}
	private static void writeSection(DataOutputStream target, Section section) throws IOException {
		if (section instanceof WritableCollection<?> collectionSection && collectionSection.size() == 0) return;
		ByteArrayOutputStream sectionData = new ByteArrayOutputStream();
		section.write(new DataOutputStream(sectionData));
		if (sectionData.size() > 0) {
			target.writeByte(section.sectionType());
			Util.writeVarUInt(target, sectionData.size());
			sectionData.writeTo(target);
		}
	}
	// https://webassembly.github.io/spec/core/binary/modules.html
	interface Section extends Writable {
		@Unsigned byte CUSTOM_SECTION = 0;
		@Unsigned byte TYPE_SECTION = 1;
		@Unsigned byte IMPORT_SECTION = 2;
		@Unsigned byte FUNCTION_SECTION = 3;
		@Unsigned byte TABLE_SECTION = 4;
		@Unsigned byte MEMORY_SECTION = 5;
		@Unsigned byte GLOBAL_SECTION = 6;
		@Unsigned byte EXPORT_SECTION = 7;
		@Unsigned byte START_SECTION = 8;
		@Unsigned byte ELEMENT_SECTION = 9;
		@Unsigned byte CODE_SECTION = 10;
		@Unsigned byte DATA_SECTION = 11;
		@Unsigned byte DATA_COUNT_SECTION = 12;
		public byte sectionType();
	}
	static class WritableCollection<T extends Writable> implements Writable {
		private final List<T> elements = new ArrayList<>();
		public int size() {
			return elements.size();
		}
		public int add(T element) {
			elements.add(element);
			return size() - 1;
		}
		@Override
		public void write(DataOutputStream target) throws IOException {
			Util.writeVarUInt(target, elements.size());
			for (T elem : elements) {
				elem.write(target);
			}
		}
	}
	static class TypeSection extends WritableCollection<TypeSection.FunctionType> implements Section {
		@Override
		public byte sectionType() {
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
	static class ImportSection extends WritableCollection<ImportSection.Import> implements Section {
		@Override
		public byte sectionType() {
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
		record Import(String moduleName, String importName, ImportDescriptor type) implements Writable {
			@Override
			public void write(DataOutputStream target) throws IOException {
				Util.writeName(target, moduleName);
				Util.writeName(target, importName);
				type.write(target);
			}
		}
	}
	static class FunctionSection extends WritableCollection<IndexType> implements Section {
		@Override
		public byte sectionType() {
			return FUNCTION_SECTION;
		}
	}
	static class TableSection extends WritableCollection<TableType> implements Section {
		@Override
		public byte sectionType() {
			return TABLE_SECTION;
		}
	}
	static class MemorySection extends WritableCollection<MemoryType> implements Section {
		@Override
		public byte sectionType() {
			return MEMORY_SECTION;
		}
	}
	static class GlobalSection extends WritableCollection<GlobalSection.Global> implements Section {
		@Override
		public byte sectionType() {
			return GLOBAL_SECTION;
		}
		record Global(GlobalType type, Expression initialiser) implements Writable {
			@Override
			public void write(DataOutputStream target) throws IOException {
				type.write(target);
				initialiser.write(target);
			}
		}
	}
	static class ExportSection extends WritableCollection<ExportSection.Export> implements Section {
		@Override
		public byte sectionType() {
			return EXPORT_SECTION;
		}
		static class ExportDescriptor implements Writable {
			protected static @Unsigned byte FUNCTION_TYPE = 0x00;
			protected static @Unsigned byte TABLE_TYPE = 0x01;
			protected static @Unsigned byte MEMORY_TYPE = 0x02;
			protected static @Unsigned byte GLOBAL_TYPE = 0x03;
			private final @Unsigned byte type;
			private final IndexType index;

			ExportDescriptor(byte type, int index) {
				this.type = type;
				this.index = new IndexType(index);
			}

			@Override
			public void write(DataOutputStream target) throws IOException {
				target.writeByte(type);
				index.write(target);
			}
		}
		record Export(String exportName, ExportDescriptor type) implements Writable {
			@Override
			public void write(DataOutputStream target) throws IOException {
				Util.writeName(target, exportName);
				type.write(target);
			}
		}
	}
	record StartSection(IndexType functionIndex) implements Section {
		@Override
		public byte sectionType() {
			return START_SECTION;
		}
		@Override
		public void write(DataOutputStream target) throws IOException {
			functionIndex.write(target);
		}
	}
	static class ElementSection extends WritableCollection<ElementSection.ElementSegment> implements Section {
		@Override
		public byte sectionType() {
			return ELEMENT_SECTION;
		}

		// https://webassembly.github.io/spec/core/binary/modules.html#binary-elemsec
		static class ElementSegment implements Writable {
			// Passive (tableIndex = 0, tableOffset = null)
			// Declarative (tableIndex != 0, tableOffset = null)
			// Active (tableIndex = ?, tableOffset != null)
			public final IndexType tableIndex;
			public final @Nullable Expression tableOffset;
			// reftype | elementkind
			public final byte type;
			// Union[WritableCollection<IndexType> | Expression]
			public final Writable initializer;

			private ElementSegment(IndexType tableIndex, @Nullable Expression tableOffset, byte type, Writable initializer) {
				this.tableIndex = tableIndex;
				this.tableOffset = tableOffset;
				this.type = type;
				this.initializer = initializer;
			}

			@Override
			public void write(DataOutputStream target) throws IOException {
				boolean passive_declarative = tableOffset == null;
				boolean declarative_table_offset = tableIndex.index() != 0;
				boolean expressions = initializer instanceof Expression;
				target.writeByte((passive_declarative ? 0b001 : 0)
						| (declarative_table_offset ? 0b010 : 0)
						| (expressions ? 0b100 : 0));
				if (!passive_declarative) {
					if (declarative_table_offset || (expressions && type != ValueType.FUNCTION_REF.asByte())
							                     || (!expressions && type != 0x00)) {
						// active_extended
						tableIndex.write(target);
						tableOffset.write(target);
						target.writeByte(type);
					} else {
						// active
						tableOffset.write(target);
					}
				} else {
					// passive/declarative
					target.writeByte(type);
				}
				initializer.write(target);
			}

			public static ElementSegment passive(byte type, WritableCollection<IndexType> initFunctions) {
				return new ElementSegment(new IndexType(0), null, type, initFunctions);
			}
			public static ElementSegment passive(ValueType refType, Expression initializer) {
				assert ValueType.isReferenceType(refType);
				return new ElementSegment(new IndexType(0), null, refType.asByte(), initializer);
			}
			public static ElementSegment declarative(byte elementKind, WritableCollection<IndexType> initFunctions) {
				return new ElementSegment(new IndexType(1), null, elementKind, initFunctions);
			}
			public static ElementSegment declarative(ValueType refType, Expression initializer) {
				assert ValueType.isReferenceType(refType);
				return new ElementSegment(new IndexType(1), null, refType.asByte(), initializer);
			}
			public static ElementSegment active(Expression tableOffset, WritableCollection<IndexType> initFunctions) {
				return new ElementSegment(new IndexType(0), tableOffset, (byte) 0x00, initFunctions);
			}
			public static ElementSegment active(Expression tableOffset, Expression initializer) {
				return new ElementSegment(new IndexType(0), tableOffset, ValueType.FUNCTION_REF.asByte(), initializer);
			}
			public static ElementSegment active(IndexType tableIndex, Expression tableOffset, byte elementKind, WritableCollection<IndexType> initFunctions) {
				return new ElementSegment(tableIndex, tableOffset, elementKind, initFunctions);
			}
			public static ElementSegment active(IndexType tableIndex, Expression tableOffset, ValueType refType, Expression initializer) {
				assert ValueType.isReferenceType(refType);
				return new ElementSegment(tableIndex, tableOffset, refType.asByte(), initializer);
			}
		}
	}
	static class DataSection extends WritableCollection<DataSection.DataSegment> implements Section {
		@Override
		public byte sectionType() {
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
	static class CodeSection extends WritableCollection<WasmFunction> implements Section {
		@Override
		public byte sectionType() {
			return CODE_SECTION;
		}
	}
}
