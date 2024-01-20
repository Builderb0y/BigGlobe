package builderb0y.scripting.bytecode.tree.flow.loop;

import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.ScopeContext.LoopName;
import builderb0y.scripting.bytecode.ScopeContext.Scope;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclareAssignInsnTree;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class WhileInsnTree implements InsnTree {

	public LoopName loopName;
	public ConditionTree condition;
	public InsnTree body;

	public WhileInsnTree(LoopName loopName, ConditionTree condition, InsnTree body) {
		this.loopName = loopName;
		this.condition = condition;
		this.body = body.asStatement();
	}

	public static InsnTree createRepeat(ExpressionParser parser, LoopName loopName, InsnTree times, InsnTree body) {
		times = times.cast(parser, TypeInfos.INT, CastMode.IMPLICIT_THROW);
		LazyVarInfo counter = new LazyVarInfo(parser.method.mangleName("counter"), TypeInfos.INT);
		InsnTree init, loadLimit;
		if (times.getConstantValue().isConstant()) {
			//var counter = 0
			//while (counter < times:
			//	body
			//	++counter
			//)
			init = new VariableDeclareAssignInsnTree(counter, ldc(0));
			loadLimit = times;
		}
		else {
			//var limit = times
			//var counter = 0
			//while (counter < limit:
			//	body
			//	++counter
			//)
			LazyVarInfo limit = new LazyVarInfo(parser.method.mangleName("limit"), TypeInfos.INT);
			init = seq(
				new VariableDeclareAssignInsnTree(limit, times),
				new VariableDeclareAssignInsnTree(counter, ldc(0))
			);
			loadLimit = load(limit);
		}
		return for_(loopName, init, lt(parser, load(counter), loadLimit), inc(counter, 1), body);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		Scope scope = method.scopes.pushLoop(this.loopName);
		this.condition.emitBytecode(method, null, scope.end.getLabel());
		this.body.emitBytecode(method);
		method.node.visitJumpInsn(GOTO, scope.start.getLabel());
		method.scopes.popLoop();
	}

	@Override
	public TypeInfo getTypeInfo() {
		return TypeInfos.VOID;
	}

	@Override
	public boolean canBeStatement() {
		return true;
	}
}