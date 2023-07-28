package builderb0y.bigglobe.settings;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

import builderb0y.autocodec.annotations.SingletonArray;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.columns.restrictions.ColumnRestriction;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.SurfaceDepthWithSlopeScript;
import builderb0y.bigglobe.util.UnregisteredObjectException;

public class BiomeLayout {

	public static final Identifier ROOT_IDENTIFIER = BigGlobeMod.modID("root");

	public final @VerifyNullable String parent;
	public final ColumnRestriction restrictions;
	public final @VerifyNullable RegistryEntry<Biome> biome;
	public final @VerifyNullable PrimarySurface primary_surface;
	public final SecondarySurface @SingletonArray @VerifyNullable [] secondary_surfaces;

	public transient BiomeLayout ifMatch, unlessMatch;
	public transient long nameHash;

	public BiomeLayout(
		@VerifyNullable String parent,
		ColumnRestriction restrictions,
		@VerifyNullable RegistryEntry<Biome> biome,
		@VerifyNullable PrimarySurface primary_surface,
		SecondarySurface @VerifyNullable [] secondary_surfaces
	) {
		this.parent = parent;
		this.restrictions = restrictions;
		this.biome = biome;
		this.primary_surface = primary_surface;
		this.secondary_surfaces = secondary_surfaces;
	}

	public static class OverworldBiomeLayout extends BiomeLayout {

		public final @VerifyNullable Boolean player_spawn_friendly;

		public OverworldBiomeLayout(
			@VerifyNullable String parent,
			ColumnRestriction restrictions,
			RegistryEntry<Biome> biome,
			@VerifyNullable PrimarySurface primary_surface,
			SecondarySurface @VerifyNullable [] secondary_surfaces,
			@VerifyNullable Boolean player_spawn_friendly
		) {
			super(parent, restrictions, biome, primary_surface, secondary_surfaces);
			this.player_spawn_friendly = player_spawn_friendly;
		}
	}

	public static class EndBiomeLayout extends BiomeLayout {

		public EndBiomeLayout(
			@VerifyNullable String parent,
			ColumnRestriction restrictions,
			RegistryEntry<Biome> biome,
			@VerifyNullable PrimarySurface primary_surface,
			SecondarySurface @VerifyNullable [] secondary_surfaces
		) {
			super(parent, restrictions, biome, primary_surface, secondary_surfaces);
		}
	}

	public <R> R search(WorldColumn column, double y, long seed, Function<BiomeLayout, R> property) {
		BiomeLayout layout = this, chosen = this;
		while (true) {
			boolean test = layout.restrictions.test(column, y, seed ^ layout.nameHash);
			if (test && property.apply(layout) != null) chosen = layout;
			BiomeLayout next = test ? layout.ifMatch : layout.unlessMatch;
			if (next == null) return property.apply(chosen);
			layout = next;
		}
	}

	public RegistryEntry<Biome> biome() { return this.biome; }
	public PrimarySurface primarySurface() { return this.primary_surface; }
	public SecondarySurface @Nullable [] secondarySurfaces() { return this.secondary_surfaces; }

	public static record PrimarySurface(BlockState top, BlockState under) {}

	public static record SecondarySurface(BlockState under, SurfaceDepthWithSlopeScript.Holder depth) {}

	public static <T_Layout extends BiomeLayout> RegistryKey<T_Layout> key(BetterRegistry<T_Layout> registry, Identifier id) {
		return RegistryKey.of(registry.getKey(), id);
	}

	@Wrapper
	public static class Holder<T_Layout extends BiomeLayout> {

		public final BetterRegistry<T_Layout> registry;
		public final transient T_Layout root;
		public final transient Set<ColumnValue<?>> usedValues;

