package builderb0y.bigglobe.columns.scripted.classes;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import it.unimi.dsi.fastutil.Hash.Strategy;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.NotNull;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.util.HashStrategies;
import builderb0y.autocodec.util.ObjectArrayFactory;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.SetBasedMutableDependencyView;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.input.ScriptUsage;

public abstract class BaseMethodSpec extends MemberSpec implements SetBasedMutableDependencyView {

	public static final Strategy<BaseMethodSpec>
		DESC_STRATEGY = HashStrategies.allOf(
			NAME_STRATEGY,
			HashStrategies.map(
				HashStrategies.orderedArrayStrategy(ParameterSpec.TYPE_STRATEGY),
				BaseMethodSpec::getParameters
			)
		);

	public transient MethodSpecDesc desc;

	public MethodSpecDesc getDescriptor() {
		if (this.desc == null) {
			this.desc = new MethodSpecDesc(this.name(), Arrays.stream(this.getParameters()).map(ParameterSpec::typeInfo).toList());
		}
		return this.desc;
	}

	public abstract RegistryEntry<ElementSpec> getReturnType();

	public abstract ParameterSpec[] getParameters();

	public abstract int flags();

	@Override
	public void verify(ClassHierarchy hierarchy, BaseClassSpec owner) throws CustomClassFormatException {
		super.verify(hierarchy, owner);
		owner.checkMethod(hierarchy, this);
		Set<String> parameters = new ObjectOpenHashSet<>(this.getParameters().length);
		for (ParameterSpec parameter : this.getParameters()) {
			parameter.verify();
			if (!parameters.add(parameter.name())) {
				throw new CustomClassFormatException("Duplicate parameter name: " + parameter.name);
			}
		}
	}

	@Override
	public void create(ClassHierarchy hierarchy, BaseClassSpec owner) {
		owner.setCompileContext(
			this,
			owner.classCompileContext.newMethod(
				this.flags(),
				this.name(),
				asType(this.getReturnType()).getTypeInfo(),
				Arrays
				.stream(this.getParameters())
				.map((ParameterSpec parameter) -> new LazyVarInfo(
					parameter.name,
					asType(parameter.type).getTypeInfo()
				))
				.toArray(LazyVarInfo.ARRAY_FACTORY)
			)
		);
	}

	public void compile(ClassHierarchy hierarchy, BaseClassSpec clazz, ScriptUsage code, Consumer<MutableScriptEnvironment> extra) throws ScriptParsingException {
		compile(hierarchy, clazz, clazz.getCompileContext(this), code, null, this, extra);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + ": " + this.name() + ' ' + Arrays.toString(this.getParameters()) + ' ' + UnregisteredObjectException.getID(this.getReturnType());
	}

	public static class ParameterSpec implements Named {

		public static final ObjectArrayFactory<ParameterSpec> ARRAY_FACTORY = new ObjectArrayFactory<>(ParameterSpec.class);
		public static final Strategy<ParameterSpec>
			TYPE_STRATEGY = HashStrategies.map(HashStrategies.identityStrategy(), (ParameterSpec parameter) -> parameter.type),
			FULL_STRATEGY = HashStrategies.allOf(NAME_STRATEGY, TYPE_STRATEGY);

		public final @IdentifierName String name;
		public final RegistryEntry<ElementSpec> type;

		public ParameterSpec(String name, RegistryEntry<ElementSpec> type) {
			this.name = name;
			this.type = type;
		}

		public void verify() throws CustomClassFormatException {
			if (asType(this.type).getTypeInfo().isVoid()) {
				throw new CustomClassFormatException("Void-typed parameter " + this.name);
			}
		}

		public RegistryEntry<ElementSpec> type() {
			return this.type;
		}

		public TypeInfo typeInfo() {
			return asType(this.type).getTypeInfo();
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

	public static record MethodSpecDesc(String name, List<TypeInfo> parameters) {

		@Override
		public @NotNull String toString() {
			return this.parameters.stream().map(TypeInfo::getSimpleClassName).collect(Collectors.joining(", ", this.name + '(', ")"));
		}
	}
}