package builderb0y.bigglobe.columns.scripted.traits;

import java.lang.invoke.*;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.SetBasedMutableDependencyView;
import builderb0y.bigglobe.columns.scripted.traits.TraitManager.TraitInfo;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.TypeInfo;

/**
common super class of runtime-generated trait classes.
*/
public class WorldTraits {

	public static final TypeInfo TYPE = TypeInfo.of(WorldTraits.class);
	public static final WeakHashMap<Class<? extends WorldTraits>, Map<Holder<WorldTrait>, WorldTraitInfo>> WORLD_TRAIT_INFOS = new WeakHashMap<>();

	public transient Map<Holder<WorldTrait>, ? extends SetBasedMutableDependencyView> dependenciesPerTrait;

	public static Map<Holder<WorldTrait>, WorldTraitInfo> getWorldTraits(ColumnEntryRegistry registry) {
		synchronized (WORLD_TRAIT_INFOS) {
			return WORLD_TRAIT_INFOS.computeIfAbsent(registry.traitManager.baseTraits.getClass(), (Class<? extends WorldTraits> clazz) -> {
				return (
					registry
					.traitManager
					.infos
					.entrySet()
					.stream()
					.map((Map.Entry<Holder<WorldTrait>, TraitInfo> entry) -> {
						MethodHandle getter = ScriptedColumn.findHandle(registry.traitManager.baseTraitsLookup, entry.getValue().getter.info);
						if (getter == null) return null;
						boolean is3D = entry.getKey().value().schema().is_3d();
						if (!is3D) getter = MethodHandles.dropArguments(getter, 2, int.class);
						getter = getter.asType(MethodType.methodType(Object.class, WorldTraits.class, ScriptedColumn.class, int.class));

						MethodHandle setter;
						if (entry.getValue().setter != null) {
							setter = ScriptedColumn.findHandle(registry.traitManager.baseTraitsLookup, entry.getValue().setter.info);
							if (setter != null) {
								if (!is3D) setter = MethodHandles.dropArguments(setter, 2, int.class);
								setter = setter.asType(MethodType.methodType(void.class, WorldTraits.class, ScriptedColumn.class, int.class, Object.class));
							}
						}
						else {
							setter = null;
						}

						MethodHandle preComputer;
						if (entry.getValue().preComputer != null) {
							preComputer = ScriptedColumn.findHandle(registry.traitManager.baseTraitsLookup, entry.getValue().preComputer.info);
							if (preComputer != null) {
								preComputer = preComputer.asType(MethodType.methodType(void.class, WorldTraits.class, ScriptedColumn.class));
							}
						}
						else {
							preComputer = null;
						}

						return new WorldTraitInfo(entry.getKey(), getter, setter, preComputer);
					})
					.filter(Objects::nonNull)
					.collect(Collectors.toMap(WorldTraitInfo::holder, Function.identity()))
				);
			});
		}
	}

	public static record WorldTraitInfo(
		Holder<WorldTrait> holder,
		MethodHandle getter, //(WorldTraits, ScriptedColumn, int) -> Object
		@Nullable MethodHandle setter, //(WorldTraits, ScriptedColumn, int, Object) -> void
		@Nullable MethodHandle preComputer //(WorldTraits, ScriptedColumn) -> void
	) {

		public Object get(WorldTraits traits, ScriptedColumn column, int y) {
			try {
				return this.getter.invokeExact(traits, column, y);
			}
			catch (Throwable throwable) {
				BigGlobeMod.LOGGER.error("Exception getting world trait:", throwable);
				return null;
			}
		}

		public void set(WorldTraits traits, ScriptedColumn column, int y, Object value) {
			if (this.setter != null) try {
				this.setter.invokeExact(traits, column, y, value);
			}
			catch (Throwable throwable) {
				BigGlobeMod.LOGGER.error("Exception setting column value:", throwable);
			}
		}

		public void preCompute(WorldTraits traits, ScriptedColumn column) {
			if (this.preComputer != null) try {
				this.preComputer.invokeExact(traits, column);
			}
			catch (Throwable throwable) {
				BigGlobeMod.LOGGER.error("Exception pre-computing column value:", throwable);
			}
		}

		public Identifier id() {
			return UnregisteredObjectException.getID(this.holder);
		}

		@Override
		public @NotNull String toString() {
			return this.id().toString();
		}
	}
}