package wntiv.wasm_output;

import java.io.DataOutputStream;
import java.io.IOException;

public interface Writable {
	void write(DataOutputStream target) throws IOException;
}
