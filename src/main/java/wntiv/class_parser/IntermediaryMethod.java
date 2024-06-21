package wntiv.class_parser;

import wntiv.wasm_output.Expression;
import wntiv.wasm_output.WasmFunction;
import wntiv.wasm_output.types.ValueType;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IntermediaryMethod implements WasmFunction {
	private final ClassHandler.MethodInfo info;
	private List<Operation> code = new ArrayList<>();

	public IntermediaryMethod(ClassHandler.MethodInfo info) {
		this.info = info;
	}

	@Override
	public Expression getCode() {
		ByteArrayOutputStream codeBinary = new ByteArrayOutputStream();
		for (Operation op : code) {
			op.writeWasm(codeBinary);
		}
		return new Expression(codeBinary.toByteArray());
	}

	@Override
	public Map<ValueType, Integer> getLocals() {
		return null;
	}
}
