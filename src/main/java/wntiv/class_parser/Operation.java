package wntiv.class_parser;

import wntiv.wasm_output.Util;
import wntiv.wasm_output.types.ValueType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.function.IntSupplier;

public interface Operation {
	enum Type {
		INT,
		LONG,
		FLOAT,
		DOUBLE,
		ARRAY,
		BYTE,
		CHAR,
		SHORT;
	}
	void writeWasm(DataOutputStream out, IntermediaryMethod context) throws IOException;
	static Operation readFromStream(DataInputStream input, IntSupplier alignment, ClassHandler.ConstantPool pool) throws IOException {
		int opcode = input.readUnsignedByte();
		return switch (opcode) {
			case 0x00 /* nop */ -> new DirectTranslation(0x01);
			case 0x01 /* aconst_null */ -> new PushConst(ValueType.EXTERNAL_REF, null); // TODO: ??
			case 0x02 /* iconst_m1 */ -> new PushConst(ValueType.I32, -1);
			case 0x03 /* iconst_0 */ -> new PushConst(ValueType.I32, 0);
			case 0x04 /* iconst_1 */ -> new PushConst(ValueType.I32, 1);
			case 0x05 /* iconst_2 */ -> new PushConst(ValueType.I32, 2);
			case 0x06 /* iconst_3 */ -> new PushConst(ValueType.I32, 3);
			case 0x07 /* iconst_4 */ -> new PushConst(ValueType.I32, 4);
			case 0x08 /* iconst_5 */ -> new PushConst(ValueType.I32, 5);
			case 0x09 /* lconst_0 */ -> new PushConst(ValueType.I64, 0L);
			case 0x0a /* lconst_1 */ -> new PushConst(ValueType.I64, 1L);
			case 0x0b /* fconst_0 */ -> new PushConst(ValueType.F32, 0F);
			case 0x0c /* fconst_1 */ -> new PushConst(ValueType.F32, 1F);
			case 0x0d /* fconst_2 */ -> new PushConst(ValueType.F32, 2F);
			case 0x0e /* dconst_0 */ -> new PushConst(ValueType.F64, 0D);
			case 0x0f /* dconst_1 */ -> new PushConst(ValueType.F64, 1D);
			case 0x10 /* bipush */ -> new PushConst(ValueType.I32, input.readByte());
			case 0x11 /* sipush */ -> new PushConst(ValueType.I32, input.readShort());
			case 0x12 /* ldc */, 0x13 /* ldc_w */ -> {
				ClassHandler.ConstantPoolItem constValue = pool.get(opcode == 0x12 ? input.readUnsignedByte()
				                                                                   : input.readUnsignedShort());
				if (constValue instanceof ClassHandler.ConstantIntegerInfo intValue)
					new PushConst(ValueType.I32, intValue.value);
				else if (constValue instanceof ClassHandler.ConstantFloatInfo floatValue)
					new PushConst(ValueType.F32, floatValue.value);
				else if (constValue instanceof ClassHandler.ConstantStringInfo stringValue)
					new PushConst(ValueType.EXTERNAL_REF, stringValue.value); // TODO: Make value in data section??
				else if (constValue instanceof ClassHandler.ConstantClassInfo object)
					new PushConst(ValueType.EXTERNAL_REF, object);
				else throw new RuntimeException("Unexpected const value");
			}
			case 0x14 /* ldc2_w */ -> {
				ClassHandler.ConstantPoolItem constValue = pool.get(input.readUnsignedShort());
				if (constValue instanceof ClassHandler.ConstantLongInfo longValue)
					new PushConst(ValueType.I64, longValue.value);
				else if (constValue instanceof ClassHandler.ConstantDoubleInfo doubleValue)
					new PushConst(ValueType.F64, doubleValue.value);
				else throw new RuntimeException("Unexpected const value");
			}
			case 0x15 /* iload */ -> new PushLocal(input.readUnsignedByte());
			case 0x16 /* lload */ -> new PushLocal(input.readUnsignedByte());
			case 0x17 /* fload */ -> new PushLocal(input.readUnsignedByte());
			case 0x18 /* dload */ -> new PushLocal(input.readUnsignedByte());
			case 0x19 /* aload */ -> new PushLocal(input.readUnsignedByte());
			case 0x1a, 0x1b, 0x1c, 0x1d /* iload_<n> */ -> new PushLocal(opcode - 0x1a);
			case 0x1e, 0x1f, 0x20, 0x21 /* lload_<n> */ -> new PushLocal(opcode - 0x1e);
			case 0x22, 0x23, 0x24, 0x25 /* fload_<n> */ -> new PushLocal(opcode - 0x22);
			case 0x26, 0x27, 0x28, 0x29 /* dload_<n> */ -> new PushLocal(opcode - 0x26);
			case 0x2a, 0x2b, 0x2c, 0x2d /* aload_<n> */ -> new PushLocal(opcode - 0x2a);
			case 0x2e /* iaload */ -> new PushArray(Type.INT);
			case 0x2f /* laload */ -> new PushArray(Type.LONG);
			case 0x30 /* faload */ -> new PushArray(Type.FLOAT);
			case 0x31 /* daload */ -> new PushArray(Type.DOUBLE);
			case 0x32 /* aaload */ -> new PushArray(Type.ARRAY);
			case 0x33 /* baload */ -> new PushArray(Type.BYTE);
			case 0x34 /* caload */ -> new PushArray(Type.CHAR);
			case 0x35 /* saload */ -> new PushArray(Type.SHORT);
			case 0x36 /* istore */ -> new PopLocal(input.readUnsignedByte());
			case 0x37 /* lstore */ -> new PopLocal(input.readUnsignedByte());
			case 0x38 /* fstore */ -> new PopLocal(input.readUnsignedByte());
			case 0x39 /* dstore */ -> new PopLocal(input.readUnsignedByte());
			case 0x3a /* astore */ -> new PopLocal(input.readUnsignedByte());
			case 0x3b, 0x3c, 0x3d, 0x3e /* istore_<n> */ -> new PopLocal(opcode - 0x3b);
			case 0x3f, 0x40, 0x41, 0x42 /* lstore_<n> */ -> new PopLocal(opcode - 0x3f);
			case 0x43, 0x44, 0x45, 0x46 /* fstore_<n> */ -> new PopLocal(opcode - 0x43);
			case 0x47, 0x48, 0x49, 0x4a /* dstore_<n> */ -> new PopLocal(opcode - 0x47);
			case 0x4b, 0x4c, 0x4d, 0x4e /* astore_<n> */ -> new PopLocal(opcode - 0x4b);
			case 0x4f /* iastore */ -> result.append("IASTORE");
			case 0x50 /* lastore */ -> result.append("LASTORE");
			case 0x51 /* fastore */ -> result.append("FASTORE");
			case 0x52 /* dastore */ -> result.append("DASTORE");
			case 0x53 /* aastore */ -> result.append("AASTORE");
			case 0x54 /* bastore */ -> result.append("BASTORE");
			case 0x55 /* castore */ -> result.append("CASTORE");
			case 0x56 /* sastore */ -> result.append("SASTORE");
			case 0x57 /* pop */ -> new DirectTranslation(0x1A);
			case 0x58 /* pop2 */ -> new DirectTranslation(0x1A); // TODO: maybe drops two values (depends on type)
			case 0x59 /* dup */ -> new Dup(1, -1);
			case 0x5a /* dup_x1 */ -> new Dup(1, 1);
			case 0x5b /* dup_x2 */ -> new Dup(1, 2);
			case 0x5c /* dup2 */ -> new Dup(2, -2);
			case 0x5d /* dup2_x1 */ -> new Dup(2, 1);
			case 0x5e /* dup2_x2 */ -> new Dup(2, 2);
			case 0x5f /* swap */ -> new Swap();
			case 0x60 /* iadd */ -> new DirectTranslation(0x6A);
			case 0x61 /* ladd */ -> new DirectTranslation(0x7C);
			case 0x62 /* fadd */ -> new DirectTranslation(0x92);
			case 0x63 /* dadd */ -> new DirectTranslation(0xA0);
			case 0x64 /* isub */ -> new DirectTranslation(0x6B);
			case 0x65 /* lsub */ -> new DirectTranslation(0x7D);
			case 0x66 /* fsub */ -> new DirectTranslation(0x93);
			case 0x67 /* dsub */ -> new DirectTranslation(0xA1);
			case 0x68 /* imul */ -> new DirectTranslation(0x6C);
			case 0x69 /* lmul */ -> new DirectTranslation(0x7E);
			case 0x6a /* fmul */ -> new DirectTranslation(0x94);
			case 0x6b /* dmul */ -> new DirectTranslation(0xA2);
			case 0x6c /* idiv */ -> new DirectTranslation(0x6D);
			case 0x6d /* ldiv */ -> new DirectTranslation(0x7F);
			case 0x6e /* fdiv */ -> new DirectTranslation(0x95);
			case 0x6f /* ddiv */ -> new DirectTranslation(0xA3);
			case 0x70 /* irem */ -> new DirectTranslation(0x6F);
			case 0x71 /* lrem */ -> new DirectTranslation(0x81);
			case 0x72 /* frem */ -> new FloatRem(ValueType.F32);
			case 0x73 /* drem */ -> new FloatRem(ValueType.F64);
			case 0x74 /* ineg */ -> new IntegerNeg(ValueType.I32);
			case 0x75 /* lneg */ -> new IntegerNeg(ValueType.I64);
			case 0x76 /* fneg */ -> new DirectTranslation(0x8C);
			case 0x77 /* dneg */ -> new DirectTranslation(0x9A);
			case 0x78 /* ishl */ -> new DirectTranslation(0x74);
			case 0x79 /* lshl */ -> new DirectTranslation(0x86);
			case 0x7a /* ishr */ -> new DirectTranslation(0x75);
			case 0x7b /* lshr */ -> new DirectTranslation(0x87);
			case 0x7c /* iushr */ -> new DirectTranslation(0x76);
			case 0x7d /* lushr */ -> new DirectTranslation(0x88);
			case 0x7e /* iand */ -> new DirectTranslation(0x71);
			case 0x7f /* land */ -> new DirectTranslation(0x83);
			case 0x80 /* ior */ -> new DirectTranslation(0x72);
			case 0x81 /* lor */ -> new DirectTranslation(0x84);
			case 0x82 /* ixor */ -> new DirectTranslation(0x73);
			case 0x83 /* lxor */ -> new DirectTranslation(0x85);
			case 0x84 /* iinc */ -> new IncrementLocal(input.readUnsignedByte(), input.readByte());
			case 0x85 /* i2l */ -> new DirectTranslation(0xAC);
			case 0x86 /* i2f */ -> new DirectTranslation(0xB2);
			case 0x87 /* i2d */ -> new DirectTranslation(0xB7);
			case 0x88 /* l2i */ -> new DirectTranslation(0xA7);
			case 0x89 /* l2f */ -> new DirectTranslation(0xB4);
			case 0x8a /* l2d */ -> new DirectTranslation(0xB9);
			case 0x8b /* f2i */ -> new DirectTranslation(0xA8);
			case 0x8c /* f2l */ -> new DirectTranslation(0xAE);
			case 0x8d /* f2d */ -> new DirectTranslation(0xBB);
			case 0x8e /* d2i */ -> new DirectTranslation(0xAA);
			case 0x8f /* d2l */ -> new DirectTranslation(0xB0);
			case 0x90 /* d2f */ -> new DirectTranslation(0xB6);
			case 0x91 /* i2b */ -> new TrimInt(1, false);
			case 0x92 /* i2c */ -> new TrimInt(2, true);
			case 0x93 /* i2s */ -> new TrimInt(2, false);
			case 0x94 /* lcmp */ -> new Compare(ValueType.I64, false); // TODO: Java uses cmp then if_<cond>,
			case 0x95 /* fcmpl */ -> new Compare(ValueType.F32, false); // whereas WASM uses cmp_<cond> then if...
			case 0x96 /* fcmpg */ -> new Compare(ValueType.F32, true); // We need to translate
			case 0x97 /* dcmpl */ -> new Compare(ValueType.F64, false);
			case 0x98 /* dcmpg */ -> new Compare(ValueType.F64, true);
			case 0x99 /* ifeq */ -> result.append("IFEQ ").append(instructionPointer + input.readShort());
			case 0x9a /* ifne */ -> result.append("IFNE ").append(instructionPointer + input.readShort());
			case 0x9b /* iflt */ -> result.append("IFLT ").append(instructionPointer + input.readShort());
			case 0x9c /* ifge */ -> result.append("IFGE ").append(instructionPointer + input.readShort());
			case 0x9d /* ifgt */ -> result.append("IFGT ").append(instructionPointer + input.readShort());
			case 0x9e /* ifle */ -> result.append("IFLE ").append(instructionPointer + input.readShort());
			case 0x9f /* if_icmpeq */ -> result.append("IF_ICMPEQ ").append(instructionPointer + input.readShort());
			case 0xa0 /* if_icmpne */ -> result.append("IF_ICMPNE ").append(instructionPointer + input.readShort());
			case 0xa1 /* if_icmplt */ -> result.append("IF_ICMPLT ").append(instructionPointer + input.readShort());
			case 0xa2 /* if_icmpge */ -> result.append("IF_ICMPGE ").append(instructionPointer + input.readShort());
			case 0xa3 /* if_icmpgt */ -> result.append("IF_ICMPGT ").append(instructionPointer + input.readShort());
			case 0xa4 /* if_icmple */ -> result.append("IF_ICMPLE ").append(instructionPointer + input.readShort());
			case 0xa5 /* if_acmpeq */ -> result.append("IF_ACMPEQ ").append(instructionPointer + input.readShort());
			case 0xa6 /* if_acmpne */ -> result.append("IF_ACMPNE ").append(instructionPointer + input.readShort());
			case 0xa7 /* goto */ -> result.append("GOTO ").append(instructionPointer + input.readShort());
			case 0xa8 /* jsr */ -> result.append("JSR ").append(instructionPointer + input.readShort());
			case 0xa9 /* ret */ -> result.append("RET ").append(input.readUnsignedByte());
			case 0xaa /* tableswitch */ -> {
				result.append("TABLESWITCH {");
				// Magic ;) dont question it
				input.skipNBytes((-iter_base.getPos()) & 0b11);
				result.append("\n\tdefault: ");
				result.append(input.readInt());
				int low = input.readInt();
				int high = input.readInt();
				int numPairs = high - low + 1;
				while (numPairs-- > 0) {
					if (numPairs == 0 || numPairs == high - low) {
						result.append(",\n\tcase 0x");
						String hexString = Integer.toUnsignedString(high - numPairs, 16);
						while (hexString.length() < 2 * Integer.BYTES) hexString = '0' + hexString;
						result.append(hexString);
						result.append(": ");
					} else result.append(",\n\t                 ");
					result.append(instructionPointer + input.readInt());
				}
				result.append("\n}");
			}
			case 0xab /* lookupswitch */ -> {
				result.append("LOOKUPSWITCH {");
				// Magic ;) dont question it
				input.skipNBytes((-iter_base.getPos()) & 0b11);
				result.append("\n\tdefault: ");
				result.append(input.readInt());
				int numPairs = input.readInt();
				while (numPairs-- > 0) {
					result.append(",\n\tcase 0x");
					String hexString = Integer.toUnsignedString(input.readInt(), 16);
					while (hexString.length() < 2 * Integer.BYTES) hexString = '0' + hexString;
					result.append(hexString);
					result.append(": ");
					result.append(input.readInt());
				}
				result.append("\n}");
			}
			case 0xac /* ireturn */ -> result.append("IRETURN");
			case 0xad /* lreturn */ -> result.append("LRETURN");
			case 0xae /* freturn */ -> result.append("FRETURN");
			case 0xaf /* dreturn */ -> result.append("DRETURN");
			case 0xb0 /* areturn */ -> result.append("ARETURN");
			case 0xb1 /* return */ -> new DirectTranslation(0x0F);
			case 0xb2 /* getstatic */ -> result.append("GETSTATIC ").append(pool.get(input.readUnsignedShort()));
			case 0xb3 /* putstatic */ -> result.append("PUT_STATIC ").append(pool.get(input.readUnsignedShort()));
			case 0xb4 /* getfield */ -> result.append("GETFIELD ").append(pool.get(input.readUnsignedShort()));
			case 0xb5 /* putfield */ -> result.append("PUT_FIELD ").append(pool.get(input.readUnsignedShort()));
			case 0xb6 /* invokevirtual */ -> result.append("INVOKE_VIRTUAL ").append(pool.get(input.readUnsignedShort()));
			case 0xb7 /* invokespecial */ -> result.append("INVOKE_SPECIAL ").append(pool.get(input.readUnsignedShort()));
			case 0xb8 /* invokestatic */ -> result.append("INVOKE_STATIC ").append(pool.get(input.readUnsignedShort()));
			case 0xb9 /* invokeinterface */ -> {
				result.append("INVOKE_INTERFACE ").append(pool.get(input.readUnsignedShort()))
						.append(" ").append(input.readUnsignedByte());
				assert input.readByte() == 0;
			}
			case 0xba /* invokedynamic */ -> {
				result.append("INVOKE_DYNAMIC ").append(pool.get(input.readUnsignedShort()));
				assert input.readShort() == 0;
			}
			case 0xbb /* new */ -> result.append("NEW ").append(pool.get(input.readUnsignedShort()));
			case 0xbc /* newarray */ -> result.append("NEW_ARRAY ").append(ARRAY_TYPES[input.readByte() - 4]);
			case 0xbd /* anewarray */ -> result.append("ANEWARRAY ").append(pool.get(input.readUnsignedShort()));
			case 0xbe /* arraylength */ -> result.append("ARRAYLENGTH");
			case 0xbf /* athrow*/ -> result.append("ATHROW");
			case 0xc0 /* checkcast */ -> result.append("CHECKCAST ").append(pool.get(input.readUnsignedShort()));
			case 0xc1 /* instanceof */ -> result.append("INSTANCEOF ").append(pool.get(input.readUnsignedShort()));
			case 0xc2 /* monitorenter */ -> result.append("MONITORENTER");
			case 0xc3 /* monitorexit */ -> result.append("MONITOREXIT");
			case 0xc5 /* multianewarray */ -> result.append("MULTIANEWARRAY ").append(pool.get(input.readUnsignedShort()))
					.append(" ").append(input.readUnsignedByte());
			case 0xc6 /* ifnull */ -> result.append("IFNULL ").append(instructionPointer + input.readShort());
			case 0xc7 /* ifnonnull */ -> result.append("IFNONNULL ").append(instructionPointer + input.readShort());
			case 0xc8 /* goto_w */ -> result.append("GOTO_W ").append(instructionPointer + input.readInt());
			case 0xc9 /* jsr_w */ -> result.append("JSR_W ").append(instructionPointer + input.readInt());
			case 0xc4 /* wide */ -> {
				result.append("WIDE ");
				switch (opcode = input.readUnsignedByte()) {
					case 0x15 /* iload */ -> new PushLocal(input.readUnsignedShort());
					case 0x16 /* lload */ -> new PushLocal(input.readUnsignedShort());
					case 0x17 /* fload */ -> new PushLocal(input.readUnsignedShort());
					case 0x18 /* dload */ -> new PushLocal(input.readUnsignedShort());
					case 0x19 /* aload */ -> new PushLocal(input.readUnsignedShort());
					case 0x36 /* istore */ -> new PopLocal(input.readUnsignedShort());
					case 0x37 /* lstore */ -> new PopLocal(input.readUnsignedShort());
					case 0x38 /* fstore */ -> new PopLocal(input.readUnsignedShort());
					case 0x39 /* dstore */ -> new PopLocal(input.readUnsignedShort());
					case 0x3a /* astore */ -> new PopLocal(input.readUnsignedShort());
					case 0x84 /* iinc */ -> new IncrementLocal(input.readUnsignedShort(), input.readShort());
					case 0xa9 /* ret */ -> result.append("RET ").append(input.readUnsignedShort());
					default -> result.append("UNKNOWN");
				}
			}
			default -> result.append("UNKNOWN");
		};
	}
	record DirectTranslation(int opcode) implements Operation {
		@Override
		public void writeWasm(DataOutputStream out, IntermediaryMethod context) throws IOException {
			out.writeByte(opcode);
		}
	}
	record PushConst(ValueType type, Object value) implements Operation {
		@Override
		public void writeWasm(DataOutputStream out, IntermediaryMethod context) throws IOException {
			switch (type) {
				case I32 -> { out.writeByte(0x41); Util.writeVarInt(out, (int) value); }
				case I64 -> { out.writeByte(0x42); Util.writeVarInt(out, (long) value); }
				case F32 -> { out.writeByte(0x43); Util.writeFloat(out, (float) value); }
				case F64 -> { out.writeByte(0x44); Util.writeDouble(out, (double) value); }
				case EXTERNAL_REF -> throw new RuntimeException("Not implemented"); // TODO: how are we doing arrays??
			}
		}
	}
	record PushLocal(int index) implements Operation {
		@Override
		public void writeWasm(DataOutputStream out, IntermediaryMethod context) throws IOException {
			out.writeByte(0x20);
			Util.writeVarUInt(out, index); // TODO: remapping from java locals to wasm locals
		}
	}
	record PushArray(Type type) implements Operation {}
	record PopLocal(int index) implements Operation {
		@Override
		public void writeWasm(DataOutputStream out, IntermediaryMethod context) throws IOException {
			out.writeByte(0x21);
			Util.writeVarUInt(out, index); // TODO: remapping from java locals to wasm locals
		}
	}
	record PopArray(Type type) implements Operation {}
	record Dup(int numValues, int offset) implements Operation {}
	record Swap() implements Operation {}
	record FloatRem(ValueType floatType) implements Operation {
		public FloatRem {
			if (!ValueType.isFloatingPointType(floatType))
				throw new RuntimeException("Invalid float type");
		}
	}
	record IntegerNeg(ValueType intType) implements Operation {
		public IntegerNeg {
			if (!ValueType.isIntegralType(intType))
				throw new RuntimeException("Invalid integer type");
		}

