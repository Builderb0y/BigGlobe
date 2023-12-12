package builderb0y.bigglobe.features.ores;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.block.BlockState;

import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.codecs.BlockStateCoder.VerifyNormal;
import builderb0y.bigglobe.randomSources.RandomSource;
import builderb0y.bigglobe.scripting.interfaces.ColumnYToDoubleScript;
import builderb0y.bigglobe.util.BlockState2ObjectMap;

public class OverworldOreFeature extends OreFeature<OverworldOreFeature.Config> {

	public OverworldOreFeature() {
		super(Config.class);
	}

	@AddPseudoField("stone_state")
	@AddPseudoField("deepslate_state")
	public static class Config extends OreFeature.Config {

		public Config(
			ColumnYToDoubleScript.Holder chance,
			RandomSource radius,
			BlockState2ObjectMap<BlockState> blocks,
			@VerifyNormal @VerifyNullable BlockState stone_state,
			@VerifyNormal @VerifyNullable BlockState deepslate_state
		) {
			super(chance, radius, blocks);
			try {
				if (stone_state != null) this.blocks.addSerialized("minecraft:stone", stone_state);
				if (deepslate_state != null) this.blocks.addSerialized("minecraft:deepslate", deepslate_state);
			}
			catch (CommandSyntaxException exception) {
				throw new AssertionError("Did minecraft change the name of stone or deepslate?", exception);
			}
		}

		//backwards compatibility.

		public @VerifyNormal @VerifyNullable BlockState stone_state() {
			return null;
		}

		public @VerifyNormal @VerifyNullable BlockState deepslate_state() {
			return null;
		}
	}
}