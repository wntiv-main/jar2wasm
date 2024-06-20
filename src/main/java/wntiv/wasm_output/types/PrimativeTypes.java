package wntiv.wasm_output.types;

import wntiv.wasm_output.Writable;

import java.io.DataOutputStream;
import java.io.IOException;

public enum PrimativeTypes implements Writable {
	I32((byte) 0x7F),
	I64((byte) 0x7E),
	F32((byte) 0x7D),
	F64((byte) 0x7C),
	VECTOR((byte) 0x7B),
	FUNCTION_REF((byte) 0x70),
	EXTERNAL_REF((byte) 0x6F);
	private final byte value;
	PrimativeTypes(byte i) {
		value = i;
	}

	@Override
	public void write(DataOutputStream target) throws IOException {
		target.writeByte(value);
	}
}