		@Override
		public void writeWasm(DataOutputStream out, IntermediaryMethod context) throws IOException {
			// TODO: better way than multiply??
			out.writeByte(intType == ValueType.I32 ? 0x41 :
			           /* intType == ValueType.I64 ? */ 0x42); // Load const int
			Util.writeVarInt(out, -1);
			out.writeByte(intType == ValueType.I32 ? 0x6C :
					/* intType == ValueType.I64 ? */ 0x7E); // Multiply
		}
	}
	record IncrementLocal(int index, short shift) implements Operation {
		@Override
		public void writeWasm(DataOutputStream out, IntermediaryMethod context) throws IOException {
			out.writeByte(0x20); // local.get
			Util.writeVarUInt(out, index); // TODO: translate locals
			out.writeByte(0x41); // i32.const
			Util.writeVarInt(out, shift);
			out.writeByte(0x6A); // i32.add
			out.writeByte(0x21); // local.set
			Util.writeVarUInt(out, index); // TODO: translate locals
		}
	}
	record TrimInt(int size, boolean unsigned) implements Operation {
		@Override
		public void writeWasm(DataOutputStream out, IntermediaryMethod context) throws IOException {
			out.writeByte(0x41); // i32.const
			int mask = 0;
			for (int i = 0; i < size; i++) {
				mask <<= 8;
				mask |= 0xFF;
			}
			Util.writeVarInt(out, mask);
			out.writeByte(0x71); // i32.and
			if (!unsigned) out.writeByte(size == 1 ? 0xC0 : 0xC1); // i32.extend<size>_s
		}
	}
	record Compare(ValueType types, boolean nanResultGreater) implements Operation {
		public Compare {
			if (!ValueType.isNumericType(types))
				throw new RuntimeException("Cannot compare types");
		}
	}
}
