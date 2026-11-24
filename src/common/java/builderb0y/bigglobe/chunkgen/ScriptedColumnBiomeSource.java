package builderb0y.bigglobe.chunkgen;

import java.util.Set;
import java.util.stream.Stream;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate.Sampler;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.ColumnScript;
import builderb0y.bigglobe.columns.ColumnScript.ColumnYToBiomeScript;
import builderb0y.bigglobe.columns.ScriptedColumn;
import builderb0y.bigglobe.columns.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.columns.ScriptedColumn.Params;
import builderb0y.bigglobe.dynamicRegistries.RegistryAdapter;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.scripting.wrappers.entries.BiomeEntry;
import builderb0y.bigglobe.scripting.wrappers.tags.BiomeTag;
import builderb0y.bigglobe.util.DelayedEntryList;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.input.ScriptUsage;
import builderb0y.scripting.util.InfoHolder;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ScriptedColumnBiomeSource extends BiomeSource {

	public static final MapCodec<ScriptedColumnBiomeSource> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(ScriptedColumnBiomeSource.class);

	public final ColumnYToBiomeScript.Catcher script;
	public final ColumnToBiomeSetScript.@VerifyNullable Catcher column_biomes;
	public final DelayedEntryList<Biome> all_possible_biomes;
	public transient BigGlobeScriptedChunkGenerator generator;
	public transient ThreadLocal<@Nullable ScriptedColumn> columnThreadLocal;
	public final RegistryAdapter<Biome> biomeRegistry;

	public ScriptedColumnBiomeSource(
		ColumnYToBiomeScript.Catcher script,
		ColumnToBiomeSetScript.@VerifyNullable Catcher column_biomes,
		DelayedEntryList<Biome> all_possible_biomes,
		RegistryAdapter<Biome> biomeRegistry
	) {
		this.script = script;
		this.column_biomes = column_biomes;
		this.all_possible_biomes = all_possible_biomes;
		this.biomeRegistry = biomeRegistry;
		this.columnThreadLocal = ThreadLocal.withInitial(() -> {
			if (this.generator != null) {
				return this.generator.columnEntryRegistry.columnFactory.create(
					new Params(
						this.generator.columnSeed,
						0,
						0,
						this.generator.height.min_y(),
						this.generator.height.max_y(),
						ColumnUsage.GENERIC.maybeDhHints(),
						this.generator.compiledWorldTraits
					)
				);
			}
			else {
				return null;
			}
		});
	}

	@Override
	public MapCodec<? extends BiomeSource> codec() {
		return CODEC;
	}

	@Override
	public Set<Holder<Biome>> possibleBiomes() {
		return this.all_possible_biomes.entrySet();
	}

	@Override
	protected Stream<Holder<Biome>> collectPossibleBiomes() {
		return this.all_possible_biomes.entryStream();
	}

	#if MC_VERSION >= MC_26_3_0

		@Override
		public BiomeResolver createResolver(Sampler sampler) {
			return (int x, int y, int z) -> this.getNoiseBiome(x, y, z, sampler);
		}

	#endif

	//@Override
	public Holder<Biome> getNoiseBiome(int x, int y, int z, Sampler noise) {
		ScriptedColumn column = this.columnThreadLocal.get();
		if (column != null) {
			column.setParams(column.params.at(x << 2, z << 2).hints(ColumnUsage.GENERIC.maybeDhHints()));
			return this.script.get(column, y << 2).entry;
		}
		else {
			return BigGlobeMod.getRegistry(Registries.BIOME).requireEntry(Biomes.PLAINS);
		}
	}

	@Override
	public String toString() {
		//some other mod injects toString() into BiomeSource,
		//which explicitly queries our biome tag.
		//toString() is called by AutoCodec immediately after deserialization,
		//which logs a stack trace that we don't want.
		return "ScriptedColumnBiomeSource";
	}

	public static interface ColumnToBiomeSetScript extends ColumnScript {

		public abstract void addBiomesTo(ScriptedColumn column, BiomeSet biomes) throws Throwable;

		@Wrapper
		public static class Catcher extends ColumnScript.BaseCatcher<ColumnToBiomeSetScript> implements ColumnToBiomeSetScript {

			public Catcher(ScriptUsage usage) {
				super(usage);
			}

			@Override
			public Class<ColumnToBiomeSetScript> getScriptClass() {
				return ColumnToBiomeSetScript.class;
			}

			@Override
			public void addExtraFunctionsToEnvironment(ImplParameters parameters, ExpressionParser parser) {
				super.addExtraFunctionsToEnvironment(parameters, parser);
				parser
				.environment
				.mutable()
				.addVariableLoad("biomes", type(BiomeSet.class))
				.addMethodInvoke(BiomeSet.INFO.addAll)
				.addMethodInvoke(BiomeSet.INFO.removeAll)
				.addMethodInvoke(BiomeSet.INFO.retainAll)
				;
			}

			@Override
			public void addBiomesTo(ScriptedColumn column, BiomeSet biomes) {
				NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
				int used = manager.used;
				try {
					this.script.addBiomesTo(column, biomes);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
				}
				finally {
					manager.used = used;
				}
			}
		}
	}

	public static class BiomeSet extends ObjectOpenHashSet<BiomeEntry> {

		public static final Info INFO = new Info();
		public static class Info extends InfoHolder {

			public MethodInfo addAll, removeAll, retainAll;
		}

		//override to add explicit type check.
		@Override
		public boolean add(BiomeEntry entry) {
			return super.add(entry);
		}

		public boolean addAll(BiomeTag tag) {
			if (tag.isEmpty()) return false;
			boolean changed = false;
			for (BiomeEntry entry : tag) {
				changed |= this.add(entry);
			}
			return changed;
		}

		public boolean removeAll(BiomeTag tag) {
			if (tag.isEmpty()) return false;
			boolean changed = false;
			for (BiomeEntry entry : tag) {
				changed |= this.remove(entry);
			}
			return changed;
		}

		public boolean retainAll(BiomeTag tag) {
			if (tag.isEmpty()) {
				if (this.isEmpty()) return false;
				this.clear();
				return true;
			}
			boolean changed = false;
			ObjectIterator<BiomeEntry> iterator = this.iterator();
			while (iterator.hasNext()) {
				if (!tag.list.contains(iterator.next().entry)) {
					iterator.remove();
					changed = true;
				}
			}
			return changed;
		}
	}
}