package builderb0y.bigglobe.classes.spec;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.Hash.Strategy;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.Holder;

import builderb0y.autocodec.util.HashStrategies;
import builderb0y.bigglobe.classes.Named;
import builderb0y.bigglobe.classes.compile.ClassHierarchy;
import builderb0y.bigglobe.classes.compile.CustomClassFormatException;
import builderb0y.bigglobe.classes.compile.DetailedException;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.input.ScriptUsage;

public abstract class BaseMethodSpec extends MemberSpec {

	public static final Strategy<BaseMethodSpec>
		DESC_STRATEGY = HashStrategies.allOf(
			Named.NAME_STRATEGY,
			HashStrategies.map(
				HashStrategies.orderedArrayStrategy(ParameterSpec.TYPE_STRATEGY),
				BaseMethodSpec::getParameters
			)
		);

	public transient MethodSpecDesc desc;
	public transient MethodCompileContext context;
	public final transient SetBasedMutableDependencyView dependencies = SetBasedMutableDependencyView.from(new HashSet<>());
	public final transient Set<Holder<ElementSpec>> overrides = new HashSet<>();

	public MethodSpecDesc getDescriptor() {
		if (this.desc == null) {
			this.desc = new MethodSpecDesc(this.name(), Arrays.stream(this.getParameters()).map(ParameterSpec::typeInfo).toList());
		}
		return this.desc;
	}

	public BaseMethodSpec(Holder<ElementSpec> owner) {
		super(owner);
	}

	public abstract Holder<ElementSpec> getReturnType();

	public abstract TypeSpec returnTypeSpec(ClassHierarchy hierarchy);

	public abstract ParameterSpec[] getParameters();

	@Override
	public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
		return (
			Stream.of(
				super.streamDirectDependencies(),
				Stream.of(this.getReturnType()),
				Arrays.stream(this.getParameters()).map(ParameterSpec::type),
				this.overrides.stream(),
				this.dependencies.streamDirectDependencies()
			)
			.flatMap(Function.identity())
		);
	}

	public abstract int accessFlags();

	@Override
	@MustBeInvokedByOverriders
	public void verify(ClassHierarchy hierarchy) throws DetailedException {
		super.verify(hierarchy);
		Set<String> parameters = new ObjectOpenHashSet<>(this.getParameters().length);
		for (ParameterSpec parameter : this.getParameters()) {
			parameter.verify();
			if (!parameters.add(parameter.name())) {
				throw new CustomClassFormatException("Duplicate parameter name: " + parameter.name);
			}
		}
	}

	@Override
	@MustBeInvokedByOverriders
	public void createRepresentation(ClassHierarchy hierarchy) throws DetailedException {
		super.createRepresentation(hierarchy);
		this.context = this.owner(hierarchy).classCompileContext.newMethod(
			this.accessFlags(),
			this.name(),
			this.returnTypeSpec(hierarchy).getTypeInfo(),
			Arrays
			.stream(this.getParameters())
			.map((ParameterSpec parameter) -> new LazyVarInfo(
				parameter.name,
				parameter.typeInfo()
			))
			.toArray(LazyVarInfo.ARRAY_FACTORY)
		);
	}

	public void compile(
		ClassHierarchy hierarchy,
		ScriptUsage code,
		InsnTree loadCustomClass,
		Consumer<ExpressionParser> extra
	)
	throws ScriptParsingException {
		compile(
			hierarchy,
			this.context,
			code,
			loadCustomClass,
			this.dependencies,
			extra
		);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + ": " + this.name() + ' ' + Arrays.toString(this.getParameters()) + ' ' + UnregisteredObjectException.getID(this.getReturnType());
	}

	public static record MethodSpecDesc(String name, List<TypeInfo> parameters) {

		@Override
		public @NotNull String toString() {
			return this.parameters.stream().map(TypeInfo::getSimpleClassName).collect(Collectors.joining(", ", this.name + '(', ")"));
		}
	}
}