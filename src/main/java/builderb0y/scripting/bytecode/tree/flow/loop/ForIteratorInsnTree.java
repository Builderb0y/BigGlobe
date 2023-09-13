package builderb0y.scripting.bytecode.tree.flow.loop;

import org.objectweb.asm.tree.LabelNode;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.ScopeContext.Scope;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclarationInsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclareAssignInsnTree;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ForIteratorInsnTree extends AbstractForIteratorInsnTree {

	public VariableDeclarationInsnTree variable;
	public VariableDeclareAssignInsnTree iterator;
	public InsnTree body;

	public ForIteratorInsnTree(
		String loopName,
		VariableDeclarationInsnTree variable,
		VariableDeclareAssignInsnTree iterator,
		InsnTree body
	) {
		super(loopName);
		this.variable = variable;
		this.iterator = iterator;
		this.body     = body;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		//(
		//	Type userVar
		//	Iterator iterator = iterable.iterator()
		//	while (iterator.hasNext():
		//		userVar = iterator.next()
		//		body
		//	)
		//)
		//
		//begin:
		//	Type userVar
		//	Iterator iterator = this.iterator
		//continuePoint:
		//	unless (iterator.hasNext(): goto(end))
		//	userVar = iterator.next().as(Type)
		//	body
		//	goto(continuePoint)
		//end:
		LabelNode continuePoint = labelNode();
		Scope scope = method.scopes.pushLoop(this.loopName, continuePoint);
		this.variable.emitBytecode(method);
		this.iterator.emitBytecode(method);
		method.node.instructions.add(continuePoint);
		this.iterator.variable.emitLoad(method);
		HAS_NEXT.emitBytecode(method);
		method.node.visitJumpInsn(IFEQ, scope.end.getLabel());
		this.iterator.variable.emitLoad(method);
		NEXT.emitBytecode(method);
		castAndStore(this.variable, method);
		this.body.emitBytecode(method);
		method.node.visitJumpInsn(GOTO, continuePoint.getLabel());

		method.scopes.popScope();
	}
}