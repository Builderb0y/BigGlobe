package builderb0y.scripting.bytecode.tree.instructions.invokers;

import com.google.common.collect.ObjectArrays;
import org.objectweb.asm.Label;

import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.elvis.ElvisGetInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.elvis.ElvisGetInsnTree.ElvisEmitters;
import builderb0y.scripting.bytecode.tree.instructions.elvis.ElvisInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeMerger;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class AfterNullableInvokeInsnTree extends BaseInvokeInsnTree {

	public AfterNullableInvokeInsnTree(MethodInfo method, LazyVarInfo script, InsnTree receiver, InsnTree... args) {
		super(method, concat2(load(script), receiver, args));
		checkArguments(method.getInvokeTypes(), this.args);
	}

	public AfterNullableInvokeInsnTree(MethodInfo method, LazyVarInfo script, InsnTree... args) {
		super(method, ObjectArrays.concat(load(script), args));
		checkArguments(method.getInvokeTypes(), this.args);
	}

	public static InsnTree[] concat2(InsnTree first, InsnTree second, InsnTree[] rest) {
		InsnTree[] result = new InsnTree[rest.length + 2];
		result[0] = first;
		result[1] = second;
		System.arraycopy(rest, 0, result, 2, rest.length);
		return result;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		Label get = label(), end = label();
		this.emitSpecificArgs(method, 0, 2);
		ElvisInsnTree.dupAndJumpIfNonNull(this.args[1].getTypeInfo(), get, method);
		method.node.visitInsn(POP2);
		if (this.args[1].getTypeInfo().isDoubleWidth()) {
			method.node.visitInsn(POP);
		}
		constantAbsent(this.getTypeInfo()).emitBytecode(method);
		method.node.visitJumpInsn(GOTO, end);

		method.node.visitLabel(get);
		this.emitSpecificArgs(method, 2, this.args.length);
		this.emitMethod(method);

		method.node.visitLabel(end);
	}

	@Override
	public InsnTree elvis(ExpressionParser parser, InsnTree alternative) {
		TypeInfo commonType = (
			alternative.jumpsUnconditionally()
			? this.getTypeInfo()
			: TypeMerger.computeMostSpecificType(
				this.getTypeInfo(),
				alternative.getTypeInfo()
			)
		);
		return new ElvisGetInsnTree(
			new ElvisEmitters(
				(MethodCompileContext method) -> this.emitSpecificArgs(method, 0, 2),
				(MethodCompileContext method) -> {
					method.node.visitInsn(POP2);
					if (this.args[1].getTypeInfo().isDoubleWidth()) {
						method.node.visitInsn(POP);
					}
				},
				(MethodCompileContext method) -> {
					this.emitSpecificArgs(method, 2, this.args.length);
					this.emitMethod(method);
				},
				alternative,
				this.getTypeInfo(),
				commonType
			)
		);
	}
}