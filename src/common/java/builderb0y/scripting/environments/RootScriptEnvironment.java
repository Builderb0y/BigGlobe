package builderb0y.scripting.environments;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.casting.DirectCastInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment.UsageCallback;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class RootScriptEnvironment extends MultiScriptEnvironment {

	public static final int
		USER_INDEX = 0,
		MUTABLE_INDEX = 1;

	//not the most efficient data structure,
	//but I expect it to be small,
	//so the inefficiencies are probably not significant.
	public List<ImportedObject> importedObjects = new ArrayList<>();

	public static record ImportedObject(String name, InsnTree object, UsageCallback callback) {

		public TypeInfo getTypeInfo() {
			return this.object.getTypeInfo();
		}
	}

	public RootScriptEnvironment() {
		this.environments.add(new UserScriptEnvironment());
		this.environments.add(new MutableScriptEnvironment().addAll(BuiltinScriptEnvironment.INSTANCE));
	}

	public RootScriptEnvironment(RootScriptEnvironment from) {
		super(from);
		this.environments.set(USER_INDEX, new UserScriptEnvironment(from.user()));
		this.environments.set(MUTABLE_INDEX, new MutableScriptEnvironment().addAll(from.mutable()));
		this.importedObjects.addAll(from.importedObjects);
	}

	public void importObject(String name, InsnTree object) {
		this.importObject(name, object, null);
	}

	public void importObject(String name, InsnTree object, UsageCallback callback) {
		ImportedObject toAdd = new ImportedObject(name, object, callback);
		for (ImportedObject existing : this.importedObjects) {
			if (existing.getTypeInfo().equals(object.getTypeInfo())) {
				throw new IllegalStateException("Object of type " + object.getTypeInfo() + " is already imported. (attempting to replace " + existing + " with " + toAdd + ")");
			}
		}
		this.mutable().addVariable(name, object);
		this.importedObjects.add(toAdd);
	}

	public @Nullable InsnTree getImportedObject(ExpressionParser parser, TypeInfo type) {
		for (ImportedObject object : this.importedObjects) {
			if (object.getTypeInfo().extendsOrImplements(type)) {
				if (object.callback != null) {
					object.callback.onUsed(parser, object.name);
				}
				return object.object;
			}
		}
		return null;
	}

	@Override
	public @Nullable InsnTree getVariable(ExpressionParser parser, String name) throws ScriptParsingException {
		InsnTree result = super.getVariable(parser, name);
		if (result == null) {
			ImportedObject chosen = null;
			for (ImportedObject object : this.importedObjects) {
				InsnTree newResult = super.getField(parser, object.object, name, GetFieldMode.NORMAL);
				if (newResult != null) {
					if (result == null) { result = newResult; chosen = object; }
					else throw new ScriptParsingException("Ambiguous variable matches fields on more than one imported object", parser.input);
				}
			}
			if (chosen != null && chosen.callback != null) {
				chosen.callback.onUsed(parser, name);
			}
		}
		return result;
	}

	@Override
	public @Nullable InsnTree getFunction(ExpressionParser parser, String name, InsnTree... arguments) throws ScriptParsingException {
		InsnTree result = super.getFunction(parser, name, arguments);
		if (result == null) {
			ImportedObject chosen = null;
			for (RootScriptEnvironment.ImportedObject object : this.importedObjects) {
				InsnTree newResult = super.getMethod(parser, object.object, name, GetMethodMode.NORMAL, arguments);
				if (newResult != null) {
					if (result == null) { result = newResult; chosen = object; }
					else throw new ScriptParsingException("Ambiguous function call matches methods on more than one imported object", parser.input);
				}
			}
			if (chosen != null && chosen.callback != null) {
				chosen.callback.onUsed(parser, name);
			}
		}
		return result;
	}

	@Override
	public @Nullable InsnTree cast(ExpressionParser parser, InsnTree value, TypeInfo to, boolean implicit, boolean nullable) {
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
					return wrapIdentityCast(value, to);
				}
				if (!implicit && (to.extendsOrImplements(from) || (to.type.isInterface && !from.isFinal))) {
					return new DirectCastInsnTree(value, to, nullable);
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
				//is Integer, Number, Object, Comparable, or a few other types.
				//we will NOT return int -> Integer when the requested type
				//is IntNbt, for example.
				TypeInfo boxed = from.box();
				if (boxed.extendsOrImplements(to)) {
					InsnTree casted = BuiltinScriptEnvironment.INSTANCE.cast(parser, value, boxed, false, nullable);
					if (casted != null) return casted;
					else throw new ClassCastException("Can't primitively cast " + value.describe() + " to " + boxed);
				}
			}
		}
		else if (from.isObject() && !implicit) {
			//object-to-primitive casting is hard-coded because
			//we expect to have generic objects needing to be
			//cast to primitives every now and then.
			TypeInfo castTo = switch (to.getSort()) {
				case BYTE                -> TypeInfos.BYTE_WRAPPER;
				case SHORT               -> TypeInfos.SHORT_WRAPPER;
				case INT                 -> TypeInfos.INT_WRAPPER;
				case LONG                -> TypeInfos.LONG_WRAPPER;
				case FLOAT               -> TypeInfos.FLOAT_WRAPPER;
				case DOUBLE              -> TypeInfos.DOUBLE_WRAPPER;
				case CHAR                -> TypeInfos.CHAR_WRAPPER;
				case BOOLEAN             -> TypeInfos.BOOLEAN_WRAPPER;
				case VOID, OBJECT, ARRAY -> null;
			};
			if (castTo != null && castTo.extendsOrImplements(from)) {
				value = new DirectCastInsnTree(value, castTo, nullable);
			}
		}
		return super.cast(parser, value, to, implicit, nullable);
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