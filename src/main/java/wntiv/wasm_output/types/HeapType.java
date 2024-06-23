package wntiv.wasm_output.types;

import wntiv.wasm_output.Util;
import wntiv.wasm_output.Writable;

import java.io.DataOutputStream;
import java.io.IOException;

public interface HeapType extends Writable {
	enum AbstractHeapType implements HeapType {
		NO_FUNC(0x73),
		NO_EXTERN(0x72),
		NONE(0x71),
		FUNC(0x70),
		EXTERN(0x6F),
		ANY(0x6E),
		EQ(0x6D),
		I31(0x6C),
		STRUCT(0x6B),
		ARRAY(0x6A);
		byte value;
		AbstractHeapType(int b) {
			value = (byte) b;
		}

		@Override
		public void write(DataOutputStream target) throws IOException {
			target.writeByte(value);
		}

		public ReferenceType asRef() {
			return new ReferenceType(this, true);
		}
	}
	record ConcreteHeapType(long typeIndex) implements HeapType {
		@Override
		public void write(DataOutputStream target) throws IOException {
			Util.writeVarInt(target, typeIndex);
		}
	}
}
