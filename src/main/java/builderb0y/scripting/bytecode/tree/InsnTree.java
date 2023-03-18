package builderb0y.scripting.bytecode.tree;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;

import builderb0y.autocodec.util.ObjectArrayFactory;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.CastingSupport.CasterData;
import builderb0y.scripting.bytecode.tree.flow.SequenceInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.unary.CastInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

/** AST-like structure which can be converted directly into bytecode. */
public interface InsnTree extends Opcodes, Typeable, BytecodeEmitter {

	public static final ObjectArrayFactory<InsnTree> ARRAY_FACTORY = new ObjectArrayFactory<>(InsnTree.class);

	/**
	provides bytecode to the provided visitor which will push
	the value represented by this InsnTree onto the stack.
	if our {@link #getTypeInfo()} is {@link TypeInfos#VOID},
	then nothing will actually be on the stack,
	but whatever side effects involved in
	computing our value will be performed.
	*/
	@Override
	public abstract void emitBytecode(MethodCompileContext method);

	/**
	returns the {@link TypeInfo} which represents the value
	which will be on the stack after this instruction is executed.
	*/
	@Override
	public abstract TypeInfo getTypeInfo();

	public default ConstantValue getConstantValue() {
		return ConstantValue.notConstant();
	}

	public default InsnTree cast(ExpressionParser parser, TypeInfo type, CastMode mode) {
		if (this.getTypeInfo().simpleEquals(type)) {
			return this;
		}
		if (this.jumpsUnconditionally()) {
			return new CastInsnTree(this, type, CasterData.ARRAY_FACTORY.empty());
		}
		if (this.getTypeInfo().isGeneric || type.isGeneric) {
			mode = mode.toExplicit();
		}
		return this.doCast(parser, type, mode);
	}

	public default InsnTree doCast(ExpressionParser parser, TypeInfo type, CastMode mode) {
		CasterData[] steps = parser.environment.castProviders.search(this.getTypeInfo(), type, mode);
		if (steps == null) return null;
		return new CastInsnTree(this, type, steps);
	}

	public static enum CastMode {
		EXPLICIT_THROW(false, false),
		EXPLICIT_NULL(false, true),
		IMPLICIT_THROW(true, false),
		IMPLICIT_NULL(true, true);

		public final boolean implicit, nullable;

		CastMode(boolean implicit, boolean nullable) {
			this.implicit = implicit;
			this.nullable = nullable;
		}

		public CastMode toExplicit() {
			return this.nullable ? EXPLICIT_NULL : EXPLICIT_THROW;
		}

		public CastMode toImplicit() {
			return this.nullable ? IMPLICIT_NULL : IMPLICIT_THROW;
		}

		public CastMode toNullable() {
			return this.implicit ? IMPLICIT_NULL : EXPLICIT_NULL;
		}

		public CastMode toThrowing() {
			return this.implicit ? IMPLICIT_THROW : EXPLICIT_THROW;
		}

		public <T> @Nullable T handleFailure(TypeInfo from, TypeInfo to) {
			if (this.nullable) return null;
			else throw new ClassCastException("Can't " + (this.implicit ? "implicitly " : "") + "cast " + from + " to " + to);
		}
	}

	public default InsnTree then(ExpressionParser parser, InsnTree nextStatement) {
		return new SequenceInsnTree(parser, this, nextStatement);
	}

	public default boolean jumpsUnconditionally() {
		return false;
	}

	public default boolean canBeStatement() {
		return false;
	}

	public default InsnTree update(ExpressionParser parser, UpdateOp op, InsnTree rightValue) throws ScriptParsingException {
		throw new ScriptParsingException("Attempt to update non-assignable value", parser.input);
	}

	public static enum UpdateOp {
		ASSIGN              (InsnTrees::seq ),
		ADD                 (InsnTrees::add ),
		SUBTRACT            (InsnTrees::sub ),
		MULTIPLY            (InsnTrees::mul ),
		DIVIDE              (InsnTrees::div ),
		MODULO              (InsnTrees::mod ),
		POWER               (InsnTrees::pow ),
		BITWISE_AND         (InsnTrees::band),
		BITWISE_OR          (InsnTrees::bor ),
		BITWISE_XOR         (InsnTrees::bxor),
		AND                 (InsnTrees::and ),
		OR                  (InsnTrees::or  ),
		XOR                 (InsnTrees::xor ),
		SIGNED_LEFT_SHIFT   (InsnTrees::shl ),
		SIGNED_RIGHT_SHIFT  (InsnTrees::shr ),
		UNSIGNED_LEFT_SHIFT (InsnTrees::ushl),
		UNSIGNED_RIGHT_SHIFT(InsnTrees::ushr);

		public final UpdateConstructor constructor;

		UpdateOp(UpdateConstructor constructor) {
			this.constructor = constructor;
		}

		public InsnTree createUpdater(ExpressionParser parser, TypeInfo leftType, InsnTree rightValue) throws ScriptParsingException {
			return this.constructor.construct(parser, getFromStack(leftType), rightValue).cast(parser, leftType, CastMode.IMPLICIT_THROW);
		}

		@FunctionalInterface
		public static interface UpdateConstructor {

			public abstract InsnTree construct(ExpressionParser parser, InsnTree left, InsnTree right);
		}
	}
}