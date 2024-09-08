package builderb0y.bigglobe.columns.scripted.traits;

import java.util.Map;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.SetBasedMutableDependencyView;
import builderb0y.scripting.bytecode.TypeInfo;

/** common super class of runtime-generated trait classes. */
public class WorldTraits {

	public static final TypeInfo TYPE = TypeInfo.of(WorldTraits.class);

	public transient Map<RegistryEntry<WorldTrait>, ? extends SetBasedMutableDependencyView> dependenciesPerTrait;
}