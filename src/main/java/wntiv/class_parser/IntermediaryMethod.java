package wntiv.class_parser;

import wntiv.wasm_output.Expression;
import wntiv.wasm_output.WasmFunction;
import wntiv.wasm_output.types.ValueType;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IntermediaryMethod implements WasmFunction {
	private final ClassHandler.MethodInfo info;
	private final ModuleContext moduleCtx;
	private List<Operation> code = new ArrayList<>();

	public IntermediaryMethod(ClassHandler.MethodInfo info, ModuleContext moduleContext) {
		this.info = info;
		moduleCtx = moduleContext;
	}

	@Override
	public Expression getCode() {
		try {
			ByteArrayOutputStream codeBinary = new ByteArrayOutputStream();
			DataOutputStream codeView = new DataOutputStream(codeBinary);
			for (Operation op : code) {
				op.writeWasm(codeView, moduleCtx);
			}
			return new Expression(codeBinary.toByteArray());
		} catch (IOException e) {
			throw new UncheckedIOException(e); // :)
		}
	}

	@Override
	public Map<ValueType, Integer> getLocals() {
		return null;
	}
}
