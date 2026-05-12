package builderb0y.bigglobe.classes.spec;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.Holder;
import org.jetbrains.annotations.Nullable;
import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.classes.compile.ClassHierarchy;
import builderb0y.bigglobe.classes.compile.CustomClassFormatException;
import builderb0y.bigglobe.classes.compile.OverrideTracker;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted.tree.StandAloneDirect3DGetterInsnTree;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.environments.MutableScriptEnvironment.FunctionHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.MethodHandler;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.environments.ScriptEnvironment.GetMethodMode;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ExpressionParser.IdentifierName;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.input.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class NormalPropertySpec extends BasePropertySpec {

	public final @IdentifierName String name;
	public final Holder<ElementSpec> property_type;
	public final @DefaultBoolean(false) boolean is_3d;
	public final ScriptUsage get;
	public final @VerifyNullable ScriptUsage set;
	public final transient Set<Holder<? extends DependencyView>> dependencies = new HashSet<>();

	public NormalPropertySpec(
		@IdentifierName String name,
		Holder<ElementSpec> property_type,
		boolean is_3d,
		ScriptUsage get,
		@VerifyNullable ScriptUsage set
	) {
		this.name = name;
		this.property_type = property_type;
		this.is_3d = is_3d;
		this.get = get;
		this.set = set;
	}

	@Override
	public boolean is3D() {
		return this.is_3d;
	}

	@Override
	public void setupEnvironment(MutableScriptEnvironment environment, BaseClassSpec owner, @Nullable InsnTree loadCustomClass) {
		PropertyCompileContext propertyContext = owner.getCompileContext(this);
		MethodInfo getterInfo = propertyContext.get.info;
		MethodInfo setterInfo = propertyContext.set != null ? propertyContext.set.info : null;
		TypeInfo ownerType = owner.getTypeInfo();
		if (this.is_3d) {
			if (this.set != null) {
				environment.addMethod(
					propertyContext.get.clazz.info,
					this.name,
					new MethodHandler.Named(
						this.name + "(int y)",
						(ExpressionParser parser, InsnTree receiver, String name, GetMethodMode mode, InsnTree... arguments) -> {
							InsnTree y = ScriptEnvironment.castArgument(parser, name, TypeInfos.INT, CastMode.IMPLICIT_NULL, arguments);
							if (y == null) return null;
							if (mode != GetMethodMode.NORMAL) {
								throw new ScriptParsingException("Nullable and receiver access modes are not supported here.", parser.input);
							}
							return new CastResult(new StandAloneDirect3DGetterInsnTree(receiver, y, getterInfo, setterInfo), y != arguments[0]);
						}
					)
				);
				if (loadCustomClass != null && loadCustomClass.getTypeInfo().extendsOrImplements(propertyContext.get.clazz.info)) {
					environment.addFunction(
						this.name,
						new FunctionHandler.Named(
							this.name + "(int y)",
							(ExpressionParser parser, String name, InsnTree... arguments) -> {
								InsnTree y = ScriptEnvironment.castArgument(parser, name, TypeInfos.INT, CastMode.IMPLICIT_NULL, arguments);
								if (y == null) return null;
								return new CastResult(new StandAloneDirect3DGetterInsnTree(load("this", ownerType), y, getterInfo, setterInfo), y != arguments[0]);
							}
						)
					);
				}
			}
			else {
				environment.addMethodInvoke(this.name, getterInfo);
				if (loadCustomClass != null && loadCustomClass.getTypeInfo().extendsOrImplements(propertyContext.get.clazz.info)) {
					environment.addFunctionInvoke(load("this", ownerType), getterInfo);
				}
			}
		}
		else {
			if (this.set != null) {
				environment.addFieldGetterSetter(propertyContext.get.clazz.info, this.name, propertyContext.get.info, propertyContext.set.info);
				if (loadCustomClass != null && loadCustomClass.getTypeInfo().extendsOrImplements(propertyContext.get.clazz.info)) {
					environment.addVariableGetterSetter(loadCustomClass, this.name, propertyContext.get.info, propertyContext.set.info);
				}
			}
			else {
				environment.addFieldInvoke(propertyContext.get.info);
				if (loadCustomClass != null && loadCustomClass.getTypeInfo().extendsOrImplements(propertyContext.get.clazz.info)) {
					environment.addVariableInvoke(loadCustomClass, propertyContext.get.info);
				}
			}
		}
	}

	@Override
	public void compile(ClassHierarchy hierarchy, BaseClassSpec owner) throws ScriptParsingException {
		PropertyCompileContext propertyContext = owner.getCompileContext(this);
		InsnTree loadY = this.is_3d ? load("y", TypeInfos.INT) : null;
		compile(
			hierarchy, owner, propertyContext.get, this.get, loadY, this, (MutableScriptEnvironment environment) -> {
				if (this.is_3d) environment.addVariableLoad("y", TypeInfos.INT);
			}
		);
		if (this.set != null) compile(
			hierarchy, owner, propertyContext.set, this.set, loadY, this, (MutableScriptEnvironment environment) -> {
				if (this.is_3d) environment.addVariableLoad("y", TypeInfos.INT);
				environment.addVariableLoad("value", asType(this.getPropertyType()).getTypeInfo());
			}
		);
	}

	@Override
	public void track(OverrideTracker tracker) throws CustomClassFormatException {
		tracker.addNormalProperty(this);
	}

	@Override
	public String name() {
		return this.name;
	}

	@Override
	public Holder<ElementSpec> getPropertyType() {
		return this.property_type;
	}

	@Override
	public boolean isSettable() {
		return this.set != null;
	}

	@Override
	public int flags() {
		return ACC_PUBLIC;
	}

	@Override
	public Set<Holder<? extends DependencyView>> getDependencies() {
		return this.dependencies;
	}
}