package builderb0y.scripting.bytecode.tree.flow;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InvalidOperandException;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.bytecode.tree.conditions.ConstantConditionTree;
import builderb0y.scripting.bytecode.tree.VariableDeclarationInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class WhileInsnTree implements InsnTree {

	public final ConditionTree condition;
	public final InsnTree body;

	public WhileInsnTree(ExpressionParser parser, ConditionTree condition, InsnTree body) {
		this.condition = condition;
		if (!body.canBeStatement()) {
			throw new IllegalArgumentException("Body is not a statement");
		}
		this.body = body.cast(parser, TypeInfos.VOID, CastMode.IMPLICIT_THROW);
	}

	public static InsnTree createRepeat(ExpressionParser parser, InsnTree times, InsnTree body) {
		if (!times.getTypeInfo().isSingleWidthInt()) {
			throw new InvalidOperandException("Number of times to repeat is not an int");
		}
		VariableDeclarationInsnTree counter = new VariableDeclarationInsnTree("counter", TypeInfos.INT);
		InsnTree init, loadLimit;
		if (times.getConstantValue().isConstant()) {
			//var counter = 0
			//while (counter < times:
			//	body
			//	++counter
			//)
			init = counter.then(parser, store(counter.loader.variable, ldc(0)));
			loadLimit = times;
		}
		else {
			//var limit = times
			//var counter = 0
			//while (counter < limit:
			//	body
			//	++counter
			//)
			VariableDeclarationInsnTree limit = new VariableDeclarationInsnTree("limit", TypeInfos.INT);
			init = seq(parser, limit, store(limit.loader.variable, times), counter, store(counter.loader.variable, ldc(0)));
			//init = limit.then(store(limit.loader.variable, times)).then(counter).then(store(counter.loader.variable, ldc(0)));
			loadLimit = limit.loader;
		}
		return for_(parser, init, lt(parser, counter.loader, loadLimit), inc(counter.loader.variable, 1), body);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		Label start = new Label(), end = new Label();
		method.node.visitLabel(start);
		this.condition.emitBytecode(method, null, end);
		this.body.emitBytecode(method);
		method.node.visitJumpInsn(Opcodes.GOTO, start);
		method.node.visitLabel(end);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return TypeInfos.VOID;
	}

	@Override
	public boolean returnsUnconditionally() {
		//while (true) doesn't need a return after it.
		return this.condition instanceof ConstantConditionTree constant && constant.value;
	}

	@Override
	public boolean canBeStatement() {
		return true;
	}
}