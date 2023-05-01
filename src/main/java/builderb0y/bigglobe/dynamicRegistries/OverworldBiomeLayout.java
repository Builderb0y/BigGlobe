package builderb0y.bigglobe.dynamicRegistries;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.columns.restrictions.ColumnRestriction;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.util.UnregisteredObjectException;

public class OverworldBiomeLayout {

	public static final RegistryKey<OverworldBiomeLayout> ROOT = RegistryKey.of(BigGlobeDynamicRegistries.OVERWORLD_BIOME_LAYOUT_REGISTRY_KEY, BigGlobeMod.modID("root"));

	public final @VerifyNullable String parent;
	public final ColumnRestriction restrictions;
	public final RegistryEntry<Biome> biome;
	public transient OverworldBiomeLayout ifMatch, unlessMatch;
	public transient long nameHash;

	public OverworldBiomeLayout(@VerifyNullable String parent, ColumnRestriction restrictions, RegistryEntry<Biome> biome) {
		this.parent = parent;
		this.restrictions = restrictions;
		this.biome = biome;
	}

	public RegistryEntry<Biome> getBiome(WorldColumn column, int y, long seed) {
		OverworldBiomeLayout layout = this, chosen = this;
		while (true) {
			boolean test = layout.restrictions.test(column, y, seed ^ layout.nameHash);
			if (test) chosen = layout;
			OverworldBiomeLayout next = test ? layout.ifMatch : layout.unlessMatch;
			if (next == null) return chosen.biome;
			layout = next;
		}
	}

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
				if (key == ROOT) {
					if (layout.parent != null) throw new IllegalStateException(key + " must have no parent.");
					if (layout.restrictions != ColumnRestriction.EMPTY) throw new IllegalStateException("root must have no restrictions.");
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
			this.root = registry.getOrThrow(ROOT).value();
		}
	}
}