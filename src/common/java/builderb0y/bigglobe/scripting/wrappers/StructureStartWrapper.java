package builderb0y.bigglobe.scripting.wrappers;

import java.util.Comparator;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import builderb0y.autocodec.util.ObjectArrayFactory;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.scripting.wrappers.entries.StructureEntry;
import builderb0y.bigglobe.scripting.wrappers.tags.BiomeTag;
import builderb0y.bigglobe.structures.DelegatingStructure;
import builderb0y.bigglobe.util.DelayedEntryList;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.TypeInfo;

public record StructureStartWrapper(
	Holder<Structure> originalStructure,
	StructureEntry structure,
	StructureStart start,
	MutableBlockPos pos,
	BoundingBox box
) {

	public static final ObjectArrayFactory<StructureStartWrapper> ARRAY_FACTORY = new ObjectArrayFactory<>(StructureStartWrapper.class);
	public static final TypeInfo TYPE = TypeInfo.of(StructureStartWrapper.class);

	public static StructureStartWrapper of(Holder<Structure> original, StructureStart start, BlockPos pos) {
		if (!start.isValid()) throw new IllegalArgumentException("Attempt to wrap invalid structure start");
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
			original,
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
			pos.mutable(),
			new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ)
		);
	}

	public static Comparator<StructureStartWrapper> compareByClosestTo(BlockPos center) {
		return Comparator.comparingLong(
			(StructureStartWrapper start) -> BigGlobeMath.squareL(
				start.pos.getX() - center.getX(),
				start.pos.getY() - center.getY(),
				start.pos.getZ() - center.getZ()
			)
		);
	}

	public int minX() { return this.box.minX(); }
	public int minY() { return this.box.minY(); }
	public int minZ() { return this.box.minZ(); }
	public int maxX() { return this.box.maxX(); }
	public int maxY() { return this.box.maxY(); }
	public int maxZ() { return this.box.maxZ(); }
	public int midX() { return (this.box.minX() + this.box.maxX() + 1) >> 1; }
	public int midY() { return (this.box.minY() + this.box.maxY() + 1) >> 1; }
	public int midZ() { return (this.box.minZ() + this.box.maxZ() + 1) >> 1; }
	public int sizeX() { return this.box.maxX() - this.box.minX() + 1; }
	public int sizeY() { return this.box.maxY() - this.box.minY() + 1; }
	public int sizeZ() { return this.box.maxZ() - this.box.minZ() + 1; }

	@SuppressWarnings("deprecation")
	public void move(int dx, int dy, int dz) {
		this.pos.move(dx, dy, dz);
		for (StructurePiece piece : this.start.getPieces()) {
			piece.move(dx, dy, dz);
		}
		this.start.getBoundingBox().move(dx, dy, dz);
	}

	public BlockPos clampedPos() {
		int x = Mth.clamp(this.pos.getX(), this.box.minX(), this.box.maxX());
		int y = Mth.clamp(this.pos.getY(), this.box.minY(), this.box.maxY());
		int z = Mth.clamp(this.pos.getZ(), this.box.minZ(), this.box.maxZ());
		return this.pos.getX() == x && this.pos.getY() == y && this.pos.getZ() == z ? this.pos : new BlockPos(x, y, z);
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

	public Identifier originalID() {
		return UnregisteredObjectException.getID(this.originalStructure);
	}

	@Override
	public String toString() {
		return this.originalID() + " (" + this.structure.id() + ") at " + this.box;
	}
}