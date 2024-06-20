package wntiv.wasm_output.types;

import wntiv.wasm_output.Writable;

import java.io.DataOutputStream;
import java.io.IOException;

public record MemoryType(Limits size) implements Writable {
	@Override
	public void write(DataOutputStream target) throws IOException {
		size.write(target);
	}
}
