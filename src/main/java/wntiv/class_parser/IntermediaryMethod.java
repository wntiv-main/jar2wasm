package wntiv.class_parser;

import wntiv.Pair;
import wntiv.wasm_output.Expression;
import wntiv.wasm_output.WasmFunction;
import wntiv.wasm_output.WasmModule;
import wntiv.wasm_output.types.ValueType;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class IntermediaryMethod implements WasmFunction {
	private final ClassHandler.MethodInfo info;
	private final WasmModule module;
	public final JarHandler bindings;
	private final List<Operation> code = new ArrayList<>();
	private final List<Integer> codeIndex = new ArrayList<>();

	public IntermediaryMethod(ClassHandler.MethodInfo method, WasmModule module, JarHandler binding) {
		this.info = method;
		this.module = module;
		bindings = binding;
		assert info.attributes.code != null;
		try {
			var codeSrc = new ByteArrayInputStream(info.attributes.code.code) {
				public int getPos() { // exposed!!!
					return pos;
				}
			};
			DataInputStream dataView = new DataInputStream(codeSrc);
			while (dataView.available() > 0) {
				int pos = codeSrc.getPos();
				Operation op = Operation.readFromStream(this, dataView, codeSrc::getPos);
				if (op instanceof Operation.GoTo) {
					// TODO: Special handling??
				} else {
					codeIndex.add(pos);
					code.add(op);
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e); // ^-^
		}
	}

	public ClassHandler.MethodInfo getInfo() {
		return info;
	}

	public WasmModule getModule() {
		return module;
	}

	@Override
	public Expression getCode() {
		try {
			ByteArrayOutputStream codeBinary = new ByteArrayOutputStream();
			DataOutputStream codeView = new DataOutputStream(codeBinary);
			for (int i = 0; i < code.size(); i++) {
				Operation op = code.get(i);
				int index = codeIndex.get(i);
				op.writeWasm(index, this, codeView);
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

	public Stream<Pair<Integer, Operation>> getOpsInRange(int i, int j) {
		int start, end = 0;
		while (codeIndex.get(end) < i) end++;
		start = end;
		while (codeIndex.get(end) <= j) end++;
		return IntStream.range(start, end).mapToObj(x -> new Pair<>(codeIndex.get(x), code.get(x)));
	}
	public Stream<Pair<Integer, Operation>> getBlockAt(int i) {
		// TODO: get code "block" that resides here
	}
}
