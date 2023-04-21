package builderb0y.bigglobe.structures.scripted;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;

import net.minecraft.block.Block;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.structure.StructureType;

import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.mixins.StructurePiece_DirectRotationSetter;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.wrappers.StructurePlacementScriptEntry;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.structures.BigGlobeStructure;
import builderb0y.bigglobe.structures.BigGlobeStructures;
import builderb0y.bigglobe.util.Directions;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.bigglobe.util.coordinators.Coordinator;

public class ScriptedStructure extends BigGlobeStructure {

	public static final Codec<ScriptedStructure> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(ScriptedStructure.class);

	public final StructureLayoutScript.Holder layout;

	public ScriptedStructure(Config config, StructureLayoutScript.Holder layout) {
		super(config);
		this.layout = layout;
	}

	@Override
	public Optional<StructurePosition> getStructurePosition(Context context) {
		Permuter permuter = Permuter.from(context.random());
		int x = context.chunkPos().getStartX() | permuter.nextInt(16);
		int z = context.chunkPos().getStartZ() | permuter.nextInt(16);
		WorldColumn column = WorldColumn.forGenerator(context.seed(), context.chunkGenerator(), context.noiseConfig(), x, z);
		int y = column.getFinalTopHeightI();
		return Optional.of(
			new StructurePosition(
				new BlockPos(x, y, z),
				collector -> {
					List<StructurePiece> pieces = Collections.checkedList(new ArrayList<>(), StructurePiece.class);
					this.layout.layout(x, z, permuter, column, pieces);
					for (StructurePiece piece : pieces) {
						collector.addPiece(piece);
					}
				}
			)
		);
	}

	@Override
	public StructureType<?> getType() {
		return BigGlobeStructures.SCRIPTED;
	}

	public static class Piece extends StructurePiece {

		public final StructurePlacementScriptEntry placement;
		public final NbtCompound data;

		public Piece(StructurePieceType type, BlockBox boundingBox, StructurePlacementScriptEntry placement, NbtCompound data) {
			super(type, 0, boundingBox);
			this.placement = placement;
			this.data = data;
		}

		/** this is the constructor that the layout script uses. */
		public Piece(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, StructurePlacementScriptEntry placement, NbtCompound data) {
			this(BigGlobeStructures.SCRIPTED_PIECE, new BlockBox(minX, minY, minZ, maxX, maxY, maxZ), placement, data);
		}

		public Piece(StructurePieceType type, NbtCompound nbt) {
			super(type, nbt);
			this.placement = StructurePlacementScriptEntry.of(nbt.getString("script"));
			this.data = nbt.getCompound("data");
		}

		@Override
		public void writeNbt(StructureContext context, NbtCompound nbt) {
			nbt.putString("script", UnregisteredObjectException.getKey(this.placement.entry()).getValue().toString());
			nbt.put("data", this.data);
		}

		public Piece withRotation(int rotation) {
			((StructurePiece_DirectRotationSetter)(this)).bigglobe_setRotationDirect(
				Directions.scriptRotation(rotation)
			);
			return this;
		}

		@Override
		public void generate(
			StructureWorldAccess world,
			StructureAccessor structureAccessor,
			ChunkGenerator chunkGenerator,
			Random random,
			BlockBox chunkBox,
			ChunkPos chunkPos,
			BlockPos pivot
		) {
			int minX = Math.max(this.boundingBox.getMinX(), chunkBox.getMinX());
			int minY = Math.max(this.boundingBox.getMinY(), chunkBox.getMinY());
			int minZ = Math.max(this.boundingBox.getMinZ(), chunkBox.getMinZ());
			int maxX = Math.min(this.boundingBox.getMaxX(), chunkBox.getMaxX());
			int maxY = Math.min(this.boundingBox.getMaxY(), chunkBox.getMaxY());
			int maxZ = Math.min(this.boundingBox.getMaxZ(), chunkBox.getMaxZ());
			Coordinator coordinator = (
				Coordinator.forWorld(world, Block.NOTIFY_ALL)
				.inBox(minX, minY, minZ, maxX, maxY, maxZ)
			);

			minX = this.boundingBox.getMinX();
			minY = this.boundingBox.getMinY();
			minZ = this.boundingBox.getMinZ();
			maxX = this.boundingBox.getMaxX();
			maxY = this.boundingBox.getMaxY();
			maxZ = this.boundingBox.getMaxZ();
			int midX = (minX + maxX + 1) >> 1;
			int midY = (minY + maxY + 1) >> 1;
			int midZ = (minZ + maxZ + 1) >> 1;

			if (this.getRotation() != null && this.getRotation() != BlockRotation.NONE) {
				coordinator = (
					coordinator
					.translate(midX, midY, midZ)
					.rotate1x(this.getRotation())
					.translate(-midX, -midY, -midZ)
				);
			}
			Permuter permuter = Permuter.from(random);
			WorldColumn column = WorldColumn.forWorld(world, 0, 0);

			this.placement.entry().value().place(
				new WorldWrapper(world, coordinator, permuter),
				column,
				minX, minY, minZ,
				maxX, maxY, maxZ,
				midX, midY, midZ,
				this.data
			);
		}
	}
}