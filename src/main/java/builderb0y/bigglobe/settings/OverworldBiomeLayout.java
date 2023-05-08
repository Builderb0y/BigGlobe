package builderb0y.bigglobe.settings;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Predicate;

import com.google.common.base.Predicates;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
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
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.SurfaceDepthWithSlopeScript;
import builderb0y.bigglobe.util.UnregisteredObjectException;

public class OverworldBiomeLayout {

	public static final RegistryKey<OverworldBiomeLayout> ROOT_KEY = RegistryKey.of(BigGlobeDynamicRegistries.OVERWORLD_BIOME_LAYOUT_REGISTRY_KEY, BigGlobeMod.modID("root"));

	public final @VerifyNullable String parent;
	public final ColumnRestriction restrictions;
	public final RegistryEntry<Biome> biome;
	public final @VerifyNullable PrimarySurface primary_surface;
	public final SecondarySurface @SingletonArray @VerifyNullable [] secondary_surfaces;

	public transient OverworldBiomeLayout ifMatch, unlessMatch;
	public transient long nameHash;

	public OverworldBiomeLayout(
		@VerifyNullable String parent,
		ColumnRestriction restrictions,
		RegistryEntry<Biome> biome,
		@VerifyNullable PrimarySurface primary_surface,
		SecondarySurface @VerifyNullable [] secondary_surfaces
	) {
		this.parent = parent;
		this.restrictions = restrictions;
		this.biome = biome;
		this.primary_surface = primary_surface;
		this.secondary_surfaces = secondary_surfaces;
	}

	public OverworldBiomeLayout search(WorldColumn column, double y, long seed, Predicate<OverworldBiomeLayout> filter) {
		OverworldBiomeLayout layout = this, chosen = this;
		while (true) {
			boolean test = layout.restrictions.test(column, y, seed ^ layout.nameHash);
			if (test && filter.test(layout)) chosen = layout;
			OverworldBiomeLayout next = test ? layout.ifMatch : layout.unlessMatch;
			if (next == null) return chosen;
			layout = next;
		}
	}

	public RegistryEntry<Biome> getBiome(WorldColumn column, double y, long seed) {
		return this.search(column, y, seed, Predicates.alwaysTrue()).biome;
	}

	public PrimarySurface getPrimarySurface(WorldColumn column, double y, long seed) {
		return this.search(column, y, seed, layout -> layout.primary_surface != null).primary_surface;
	}

	public SecondarySurface @Nullable [] getSecondarySurfaces(WorldColumn column, double y, long seed) {
		return this.search(column, y, seed, layout -> layout.secondary_surfaces != null).secondary_surfaces;
	}

	public static record PrimarySurface(BlockState top, BlockState under) {}

	public static record SecondarySurface(BlockState under, SurfaceDepthWithSlopeScript.Holder depth) {}

	@Wrapper
	public static class Holder {

		public final RegistryWrapper<OverworldBiomeLayout> registry;
		public final transient OverworldBiomeLayout root;
		public final transient Set<ColumnValue<?>> usedValues;

		public Holder(RegistryWrapper<OverworldBiomeLayout> registry) {
			this.registry = registry;
			this.usedValues = new HashSet<>();
			registry.streamEntries().sequential().forEachOrdered(entry -> {
				RegistryKey<OverworldBiomeLayout> key = UnregisteredObjectException.getKey(entry);
				OverworldBiomeLayout layout = entry.value();
				layout.nameHash = Permuter.permute(0x5DE1C3307454F391L, key.getValue());
				if (key == ROOT_KEY) {
					if (layout.parent != null) throw new IllegalStateException(key + " must have no parent.");
					if (layout.restrictions != ColumnRestriction.EMPTY) throw new IllegalStateException(key + " must have no restrictions.");
					if (layout.primary_surface == null) throw new IllegalStateException(key + " must have a primary surface.");
				}
				else {
					layout.restrictions.forEachValue(this.usedValues::add);
					String parentName = layout.parent;
					if (parentName == null) throw new IllegalStateException(key + " must have a parent.");
					boolean negated = !parentName.isEmpty() && parentName.charAt(0) == '!';
					if (negated) parentName = parentName.substring(1);
					OverworldBiomeLayout parent = registry.getOrThrow(RegistryKey.of(BigGlobeDynamicRegistries.OVERWORLD_BIOME_LAYOUT_REGISTRY_KEY, new Identifier(parentName))).value();
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
			this.root = registry.getOrThrow(ROOT_KEY).value();
			if (BigGlobeConfig.INSTANCE.get().printOverworldBiomeLayoutTree) {
				BigGlobeMod.LOGGER.info(Printer.parse(registry).print(new StringBuilder("Overworld biome layout tree:\n")).toString());
			}
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

		public static Printer parse(RegistryWrapper<OverworldBiomeLayout> registry) {
			Map<OverworldBiomeLayout, Printer> map = new IdentityHashMap<>();
			//map all the layouts to printers.
			registry.streamEntries().sequential().forEachOrdered((RegistryEntry<OverworldBiomeLayout> entry) -> {
				map.put(entry.value(), new Printer(UnregisteredObjectException.getID(entry).toString()));
			});
			//build the hierarchy.
			registry.streamEntries().sequential().forEachOrdered((RegistryEntry<OverworldBiomeLayout> entry) -> {
				OverworldBiomeLayout layout = entry.value();
				Printer printer = map.get(layout);
				if (layout.ifMatch != null) printer.ifMatch = map.get(layout.ifMatch);
				if (layout.unlessMatch != null) printer.unlessMatch = map.get(layout.unlessMatch);
			});
			//assemble metadata based on hierarchy.
			Printer root = map.get(registry.getOrThrow(ROOT_KEY).value());
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