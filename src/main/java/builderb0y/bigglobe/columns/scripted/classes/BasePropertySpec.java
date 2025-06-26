package builderb0y.bigglobe.columns.scripted.classes;

import it.unimi.dsi.fastutil.Hash;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.util.HashStrategies;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.SetBasedMutableDependencyView;
import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.util.TypeInfos;

public abstract class BasePropertySpec extends MemberSpec implements SetBasedMutableDependencyView {

	public static final Hash.Strategy<BasePropertySpec>
		TYPE_STRATEGY = HashStrategies.map(HashStrategies.identityStrategy(), BasePropertySpec::getPropertyType),
		FULL_STRATEGY = HashStrategies.allOf(NAME_STRATEGY, TYPE_STRATEGY);

	public abstract RegistryEntry<ElementSpec> getPropertyType();

	public abstract int flags();

	public abstract boolean isSettable();

	@Override
	public void create(ClassHierarchy hierarchy, BaseClassSpec owner) {
		owner.setCompileContext(
			this,
			new PropertyCompileContext(
				owner.classCompileContext.newMethod(
					this.flags(),
					this.name(),
					asType(this.getPropertyType()).getTypeInfo()
				),
				this.isSettable()
				? owner.classCompileContext.newMethod(
					this.flags(),
					this.name(),
					TypeInfos.VOID,
					new LazyVarInfo("value", asType(this.getPropertyType()).getTypeInfo())
				)
				: null
			)
		);
	}

	@Override
	public void verify(ClassHierarchy hierarchy, BaseClassSpec owner) throws CustomClassFormatException {
		super.verify(hierarchy, owner);
		if (asType(this.getPropertyType()).getTypeInfo().isVoid()) {
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