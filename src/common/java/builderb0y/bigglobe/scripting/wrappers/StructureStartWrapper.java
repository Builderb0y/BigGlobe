package builderb0y.bigglobe.scripting.wrappers;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import builderb0y.autocodec.util.ObjectArrayFactory;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.scripting.wrappers.entries.StructureEntry;
import builderb0y.bigglobe.scripting.wrappers.tags.BiomeTag;
import builderb0y.bigglobe.structures.DelegatingStructure;
import builderb0y.bigglobe.util.DelayedEntryList;
import builderb0y.scripting.bytecode.TypeInfo;

public record StructureStartWrapper(
	StructureEntry entry,
	StructureStart start,
	BoundingBox box
) {

	public static final ObjectArrayFactory<StructureStartWrapper> ARRAY_FACTORY = new ObjectArrayFactory<>(StructureStartWrapper.class);
	public static final TypeInfo TYPE = TypeInfo.of(StructureStartWrapper.class);

	public static StructureStartWrapper of(Holder<Structure> original, StructureStart start) {
		//the bounding box of the start might be expanded,
		//but we don't want to expose that expansion to scripts.
		//so, re-calculate the size.
		int
			minX = Integer.MAX_VALUE,
			minY = Integer.MAX_VALUE,
			minZ = Integer.MAX_VALUE,
			maxX = Integer.MIN_VALUE,
			maxY = Integer.MIN_VALUE,
			maxZ = Integer.MIN_VALUE;
		for (StructurePiece child : start.getPieces()) {
			BoundingBox box = child.getBoundingBox();
			minX = Math.min(minX, box.minX());
			minY = Math.min(minY, box.minY());
			minZ = Math.min(minZ, box.minZ());
			maxX = Math.max(maxX, box.maxX());
			maxY = Math.max(maxY, box.maxY());
			maxZ = Math.max(maxZ, box.maxZ());
		}
		Holder<Structure> entry = original;
		while (entry.value() instanceof DelegatingStructure delegating) {
			entry = delegating.delegate();
		}
		return new StructureStartWrapper(
			new StructureEntry(
				entry,
				new BiomeTag(
					new DelayedEntryList<>(
						BigGlobeMod.getRegistry(Registries.BIOME),
						original.value().biomes()
					)
				),
				original.value().step(),
				original.value().terrainAdaptation()
			),
			start,
			new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ)
		);
	}

	public int minX() {
		return this.box.minX();
	}

	public int minY() {
		return this.box.minY();
	}

	public int minZ() {
		return this.box.minZ();
	}

	public int maxX() {
		return this.box.maxX();
	}

	public int maxY() {
		return this.box.maxY();
	}

	public int maxZ() {
		return this.box.maxZ();
	}

	public int midX() {
		return (this.box.minX() + this.box.maxX() + 1) >> 1;
	}

	public int midY() {
		return (this.box.minY() + this.box.maxY() + 1) >> 1;
	}

	public int midZ() {
		return (this.box.minZ() + this.box.maxZ() + 1) >> 1;
	}

	public int sizeX() {
		return this.box.maxX() - this.box.minX();
	}

	public int sizeY() {
		return this.box.maxY() - this.box.minY();
	}

	public int sizeZ() {
		return this.box.maxZ() - this.box.minZ();
	}

	public StructureEntry structure() {
		return this.entry;
	}

	public List<StructurePiece> pieces() {
		return this.start.getPieces();
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (
			obj instanceof StructureStartWrapper that &&
			this.start.equals(that.start)
		);
	}

	@Override
	public int hashCode() {
		return this.start.hashCode();
	}

	@Override
	public String toString() {
		return "StructureStart" + this.pieces();
	}
}