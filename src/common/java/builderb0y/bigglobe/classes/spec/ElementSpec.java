package builderb0y.bigglobe.classes.spec;

import java.util.function.Supplier;

import net.minecraft.core.Holder;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.classes.ElementSpecTypes;
import builderb0y.bigglobe.classes.Named;
import builderb0y.bigglobe.classes.compile.ClassHierarchy;
import builderb0y.bigglobe.classes.compile.StagedCompileable;
import builderb0y.bigglobe.codecs.CoderRegistry;
import builderb0y.bigglobe.codecs.CoderRegistryTyped;
import builderb0y.bigglobe.columns.scripted2.ExternalEnvironmentParams;
import builderb0y.bigglobe.columns.scripted2.dependencies.DependencyView.SimpleDependencyView;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.environments.MutableScriptEnvironment;

@UseCoder(name = "REGISTRY", in = ElementSpec.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public abstract class ElementSpec extends StagedCompileable<ClassHierarchy> implements Named, CoderRegistryTyped<ElementSpec>, SimpleDependencyView {

	public static final CoderRegistry<ElementSpec> REGISTRY = new CoderRegistry<>(BigGlobeMod.modID("custom_classes"), "element_type");
	static {
		REGISTRY.registerAuto(BigGlobeMod.modID(ElementSpecTypes.   CLASS_BUILTIN ),      BuiltinTypeSpec.class);
		REGISTRY.registerAuto(BigGlobeMod.modID(ElementSpecTypes.   CLASS_NORMAL  ),            ClassSpec.class);
		REGISTRY.registerAuto(BigGlobeMod.modID(ElementSpecTypes.   CLASS_ENUM    ),        EnumClassSpec.class);
		REGISTRY.registerAuto(BigGlobeMod.modID(ElementSpecTypes.   FIELD_NORMAL  ),            FieldSpec.class);
		REGISTRY.registerAuto(BigGlobeMod.modID(ElementSpecTypes.   FIELD_CONSTANT),    ConstantFieldSpec.class);
		REGISTRY.registerAuto(BigGlobeMod.modID(ElementSpecTypes.   FIELD_ENUM    ),        EnumValueSpec.class);
		REGISTRY.registerAuto(BigGlobeMod.modID(ElementSpecTypes.  METHOD_STATIC  ),     StaticMethodSpec.class);
		REGISTRY.registerAuto(BigGlobeMod.modID(ElementSpecTypes.  METHOD_NORMAL  ),     NormalMethodSpec.class);
		REGISTRY.registerAuto(BigGlobeMod.modID(ElementSpecTypes.  METHOD_OVERRIDE),   OverrideMethodSpec.class);
		REGISTRY.registerAuto(BigGlobeMod.modID(ElementSpecTypes.  METHOD_ABSTRACT),   AbstractMethodSpec.class);
		REGISTRY.registerAuto(BigGlobeMod.modID(ElementSpecTypes.PROPERTY_NORMAL  ),   NormalPropertySpec.class);
		REGISTRY.registerAuto(BigGlobeMod.modID(ElementSpecTypes.PROPERTY_OVERRIDE), OverridePropertySpec.class);
		REGISTRY.registerAuto(BigGlobeMod.modID(ElementSpecTypes.PROPERTY_ABSTRACT), AbstractPropertySpec.class);
	}

	@SuppressWarnings("unchecked")
	public static <T extends ElementSpec> T requireType(Holder<ElementSpec> entry, Class<T> clazz, Supplier<String> location) {
		ElementSpec value = entry.value();
		if (clazz.isInstance(value)) {
			return (T)(value);
		}
		else {
			throw new IllegalStateException(
				location.get() +
				" is expected to point to a " +
				clazz.getSimpleName() +
				", but it instead points to " +
				UnregisteredObjectException.getID(entry) +
				", which is a " +
				value.getClass().getSimpleName() +
				", also known as " +
				REGISTRY.getType(value)
			);
		}
	}

	public abstract void setupEnvironment(Holder<ElementSpec> self, MutableScriptEnvironment environment, ExternalEnvironmentParams params);
}