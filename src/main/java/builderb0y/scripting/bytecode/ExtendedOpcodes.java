package builderb0y.scripting.bytecode;

import org.objectweb.asm.Opcodes;

import builderb0y.scripting.bytecode.tree.instructions.binary.PowerInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.binary.SignedLeftShiftInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.binary.SignedRightShiftInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.binary.UnsignedLeftShiftInsnTree;

public interface ExtendedOpcodes extends Opcodes {

	public static final int
		/** int raised to the power of int; used by {@link PowerInsnTree}. */
		IIPOW = 256,
		/** long raised to the power of int; used by {@link PowerInsnTree}. */
		LIPOW = 257,
		/** float raised to the power of int; used by {@link PowerInsnTree}. */
		FIPOW = 258,
		/** double raised to the power of int; used by {@link PowerInsnTree}. */
		DIPOW = 259,
		/** float raised to the power of float; used by {@link PowerInsnTree}. */
		FFPOW = 260,
		/** double raised to the power of double; used by {@link PowerInsnTree}. */
		DDPOW = 261,
		/** int shifted left in an unsigned fashion; used by {@link UnsignedLeftShiftInsnTree}. */
		IUSHL = 262,
		/** long shifted left in n unsigned fashion; used by {@link UnsignedLeftShiftInsnTree}. */
		LUSHL = 263,
		/** float shifted left in a signed fashion; used by {@link SignedLeftShiftInsnTree}. */
		FSHL  = 264,
		/** double shifted left in a signed fashion; used by {@link SignedLeftShiftInsnTree}. */
		DSHL  = 265,
		/** float shifted left in a signed fashion; used by {@link SignedRightShiftInsnTree}. */
		FSHR  = 266,
		/** double shifted left in a signed fashion; used by {@link SignedRightShiftInsnTree}. */
		DSHR  = 267;

	public static final int
		/**
		indicates that a method has no side effects and that its
		return value is determined solely by its parameter values.
		constant folding may eagerly evaluate such methods.
		*/
		ACC_PURE = MethodInfo.PURE;
}