package builderb0y.scripting.bytecode.tree.instructions.elvis;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.invokers.BaseInvokeInsnTree;
import builderb0y.scripting.util.TypeMerger;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ElvisGetInsnTree implements InsnTree {

	public ElvisEmitters emitters;

	public ElvisGetInsnTree(ElvisEmitters emitters) {
		this.emitters = emitters;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		Label alternative = label(), get = label(), end = label();

		this.emitters.object.emitBytecode(method); //object
		method.node.visitInsn(DUP); //object object
		method.node.visitJumpInsn(IFNONNULL, get); //object
		this.emitters.popNull.emitBytecode(method); //
		method.node.visitJumpInsn(GOTO, alternative); //

		method.node.visitLabel(get); //object
		this.emitters.getter.emitBytecode(method); //value
		ElvisInsnTree.dupAndJumpIfNonNull(this.emitters.getterType, end, method); //value
		method.node.visitInsn(this.emitters.getterType.isDoubleWidth() ? POP2 : POP); //

		method.node.visitLabel(alternative);
		this.emitters.alternative.emitBytecode(method); //value

		method.node.visitLabel(end); //value
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.emitters.commonType;
	}

	public static record ElvisEmitters(
		BytecodeEmitter object,
		BytecodeEmitter popNull,
		BytecodeEmitter getter,
		BytecodeEmitter alternative,
		TypeInfo getterType,
		TypeInfo commonType
	) {

		public static final BytecodeEmitter
			POP  = (MethodCompileContext method) -> method.node.visitInsn(Opcodes.POP);

		public static ElvisEmitters forField(
			InsnTree object,
			FieldInfo field,
			InsnTree alternative
		) {
			TypeInfo commonType = (
				alternative.jumpsUnconditionally()
				? field.type
				: TypeMerger.computeMostSpecificType(
					field.type,
					alternative.getTypeInfo()
				)
			);
			return new ElvisEmitters(
				object,
				POP,
				field::emitGet,
				alternative,
				field.type,
				commonType
			);
		}

		public static ElvisEmitters forGetter(
			InsnTree object,
			MethodInfo getter,
			InsnTree alternative
		) {
			TypeInfo commonType = (
				alternative.jumpsUnconditionally()
				? getter.returnType
				: TypeMerger.computeMostSpecificType(
					getter.returnType,
					alternative.getTypeInfo()
				)
			);
			return new ElvisEmitters(
				object,
				POP,
				getter,
				alternative,
				getter.returnType,
				commonType
			);
		}

		public static ElvisEmitters forMethod(
			BaseInvokeInsnTree method,
			InsnTree alternative
		) {
			TypeInfo commonType = (
				alternative.jumpsUnconditionally()
				? method.getTypeInfo()
				: TypeMerger.computeMostSpecificType(
					method.getTypeInfo(),
					alternative.getTypeInfo()
				)
			);
			return new ElvisEmitters(
				method::emitFirstArg,
				POP,
				(MethodCompileContext context) -> {
					method.emitAllArgsExceptFirst(context);
					method.emitMethod(context);
				},
				alternative,
				method.getTypeInfo(),
				commonType
			);
		}
	}
}