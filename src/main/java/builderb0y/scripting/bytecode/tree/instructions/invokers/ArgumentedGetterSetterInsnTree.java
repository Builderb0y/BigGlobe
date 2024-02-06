package builderb0y.scripting.bytecode.tree.instructions.invokers;

import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.ArgumentedObjectUpdateInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.update.ArgumentedObjectUpdateInsnTree.ArgumentedObjectUpdateEmitters;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;

public class ArgumentedGetterSetterInsnTree extends NormalInvokeInsnTree {

	public MethodInfo setter;
	public InsnTree argument;

	public ArgumentedGetterSetterInsnTree(InsnTree receiver, MethodInfo getter, MethodInfo setter, InsnTree argument) {
		super(receiver, getter, argument);
		this.setter = setter;
		this.argument = argument;
		checkArgumentedGetterSetter(receiver, getter, setter);
	}

	public static void checkArgumentedGetterSetter(InsnTree receiver, MethodInfo getter, MethodInfo setter) {
		if (getter.getInvokeTypes().length != 2) {
			throw new IllegalArgumentException("Getter should take exactly 2 arguments: " + getter);
		}
		if (getter.returnType.isVoid()) {
			throw new IllegalArgumentException("Getter should not return void: " + getter);
		}
		if (setter.getInvokeTypes().length != 3) {
			throw new IllegalArgumentException("Setter should take exactly 3 arguments: " + setter);
		}
		if (setter.returnType.isValue()) {
			throw new IllegalArgumentException("Setter should return void: " + setter);
		}
		if (!getter.getInvokeTypes()[0].equals(setter.getInvokeTypes()[0])) {
			throw new IllegalArgumentException("Getter and setter operate on different types: " + getter + "; " + setter);
		}
		if (!getter.getInvokeTypes()[1].equals(setter.getInvokeTypes()[1])) {
			throw new IllegalArgumentException("Getter and setter take different extra argument types: " + getter + "; " + setter);
		}
		if (getter.getInvokeTypes()[1].isDoubleWidth()) {
			throw new IllegalArgumentException("Extra argument must be single-width: " + getter.getInvokeTypes()[1]);
		}
		if (!getter.returnType.equals(setter.getInvokeTypes()[2])) {
			throw new IllegalArgumentException("Getter return type does not match setter value type: " + getter + "; " + setter);
		}
		if (!receiver.getTypeInfo().extendsOrImplements(getter.getInvokeTypes()[0])) {
			throw new IllegalArgumentException("Receiver is of the wrong type: expected " + getter + ", got " + receiver.describe());
		}
	}

	@Override
	public InsnTree update(ExpressionParser parser, UpdateOp op, UpdateOrder order, InsnTree rightValue) throws ScriptParsingException {
		if (op == UpdateOp.ASSIGN) {
			InsnTree cast = rightValue.cast(parser, this.method.returnType, CastMode.IMPLICIT_THROW);
			return new ArgumentedObjectUpdateInsnTree(order, true, ArgumentedObjectUpdateEmitters.forGetterSetter(this.args[0], this.argument, this.method, this.setter, cast));
		}
		else {
			InsnTree updater = op.createUpdater(parser, this.method.returnType, rightValue);
			return new ArgumentedObjectUpdateInsnTree(order, false, ArgumentedObjectUpdateEmitters.forGetterSetter(this.args[0], this.argument, this.method, this.setter, updater));
		}
	}
}