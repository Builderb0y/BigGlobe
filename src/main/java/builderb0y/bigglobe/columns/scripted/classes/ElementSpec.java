package builderb0y.bigglobe.columns.scripted.classes;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.CoderRegistry;
import builderb0y.bigglobe.codecs.CoderRegistryTyped;
import builderb0y.bigglobe.util.UnregisteredObjectException;

@UseCoder(name = "REGISTRY", in = ElementSpec.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public abstract class ElementSpec implements Named, CoderRegistryTyped<ElementSpec> {

	public static final CoderRegistry<ElementSpec> REGISTRY = new CoderRegistry<>(BigGlobeMod.modID("custom_classes"));
	static {
		REGISTRY.registerAuto(BigGlobeMod.modID(ElementSpecTypes.CLASS_BUILTIN     ),      BuiltinTypeSpec.class);
		REGISTRY.registerAuto(BigGlobeMod.modID(ElementSpecTypes.CLASS_NORMAL      ),            ClassSpec.class);
		REGISTRY.registerAuto(BigGlobeMod.modID(ElementSpecTypes.CLASS_VORONOI     ),     VoronoiClassSpec.class);
		REGISTRY.registerAuto(BigGlobeMod.modID(ElementSpecTypes.FIELD_NORMAL      ),            FieldSpec.class);
		REGISTRY.registerAuto(BigGlobeMod.modID(ElementSpecTypes.CONSTRUCTOR_NORMAL),      ConstructorSpec.class);
		REGISTRY.registerAuto(BigGlobeMod.modID(ElementSpecTypes.METHOD_NORMAL     ),     NormalMethodSpec.class);
		REGISTRY.registerAuto(BigGlobeMod.modID(ElementSpecTypes.METHOD_OVERRIDE   ),   OverrideMethodSpec.class);
		REGISTRY.registerAuto(BigGlobeMod.modID(ElementSpecTypes.METHOD_ABSTRACT   ),   AbstractMethodSpec.class);
		REGISTRY.registerAuto(BigGlobeMod.modID(ElementSpecTypes.PROPERTY_NORMAL   ),   NormalPropertySpec.class);
		REGISTRY.registerAuto(BigGlobeMod.modID(ElementSpecTypes.PROPERTY_OVERRIDE ), OverridePropertySpec.class);
		REGISTRY.registerAuto(BigGlobeMod.modID(ElementSpecTypes.PROPERTY_ABSTRACT ), AbstractPropertySpec.class);
	}

	public static TypeSpec asType(RegistryEntry<ElementSpec> entry) {
		if (entry.value() instanceof TypeSpec type) {
			return type;
		}
		else {
			throw new IllegalStateException(UnregisteredObjectException.getID(entry) + " was used in a place where a type was required, but it does not represent a type (is a " + ElementSpec.REGISTRY.lookup().encode.get(entry.value().getCoder()) + ')');
		}
	}

	public static MemberSpec asMember(RegistryEntry<ElementSpec> entry) {
		if (entry.value() instanceof MemberSpec member) {
			return member;
		}
		else {
			throw new IllegalStateException(UnregisteredObjectException.getID(entry) + " was used in a place where a member was required, but it does not represent a member (is a " + ElementSpec.REGISTRY.lookup().encode.get(entry.value().getCoder()) + ')');
		}
	}
}