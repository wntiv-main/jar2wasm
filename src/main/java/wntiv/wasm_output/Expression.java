package wntiv.wasm_output;

import java.io.DataOutputStream;
import java.io.IOException;

public class Expression implements Writable {
	private final byte[] value;

	public Expression(byte[] value) {
		this.value = value;
	}

	@Override
	public void write(DataOutputStream target) throws IOException {
		target.write(value);
		target.writeByte(0x0B);
	}
}
