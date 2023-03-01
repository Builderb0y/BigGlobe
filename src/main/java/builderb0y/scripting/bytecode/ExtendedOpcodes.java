package builderb0y.scripting.bytecode;

import org.objectweb.asm.Opcodes;

public interface ExtendedOpcodes extends Opcodes {

	public static final int
		IIPOW = 256,
		LIPOW = 257,
		FIPOW = 258,
		DIPOW = 259,
		FFPOW = 260,
		DDPOW = 261,
		IUSHL = 262,
		LUSHL = 263,
		FSHL  = 264,
		DSHL  = 265,
		FSHR  = 266,
		DSHR  = 267;

	public static final int
		ACC_PURE = MethodInfo.PURE;
}