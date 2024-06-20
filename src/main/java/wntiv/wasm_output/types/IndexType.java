package wntiv.wasm_output.types;

import wntiv.wasm_output.Util;
import wntiv.wasm_output.Writable;

import java.io.DataOutputStream;
import java.io.IOException;

public record IndexType(int index) implements Writable {
	@Override
	public void write(DataOutputStream target) throws IOException {
		Util.writeVarUInt(target, index);
	}
}
