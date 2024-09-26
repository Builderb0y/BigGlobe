package builderb0y.bigglobe.structures.scripted;

import java.util.Optional;
import java.util.random.RandomGenerator;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.structure.StructurePiecesCollector;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.structure.StructureType;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.scripted.ColumnScript.ColumnToIntScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Params;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Purpose;
import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.bigglobe.compat.DistantHorizonsCompat;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.wrappers.StructurePlacementScriptEntry;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper.Coordination;
import builderb0y.bigglobe.structures.BigGlobeStructure;
import builderb0y.bigglobe.structures.BigGlobeStructures;
import builderb0y.bigglobe.structures.RawGenerationStructure;
import builderb0y.bigglobe.util.*;
import builderb0y.bigglobe.util.WorldOrChunk.ChunkDelegator;
import builderb0y.bigglobe.util.WorldOrChunk.WorldDelegator;

public class ScriptedStructure extends BigGlobeStructure implements RawGenerationStructure {

	#if MC_VERSION >= MC_1_20_5
		public static final MapCodec<ScriptedStructure> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(ScriptedStructure.class);
	#else
		public static final Codec<ScriptedStructure> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(ScriptedStructure.class).codec();
	#endif

	public final StructureLayoutScript.Holder layout;

	public ScriptedStructure(Config config, StructureLayoutScript.Holder layout) {
		super(config, null);
		this.layout = layout;
	}

	@Override
	public Optional<StructurePosition> getStructurePosition(Context context) {
		if (!(context.chunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator)) return Optional.empty();
		Permuter permuter = Permuter.from(context.random());
		int x = context.chunkPos().getStartX() | permuter.nextInt(16);
		int z = context.chunkPos().getStartZ() | permuter.nextInt(16);
		boolean distantHorizons = DistantHorizonsCompat.isOnDistantHorizonThread();
		ScriptedColumnLookup lookup = new ScriptedColumnLookup.Impl(generator.columnEntryRegistry.columnFactory, new Params(generator.columnSeed, 0, 0, context.world(), Purpose.generic(distantHorizons), generator.compiledWorldTraits));
		CheckedList<StructurePiece> pieces = new CheckedList<>(StructurePiece.class);
		this.layout.layout(lookup, x, z, generator.columnSeed, permuter, pieces, distantHorizons);
		StructurePiecesCollector collector = new StructurePiecesCollector();
		int minY = Integer.MAX_VALUE;
		int maxY = Integer.MIN_VALUE;
		for (StructurePiece piece : pieces) {
			collector.addPiece(piece);
			minY = Math.min(minY, piece.getBoundingBox().getMinY());
			maxY = Math.max(maxY, piece.getBoundingBox().getMaxY());
		}
		return Optional.of(
			new StructurePosition(
				new BlockPos(x, (maxY + minY + 1) >> 1, z),
				Either.right(collector)
			)
		);
	}

	@Override
	public StructureType<?> getType() {
		return BigGlobeStructures.SCRIPTED;
	}

	public static record CombinedStructureScripts(
		StructurePlacementScript.Holder placement,
		StructurePlacementScript.@VerifyNullable Holder raw_placement
	) {}

	public static class Piece extends StructurePiece implements RawGenerationStructurePiece {

		public final BlockBox originalBoundingBox;
		public SymmetricOffset transformation;
		public final StructurePlacementScriptEntry placement;
		public final NbtCompound data;

		@Override
		public String toString() {
			return "ScriptedStructurePiece: { original bounding box: " + this.originalBoundingBox + ", current bounding box: " + this.boundingBox + ", transformation: " + this.transformation + ", placement: " + this.placement.id() + ", data: " + this.data + " }";
		}

		public Piece(StructurePieceType type, BlockBox boundingBox, StructurePlacementScriptEntry placement, NbtCompound data) {
			super(type, 0, boundingBox);
			this.originalBoundingBox = boundingBox;
			this.placement = placement;
			this.data = data;
			this.transformation = SymmetricOffset.IDENTITY;
		}

		/** this is the constructor that the layout script uses. */
		public Piece(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, StructurePlacementScriptEntry placement, NbtCompound data) {
			this(BigGlobeStructures.SCRIPTED_PIECE, new BlockBox(minX, minY, minZ, maxX, maxY, maxZ), placement, data);
		}

