package wntiv.wasm_output;

import wntiv.wasm_output.types.ValueType;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

public interface WasmFunction extends Writable {
//	private final ByteArrayOutputStream code = new ByteArrayOutputStream();
//	private final Map<ValueType, Integer> locals = new HashMap<>();

	Expression getCode();

	Map<ValueType, Integer> getLocals();

	@Override
	default void write(DataOutputStream target) throws IOException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		DataOutputStream bufView = new DataOutputStream(buf);
		for (var entry : getLocals().entrySet()) {
			Util.writeVarUInt(bufView, entry.getValue() /* count */);
			entry.getKey() /* type */.write(bufView);
		}
		getCode().write(bufView);
		Util.writeVarUInt(target, buf.size()); // Prefix length
		buf.writeTo(target);
	}
}
