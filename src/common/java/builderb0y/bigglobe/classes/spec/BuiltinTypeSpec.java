package builderb0y.bigglobe.classes.spec;

import java.util.LinkedHashSet;
import java.util.stream.Stream;

import org.jetbrains.annotations.MustBeInvokedByOverriders;

import net.minecraft.core.Holder;

import builderb0y.autocodec.data.Data;
import builderb0y.bigglobe.classes.compile.ClassHierarchy;
import builderb0y.bigglobe.classes.compile.ConstantFormatException;
import builderb0y.bigglobe.classes.compile.DetailedException;
import builderb0y.bigglobe.columns.scripted.ExternalEnvironmentParams;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class BuiltinTypeSpec extends TypeSpec {

	public final BuiltinType java_type;
	public transient TypeInfo columnType;

	public BuiltinTypeSpec(BuiltinType java_type) {
		this.java_type = java_type;
	}

	@Override
	public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
		return Stream.empty();
	}

	@Override
	@MustBeInvokedByOverriders
	public void reference(ClassHierarchy hierarchy) throws DetailedException {
		super.reference(hierarchy);
		hierarchy.rootTypes.add(hierarchy.entryOf(this));
	}

	@Override
	@MustBeInvokedByOverriders
	public void createTypeInfo(ClassHierarchy hierarchy, LinkedHashSet<Holder<ElementSpec>> cyclicDetector) throws DetailedException {
		super.createTypeInfo(hierarchy, cyclicDetector);
		this.columnType = hierarchy.registry.columnCompileContext.columnTypeInfo();
	}

	@Override
	public TypeInfo getTypeInfo() {
		if (this.columnType == null) {
			throw new IllegalStateException("Haven't created scripted column type info yet!");
		}
		return this.java_type.getTypeInfo(this);
	}

	@Override
	public boolean isFinal() {
		//prevent custom classes extending builtin classes,
		//even if that class isn't actually final.
		return true;
	}

	@Override
	public void setupEnvironment(Holder<ElementSpec> self, MutableScriptEnvironment environment, ExternalEnvironmentParams params) {
		this.java_type.setupEnvironment(environment, params, self);
	}

	@Override
	public String name() {
		return this.java_type.name();
	}

	@Override
	public InsnTree parseConstant(ClassHierarchy hierarchy, Data data) throws ConstantFormatException {
		if (data.isEmpty()) return ldcZero(this.getTypeInfo());
		return this.java_type.parseConstant(hierarchy, this, data);
	}
}