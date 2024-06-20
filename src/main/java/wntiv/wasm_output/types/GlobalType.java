package wntiv.wasm_output.types;

import wntiv.wasm_output.Writable;

import java.io.DataOutputStream;
import java.io.IOException;

public record GlobalType(ValueType type, boolean mutable) implements Writable {
	@Override
	public void write(DataOutputStream target) throws IOException {
		type.write(target);
		target.writeBoolean(mutable);
	}
}
