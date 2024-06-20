package wntiv.wasm_output.types;

import wntiv.wasm_output.Util;
import wntiv.wasm_output.Writable;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class ResultType extends ArrayList<ValueType> implements Writable {
	@Override
	public void write(DataOutputStream target) throws IOException {
		Util.writeVarUInt(target, size());
		for (ValueType result : this) {
			result.write(target);
		}
	}
}
