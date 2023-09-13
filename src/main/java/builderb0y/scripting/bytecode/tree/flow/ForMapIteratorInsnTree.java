package builderb0y.scripting.bytecode.tree.flow;

import java.util.Map;

import org.objectweb.asm.tree.LabelNode;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.ScopeContext.Scope;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclarationInsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclareAssignInsnTree;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ForMapIteratorInsnTree extends AbstractForIteratorInsnTree {

	public static final MethodInfo
		ENTRY_SET = MethodInfo.getMethod(Map.class, "entrySet"),
		GET_KEY   = MethodInfo.getMethod(Map.Entry.class, "getKey"),
		GET_VALUE = MethodInfo.getMethod(Map.Entry.class, "getValue");

	public VariableDeclarationInsnTree keyVariable, valueVariable;
	public VariableDeclareAssignInsnTree iterator;
	public InsnTree body;

	public ForMapIteratorInsnTree(
		String loopName,
		VariableDeclarationInsnTree keyVariable,
		VariableDeclarationInsnTree valueVariable,
		VariableDeclareAssignInsnTree iterator,
		InsnTree body
	) {
		super(loopName);
		this.keyVariable   = keyVariable;
		this.valueVariable = valueVariable;
		this.iterator      = iterator;
		this.body          = body;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		//(
		//	Type keyVar
		//	Type valueVar
		//	Iterator iterator = iterable.iterator()
		//	while (iterator.hasNext():
		//		keyVar, valueVar = iterator.next().(getKey(), getValue())
		//		body
		//	)
		//)
		//
		//begin:
		//	Type keyVar
		//	Type valueVar
		//	Iterator iterator = this.iterator
		//continuePoint:
		//	unless (iterator.hasNext(): goto(end))
		//	keyVar, valueVar = iterator.next().(getKey(), getValue())
		//	body
		//	goto(continuePoint)
		//end:
		LabelNode continuePoint = labelNode();
		Scope scope = method.scopes.pushLoop(this.loopName, continuePoint);
		this.keyVariable.emitBytecode(method);
		this.valueVariable.emitBytecode(method);
		this.iterator.emitBytecode(method);
		method.node.instructions.add(continuePoint);
		this.iterator.variable.emitLoad(method);
		ForIteratorInsnTree.HAS_NEXT.emitBytecode(method);
		method.node.visitJumpInsn(IFEQ, scope.end.getLabel());
		this.iterator.variable.emitLoad(method);
		ForIteratorInsnTree.NEXT.emitBytecode(method);
		method.node.visitInsn(DUP);
		GET_KEY.emitBytecode(method);
		castAndStore(this.keyVariable, method);
		GET_VALUE.emitBytecode(method);
		castAndStore(this.valueVariable, method);
		this.body.emitBytecode(method);
		method.node.visitJumpInsn(GOTO, continuePoint.getLabel());

		method.scopes.popScope();
	}
}