package wntiv.wasm_output.types;

import java.io.DataOutputStream;
import java.io.IOException;

public enum NumericType implements ValueType {
	I32((byte) 0x7F),
	I64((byte) 0x7E),
	F32((byte) 0x7D),
	F64((byte) 0x7C),
	VECTOR((byte) 0x7B); // TODO: Technically not numeric type
	private final byte value;
	NumericType(byte i) {
		value = i;
	}

	public byte asByte() {
		return value;
	}

	@Override
	public void write(DataOutputStream target) throws IOException {
		target.writeByte(value);
	}
	public static boolean isNumericType(ValueType type) {
		return type instanceof NumericType;
	}
	public static boolean isIntegralType(ValueType type) {
		return type == I32 || type == I64;
	}
	public static boolean isFloatingPointType(NumericType type) {
		return type == F32 || type == F64;
	}
}
