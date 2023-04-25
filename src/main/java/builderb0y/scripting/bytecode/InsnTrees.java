package builderb0y.scripting.bytecode;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.function.BinaryOperator;

import com.google.common.collect.ObjectArrays;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.LabelNode;

import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.conditions.*;
import builderb0y.scripting.bytecode.tree.flow.*;
import builderb0y.scripting.bytecode.tree.instructions.*;
import builderb0y.scripting.bytecode.tree.instructions.binary.*;
import builderb0y.scripting.bytecode.tree.instructions.unary.InstanceOfInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.unary.NegateInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.unary.SquareInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.CollectionTransformer;
import builderb0y.scripting.util.TypeInfos;

/** a collection of convenience methods for constructing various objects related to bytecode. */
public class InsnTrees implements ExtendedOpcodes {

	public static final NoopInsnTree noop = NoopInsnTree.INSTANCE;

	public static TypeInfo type(Class<?> clazz) {
		return TypeInfo.of(clazz);
	}

	public static TypeInfo[] types(Class<?>... classes) {
		return CollectionTransformer.convertArray(classes, TypeInfo.ARRAY_FACTORY, TypeInfo::of);
	}

	public static TypeInfo type(String desc) {
		return TypeInfo.parse(desc);
	}

	public static TypeInfo[] types(String descs) {
		return TypeInfo.parseAll(descs);
	}

	public static VarInfo variable(String name, int index, TypeInfo type) {
		return new VarInfo(name, index, type);
	}

	public static VarInfo variable(String name, int index, Class<?> type) {
		return new VarInfo(name, index, type(type));
	}

	public static MethodInfo method(int access, TypeInfo owner, String name, TypeInfo returnType, TypeInfo... paramTypes) {
		return new MethodInfo(access, owner, name, returnType, paramTypes);
	}

	public static MethodInfo method(int access, Class<?> owner, String name, Class<?> returnType, Class<?>... paramTypes) {
		return new MethodInfo(access, type(owner), name, type(returnType), types(paramTypes));
	}

	public static MethodInfo constructor(int access, TypeInfo owner, TypeInfo... paramTypes) {
		return new MethodInfo(access, owner, "<init>", TypeInfos.VOID, paramTypes);
	}

	public static MethodInfo constructor(int access, Class<?> owner, Class<?>... paramTypes) {
		return new MethodInfo(access, type(owner), "<init>", TypeInfos.VOID, types(paramTypes));
	}

	public static FieldInfo field(int access, TypeInfo owner, String name, TypeInfo type) {
		return new FieldInfo(access, owner, name, type);
	}

	public static FieldInfo field(int access, Class<?> owner, String name, Class<?> type) {
		return new FieldInfo(access, type(owner), name, type(type));
	}

	public static ConstantValue constant(boolean  value) { return ConstantValue.of(value); }
	public static ConstantValue constant(byte     value) { return ConstantValue.of(value); }
	public static ConstantValue constant(char     value) { return ConstantValue.of(value); }
	public static ConstantValue constant(short    value) { return ConstantValue.of(value); }
	public static ConstantValue constant(int      value) { return ConstantValue.of(value); }
	public static ConstantValue constant(long     value) { return ConstantValue.of(value); }
	public static ConstantValue constant(float    value) { return ConstantValue.of(value); }
	public static ConstantValue constant(double   value) { return ConstantValue.of(value); }
	public static ConstantValue constant(String   value) { return ConstantValue.of(value); }
	public static ConstantValue constant(TypeInfo value) { return ConstantValue.of(value); }
	public static ConstantValue constant(Object   value, TypeInfo type) { return ConstantValue.of(value, type); }
	public static ConstantValue constant(MethodInfo bootstrapMethod, ConstantValue... bootstrapArgs) { return ConstantValue.dynamic(bootstrapMethod, bootstrapArgs); }

	public static InsnTree ldc(boolean  value) { return new ConstantInsnTree(constant(value)); }
	public static InsnTree ldc(byte     value) { return new ConstantInsnTree(constant(value)); }
	public static InsnTree ldc(char     value) { return new ConstantInsnTree(constant(value)); }
	public static InsnTree ldc(short    value) { return new ConstantInsnTree(constant(value)); }
	public static InsnTree ldc(int      value) { return new ConstantInsnTree(constant(value)); }
	public static InsnTree ldc(long     value) { return new ConstantInsnTree(constant(value)); }
	public static InsnTree ldc(float    value) { return new ConstantInsnTree(constant(value)); }
	public static InsnTree ldc(double   value) { return new ConstantInsnTree(constant(value)); }
	public static InsnTree ldc(String   value) { return new ConstantInsnTree(constant(value)); }
	public static InsnTree ldc(TypeInfo value) { return new ConstantInsnTree(constant(value)); }
	public static InsnTree ldc(Object   value, TypeInfo type) { return new ConstantInsnTree(constant(value, type)); }
	public static InsnTree ldc(MethodInfo bootstrapMethod, ConstantValue... bootstrapArgs) { return new ConstantInsnTree(constant(bootstrapMethod, bootstrapArgs)); }
	public static InsnTree ldc(ConstantValue value) { return new ConstantInsnTree(value); }

