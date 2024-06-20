package wntiv.wasm_output.types;

import wntiv.wasm_output.Writable;

import java.io.DataOutputStream;
import java.io.IOException;

public record TableType(Limits size, ValueType refType) implements Writable {
	public TableType {
		if (!ValueType.isReferenceType(refType))
			throw new IllegalArgumentException("refType must be a valid reference type");
	}
	@Override
	public void write(DataOutputStream target) throws IOException {
		size.write(target);
		refType.write(target);
	}
}
