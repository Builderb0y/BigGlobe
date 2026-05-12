package builderb0y.bigglobe.classes.spec;

import it.unimi.dsi.fastutil.Hash;
import net.minecraft.core.Holder;
import org.jetbrains.annotations.Nullable;
import builderb0y.autocodec.util.HashStrategies;
import builderb0y.bigglobe.classes.compile.ClassHierarchy;
import builderb0y.bigglobe.classes.compile.CustomClassFormatException;
import builderb0y.bigglobe.classes.Named;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.SetBasedMutableDependencyView;
import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.util.ArrayBuilder;
import builderb0y.scripting.util.TypeInfos;

public abstract class BasePropertySpec extends MemberSpec implements SetBasedMutableDependencyView {

	public static final Hash.Strategy<BasePropertySpec>
		TYPE_STRATEGY = HashStrategies.map(HashStrategies.identityStrategy(), BasePropertySpec::getPropertyType),
		FULL_STRATEGY = HashStrategies.allOf(Named.NAME_STRATEGY, TYPE_STRATEGY);

	public abstract Holder<ElementSpec> getPropertyType();

	public abstract int flags();

	public abstract boolean isSettable();

	public abstract boolean is3D();

	@Override
	public void create(ClassHierarchy hierarchy, BaseClassSpec owner) {
		ArrayBuilder<LazyVarInfo> builder = new ArrayBuilder<>(2);
		owner.setCompileContext(
			this,
			new PropertyCompileContext(
				owner.classCompileContext.newMethod(
					this.flags(),
					this.name(),
					asType(this.getPropertyType()).getTypeInfo(),
					builder.appendIfNotNull(this.is3D() ? new LazyVarInfo("y", TypeInfos.INT) : null).toArray(LazyVarInfo.ARRAY_FACTORY)
				),
				this.isSettable()
					? owner.classCompileContext.newMethod(
					this.flags(),
					this.name(),
					TypeInfos.VOID,
					builder.append(new LazyVarInfo("value", asType(this.getPropertyType()).getTypeInfo())).toArray(LazyVarInfo.ARRAY_FACTORY)
				)
					: null
			)
		);
	}

	@Override
	public void verify(ClassHierarchy hierarchy, BaseClassSpec owner) throws CustomClassFormatException {
		super.verify(hierarchy, owner);
		owner.checkProperty(hierarchy, this);
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