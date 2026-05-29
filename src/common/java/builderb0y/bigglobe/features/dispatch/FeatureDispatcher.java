package builderb0y.bigglobe.features.dispatch;

import net.minecraft.core.Holder;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.CoderRegistry;
import builderb0y.bigglobe.codecs.CoderRegistryTyped;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.SimpleDependencyView;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;

@UseCoder(name = "REGISTRY", in = FeatureDispatcher.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public interface FeatureDispatcher extends CoderRegistryTyped<FeatureDispatcher>, SimpleDependencyView {

	public static final CoderRegistry<FeatureDispatcher> REGISTRY = new CoderRegistry<>(BigGlobeMod.modID("feature_dispatcher"));
	public static final Object INITIALIZER = new Object() {{
		REGISTRY.registerAuto(BigGlobeMod.modID("script"), ScriptedFeatureDispatcher.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("group"), GroupFeatureDispatcher.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("noop"), NoopFeatureDispatcher.class);
		REGISTRY.registerAuto(BigGlobeMod.modID("tag"), TagFeatureDispatcher.class);
	}};

	public abstract void generate(WorldWrapper world, Permuter random, long chunkSeed, Holder<FeatureDispatcher> selfEntry);

	public static int minModifiableX(WorldWrapper world) {
		return world.coordination.mutableArea().minX();
	}

	public static int minModifiableY(WorldWrapper world) {
		return world.coordination.mutableArea().minY();
	}

	public static int minModifiableZ(WorldWrapper world) {
		return world.coordination.mutableArea().minZ();
	}

	public static int maxModifiableX(WorldWrapper world) {
		return world.coordination.mutableArea().maxX();
	}

	public static int maxModifiableY(WorldWrapper world) {
		return world.coordination.mutableArea().maxY();
	}

	public static int maxModifiableZ(WorldWrapper world) {
		return world.coordination.mutableArea().maxZ();
	}

	public static int minAccessibleX(WorldWrapper world) {
		return world.coordination.immutableArea().minX();
	}

	public static int minAccessibleY(WorldWrapper world) {
		return world.coordination.immutableArea().minY();
	}

	public static int minAccessibleZ(WorldWrapper world) {
		return world.coordination.immutableArea().minZ();
	}

	public static int maxAccessibleX(WorldWrapper world) {
		return world.coordination.immutableArea().maxX();
	}

	public static int maxAccessibleY(WorldWrapper world) {
		return world.coordination.immutableArea().maxY();
	}

	public static int maxAccessibleZ(WorldWrapper world) {
		return world.coordination.immutableArea().maxZ();
	}
}