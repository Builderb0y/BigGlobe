package builderb0y.bigglobe.features;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import builderb0y.autocodec.annotations.VerifyFloatRange;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.chunkgen.BigGlobeScriptedChunkGenerator;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.restrictions.ColumnRestriction;
import builderb0y.bigglobe.columns.scripted2.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted2.ScriptedColumn.ColumnUsage;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomLists.DelegatingContainedRandomList;
import builderb0y.bigglobe.randomLists.IRestrictedListElement;
import builderb0y.bigglobe.randomSources.RandomRangeVerifier.VerifyRandomRange;
import builderb0y.bigglobe.randomSources.RandomSource;
import builderb0y.bigglobe.settings.VariationsList;
import builderb0y.bigglobe.versions.BlockStateVersions;

public class NetherFlowerFeature extends Feature<NetherFlowerFeature.Config> {

	public NetherFlowerFeature(Codec<Config> configCodec) {
		super(configCodec);
	}

	public NetherFlowerFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	@Override
	public boolean place(FeaturePlaceContext<Config> context) {
		if (context.config().requiresColumn && !(context.chunkGenerator() instanceof BigGlobeScriptedChunkGenerator)) return false;
		ScriptedColumn column = (
			context.config().requiresColumn
				? ((BigGlobeScriptedChunkGenerator)(context.chunkGenerator())).newColumn(
				context.level(),
				context.origin().getX(),
				context.origin().getZ(),
				ColumnUsage.FEATURES.maybeDhHints()
			)
				: null
		);
		BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
		Permuter permuter = Permuter.from(context.random());
		DelegatingContainedRandomList<Entry> entries = new DelegatingContainedRandomList<>(context.config().entries.elements);
		if (Permuter.nextChancedBoolean(permuter, context.config().randomize_chance)) {
			double radius = context.config().randomize_radius.get(column, context.origin().getY(), permuter);
			int count = Permuter.roundRandomlyI(permuter, radius * radius * 0.25D);
			for (int attempt = 0; attempt < count; attempt++) {
				Entry entry = entries.getRandomElement(permuter);
				this.generate(context, permuter, entry, radius, mutablePos);
			}
		}
		else {
			Entry entry = entries.getRandomElement(permuter);
			double radius = entry.radius.get(column, context.origin().getY(), permuter);
			int count = Permuter.roundRandomlyI(permuter, radius * radius * 0.25D);
			for (int attempt = 0; attempt < count; attempt++) {
				this.generate(context, permuter, entry, radius, mutablePos);
			}
		}
		return true;
	}

	public void generate(FeaturePlaceContext<Config> context, Permuter permuter, Entry entry, double radius, BlockPos.MutableBlockPos mutablePos) {
		double r = permuter.nextDouble(radius);
		double theta = permuter.nextDouble(BigGlobeMath.TAU);
		double x = Math.cos(theta) * r + context.origin().getX() + 0.5D;
		double z = Math.sin(theta) * r + context.origin().getZ() + 0.5D;

		mutablePos.set(BigGlobeMath.floorI(x), context.origin().getY(), BigGlobeMath.floorI(z));
		ChunkAccess chunk = context.level().getChunk(mutablePos);
		while (BlockStateVersions.isReplaceable(chunk.getBlockState(mutablePos))) {
			mutablePos.setY(mutablePos.getY() - 1);
			if (r * r + BigGlobeMath.squareD(mutablePos.getY() - context.origin().getY()) > radius * radius) return;
		}
		while (!BlockStateVersions.isReplaceable(chunk.getBlockState(mutablePos))) {
			mutablePos.setY(mutablePos.getY() + 1);
			if (r * r + BigGlobeMath.squareD(mutablePos.getY() - context.origin().getY()) > radius * radius) return;
		}
		if (entry.under != null) {
			mutablePos.setY(mutablePos.getY() - 1);
			BlockState revert = chunk.getBlockState(mutablePos);
			if (SingleBlockFeature.place(context.level(), mutablePos, permuter, entry.under)) {
				mutablePos.setY(mutablePos.getY() + 1);
				if (!SingleBlockFeature.place(context.level(), mutablePos, permuter, entry.state)) {
					mutablePos.setY(mutablePos.getY() - 1);
					context.level().setBlock(mutablePos, revert, Block.UPDATE_ALL);
				}
			}
		}
		else {
			SingleBlockFeature.place(context.level(), mutablePos, permuter, entry.state);
		}
	}

	public static class Config implements FeatureConfiguration {

		public final @VerifyFloatRange(min = 0.0D, max = 1.0D) double randomize_chance;
		public final @VerifyRandomRange(min = 0.0D, minInclusive = false, max = 16.0D) RandomSource randomize_radius;
		public final VariationsList<Entry> entries;
		public final boolean requiresColumn;

		public Config(
			double randomize_chance,
			RandomSource randomize_radius,
			VariationsList<Entry> entries
		) {
			this.randomize_chance = randomize_chance;
			this.randomize_radius = randomize_radius;
			this.entries = entries;
			this.requiresColumn = this.requiresColumn();
		}

		public boolean requiresColumn() {
			if (this.randomize_radius.requiresColumn()) return true;
			for (Entry entry : this.entries.elements) {
				if (entry.radius.requiresColumn()) return true;
			}
			return false;
		}
	}

	public static class Entry implements IRestrictedListElement {

		public final double weight;
		public final ColumnRestriction restrictions;
		public final @VerifyRandomRange(min = 0.0D, minInclusive = false, max = 16.0D) RandomSource radius;
		public final SingleBlockFeature.Config state;
		public final SingleBlockFeature.@VerifyNullable Config under;

		public Entry(
			double weight,
			ColumnRestriction restrictions,
			RandomSource radius,
			SingleBlockFeature.Config state,
			SingleBlockFeature.@VerifyNullable Config under
		) {
			this.weight = weight;
			this.restrictions = restrictions;
			this.radius = radius;
			this.state = state;
			this.under = under;
		}

		@Override
		public double getWeight() {
			return this.weight;
		}

		@Override
		public ColumnRestriction getRestrictions() {
			return this.restrictions;
		}
	}
}