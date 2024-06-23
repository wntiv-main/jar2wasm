package wntiv.wasm_output.types;

import java.io.DataOutputStream;
import java.io.IOException;

public enum PackedType implements StorageType {
	I8(0x78),
	I16(0x77);

	private final byte value;
	PackedType(int i) {
		value = (byte) i;
	}

	@Override
	public void write(DataOutputStream target) throws IOException {
		target.writeByte(value);
	}
}
