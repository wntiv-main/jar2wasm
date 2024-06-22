package wntiv.class_parser;

import jdk.jfr.Unsigned;
import wntiv.Pair;
import wntiv.wasm_output.Util;
import wntiv.wasm_output.WasmModule;
import wntiv.wasm_output.types.ValueType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.function.IntSupplier;
import java.util.stream.IntStream;

public interface Operation {
	String[] ARRAY_TYPES = new String[]{"BOOLEAN", "CHAR", "FLOAT", "DOUBLE", "BYTE", "SHORT", "INT", "LONG"};
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
	default void writeWasm(int index, IntermediaryMethod context, DataOutputStream out) throws IOException {}
	static Operation readFromStream(IntermediaryMethod method, DataInputStream input, IntSupplier methodIndex, ClassHandler.ConstantPool pool) throws IOException {
		int opcode = input.readUnsignedByte();
//		StringBuilder result = new StringBuilder(); // TODO: TEMP
//		int instructionPointer = 0;
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
					yield new PushConst(ValueType.I32, intValue.value);
				else if (constValue instanceof ClassHandler.ConstantFloatInfo floatValue)
					yield new PushConst(ValueType.F32, floatValue.value);
				else if (constValue instanceof ClassHandler.ConstantStringInfo stringValue)
					yield new PushConst(ValueType.EXTERNAL_REF, stringValue.value); // TODO: Make value in data section??
				else if (constValue instanceof ClassHandler.ConstantClassInfo object)
					yield new PushConst(ValueType.EXTERNAL_REF, object);
				else throw new RuntimeException("Unexpected const value");
			}
			case 0x14 /* ldc2_w */ -> {
				ClassHandler.ConstantPoolItem constValue = pool.get(input.readUnsignedShort());
				if (constValue instanceof ClassHandler.ConstantLongInfo longValue)
					yield new PushConst(ValueType.I64, longValue.value);
				else if (constValue instanceof ClassHandler.ConstantDoubleInfo doubleValue)
					yield new PushConst(ValueType.F64, doubleValue.value);
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
			case 0x4f /* iastore */ -> new PopArray(Type.INT);
			case 0x50 /* lastore */ -> new PopArray(Type.LONG);
			case 0x51 /* fastore */ -> new PopArray(Type.FLOAT);
			case 0x52 /* dastore */ -> new PopArray(Type.DOUBLE);
			case 0x53 /* aastore */ -> new PopArray(Type.ARRAY);
			case 0x54 /* bastore */ -> new PopArray(Type.BYTE);
			case 0x55 /* castore */ -> new PopArray(Type.CHAR);
			case 0x56 /* sastore */ -> new PopArray(Type.SHORT);
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
			case 0x99 /* ifeq */ -> new ComparisonConditional(0x46, input.readShort());
			case 0x9a /* ifne */ -> new ComparisonConditional(0x47, input.readShort());
			case 0x9b /* iflt */ -> new ComparisonConditional(0x48, input.readShort());
			case 0x9c /* ifge */ -> new ComparisonConditional(0x4E, input.readShort());
			case 0x9d /* ifgt */ -> new ComparisonConditional(0x4A, input.readShort());
			case 0x9e /* ifle */ -> new ComparisonConditional(0x4C, input.readShort());
			case 0x9f /* if_icmpeq */ -> new FullComparisonConditional(0x46, input.readShort());
			case 0xa0 /* if_icmpne */ -> new FullComparisonConditional(0x47, input.readShort());
			case 0xa1 /* if_icmplt */ -> new FullComparisonConditional(0x48, input.readShort());
			case 0xa2 /* if_icmpge */ -> new FullComparisonConditional(0x4E, input.readShort());
			case 0xa3 /* if_icmpgt */ -> new FullComparisonConditional(0x4A, input.readShort());
			case 0xa4 /* if_icmple */ -> new FullComparisonConditional(0x4C, input.readShort());
			case 0xa5 /* if_acmpeq */ -> new FullComparisonConditional(0xD3, input.readShort()); // GC ext, more research needed
			case 0xa6 /* if_acmpne */ -> throw new RuntimeException("IF_ACMPNE " + input.readShort());
			case 0xa7 /* goto */ -> new GoTo(input.readShort());
			case 0xa8 /* jsr */ -> throw new RuntimeException("JSR " + input.readShort());
			case 0xa9 /* ret */ -> throw new RuntimeException("RET " + input.readUnsignedByte());
			case 0xaa /* tableswitch */ -> {
				// Magic ;) dont question it
				input.skipNBytes((-methodIndex.getAsInt()) & 0b11);
				int defaultValue = input.readInt();
				int low = input.readInt();
				int high = input.readInt();
				int numPairs = high - low + 1;
				List<Integer> mappings = new ArrayList<>(numPairs);
				while (numPairs-- > 0) {
					mappings.add(high - numPairs, input.readInt());
				}
				yield new JumpTable(defaultValue, low, mappings);
			}
			case 0xab /* lookupswitch */ -> {
				// Magic ;) dont question it
				input.skipNBytes((-methodIndex.getAsInt()) & 0b11);
				int defaultValue = input.readInt();
				int numPairs = input.readInt();
				Map<Integer, Integer> mapping = new HashMap<>();
				while (numPairs-- > 0) {
					mapping.put(input.readInt(), input.readInt());
				}
				yield new LookupTable(mapping, defaultValue);
			}
			case 0xac /* ireturn */ -> throw new RuntimeException("IRETURN");
			case 0xad /* lreturn */ -> throw new RuntimeException("LRETURN");
			case 0xae /* freturn */ -> throw new RuntimeException("FRETURN");
			case 0xaf /* dreturn */ -> throw new RuntimeException("DRETURN");
			case 0xb0 /* areturn */ -> throw new RuntimeException("ARETURN");
			case 0xb1 /* return */ -> new DirectTranslation(0x0F);
			case 0xb2 /* getstatic */ -> {
				if (!(pool.get(input.readUnsignedShort()) instanceof ClassHandler.ConstantFieldRefInfo field))
					throw new RuntimeException("Not a field");
				yield new GetStatic(field);
			}
			case 0xb3 /* putstatic */ -> {
				if (!(pool.get(input.readUnsignedShort()) instanceof ClassHandler.ConstantFieldRefInfo field))
					throw new RuntimeException("Not a field");
				yield new PutStatic(field);
			}
			case 0xb4 /* getfield */ -> throw new RuntimeException("GETFIELD " + pool.get(input.readUnsignedShort()));
			case 0xb5 /* putfield */ -> throw new RuntimeException("PUT_FIELD " + pool.get(input.readUnsignedShort()));
			case 0xb6 /* invokevirtual */ -> throw new RuntimeException("INVOKE_VIRTUAL " + pool.get(input.readUnsignedShort()));
			case 0xb7 /* invokespecial */ -> throw new RuntimeException("INVOKE_SPECIAL " + pool.get(input.readUnsignedShort()));
			case 0xb8 /* invokestatic */ -> {
				if (!(pool.get(input.readUnsignedShort()) instanceof ClassHandler.ConstantMethodRefInfo func))
					throw new RuntimeException("Not a method");
				yield new InvokeMethod(func);
			}
			case 0xb9 /* invokeinterface */ -> {
				result.append("INVOKE_INTERFACE ").append(pool.get(input.readUnsignedShort()))
						.append(" ").append(input.readUnsignedByte());
				assert input.readByte() == 0;
				yield null;
			}
			case 0xba /* invokedynamic */ -> {
				throw new RuntimeException("INVOKE_DYNAMIC " + pool.get(input.readUnsignedShort()));
				assert input.readShort() == 0;
				yield null;
			}
			case 0xbb /* new */ -> throw new RuntimeException("NEW " + pool.get(input.readUnsignedShort()));
			case 0xbc /* newarray */ -> throw new RuntimeException("NEW_ARRAY " + ARRAY_TYPES[input.readByte() - 4]);
			case 0xbd /* anewarray */ -> throw new RuntimeException("ANEWARRAY " + pool.get(input.readUnsignedShort()));
			case 0xbe /* arraylength */ -> throw new RuntimeException("ARRAYLENGTH");
			case 0xbf /* athrow*/ -> throw new RuntimeException("ATHROW");
			case 0xc0 /* checkcast */ -> throw new RuntimeException("CHECKCAST " + pool.get(input.readUnsignedShort()));
			case 0xc1 /* instanceof */ -> throw new RuntimeException("INSTANCEOF " + pool.get(input.readUnsignedShort()));
			case 0xc2 /* monitorenter */ -> throw new RuntimeException("MONITORENTER");
			case 0xc3 /* monitorexit */ -> throw new RuntimeException("MONITOREXIT");
			case 0xc5 /* multianewarray */ -> result.append("MULTIANEWARRAY ").append(pool.get(input.readUnsignedShort()))
					.append(" ").append(input.readUnsignedByte());
			case 0xc6 /* ifnull */ -> new NullCheck(false, input.readShort());
			case 0xc7 /* ifnonnull */ -> new NullCheck(true, input.readShort());
			case 0xc8 /* goto_w */ -> throw new RuntimeException("GOTO_W " + input.readInt());
			case 0xc9 /* jsr_w */ -> throw new RuntimeException("JSR_W " + input.readInt());
			case 0xc4 /* wide */ -> switch (opcode = input.readUnsignedByte()) {
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
				case 0xa9 /* ret */ -> throw new RuntimeException("RET " + input.readUnsignedShort());
				default -> throw new RuntimeException("UNKNOWN");
			};
			default -> throw new RuntimeException("UNKNOWN");
		};
	}
	record DirectTranslation(int opcode) implements Operation {
		@Override
		public void writeWasm(int index, IntermediaryMethod context, DataOutputStream out) throws IOException {
			out.writeByte(opcode);
		}
	}
	record PushConst(ValueType type, Object value) implements Operation {
		@Override
		public void writeWasm(int index, IntermediaryMethod context, DataOutputStream out) throws IOException {
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
		public void writeWasm(int index, IntermediaryMethod context, DataOutputStream out) throws IOException {
			out.writeByte(0x20);
			Util.writeVarUInt(out, this.index); // TODO: remapping from java locals to wasm locals
		}
	}
	record PushArray(Type type) implements Operation {}
	record PopLocal(int index) implements Operation {
		@Override
		public void writeWasm(int index, IntermediaryMethod context, DataOutputStream out) throws IOException {
			out.writeByte(0x21);
			Util.writeVarUInt(out, this.index); // TODO: remapping from java locals to wasm locals
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
		public void writeWasm(int index, IntermediaryMethod context, DataOutputStream out) throws IOException {
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
		public void writeWasm(int index, IntermediaryMethod context, DataOutputStream out) throws IOException {
			out.writeByte(0x20); // local.get
			Util.writeVarUInt(out, this.index); // TODO: translate locals
			out.writeByte(0x41); // i32.const
			Util.writeVarInt(out, shift);
			out.writeByte(0x6A); // i32.add
			out.writeByte(0x21); // local.set
			Util.writeVarUInt(out, this.index); // TODO: translate locals
		}
	}
	record TrimInt(int size, boolean unsigned) implements Operation {
		@Override
		public void writeWasm(int index, IntermediaryMethod context, DataOutputStream out) throws IOException {
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
	record InvokeMethod(ClassHandler.ConstantMethodRefInfo method) implements Operation {
		@Override
		public void writeWasm(int index, IntermediaryMethod context, DataOutputStream out) throws IOException {
			out.writeByte(0x10); // call
			Util.writeVarUInt(out, context.methodBindings.getFunctionIndex(method));
		}
	}
	record PutStatic(ClassHandler.ConstantFieldRefInfo field) implements Operation {
		public PutStatic {

		}
		@Override
		public void writeWasm(int index, IntermediaryMethod context, DataOutputStream out) throws IOException {
			out.writeByte(0x24); // global.set
			Util.writeVarUInt(out, context.methodBindings.getGlobalIndex(field));
		}
	}
	record GetStatic(ClassHandler.ConstantFieldRefInfo field) implements Operation {
		@Override
		public void writeWasm(int index, IntermediaryMethod context, DataOutputStream out) throws IOException {
			out.writeByte(0x23); // global.set
			Util.writeVarUInt(out, context.getGlobalIndex(field));
		}
	}
	interface Conditional extends Operation {
		int jumpTarget();
		@Override
		default void writeWasm(int index, IntermediaryMethod context, DataOutputStream out) throws IOException {
			if (jumpTarget() < 0) {
				out.writeByte(0x03); // loop
			} else out.writeByte(0x04); // if
			context.getOpsInRange(index + jumpTarget(), index).forEachOrdered(pair -> {
				try {
					pair.second().writeWasm(pair.first(), context, out);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
			out.writeByte(0x05); // else
			// TODO: Handle re-join of branches
		}
	}
	record NullCheck(boolean invert, int jumpTarget) implements Conditional {
		@Override
		public void writeWasm(int index, IntermediaryMethod context, DataOutputStream out) throws IOException {
			out.writeByte(0xD1); // ref.is_null
			if (invert) {
				out.writeByte(0x41); // i32.const
				Util.writeVarInt(out, 1);
				out.writeByte(0x73); // i32.xor
			}
			Conditional.super.writeWasm(index, context, out);
		}
	}
	record ComparisonConditional(int compareCode, int jumpTarget) implements Conditional {
		@Override
		public void writeWasm(int index, IntermediaryMethod context, DataOutputStream out) throws IOException {
			out.writeByte(0x41); // i32.const
			Util.writeVarUInt(out, 0);
			out.writeByte(compareCode);
			Conditional.super.writeWasm(index, context, out);
		}
	}
	record FullComparisonConditional(int compareCode, int jumpTarget) implements Conditional {
		@Override
		public void writeWasm(int index, IntermediaryMethod context, DataOutputStream out) throws IOException {
			out.writeByte(compareCode);
			Conditional.super.writeWasm(index, context, out);
		}
	}
	record GoTo(int jumpTarget) implements Operation {}
	record JumpTable(int defaultIndex, int firstMatch, List<Integer> jumpIndices) implements Operation {
		@Override
		public void writeWasm(int index, IntermediaryMethod context, DataOutputStream out) throws IOException {
			// valueIndex -> [jumpIndex]
			var indicesByValue = IntStream.range(0, jumpIndices.size() + 1)
							.mapToObj(i -> new Pair<>(i, i >= jumpIndices.size() ? defaultIndex : jumpIndices.get(i)))
							.sorted(Comparator.comparingInt(Pair::second)).toList();
			// placementIndex[jumpIndex]
			var valueOrder = IntStream.range(0, jumpIndices.size() + 1)
							.mapToObj(i -> new Pair<>(i, indicesByValue.get(i).first()))
							.sorted(Comparator.comparingInt(Pair::second))
							.map(Pair::first).toList();
			out.writeByte(0x41); // i32.const
			Util.writeVarInt(out, firstMatch);
			out.writeByte(0x6B); // i32.sub (now firstIndex is at 0)
			// block entries
			for (int i = 0; i <= jumpIndices.size(); i++) {
				out.writeByte(0x02); // block
				out.writeByte(0x40); // no result (TODO: should this be this way?)
			}
			out.writeByte(0x0E); // br_table
			Util.writeVarUInt(out, jumpIndices.size());
			valueOrder.forEach(idx -> { // write block ordering
				try {
					Util.writeVarUInt(out, idx);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
			// block closures
			for (int i = 0; i < indicesByValue.size(); i++) {
				context.getBlockAt(index + indicesByValue.get(i).second()).forEachOrdered(pair -> {
					try {
						pair.second().writeWasm(pair.first(), context, out);
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				});
				try {
					if (indicesByValue.size() - i > 1) {
						out.writeByte(0x0C); // br: break out of switch (TODO: continuations)
						Util.writeVarInt(out, indicesByValue.size() - i);
					}
					out.writeByte(0x0B); // end
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}
	public static class LookupTable implements Operation {
		public final Map<Integer, Integer> mappings;
		public final int defaultValue;
		private final @Unsigned int tableIndex;
		private final int start;
		private final int size;

		LookupTable(Map<Integer, Integer> mappings, int defaultValue, WasmModule module) {
			this.mappings = mappings;
			this.defaultValue = defaultValue;
			int min = -Integer.MAX_VALUE, max = Integer.MIN_VALUE;
			for (int index : mappings.keySet()) {
				min = Integer.min(min, index);
				max = Integer.max(max, index);
			}
			start = min;
			size = max - min;
			tableIndex = module.createTable(size, ValueType.I32);
			int[] lookups = new int[size];
			for (int i = 0; i < size; i++) {
				lookups[i] =  mappings.getOrDefault(i, defaultValue);
			}
			module.initTable(tableIndex, lookups);
		}

		// Implementation: we probably want to vary depending on what distribution of values we see
		// We definately cant represent all 4billion possible values
		// Fortunately java is under same limitation - their tables cannot be too large due to the
		// table being inlined in the bytecode. Unfortunately their implementation can allow for widely
		// distributed values, which we cannot represent with a table.
		@Override
		public void writeWasm(int index, IntermediaryMethod context, DataOutputStream out) throws IOException {
			// Naive implementation:
			out.writeByte(0x41); // i32.const
			Util.writeVarInt(out, start);
			out.writeByte(0x6B); // i32.sub
			out.writeByte(0x25); // table.get
			Util.writeVarUInt(out, tableIndex);
			// TODO: bounds checks
		}
	}
}
