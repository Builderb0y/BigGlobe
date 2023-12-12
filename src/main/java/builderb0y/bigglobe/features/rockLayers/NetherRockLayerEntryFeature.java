package builderb0y.bigglobe.features.rockLayers;

import com.mojang.serialization.Codec;

import net.minecraft.block.BlockState;
import net.minecraft.command.argument.BlockArgumentParser;

import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.codecs.BlockStateCoder.VerifyNormal;
import builderb0y.bigglobe.columns.restrictions.ColumnRestriction;
import builderb0y.bigglobe.noise.Grid2D;
import builderb0y.bigglobe.util.BlockState2ObjectMap;

public class NetherRockLayerEntryFeature extends RockLayerEntryFeature<NetherRockLayerEntryFeature.Entry> {

	public NetherRockLayerEntryFeature(Codec<Config<Entry>> codec) {
		super(codec);
	}

	public NetherRockLayerEntryFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(new ReifiedType<>() {}));
	}

	@AddPseudoField("place")
	@AddPseudoField("replace")
	public static class Entry extends RockLayerEntryFeature.Entry {

		public Entry(
			double weight,
			ColumnRestriction restrictions,
			Grid2D center,
			Grid2D thickness,
			@VerifyNormal BlockState place,
			@VerifyNormal BlockState replace,
			BlockState2ObjectMap<BlockState> blocks
		) {
			super(weight, restrictions, center, thickness, blocks);
			if (place != null && replace != null) {
				this.blocks.serializedStates.put(BlockArgumentParser.stringifyBlockState(replace), place);
				this.blocks.runtimeStates.put(replace, place);
			}
		}

		//backwards compatibility.

		public @VerifyNormal @VerifyNullable BlockState place() {
			return null;
		}

		public @VerifyNormal @VerifyNullable BlockState replace() {
			return null;
		}
	}
}