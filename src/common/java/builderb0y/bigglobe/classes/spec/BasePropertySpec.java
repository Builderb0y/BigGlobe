package builderb0y.bigglobe.classes.spec;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.Hash;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Holder;

import builderb0y.autocodec.util.HashStrategies;
import builderb0y.bigglobe.classes.Named;
import builderb0y.bigglobe.classes.compile.ClassHierarchy;
import builderb0y.bigglobe.classes.compile.CustomClassFormatException;
import builderb0y.bigglobe.classes.compile.DetailedException;
import builderb0y.bigglobe.columns.scripted2.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted2.dependencies.DependencyView.SetBasedMutableDependencyView;
import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.util.TypeInfos;

public abstract class BasePropertySpec extends MemberSpec {

	public static final Hash.Strategy<BasePropertySpec>
		TYPE_STRATEGY = HashStrategies.map(HashStrategies.identityStrategy(), BasePropertySpec::getPropertyType),
		FULL_STRATEGY = HashStrategies.allOf(Named.NAME_STRATEGY, TYPE_STRATEGY);

	public transient PropertyCompileContext context;
	public final transient Set<Holder<ElementSpec>> overrides = new HashSet<>();

	public BasePropertySpec(Holder<ElementSpec> owner) {
		super(owner);
	}

	public abstract Holder<ElementSpec> getPropertyType();

	public abstract TypeSpec getPropertyTypeSpec(ClassHierarchy hierarchy);

	public abstract int flags();

	public abstract boolean isSettable();

	@Override
	public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
		return (
			Stream.of(
				super.streamDirectDependencies(),
				Stream.of(this.getPropertyType()),
				this.overrides.stream()
			)
			.flatMap(Function.identity())
		);
	}

	@Override
	@MustBeInvokedByOverriders
	public void createRepresentation(ClassHierarchy hierarchy) throws DetailedException {
		super.createRepresentation(hierarchy);
		BaseClassSpec owner = this.owner(hierarchy);
		this.context = new PropertyCompileContext(
			owner.classCompileContext.newMethod(
				this.flags(),
				this.name(),
				this.getPropertyTypeSpec(hierarchy).getTypeInfo()
			),
			this.isSettable()
			? owner.classCompileContext.newMethod(
				this.flags(),
				this.name(),
				TypeInfos.VOID,
				new LazyVarInfo("value", this.getPropertyTypeSpec(hierarchy).getTypeInfo())
			)
			: null
		);
	}

	@Override
	@MustBeInvokedByOverriders
	public void verify(ClassHierarchy hierarchy) throws DetailedException {
		super.verify(hierarchy);
		if (this.getPropertyTypeSpec(hierarchy).getTypeInfo().isVoid()) {
			throw new CustomClassFormatException("Void-typed property: " + this);
		}
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + ": { name: " + this.name() + ", property type: " + this.getPropertyType() + " }";
	}

	public static class PropertyCompileContext {

		public MethodCompileContext get;
		public @Nullable MethodCompileContext set;

		public PropertyCompileContext(MethodCompileContext get, @Nullable MethodCompileContext set) {
			this.get = get;
			this.set = set;
		}
	}
}