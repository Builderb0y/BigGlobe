package builderb0y.bigglobe.overriders;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.coders.KeyDispatchCoder;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnValueInfo;
import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted.dependencies.IndirectDependencyCollector;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;

@UseCoder(name = "CODER", in = Overrider.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public sealed interface Overrider permits CollisionOverrider.Entry, ColumnValueOverrider.Entry, StructureOverrider.Entry {

	public static final AutoCoder<Overrider> CODER = new KeyDispatchCoder<>(ReifiedType.from(Overrider.class), BigGlobeAutoCodec.AUTO_CODEC.createCoder(Type.class)) {

		@Override
		public @Nullable Type getKey(@NotNull Overrider object) {
			return object.getOverriderType();
		}

		@Override
		public @Nullable AutoCoder<? extends Overrider> getCoder(@NotNull Type type) {
			return type.coder;
		}
	};

	public abstract Type getOverriderType();

	public static enum Type {
		STRUCTURE(StructureOverrider.Entry.class),
		COLLISION(CollisionOverrider.Entry.class),
		COLUMN_VALUE(ColumnValueOverrider.Entry.class);

		public final Class<? extends Overrider> overriderClass;
		public final AutoCoder<? extends Overrider> coder;

		Type(Class<? extends Overrider> overriderClass) {
			this.overriderClass = overriderClass;
			this.coder = BigGlobeAutoCodec.AUTO_CODEC.createCoder(overriderClass);
		}
	}

	public static record ColumnValueOverridersWithRadiusCache(
		Holder<ColumnValueOverrider.Entry>[] overriders,
		Reference2IntMap<Holder<StructureSet>> radii,
		Reference2ObjectMap<Structure, int[]> indices
	) {

		public ColumnValueOverridersWithRadiusCache(Holder<ColumnValueOverrider.Entry>[] overriders) {
			this(overriders, new Reference2IntOpenHashMap<>(64), new Reference2ObjectOpenHashMap<>(64));
		}

		public int getSearchRadius(Holder<StructureSet> set) {
			synchronized (this.radii) {
				return this.radii.computeIfAbsent(set, (Holder<StructureSet> set_) -> {
					return ColumnValueOverrider.getSearchRadius(this.overriders, set_.value());
				});
			}
		}

		public int[] getIndices(Structure structure) {
			synchronized (this.indices) {
				return this.indices.computeIfAbsent(
					structure,
					(Structure structure_) -> {
						int length = this.overriders.length;
						IntArrayList list = new IntArrayList(length);
						for (int index = 0; index < length; index++) {
							if (this.overriders[index].value().matches(structure_)) {
								list.add(index);
							}
						}
						return list.toIntArray();
					}
				);
			}
		}
	}

	public static class SortedOverriders {

		public final List<Holder<StructureOverrider.Entry>> structures;
		public final CollisionOverrider.Entry[] collisions;
		public final ColumnValueOverridersWithRadiusCache rawColumnValues, featureColumnValues;
		public final ColumnValueInfo[] rawColumnValueDependencies, featureColumnValueDependencies;

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public SortedOverriders(BigGlobeScriptedChunkGenerator generator) {
			Map<Type, List<Holder<Overrider>>> map = generator.overriders.entryStream().collect(Collectors.groupingBy((Holder<Overrider> entry) -> entry.value().getOverriderType()));
			this.structures = (List)(map.getOrDefault(Type.STRUCTURE, Collections.emptyList()));
			this.collisions = map.getOrDefault(Type.COLLISION, Collections.emptyList()).stream().map(Holder<Overrider>::value).map(CollisionOverrider.Entry.class::cast).toArray(CollisionOverrider.Entry[]::new);
			List<Holder<Overrider>> columnValueOverriders = map.getOrDefault(Type.COLUMN_VALUE, Collections.emptyList());
			this.rawColumnValues = new ColumnValueOverridersWithRadiusCache(columnValueOverriders.stream().filter((Holder<Overrider> overrider) -> ((ColumnValueOverrider.Entry)(overrider.value())).raw_generation).toArray(Holder[]::new));
			this.featureColumnValues = new ColumnValueOverridersWithRadiusCache(columnValueOverriders.stream().filter((Holder<Overrider> overrider) -> ((ColumnValueOverrider.Entry)(overrider.value())).feature_generation).toArray(Holder[]::new));
			this.rawColumnValueDependencies = this.extractDependencies(this.rawColumnValues.overriders, generator);
			this.featureColumnValueDependencies = this.extractDependencies(this.featureColumnValues.overriders, generator);
		}

		public int getCollisionPriority(
			ScriptedColumnLookup columns,
			StructureStartWrapper currentStructure,
			StructureStartWrapper otherStructure
		) {
			for (CollisionOverrider.Entry collision : this.collisions) {
				int priority = collision.script().override(columns, currentStructure, otherStructure);
				if (priority != 0) return priority;
			}
			return 0;
		}

		public ColumnValueInfo[] extractDependencies(Holder<ColumnValueOverrider.Entry>[] holders, BigGlobeScriptedChunkGenerator generator) {
			IndirectDependencyCollector collector = new IndirectDependencyCollector(generator);
			for (Holder<ColumnValueOverrider.Entry> entry : holders) {
				entry.value().script.streamDirectDependencies().forEach(collector);
			}
			Map<Holder<ColumnEntry>, ColumnValueInfo> values = ScriptedColumn.getColumnValues(generator.columnEntryRegistry);
			return (
				collector
				.stream()
				.filter((Holder<? extends DependencyView> registryEntry) -> {
					return (
						registryEntry.value() instanceof ColumnEntry columnEntry &&
						columnEntry.hasFieldSetterAndFlag()
					);
				})
				.map(values::get)
				.toArray(ColumnValueInfo[]::new)
			);
		}
	}
}