package wntiv.wasm_output.types;

import jdk.jfr.Unsigned;
import wntiv.wasm_output.Util;
import wntiv.wasm_output.Writable;

import java.io.DataOutputStream;
import java.io.IOException;

// https://webassembly.github.io/spec/core/syntax/types.html#syntax-limits
public class Limits implements Writable {
	public final @Unsigned int lowerBound;
	public final @Unsigned int upperBound;
	public final boolean hasUpperBound;

	public Limits(int lower, int upper) {
		lowerBound = lower;
		upperBound = upper;
		hasUpperBound = true;
	}
	public Limits(int lower) {
		lowerBound = lower;
		upperBound = -1;
		hasUpperBound = false;
	}

	@Override
	public void write(DataOutputStream target) throws IOException {
		target.writeBoolean(hasUpperBound);
		Util.writeVarUInt(target, lowerBound);
		if (hasUpperBound) Util.writeVarUInt(target, lowerBound);
	}
}
