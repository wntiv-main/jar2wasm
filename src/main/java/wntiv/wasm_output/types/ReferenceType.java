package wntiv.wasm_output.types;

import java.io.DataOutputStream;
import java.io.IOException;

public record ReferenceType(HeapType targetType, boolean nullable) implements ValueType {
	@Override
	public void write(DataOutputStream target) throws IOException {
		if (nullable && targetType instanceof HeapType.AbstractHeapType) {
			// Short form
			targetType.write(target);
		} else {
			target.writeByte(nullable ? 0x63 : 0x64);
			targetType.write(target);
		}
	}
}
