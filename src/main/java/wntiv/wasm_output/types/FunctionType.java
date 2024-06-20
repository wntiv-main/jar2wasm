package wntiv.wasm_output.types;

import wntiv.wasm_output.Writable;

import java.io.DataOutputStream;
import java.io.IOException;

public record FunctionType(ResultType arguments, ResultType results) implements Writable {
	@Override
	public void write(DataOutputStream target) throws IOException {
		target.writeByte(0x60); // https://webassembly.github.io/spec/core/binary/types.html#binary-functype
		arguments.write(target);
		results.write(target);
	}
}
