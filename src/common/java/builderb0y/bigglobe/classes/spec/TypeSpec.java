package builderb0y.bigglobe.classes.spec;

import java.nio.file.Path;
import java.util.LinkedHashSet;

import org.jetbrains.annotations.MustBeInvokedByOverriders;

import net.minecraft.core.Holder;

import builderb0y.autocodec.data.Data;
import builderb0y.bigglobe.classes.compile.ClassHierarchy;
import builderb0y.bigglobe.classes.compile.ConstantFormatException;
import builderb0y.bigglobe.classes.compile.DetailedException;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ScriptClassLoader;

public abstract class TypeSpec extends ElementSpec {

	public static final Path DUMP_DIRECTORY = ScriptClassLoader.initDumpDirectory("builderb0y.bigglobe.dumpCustomClasses", "bigglobe_custom_classes");

	public abstract InsnTree parseConstant(ClassHierarchy hierarchy, Data data) throws ConstantFormatException;

	public abstract TypeInfo getTypeInfo();

	public abstract boolean isFinal();

	@MustBeInvokedByOverriders
	public void createTypeInfo(ClassHierarchy hierarchy, LinkedHashSet<Holder<ElementSpec>> cyclicDetector) throws DetailedException {}

	@Override
	@MustBeInvokedByOverriders
	public void createTypeInfo(ClassHierarchy hierarchy) throws DetailedException {
		super.createTypeInfo(hierarchy);
		this.createTypeInfo(hierarchy, new LinkedHashSet<>());
	}

	@Override
	@MustBeInvokedByOverriders
	public void verify(ClassHierarchy hierarchy) throws DetailedException {
		super.verify(hierarchy);
		hierarchy.checkName(this);
	}
}