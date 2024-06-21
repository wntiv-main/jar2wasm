package wntiv.class_parser;

import wntiv.wasm_output.WasmFunction;

import java.util.ArrayList;
import java.util.List;

public class IntermediaryMethod implements WasmFunction {
	private final ClassHandler.MethodInfo info;
	private List<Operation> code = new ArrayList<>();

	public IntermediaryMethod(ClassHandler.MethodInfo info) {
		this.info = info;
	}
}
