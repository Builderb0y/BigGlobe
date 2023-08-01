package builderb0y.scripting.environments;

import org.jetbrains.annotations.Nullable;

import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.casting.DirectCastInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.casting.IdentityCastInsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class RootScriptEnvironment extends MultiScriptEnvironment {

	public static final int
		USER_INDEX    = 0,
		MUTABLE_INDEX = 1;

	public RootScriptEnvironment() {
		this.environments.add(new UserScriptEnvironment());
		this.environments.add(new MutableScriptEnvironment().addAll(BuiltinScriptEnvironment.INSTANCE));
	}

	public RootScriptEnvironment(RootScriptEnvironment from) {
		super(from);
		this.environments.set(USER_INDEX, new UserScriptEnvironment());
		this.environments.set(MUTABLE_INDEX, new MutableScriptEnvironment().addAll(from.mutable()));
	}

	@Override
	public @Nullable InsnTree cast(ExpressionParser parser, InsnTree value, TypeInfo to, boolean implicit) {
		TypeInfo from = value.getTypeInfo();
		if (to.isObject()) {
			if (from.isObject()) {
				//object-to-object casting should always attempt
				//to cast the object directly, if necessary.
				//it would be impractical to add a special caster
				//for every possible pair of types being cast,
				//especially when the logic for checking which types
				//can be direct-cast to which other types is so simple.
				//so, we hard-code that logic here.
				if (from.extendsOrImplements(to)) {
					return new IdentityCastInsnTree(value, to);
				}
				if (!implicit && (to.extendsOrImplements(from) || to.type.isInterface)) {
					return new DirectCastInsnTree(value, to);
				}
			}
			else {
				//primitive-to-object is hard-coded because we expect
				//primitives to be implicitly cast to a variety of types,
				//including their canonical boxed values, but also other
				//representations like NBT or JSON data, and in the future,
				//it is possible that user-defined boxes may be allowed.
				//we want to enforce that whenever a primitive is cast
				//to an object, the canonical boxed value takes priority.
				//with that said, we do NOT want to enforce that
				//canonical boxing is the only boxing that is allowed.
				//as such, we will only return a canonical boxing operation
				//here if the result would extend or implement the requested type.
				//for example, we return int -> Integer when the requested type
				//is Integer, Object, Comparable, or a few other types.
				//we will NOT return int -> Integer when the requested type
				//is IntNbt, for example.
				TypeInfo boxed = from.box();
				if (boxed.extendsOrImplements(to)) {
					return invokeStatic(new MethodInfo(ACC_PUBLIC | ACC_STATIC | ACC_PURE, boxed, "valueOf", boxed, from), value);
				}
			}
		}
		else if (from.isObject() && !implicit) {
			//object-to-primitive casting is hard-coded because
			//we expect to have generic objects needing to be
			//cast to primitives every now and then.
			TypeInfo castTo = switch (to.getSort()) {
				case BYTE -> TypeInfos.BYTE_WRAPPER;
				case SHORT -> TypeInfos.SHORT_WRAPPER;
				case INT -> TypeInfos.INT_WRAPPER;
				case LONG -> TypeInfos.LONG_WRAPPER;
				case FLOAT -> TypeInfos.FLOAT_WRAPPER;
				case DOUBLE -> TypeInfos.DOUBLE_WRAPPER;
				case CHAR -> TypeInfos.CHAR_WRAPPER;
				case BOOLEAN -> TypeInfos.BOOLEAN_WRAPPER;
				case VOID, OBJECT, ARRAY -> null;
			};
			if (castTo != null && castTo.extendsOrImplements(from)) {
				value = new DirectCastInsnTree(value, castTo);
			}
		}
		return super.cast(parser, value, to, implicit);
	}

	public UserScriptEnvironment user() {
		return (UserScriptEnvironment)(this.environments.get(USER_INDEX));
	}

	public MutableScriptEnvironment mutable() {
		return (MutableScriptEnvironment)(this.environments.get(MUTABLE_INDEX));
	}

	public RootScriptEnvironment user(UserScriptEnvironment user) {
		this.environments.set(USER_INDEX, user);
		return this;
	}

	public RootScriptEnvironment mutable(MutableScriptEnvironment mutable) {
		this.environments.set(MUTABLE_INDEX, mutable);
		return this;
	}
}