	public static InsnTree load(VarInfo info) {
		return new LoadInsnTree(info);
	}

	public static InsnTree load(String name, int index, TypeInfo type) {
		return new LoadInsnTree(new VarInfo(name, index, type));
	}

	public static InsnTree store(VarInfo variable, InsnTree value) {
		return new StoreInsnTree(variable, value);
	}

	public static InsnTree arrayLoad(InsnTree array, InsnTree index) {
		return ArrayLoadInsnTree.create(array, index);
	}

	public static InsnTree arrayStore(InsnTree array, InsnTree index, InsnTree value) {
		return ArrayStoreInsnTree.create(array, index, value);
	}

	public static InsnTree return_(InsnTree value) {
		return ReturnInsnTree.create(value);
	}

	public static InsnTree throw_(InsnTree value) {
		return ThrowInsnTree.create(value);
	}

	public static InsnTree add(ExpressionParser parser, InsnTree left, InsnTree right) {
		return AddInsnTree.create(parser, left, right);
	}

	public static InsnTree add(ExpressionParser parser, InsnTree... operands) {
		return reduceWithParser(parser, AddInsnTree::create, operands);
	}

	public static InsnTree sub(ExpressionParser parser, InsnTree left, InsnTree right) {
		return SubtractInsnTree.create(parser, left, right);
	}

	public static InsnTree mul(ExpressionParser parser, InsnTree left, InsnTree right) {
		return MultiplyInsnTree.create(parser, left, right);
	}

	public static InsnTree mul(ExpressionParser parser, InsnTree... operands) {
		return reduceWithParser(parser, MultiplyInsnTree::create, operands);
	}

	public static InsnTree div(ExpressionParser parser, InsnTree left, InsnTree right) {
		return DivideInsnTree.create(parser, left, right);
	}

	public static InsnTree mod(ExpressionParser parser, InsnTree left, InsnTree right) {
		return ModuloInsnTree.create(parser, left, right);
	}

	public static InsnTree pow(ExpressionParser parser, InsnTree left, InsnTree right) {
		return PowerInsnTree.create(parser, left, right);
	}

	public static InsnTree band(ExpressionParser parser, InsnTree left, InsnTree right) {
		return BitwiseAndInsnTree.create(parser, left, right);
	}

	public static InsnTree band(ExpressionParser parser, InsnTree... operands) {
		return reduceWithParser(parser, BitwiseAndInsnTree::create, operands);
	}

	public static InsnTree bor(ExpressionParser parser, InsnTree left, InsnTree right) {
		return BitwiseOrInsnTree.create(parser, left, right);
	}

	public static InsnTree bor(ExpressionParser parser, InsnTree... operands) {
		return reduceWithParser(parser, BitwiseOrInsnTree::create, operands);
	}

	public static InsnTree bxor(ExpressionParser parser, InsnTree left, InsnTree right) {
		return BitwiseXorInsnTree.create(parser, left, right);
	}

	public static InsnTree bxor(ExpressionParser parser, InsnTree... operands) {
		return reduceWithParser(parser, BitwiseXorInsnTree::create, operands);
	}

	public static InsnTree shl(ExpressionParser parser, InsnTree left, InsnTree right) {
		return SignedLeftShiftInsnTree.create(parser, left, right);
	}

	public static InsnTree ushl(ExpressionParser parser, InsnTree left, InsnTree right) {
		return UnsignedLeftShiftInsnTree.create(parser, left, right);
	}

	public static InsnTree shr(ExpressionParser parser, InsnTree left, InsnTree right) {
		return SignedRightShiftInsnTree.create(parser, left, right);
	}

	public static InsnTree ushr(ExpressionParser parser, InsnTree left, InsnTree right) {
		return UnsignedRightShiftInsnTree.create(parser, left, right);
	}

