package wntiv.wasm_output;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Util {
	private Util() {}

	public static void writeVarInt(DataOutputStream to, long value) throws IOException {
		// Using SLEB128
		do {
			to.writeByte((int) (value & 0x7F) | ((value >>= 7) != 0 && value != -1 ? 0x80 : 0));
		} while (value != 0 && value != -1);
	}
	public static void writeVarUInt(DataOutputStream to, long value) throws IOException {
		// Using ULEB128
		do {
			to.writeByte((int) (value & 0x7F) | ((value >>>= 7) != 0 ? 0x80 : 0));
		} while (value != 0);
	}
	public static void writeFloat(DataOutputStream to, float value) throws IOException {
		to.writeInt(Integer.reverseBytes(Float.floatToRawIntBits(value)));
	}
	public static void writeDouble(DataOutputStream to, double value) throws IOException {
		to.writeLong(Long.reverseBytes(Double.doubleToRawLongBits(value)));
	}
	public static void writeName(DataOutputStream to, String value) throws IOException {
		// https://webassembly.github.io/spec/core/binary/values.html#binary-name
		writeVarUInt(to, value.length());
		to.write(value.getBytes(StandardCharsets.UTF_8));
	}
}
