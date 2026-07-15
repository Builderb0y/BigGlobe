package builderb0y.bigglobe.classes.spec;

import it.unimi.dsi.fastutil.Hash.Strategy;

import net.minecraft.core.Holder;

import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.autocodec.annotations.UseName;
import builderb0y.autocodec.util.HashStrategies;
import builderb0y.autocodec.util.ObjectArrayFactory;
import builderb0y.bigglobe.classes.Named;
import builderb0y.bigglobe.classes.compile.CustomClassFormatException;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;

public class ParameterSpec implements Named {

	public static final ObjectArrayFactory<ParameterSpec> ARRAY_FACTORY = new ObjectArrayFactory<>(ParameterSpec.class);
	public static final Strategy<ParameterSpec>
		TYPE_STRATEGY = HashStrategies.map(HashStrategies.identityStrategy(), (ParameterSpec parameter) -> parameter.type),
		FULL_STRATEGY = HashStrategies.allOf(NAME_STRATEGY, TYPE_STRATEGY);

	public final @IdentifierName String name;
	public final Holder<ElementSpec> type;

	public TypeSpec typeSpec() {
		return ElementSpec.requireType(this.type, TypeSpec.class, () -> "parameter type");
	}

	public final @DefaultBoolean(false) @UseName("import") boolean import_;

	public ParameterSpec(String name, Holder<ElementSpec> type, boolean import_) {
		this.name = name;
		this.type = type;
		this.import_ = import_;
	}

	public void verify() throws CustomClassFormatException {
		if (this.typeSpec().getTypeInfo().isVoid()) {
			throw new CustomClassFormatException("Void-typed parameter " + this.name);
		}
	}

	public Holder<ElementSpec> type() {
		return this.type;
	}

	public TypeInfo typeInfo() {
		return this.typeSpec().getTypeInfo();
	}

	@Override
	public String name() {
		return this.name;
	}

	@Override
	public String toString() {
		return UnregisteredObjectException.getID(this.type) + " " + this.name;
	}
}