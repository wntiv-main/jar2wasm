package wntiv.wasm_output;

import wntiv.wasm_output.types.ValueType;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class FunctionFactory {
	private final ByteArrayOutputStream code = new ByteArrayOutputStream();
	private final Map<ValueType, Integer> locals = new HashMap<>();

	public byte[] getCode() {
		return code.toByteArray();
	}

	public Map<ValueType, Integer> getLocals() {
		return locals;
	}
}
