package builderb0y.bigglobe.features.flowers;

import com.mojang.serialization.Codec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;

import builderb0y.autocodec.annotations.VerifyFloatRange;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.restrictions.ColumnRestriction;
import builderb0y.bigglobe.features.SingleBlockFeature;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomLists.DelegatingContainedRandomList;
import builderb0y.bigglobe.randomLists.IRestrictedListElement;
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
	public boolean generate(FeatureContext<Config> context) {
		BlockPos.Mutable mutablePos = new BlockPos.Mutable();
		Permuter permuter = Permuter.from(context.getRandom());
		DelegatingContainedRandomList<Entry> entries = new DelegatingContainedRandomList<>(context.getConfig().entries.elements);
		if (Permuter.nextChancedBoolean(permuter, context.getConfig().randomize_chance)) {
			double radius = context.getConfig().randomize_radius.get(permuter);
			int count = Permuter.roundRandomlyI(permuter, radius * radius * 0.25D);
			for (int attempt = 0; attempt < count; attempt++) {
				Entry entry = entries.getRandomElement(permuter);
				this.generate(context, permuter, entry, radius, mutablePos);
			}
		}
		else {
			Entry entry = entries.getRandomElement(permuter);
			double radius = entry.radius.get(permuter);
			int count = Permuter.roundRandomlyI(permuter, radius * radius * 0.25D);
			for (int attempt = 0; attempt < count; attempt++) {
				this.generate(context, permuter, entry, radius, mutablePos);
			}
		}
		return true;
	}

	public void generate(FeatureContext<Config> context, Permuter permuter, Entry entry, double radius, BlockPos.Mutable mutablePos) {
		double r = permuter.nextDouble(radius);
		double theta = permuter.nextDouble(BigGlobeMath.TAU);
		double x = Math.cos(theta) * r + context.getOrigin().getX() + 0.5D;
		double z = Math.sin(theta) * r + context.getOrigin().getZ() + 0.5D;

		mutablePos.set(BigGlobeMath.floorI(x), context.getOrigin().getY(), BigGlobeMath.floorI(z));
		Chunk chunk = context.getWorld().getChunk(mutablePos);
		while (BlockStateVersions.isReplaceable(chunk.getBlockState(mutablePos))) {
			mutablePos.setY(mutablePos.getY() - 1);
			if (r * r + BigGlobeMath.squareD(mutablePos.getY() - context.getOrigin().getY()) > radius * radius) return;
		}
		while (!BlockStateVersions.isReplaceable(chunk.getBlockState(mutablePos))) {
			mutablePos.setY(mutablePos.getY() + 1);
			if (r * r + BigGlobeMath.squareD(mutablePos.getY() - context.getOrigin().getY()) > radius * radius) return;
		}
		if (entry.under != null) {
			mutablePos.setY(mutablePos.getY() - 1);
			BlockState revert = chunk.getBlockState(mutablePos);
			if (SingleBlockFeature.place(context.getWorld(), mutablePos, permuter, entry.under)) {
				mutablePos.setY(mutablePos.getY() + 1);
				if (!SingleBlockFeature.place(context.getWorld(), mutablePos, permuter, entry.state)) {
					mutablePos.setY(mutablePos.getY() - 1);
					context.getWorld().setBlockState(mutablePos, revert, Block.NOTIFY_ALL);
				}
			}
		}
		else {
			SingleBlockFeature.place(context.getWorld(), mutablePos, permuter, entry.state);
		}
	}

	public static class Config implements FeatureConfig {

		public final @VerifyFloatRange(min = 0.0D, max = 1.0D) double randomize_chance;
		public final RandomSource randomize_radius;
		public final VariationsList<Entry> entries;

		public Config(
			double randomize_chance,
			RandomSource randomize_radius,
			VariationsList<Entry> entries
		) {
			this.randomize_chance = randomize_chance;
			this.randomize_radius = randomize_radius;
			this.entries = entries;
		}
	}

	public static class Entry implements IRestrictedListElement {

		public final double weight;
		public final ColumnRestriction restrictions;
		public final RandomSource radius;
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