		public Piece(StructurePieceType type, StructureContext context, NbtCompound nbt) {
			super(type, nbt);
			this.originalBoundingBox = WorldUtil.blockBoxFromIntArray(nbt.getIntArray("OBB"));
			NbtElement transform = nbt.get("transform");
			if (transform != null) try {
				this.transformation = BigGlobeAutoCodec.AUTO_CODEC.decode(SymmetricOffset.CODER, transform, NbtOps.INSTANCE);
			}
			catch (DecodeException exception) {
				throw new RuntimeException(exception);
			}
			else {
				this.transformation = SymmetricOffset.IDENTITY;
			}
			BlockRotation legacyRotation = Directions.ROTATIONS[nbt.getByte("rot")];
			if (legacyRotation != BlockRotation.NONE) {
				this.transformation = this.transformation.rotateAround(
					(this.originalBoundingBox.getMinX() + this.originalBoundingBox.getMaxX() + 1) >> 1,
					(this.originalBoundingBox.getMinZ() + this.originalBoundingBox.getMaxZ() + 1) >> 1,
					Symmetry.of(legacyRotation)
				);
				this.updateBoundingBox();
			}
			this.placement = StructurePlacementScriptEntry.of(nbt.getString("script"));
			this.data = nbt.getCompound("data");
		}

		@Override
		public void writeNbt(StructureContext context, NbtCompound nbt) {
			nbt.putString("script", this.placement.id());
			nbt.put("data", this.data);
			nbt.put("transform", BigGlobeAutoCodec.AUTO_CODEC.encode(SymmetricOffset.CODER, this.transformation, NbtOps.INSTANCE));
			nbt.put("OBB", WorldUtil.blockBoxToNbt(this.originalBoundingBox));
		}

		public Piece symmetrify(Symmetry symmetry) {
			return this.symmetrifyAround(
				(this.boundingBox.getMinX() + this.boundingBox.getMaxX() + 1) >> 1,
				(this.boundingBox.getMinZ() + this.boundingBox.getMaxZ() + 1) >> 1,
				symmetry
			);
		}

		public Piece withRotation(int rotation) {
			return this.symmetrify(Symmetry.rotation(rotation));
		}

		public Piece rotateAround(int x, int z, int rotation) {
			return this.symmetrifyAround(x, z, Symmetry.rotation(rotation));
		}

		public Piece symmetrifyAround(int x, int z, Symmetry symmetry) {
			this.setTransformation(this.transformation.rotateAround(x, z, symmetry));
			return this;
		}

		public Piece rotateRandomly(RandomGenerator random) {
			return this.symmetrify(Symmetry.VALUES[random.nextInt(4)]);
		}

		public Piece rotateAndFlipRandomly(RandomGenerator random) {
			return this.symmetrify(Symmetry.VALUES[random.nextInt(8)]);
		}

		public Piece offset(int x, int y, int z) {
			this.translate(x, y, z);
			return this;
		}

		@Override
		public void translate(int x, int y, int z) {
			this.setTransformation(this.transformation.offset(x, y, z));
		}

		public Symmetry symmetry() {
			return this.transformation.symmetry();
		}

		public int offsetX() {
			return this.transformation.offsetX();
		}

		public int offsetY() {
			return this.transformation.offsetY();
		}

		public int offsetZ() {
			return this.transformation.offsetZ();
		}

		public StructurePlacementScriptEntry placement() {
			return this.placement;
		}

		public void setTransformation(SymmetricOffset transformation) {
			this.transformation = transformation;
			this.updateBoundingBox();
		}

		public void updateBoundingBox() {
			BlockPos.Mutable pos1 = Coordination.rotate(
				new BlockPos.Mutable(
					this.originalBoundingBox.getMinX(),
					this.originalBoundingBox.getMinY(),
					this.originalBoundingBox.getMinZ()
				),
				this.transformation
			);
			BlockPos.Mutable pos2 = Coordination.rotate(
				new BlockPos.Mutable(
					this.originalBoundingBox.getMaxX(),
					this.originalBoundingBox.getMaxY(),
					this.originalBoundingBox.getMaxZ()
				),
				this.transformation
			);
			this.boundingBox = WorldUtil.createBlockBox(pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ());
		}

