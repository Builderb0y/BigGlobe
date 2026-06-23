package builderb0y.bigglobe.structures.scripted;

import java.util.Map;
import java.util.Optional;
import java.util.random.RandomGenerator;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import builderb0y.autocodec.annotations.DefaultInt;
import builderb0y.autocodec.annotations.ForceOrdinal;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.codecs.UseSuperClass;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.bigglobe.mixinInterfaces.NbtCompoundExtensions;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper.Coordination;
import builderb0y.bigglobe.scripting.wrappers.entries.StructurePlacementScriptEntry;
import builderb0y.bigglobe.structures.BigGlobeStructure;
import builderb0y.bigglobe.structures.BigGlobeStructures;
import builderb0y.bigglobe.structures.RawGenerationStructure;
import builderb0y.bigglobe.util.CheckedList;
import builderb0y.bigglobe.util.CheckedList.NullPolicy;
import builderb0y.bigglobe.util.SymmetricOffset;
import builderb0y.bigglobe.util.Symmetry;
import builderb0y.bigglobe.util.WorldOrChunk.ChunkDelegator;
import builderb0y.bigglobe.util.WorldOrChunk.WorldDelegator;
import builderb0y.bigglobe.util.WorldUtil;

public class ScriptedStructure extends BigGlobeStructure implements RawGenerationStructure {

	public static final MapCodec<ScriptedStructure> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(ScriptedStructure.class);

	public final StructureLayoutScript.Catcher layout;
	public final @DefaultInt(8) int max_radius_in_chunks;

	public ScriptedStructure(StructureSettings config, StructureLayoutScript.Catcher layout, int max_radius_in_chunks) {
		super(config, null, null);
		this.layout = layout;
		this.max_radius_in_chunks = max_radius_in_chunks;
	}

	@Override
	public int bigglobe_getMaxRadiusInChunks() {
		return this.max_radius_in_chunks;
	}