		public Holder(BetterRegistry<T_Layout> registry) {
			this.registry = registry;
			this.usedValues = new HashSet<>();
			registry.streamEntries().sequential().forEachOrdered(entry -> {
				RegistryKey<T_Layout> key = UnregisteredObjectException.getKey(entry);
				T_Layout layout = entry.value();
				layout.nameHash = Permuter.permute(0x5DE1C3307454F391L, key.getValue());
				if (key.getValue().equals(ROOT_IDENTIFIER)) {
					if (layout.parent != null) throw new IllegalStateException(key + " must have no parent.");
					if (layout.biome == null) throw new IllegalStateException(key + " must have a biome.");
					if (layout.restrictions != ColumnRestriction.EMPTY) throw new IllegalStateException(key + " must have no restrictions.");
					if (layout.primary_surface == null) throw new IllegalStateException(key + " must have a primary surface.");
				}
				else {
					layout.restrictions.forEachValue(this.usedValues::add);
					String parentName = layout.parent;
					if (parentName == null) throw new IllegalStateException(key + " must have a parent.");
					boolean negated = !parentName.isEmpty() && parentName.charAt(0) == '!';
					if (negated) parentName = parentName.substring(1);
					T_Layout parent = registry.getOrCreateEntry(key(registry, new Identifier(parentName))).value();
					if (negated) {
						if (parent.unlessMatch != null && parent.unlessMatch != layout) {
							throw new IllegalStateException(parentName + " already has a non-matching child.");
						}
						parent.unlessMatch = layout;
					}
					else {
						if (parent.ifMatch != null && parent.ifMatch != layout) {
							throw new IllegalStateException(parentName + " already has a matching child.");
						}
						parent.ifMatch = layout;
					}
				}
			});
			this.root = registry.getOrCreateEntry(key(registry, ROOT_IDENTIFIER)).value();
			if (BigGlobeConfig.INSTANCE.get().printBiomeLayoutTrees) {
				BigGlobeMod.LOGGER.info(Printer.parse(registry).print(new StringBuilder(128).append(registry.getKey().getValue()).append(" tree, as requested in Big Globe's config file:\n")).toString());
			}
		}

		public RegistryEntry<Biome> getBiome(WorldColumn column, double y, long seed) {
			return this.root.search(column, y, seed, BiomeLayout::biome);
		}

		public PrimarySurface getPrimarySurface(WorldColumn column, double y, long seed) {
			return this.root.search(column, y, seed, BiomeLayout::primarySurface);
		}

		public SecondarySurface @Nullable [] getSecondarySurfaces(WorldColumn column, double y, long seed) {
			return this.root.search(column, y, seed, BiomeLayout::secondarySurfaces);
		}
	}

	public static class Printer {

		public String name;
		public Printer ifMatch, unlessMatch;
		public int depth = -1;
		/**
		for each bit: 0 means this is the unlessMatch child of our parent,
		and 1 means this is the ifMatch child of our parent.
		the most significant bit corresponds to this printer's direct parent.
		the least significant bit is for the root node.
		*/
		public BigInteger path = BigInteger.ZERO;

		public Printer(String name) {
			this.name = name;
		}

		public static <T_Layout extends BiomeLayout> Printer parse(BetterRegistry<T_Layout> registry) {
			Map<BiomeLayout, Printer> map = new IdentityHashMap<>();
			//map all the layouts to printers.
			registry.streamEntries().sequential().forEachOrdered((RegistryEntry<? extends BiomeLayout> entry) -> {
				map.put(entry.value(), new Printer(UnregisteredObjectException.getID(entry).toString()));
			});
			//build the hierarchy.
			registry.streamEntries().sequential().forEachOrdered((RegistryEntry<? extends BiomeLayout> entry) -> {
				BiomeLayout layout = entry.value();
				Printer printer = map.get(layout);
				if (layout.ifMatch != null) printer.ifMatch = map.get(layout.ifMatch);
				if (layout.unlessMatch != null) printer.unlessMatch = map.get(layout.unlessMatch);
			});
			//assemble metadata based on hierarchy.
			Printer root = map.get(registry.getOrCreateEntry(key(registry, ROOT_IDENTIFIER)).value());
			root.updateDepthSize(0, BigInteger.ZERO);
			return root;
		}

		public void updateDepthSize(int depth, BigInteger path) {
			this.depth = depth;
			this.path = path;
			if (this.ifMatch != null) {
				this.ifMatch.updateDepthSize(depth + 1, path.setBit(depth));
			}
			if (this.unlessMatch != null) {
				this.unlessMatch.updateDepthSize(depth + 1, path);
			}
		}

		public StringBuilder print(StringBuilder builder) {
			if (this.ifMatch != null) this.ifMatch.print(builder);
			BigInteger bits = this.path.xor(this.path.shiftRight(1));
			for (int index = 0; index < this.depth; index++) {
				builder.append(
					index == this.depth - 1
					? (this.path.testBit(this.depth - 1) ? "┌───" : "└───")
					: (bits.testBit(index) ? "│   " : "    ")
				);
			}
			builder.append(this.name).append('\n');
			if (this.unlessMatch != null) this.unlessMatch.print(builder);
			return builder;
		}
	}
}