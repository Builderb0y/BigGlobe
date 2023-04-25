package builderb0y.bigglobe.scripting.wrappers;

import java.util.List;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockBox;
import net.minecraft.world.gen.structure.Structure;

import builderb0y.scripting.bytecode.TypeInfo;

public record StructureStartWrapper(StructureEntry entry, StructureStart start, BlockBox box) {

	public static final TypeInfo TYPE = TypeInfo.of(StructureStartWrapper.class);

	public static StructureStartWrapper of(RegistryEntry<Structure> entry, StructureStart start) {
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
		for (StructurePiece child : start.getChildren()) {
			BlockBox box = child.getBoundingBox();
			minX = Math.min(minX, box.getMinX());
			minY = Math.min(minY, box.getMinY());
			minZ = Math.min(minZ, box.getMinZ());
			maxX = Math.max(maxX, box.getMaxX());
			maxY = Math.max(maxY, box.getMaxY());
			maxZ = Math.max(maxZ, box.getMaxZ());
		}
		return new StructureStartWrapper(new StructureEntry(entry), start, new BlockBox(minX, minY, minZ, maxX, maxY, maxZ));
	}

	public int minX() { return this.box.getMinX(); }
	public int minY() { return this.box.getMinY(); }
	public int minZ() { return this.box.getMinZ(); }
	public int maxX() { return this.box.getMaxX(); }
	public int maxY() { return this.box.getMaxY(); }
	public int maxZ() { return this.box.getMaxZ(); }
	public double midX() { return (this.box.getMinX() + this.box.getMaxX()) * 0.5D; }
	public double midY() { return (this.box.getMinY() + this.box.getMaxY()) * 0.5D; }
	public double midZ() { return (this.box.getMinZ() + this.box.getMaxZ()) * 0.5D; }
	public int sizeX() { return this.box.getMaxX() - this.box.getMinX(); }
	public int sizeY() { return this.box.getMaxY() - this.box.getMinY(); }
	public int sizeZ() { return this.box.getMaxZ() - this.box.getMinZ(); }

	public StructureEntry structure() {
		return this.entry;
	}

	public List<StructurePiece> pieces() {
		return this.start.getChildren();
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