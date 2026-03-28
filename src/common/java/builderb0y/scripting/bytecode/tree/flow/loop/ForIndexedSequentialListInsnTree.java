package builderb0y.scripting.bytecode.tree.flow.loop;

import java.util.ListIterator;

import org.objectweb.asm.tree.LabelNode;

import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.ScopeContext.LoopName;
import builderb0y.scripting.bytecode.ScopeContext.Scope;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclarationInsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclareAssignInsnTree;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ForIndexedSequentialListInsnTree extends AbstractForIteratorInsnTree {

	public static final MethodInfo
		LIST_ITERATOR_PREV_INDEX = MethodInfo.getMethod(ListIterator.class, "previousIndex");

	public VariableDeclarationInsnTree indexVariable, elementVariable;
	public VariableDeclareAssignInsnTree iterator;
	public InsnTree body;

	public ForIndexedSequentialListInsnTree(
		LoopName loopName,
		VariableDeclarationInsnTree indexVariable,
		VariableDeclarationInsnTree elementVariable,
		VariableDeclareAssignInsnTree iterator,
		InsnTree body
	) {
		super(loopName);
		this.indexVariable = indexVariable;
		this.elementVariable = elementVariable;
		this.iterator = iterator;
		this.body = body;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		//(
		//	int index
		//	Type element
		//	ListIterator iterator = iterable.iterator()
		//	while (iterator.hasNext():
		//		element = iterator.next()
		//		index = iterator.previousIndex()
		//		body
		//	)
		//)
		//
		//begin:
		//	int index
		//	Type element
		//	List iterator = this.iterator
		//continuePoint:
		//	unless (iterator.hasNext(): goto(end))
		//	element = iterator.next()
		//	index = iterator.previousIndex()
		//	body
		//	goto(continuePoint)
		//end:
		LabelNode continuePoint = labelNode();
		Scope scope = method.scopes.pushLoop(this.loopName, continuePoint);
		this.indexVariable.emitBytecode(method);
		this.elementVariable.emitBytecode(method);
		this.iterator.emitBytecode(method);
		method.node.instructions.add(continuePoint);
		this.iterator.variable.emitLoad(method);
		ForIteratorInsnTree.HAS_NEXT.emitBytecode(method);
		method.node.visitJumpInsn(IFEQ, scope.end.getLabel());
		this.iterator.variable.emitLoad(method);
		ForIteratorInsnTree.NEXT.emitBytecode(method);
		castAndStore(this.elementVariable, method);
		this.iterator.variable.emitLoad(method);
		LIST_ITERATOR_PREV_INDEX.emitBytecode(method);
		this.indexVariable.variable.emitStore(method);
		this.body.emitBytecode(method);
		method.node.visitJumpInsn(GOTO, continuePoint.getLabel());

		method.scopes.popScope();
	}
}