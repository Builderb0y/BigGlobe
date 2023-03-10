package builderb0y.scripting.bytecode.tree.instructions;

import org.objectweb.asm.Label;
import org.objectweb.asm.tree.LabelNode;

import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public class LineNumberInsnTree implements InsnTree {

	public final InsnTree content;
	public final int lineNumber;

	public LineNumberInsnTree(InsnTree content, int lineNumber) {
		this.content = content;
		this.lineNumber = lineNumber;
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		Label label;
		if (method.node.instructions.getLast() instanceof LabelNode labelNode) {
			label = labelNode.getLabel(); //might populate label field without visiting it.
			label.info = labelNode; //ensure it gets "visited".
		}
		else {
			method.node.visitLabel(label = new Label());
		}
		method.node.visitLineNumber(this.lineNumber, label);
		this.content.emitBytecode(method);
	}

	@Override
	public TypeInfo getTypeInfo() {
		return this.content.getTypeInfo();
	}

	@Override
	public ConstantValue getConstantValue() {
		return this.content.getConstantValue();
	}

	@Override
	public InsnTree doCast(ExpressionParser parser, TypeInfo type, CastMode mode) {
		InsnTree cast = this.content.doCast(parser, type, mode);
		if (cast == null) return null;
		return new LineNumberInsnTree(cast, this.lineNumber);
	}

	@Override
	public InsnTree update(ExpressionParser parser, UpdateOp op, InsnTree rightValue) throws ScriptParsingException {
		return new LineNumberInsnTree(this.content.update(parser, op, rightValue), this.lineNumber);
	}

	@Override
	public boolean returnsUnconditionally() {
		return this.content.returnsUnconditionally();
	}

	@Override
	public boolean canBeStatement() {
		return this.content.canBeStatement();
	}
}