	public static InsnTree instanceOf(InsnTree operand, TypeInfo type) {
		return InstanceOfInsnTree.create(operand, type);
	}

	public static InsnTree neg(InsnTree value) {
		return NegateInsnTree.create(value);
	}

	public static InsnTree square(InsnTree value) {
		return SquareInsnTree.create(value);
	}

	public static InsnTree inc(VarInfo variable, int amount) {
		return IncrementInsnTree.create(variable, amount);
	}

	public static InsnTree invokeStatic(MethodInfo method, InsnTree... args) {
		return InvokeStaticInsnTree.create(method, args);
	}

	public static InsnTree invokeVirtual(InsnTree receiver, MethodInfo method, InsnTree... args) {
		return new InvokeInsnTree(INVOKEVIRTUAL, receiver, method, args);
	}

	public static InsnTree invokeSpecial(InsnTree receiver, MethodInfo method, InsnTree... args) {
		return new InvokeInsnTree(INVOKESPECIAL, receiver, method, args);
	}

	public static InsnTree invokeInterface(InsnTree receiver, MethodInfo method, InsnTree... args) {
		return new InvokeInsnTree(INVOKEINTERFACE, receiver, method, args);
	}

	public static InsnTree invokeVirtualOrInterface(InsnTree receiver, MethodInfo method, InsnTree... args) {
		return new InvokeInsnTree(method.isInterface() ? INVOKEINTERFACE : INVOKEVIRTUAL, receiver, method, args);
	}

	public static InsnTree invokeDynamic(MethodInfo bootstrapMethod, MethodInfo runtimeMethod, ConstantValue[] bootstrapArgs, InsnTree[] runtimeArgs) {
		return new InvokeDynamicInsnTree(bootstrapMethod, runtimeMethod, bootstrapArgs, runtimeArgs);
	}

	public static InsnTree invokeWrapped(InsnTree receiver, MethodInfo method, InsnTree... args) {
		return invokeStatic(method, ObjectArrays.concat(receiver, args));
	}

	public static InsnTree newInstance(MethodInfo constructor, InsnTree... args) {
		return new NewInsnTree(constructor, args);
	}

	public static InsnTree newArrayWithLength(TypeInfo arrayType, InsnTree length) {
		return NewArrayWithLengthInsnTree.create(arrayType, length);
	}

	public static InsnTree newArrayWithContents(ExpressionParser parser, TypeInfo arrayType, InsnTree... elements) {
		return NewArrayWithContentsInsnTree.create(parser, arrayType, elements);
	}

	public static InsnTree getStatic(FieldInfo field) {
		return GetStaticInsnTree.create(field);
	}

	public static InsnTree getStatic(int access, TypeInfo owner, String name, TypeInfo desc) {
		return GetStaticInsnTree.create(new FieldInfo(access, owner, name, desc));
	}

	public static InsnTree putStatic(FieldInfo field, InsnTree value) {
		return PutStaticInsnTree.create(field, value);
	}

	public static InsnTree getField(InsnTree receiver, FieldInfo field) {
		return GetFieldInsnTree.create(receiver, field);
	}

	public static InsnTree putField(InsnTree receiver, FieldInfo field, InsnTree value) {
		return PutFieldInsnTree.create(receiver, field, value);
	}

	public static ConditionTree condition(ExpressionParser parser, InsnTree bool) {
		return BooleanToConditionTree.create(parser, bool);
	}

	public static InsnTree bool(ConditionTree condition) {
		return ConditionToBooleanInsnTree.create(condition);
	}

	public static ConditionTree and(ConditionTree left, ConditionTree right) {
		return AndConditionTree.create(left, right);
	}

	public static ConditionTree and(ConditionTree... conditions) {
		return reduce(AndConditionTree::create, conditions);
	}

	public static InsnTree and(ExpressionParser parser, InsnTree left, InsnTree right) {
		return bool(and(condition(parser, left), condition(parser, right)));
	}

	public static InsnTree and(ExpressionParser parser, InsnTree... bools) {
		return reduceWithParser(parser, InsnTrees::and, bools);
	}

	public static ConditionTree or(ConditionTree left, ConditionTree right) {
		return OrConditionTree.create(left, right);
	}

	public static ConditionTree or(ConditionTree... conditions) {
		return reduce(OrConditionTree::create, conditions);
	}

	public static InsnTree or(ExpressionParser parser, InsnTree left, InsnTree right) {
		return bool(or(condition(parser, left), condition(parser, right)));
	}

	public static InsnTree or(ExpressionParser parser, InsnTree... bools) {
		return reduceWithParser(parser, InsnTrees::or, bools);
	}

