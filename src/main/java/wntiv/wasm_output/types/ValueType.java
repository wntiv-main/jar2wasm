package wntiv.wasm_output.types;

import wntiv.wasm_output.Writable;

import java.io.DataOutputStream;
import java.io.IOException;

public enum ValueType implements Writable {
	I32((byte) 0x7F),
	I64((byte) 0x7E),
	F32((byte) 0x7D),
	F64((byte) 0x7C),
	VECTOR((byte) 0x7B),
	FUNCTION_REF((byte) 0x70),
	EXTERNAL_REF((byte) 0x6F);
	private final byte value;
	ValueType(byte i) {
		value = i;
	}

	@Override
	public void write(DataOutputStream target) throws IOException {
		target.writeByte(value);
	}
	public static boolean isReferenceType(ValueType type) {
		return type == FUNCTION_REF || type == EXTERNAL_REF;
	}
	public static boolean isIntegralType(ValueType type) {
		return type == I32 || type == I64;
	}
	public static boolean isFloatingPointType(ValueType type) {
		return type == F32 || type == F64;
	}
	public static boolean isNumericType(ValueType type) {
		return isIntegralType(type) || isFloatingPointType(type);
	}
}
