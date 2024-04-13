package builderb0y.bigglobe.features.dispatch;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.CoderRegistry;
import builderb0y.bigglobe.codecs.CoderRegistryTyped;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;

@UseCoder(name = "REGISTRY", in = FeatureDispatcher.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public interface FeatureDispatcher extends CoderRegistryTyped<FeatureDispatcher>, DependencyView {

	public static final CoderRegistry<FeatureDispatcher> REGISTRY = new CoderRegistry<>(BigGlobeMod.modID("feature_dispatcher"));
	public static final Object INITIALIZER = new Object() {{
		REGISTRY.registerAuto(BigGlobeMod.modID("script"), ScriptedFeatureDispatcher.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("group"),     GroupFeatureDispatcher.class);
	}};

	public abstract void generate(WorldWrapper world, Permuter random, long chunkSeed, RegistryEntry<FeatureDispatcher> selfEntry);

	public static int minModifiableX(WorldWrapper world) { return world.coordination.mutableArea().getMinX(); }
	public static int minModifiableY(WorldWrapper world) { return world.coordination.mutableArea().getMinY(); }
	public static int minModifiableZ(WorldWrapper world) { return world.coordination.mutableArea().getMinZ(); }
	public static int maxModifiableX(WorldWrapper world) { return world.coordination.mutableArea().getMaxX(); }
	public static int maxModifiableY(WorldWrapper world) { return world.coordination.mutableArea().getMaxY(); }
	public static int maxModifiableZ(WorldWrapper world) { return world.coordination.mutableArea().getMaxZ(); }
	public static int minAccessibleX(WorldWrapper world) { return world.coordination.immutableArea().getMinX(); }
	public static int minAccessibleY(WorldWrapper world) { return world.coordination.immutableArea().getMinY(); }
	public static int minAccessibleZ(WorldWrapper world) { return world.coordination.immutableArea().getMinZ(); }
	public static int maxAccessibleX(WorldWrapper world) { return world.coordination.immutableArea().getMaxX(); }
	public static int maxAccessibleY(WorldWrapper world) { return world.coordination.immutableArea().getMaxY(); }
	public static int maxAccessibleZ(WorldWrapper world) { return world.coordination.immutableArea().getMaxZ(); }
}