	public static InsnTree xor(ExpressionParser parser, InsnTree left, InsnTree right) {
		left  = left .cast(parser, TypeInfos.BOOLEAN, CastMode.IMPLICIT_THROW);
		right = right.cast(parser, TypeInfos.BOOLEAN, CastMode.IMPLICIT_THROW);
		return bxor(parser, left, right);
	}

	public static ConditionTree xor(ExpressionParser parser, ConditionTree left, ConditionTree right) {
		return condition(parser, xor(parser, bool(left), bool(right)));
	}

	public static ConditionTree eq(ExpressionParser parser, InsnTree left, InsnTree right) {
		return CompareConditionTree.equal(parser, left, right);
	}

	public static ConditionTree ne(ExpressionParser parser, InsnTree left, InsnTree right) {
		return CompareConditionTree.notEqual(parser, left, right);
	}

	public static ConditionTree gt(ExpressionParser parser, InsnTree left, InsnTree right) {
		return CompareConditionTree.greaterThan(parser, left, right);
	}

	public static ConditionTree lt(ExpressionParser parser, InsnTree left, InsnTree right) {
		return CompareConditionTree.lessThan(parser, left, right);
	}

	public static ConditionTree ge(ExpressionParser parser, InsnTree left, InsnTree right) {
		return CompareConditionTree.greaterThanOrEqual(parser, left, right);
	}

	public static ConditionTree le(ExpressionParser parser, InsnTree left, InsnTree right) {
		return CompareConditionTree.lessThanOrEqual(parser, left, right);
	}

	public static ConditionTree not(ConditionTree condition) {
		return NotConditionTree.create(condition);
	}

	public static InsnTree not(ExpressionParser parser, InsnTree bool) {
		return bool(not(condition(parser, bool)));
	}

	public static InsnTree ifThen(ConditionTree condition, InsnTree body) {
		return IfInsnTree.create(condition, body);
	}

	public static InsnTree ifElse(ExpressionParser parser, ConditionTree conditionTree, InsnTree trueBody, InsnTree falseBody) throws ScriptParsingException {
		return IfElseInsnTree.create(parser, conditionTree, trueBody, falseBody);
	}

	public static InsnTree while_(ConditionTree condition, InsnTree body) {
		return new WhileInsnTree(condition, body);
	}

	public static InsnTree doWhile(ExpressionParser parser, ConditionTree condition, InsnTree body) {
		return new DoWhileInsnTree(parser, condition, body);
	}

	public static InsnTree for_(InsnTree initializer, ConditionTree condition, InsnTree step, InsnTree body) {
		return new ForInsnTree(initializer, condition, step, body);
	}

	public static InsnTree seq(InsnTree first, InsnTree second) {
		return new SequenceInsnTree(first, second);
	}

	public static InsnTree seq(InsnTree... statements) {
		return reduce(InsnTrees::seq, statements);
	}

	public static InsnTree switch_(ExpressionParser parser, InsnTree value, Int2ObjectSortedMap<InsnTree> cases) {
		return SwitchInsnTree.create(parser, value, cases);
	}

	public static InsnTree scoped(InsnTree body) {
		return ScopedInsnTree.create(body);
	}

	public static InsnTree block(InsnTree body) {
		return new BlockInsnTree(body);
	}

	public static InsnTree getFromStack(TypeInfo type) {
		return new GetFromStackInsnTree(type);
	}

	public static Label label() {
		Label label = new Label();
		label.info = new LabelNode(label);
		return label;
	}

	public static LabelNode labelNode() {
		LabelNode node = new LabelNode();
		node.getLabel().info = node;
		return node;
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] repeat(T element, int times) {
		T[] array = (T[])(Array.newInstance(element.getClass(), times));
		Arrays.fill(array, element);
		return array;
	}

	@SafeVarargs
	public static <T> T reduce(BinaryOperator<T> reducer, T... operands) {
		T result = operands[0];
		for (int index = 1, length = operands.length; index < length; index++) {
			result = reducer.apply(result, operands[index]);
		}
		return result;
	}

	@SafeVarargs
	public static <T> T reduceWithParser(ExpressionParser parser, ParserReducer<T> reducer, T... operands) {
		T result = operands[0];
		for (int index = 1, length = operands.length; index < length; index++) {
			result = reducer.apply(parser, result, operands[index]);
		}
		return result;
	}

	@FunctionalInterface
	public static interface ParserReducer<T> {

		public abstract T apply(ExpressionParser parser, T left, T right);
	}
}