		@Override
		public void generateRaw(Context context) {
			StructurePlacementScript.Holder rawPlacement = this.placement.entry().value().raw_placement;
			if (rawPlacement == null) return;
			int minX = this.originalBoundingBox.getMinX();
			int minY = this.originalBoundingBox.getMinY();
			int minZ = this.originalBoundingBox.getMinZ();
			int maxX = this.originalBoundingBox.getMaxX();
			int maxY = this.originalBoundingBox.getMaxY();
			int maxZ = this.originalBoundingBox.getMaxZ();
			int midX = (minX + maxX + 1) >> 1;
			int midY = (minY + maxY + 1) >> 1;
			int midZ = (minZ + maxZ + 1) >> 1;
			BlockBox chunkBox = WorldUtil.chunkBox(context.chunk);
			int effectiveMinX = Math.max(this.boundingBox.getMinX(), chunkBox.getMinX());
			int effectiveMinY = Math.max(this.boundingBox.getMinY(), chunkBox.getMinY());
			int effectiveMinZ = Math.max(this.boundingBox.getMinZ(), chunkBox.getMinZ());
			int effectiveMaxX = Math.min(this.boundingBox.getMaxX(), chunkBox.getMaxX());
			int effectiveMaxY = Math.min(this.boundingBox.getMaxY(), chunkBox.getMaxY());
			int effectiveMaxZ = Math.min(this.boundingBox.getMaxZ(), chunkBox.getMaxZ());
			rawPlacement.place(
				new WorldWrapper(
					new ChunkDelegator(
						context.chunk,
						context.columnSeed
					),
					context.generator,
					new Permuter(context.pieceSeed),
					new Coordination(
						this.transformation,
						new BlockBox(
							effectiveMinX,
							effectiveMinY,
							effectiveMinZ,
							effectiveMaxX,
							effectiveMaxY,
							effectiveMaxZ
						),
						chunkBox
					),
					Purpose.rawGeneration(context.distantHorizons)
				),
				minX, minY, minZ,
				maxX, maxY, maxZ,
				midX, midY, midZ,
				chunkBox.getMinX(),
				chunkBox.getMinY(),
				chunkBox.getMinZ(),
				chunkBox.getMaxX(),
				chunkBox.getMaxY(),
				chunkBox.getMaxZ(),
				this
			);
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
			if (!(chunkGenerator instanceof BigGlobeScriptedChunkGenerator generator)) return;
			int minX = this.originalBoundingBox.getMinX();
			int minY = this.originalBoundingBox.getMinY();
			int minZ = this.originalBoundingBox.getMinZ();
			int maxX = this.originalBoundingBox.getMaxX();
			int maxY = this.originalBoundingBox.getMaxY();
			int maxZ = this.originalBoundingBox.getMaxZ();
			int midX = (minX + maxX + 1) >> 1;
			int midY = (minY + maxY + 1) >> 1;
			int midZ = (minZ + maxZ + 1) >> 1;
			int effectiveMinX = Math.max(this.boundingBox.getMinX(), chunkBox.getMinX());
			int effectiveMinY = Math.max(this.boundingBox.getMinY(), chunkBox.getMinY());
			int effectiveMinZ = Math.max(this.boundingBox.getMinZ(), chunkBox.getMinZ());
			int effectiveMaxX = Math.min(this.boundingBox.getMaxX(), chunkBox.getMaxX());
			int effectiveMaxY = Math.min(this.boundingBox.getMaxY(), chunkBox.getMaxY());
			int effectiveMaxZ = Math.min(this.boundingBox.getMaxZ(), chunkBox.getMaxZ());

			Permuter permuter = Permuter.from(random);

			this.placement.object().placement.place(
				new WorldWrapper(
					new WorldDelegator(world),
					generator,
					permuter,
					new Coordination(
						this.transformation,
						new BlockBox(
							effectiveMinX,
							effectiveMinY,
							effectiveMinZ,
							effectiveMaxX,
							effectiveMaxY,
							effectiveMaxZ
						),
						WorldUtil.surroundingChunkBox(chunkPos, world)
					),
					Purpose.features()
				),
				minX, minY, minZ,
				maxX, maxY, maxZ,
				midX, midY, midZ,
				chunkBox.getMinX(),
				chunkBox.getMinY(),
				chunkBox.getMinZ(),
				chunkBox.getMaxX(),
				chunkBox.getMaxY(),
				chunkBox.getMaxZ(),
				this
			);
		}
	}
}