	@Override
	public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		if (!(context.chunkGenerator() instanceof BigGlobeScriptedChunkGenerator generator)) return Optional.empty();
		long seed = context.random().nextLong();
		int x = context.chunkPos().getMinBlockX() | context.random().nextInt(16);
		int z = context.chunkPos().getMinBlockZ() | context.random().nextInt(16);
		int y = Mth.clamp(generator.getFirstOccupiedHeight(x, z, Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState()), generator.height.min_y(), generator.height.max_y() - 1);
		return Optional.of(
			new GenerationStub(
				new BlockPos(x, y, z),
				(StructurePiecesBuilder collector) -> {
					Permuter permuter = new Permuter(seed);
					ScriptedColumnLookup lookup = new ScriptedColumnLookup.Impl(
						generator.configuredColumnFactory(
							context.heightAccessor(),
							ColumnUsage.GENERIC.maybeDhHints()
						)
					);
					CheckedList<StructurePiece> pieces = new CheckedList<>(StructurePiece.class, NullPolicy.THROW);
					this.layout.layout(lookup, x, z, generator.columnSeed, permuter, pieces);
					int minY = Integer.MAX_VALUE;
					int maxY = Integer.MIN_VALUE;
					for (StructurePiece piece : pieces) {
						collector.addPiece(piece);
						minY = Math.min(minY, piece.getBoundingBox().minY());
						maxY = Math.max(maxY, piece.getBoundingBox().maxY());
					}
				}
			)
		);
	}

	@Override
	public StructureType<?> type() {
		return BigGlobeStructures.SCRIPTED;
	}

	public static record CombinedStructureScripts(
		StructurePlacementScript.Catcher placement,
		StructurePlacementScript.@VerifyNullable Catcher raw_placement
	) {}

	public static class Piece extends StructurePiece implements RawGenerationStructurePiece {

		public final BoundingBox originalBoundingBox;
		public SymmetricOffset transformation;
		public final StructurePlacementScriptEntry placement;
		public final CompoundTag data;

		@Override
		public String toString() {
			return "ScriptedStructurePiece: { original bounding box: " + this.originalBoundingBox + ", current bounding box: " + this.boundingBox + ", transformation: " + this.transformation + ", placement: " + this.placement.id() + ", data: " + this.data + " }";
		}

		public Piece(StructurePieceType type, BoundingBox boundingBox, StructurePlacementScriptEntry placement, CompoundTag data) {
			super(type, 0, boundingBox);
			this.originalBoundingBox = boundingBox;
			this.placement = placement;
			this.data = data;
			this.transformation = SymmetricOffset.IDENTITY;
		}

		/**
		this is the constructor that the layout script uses.
		*/
		public Piece(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, StructurePlacementScriptEntry placement, CompoundTag data) {
			this(BigGlobeStructures.SCRIPTED_PIECE, new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ), placement, data);
		}

		public Piece(StructurePieceType type, StructurePieceSerializationContext context, CompoundTag nbt) {
			super(type, nbt);
			SerialData data;
			try {
				data = BigGlobeAutoCodec.AUTO_CODEC.decode(SerialData.CODER, nbt, NbtOps.INSTANCE);
			}
			catch (DecodeException exception) {
				throw new RuntimeException(exception);
			}
			this.originalBoundingBox = data.OBB;
			this.transformation = (
				data.transform != null
					? data.transform
					: SymmetricOffset.IDENTITY
			);
			if (data.rot != null && data.rot != Rotation.NONE) {
				this.transformation = this.transformation.rotateAround(
					(this.originalBoundingBox.minX() + this.originalBoundingBox.maxX() + 1) >> 1,
					(this.originalBoundingBox.minZ() + this.originalBoundingBox.maxZ() + 1) >> 1,
					Symmetry.of(data.rot)
				);
			}
			this.updateBoundingBox();
			this.placement = StructurePlacementScriptEntry.of(data.script, 0);
			this.data = data.data;
		}

		@Override
		public void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
			for (Map.Entry<String, Tag> entry : ((NbtCompoundExtensions)(Object)(BigGlobeAutoCodec.AUTO_CODEC.encode(SerialData.CODER, this.serialize(), NbtOps.INSTANCE))).bigglobe_getEntrySet()) {
				nbt.put(entry.getKey(), entry.getValue());
			}
		}

		public SerialData serialize() {
			return new SerialData(
				this.placement.id(),
				this.transformation,
				null,
				this.originalBoundingBox,
				this.data
			);
		}

		public static record SerialData(
			String script,
			@VerifyNullable SymmetricOffset transform,
			@VerifyNullable @ForceOrdinal(true) Rotation rot,
			BoundingBox OBB,
			@UseSuperClass(Tag.class) CompoundTag data
		) {

			public static final AutoCoder<SerialData> CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(SerialData.class);
		}

		public Piece symmetrify(Symmetry symmetry) {
			return this.symmetrifyAround(
				(this.boundingBox.minX() + this.boundingBox.maxX() + 1) >> 1,
				(this.boundingBox.minZ() + this.boundingBox.maxZ() + 1) >> 1,
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

		public Piece rotateRandomly(long seed) {
			return this.symmetrify(Symmetry.VALUES[Permuter.nextBoundedInt(seed, 4)]);
		}

		public Piece rotateAndFlipRandomly(long seed) {
			return this.symmetrify(Symmetry.VALUES[Permuter.nextBoundedInt(seed, 8)]);
		}

		public Piece offset(int x, int y, int z) {
			this.move(x, y, z);
			return this;
		}

		@Override
		public void move(int x, int y, int z) {
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
			BlockPos.MutableBlockPos pos1 = Coordination.rotate(
				new BlockPos.MutableBlockPos(
					this.originalBoundingBox.minX(),
					this.originalBoundingBox.minY(),
					this.originalBoundingBox.minZ()
				),
				this.transformation
			);
			BlockPos.MutableBlockPos pos2 = Coordination.rotate(
				new BlockPos.MutableBlockPos(
					this.originalBoundingBox.maxX(),
					this.originalBoundingBox.maxY(),
					this.originalBoundingBox.maxZ()
				),
				this.transformation
			);
			this.boundingBox = WorldUtil.createBlockBox(pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ());
		}

		@Override
		public void generateRaw(Context context) {
			StructurePlacementScript.Catcher rawPlacement = this.placement.entry.value().raw_placement;
			if (rawPlacement == null) return;
			int minX = this.originalBoundingBox.minX();
			int minY = this.originalBoundingBox.minY();
			int minZ = this.originalBoundingBox.minZ();
			int maxX = this.originalBoundingBox.maxX();
			int maxY = this.originalBoundingBox.maxY();
			int maxZ = this.originalBoundingBox.maxZ();
			int midX = (minX + maxX + 1) >> 1;
			int midY = (minY + maxY + 1) >> 1;
			int midZ = (minZ + maxZ + 1) >> 1;
			BoundingBox chunkBox = WorldUtil.chunkBox(context.chunk);
			int effectiveMinX = Math.max(this.boundingBox.minX(), chunkBox.minX());
			int effectiveMinY = Math.max(this.boundingBox.minY(), chunkBox.minY());
			int effectiveMinZ = Math.max(this.boundingBox.minZ(), chunkBox.minZ());
			int effectiveMaxX = Math.min(this.boundingBox.maxX(), chunkBox.maxX());
			int effectiveMaxY = Math.min(this.boundingBox.maxY(), chunkBox.maxY());
			int effectiveMaxZ = Math.min(this.boundingBox.maxZ(), chunkBox.maxZ());
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
						new BoundingBox(
							effectiveMinX,
							effectiveMinY,
							effectiveMinZ,
							effectiveMaxX,
							effectiveMaxY,
							effectiveMaxZ
						),
						chunkBox
					),
					ColumnUsage.RAW_GENERATION.maybeDhHints(context.distantHorizons)
				),
				minX, minY, minZ,
				maxX, maxY, maxZ,
				midX, midY, midZ,
				chunkBox.minX(),
				chunkBox.minY(),
				chunkBox.minZ(),
				chunkBox.maxX(),
				chunkBox.maxY(),
				chunkBox.maxZ(),
				this
			);
		}

		@Override
		public void postProcess(
			WorldGenLevel world,
			StructureManager structureAccessor,
			ChunkGenerator chunkGenerator,
			RandomSource random,
			BoundingBox chunkBox,
			ChunkPos chunkPos,
			BlockPos pivot
		) {
			if (!(chunkGenerator instanceof BigGlobeScriptedChunkGenerator generator)) return;
			int minX = this.originalBoundingBox.minX();
			int minY = this.originalBoundingBox.minY();
			int minZ = this.originalBoundingBox.minZ();
			int maxX = this.originalBoundingBox.maxX();
			int maxY = this.originalBoundingBox.maxY();
			int maxZ = this.originalBoundingBox.maxZ();
			int midX = (minX + maxX + 1) >> 1;
			int midY = (minY + maxY + 1) >> 1;
			int midZ = (minZ + maxZ + 1) >> 1;
			int effectiveMinX = Math.max(this.boundingBox.minX(), chunkBox.minX());
			int effectiveMinY = Math.max(this.boundingBox.minY(), chunkBox.minY());
			int effectiveMinZ = Math.max(this.boundingBox.minZ(), chunkBox.minZ());
			int effectiveMaxX = Math.min(this.boundingBox.maxX(), chunkBox.maxX());
			int effectiveMaxY = Math.min(this.boundingBox.maxY(), chunkBox.maxY());
			int effectiveMaxZ = Math.min(this.boundingBox.maxZ(), chunkBox.maxZ());

			Permuter permuter = Permuter.from(random);

			this.placement.object().placement.place(
				new WorldWrapper(
					new WorldDelegator(world),
					generator,
					permuter,
					new Coordination(
						this.transformation,
						new BoundingBox(
							effectiveMinX,
							effectiveMinY,
							effectiveMinZ,
							effectiveMaxX,
							effectiveMaxY,
							effectiveMaxZ
						),
						WorldUtil.surroundingChunkBox(chunkPos, world)
					),
					ColumnUsage.FEATURES.maybeDhHints()
				),
				minX, minY, minZ,
				maxX, maxY, maxZ,
				midX, midY, midZ,
				chunkBox.minX(),
				chunkBox.minY(),
				chunkBox.minZ(),
				chunkBox.maxX(),
				chunkBox.maxY(),
				chunkBox.maxZ(),
				this
			);
		}
	}
}