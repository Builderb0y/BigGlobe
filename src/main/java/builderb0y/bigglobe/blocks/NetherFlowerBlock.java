package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;

import net.minecraft.block.BlockState;
import net.minecraft.block.FlowerBlock;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

@AddPseudoField("suspicious_stew_effect")
@AddPseudoField("effect_duration")
public class NetherFlowerBlock extends FlowerBlock {

	#if MC_VERSION >= MC_1_20_3
		public static final MapCodec<NetherFlowerBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(NetherFlowerBlock.class);

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public MapCodec getCodec() {
			return CODEC;
		}
	#endif

	#if MC_VERSION >= MC_1_20_5

		public NetherFlowerBlock(RegistryEntry<StatusEffect> suspicious_stew_effect, float effect_duration, Settings settings) {
			super(suspicious_stew_effect, effect_duration, settings);
		}

		public RegistryEntry<StatusEffect> suspicious_stew_effect() {
			return this.getStewEffects().effects().get(0).effect();
		}

		public float effect_duration() {
			return this.getStewEffects().effects().get(0).duration() / 20.0F;
		}
	#else

		public NetherFlowerBlock(StatusEffect suspicious_stew_effect, int effect_duration, Settings settings) {
			super(suspicious_stew_effect, effect_duration, settings);
		}

		#if MC_VERSION >= MC_1_20_3
			public StatusEffect suspicious_stew_effect() {
				return this.getStewEffects().get(0).effect();
			}

			public int effect_duration() {
				return this.getStewEffects().get(0).duration();
			}
		#endif
	#endif

	@Override
	public boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
		return floor.isOpaqueFullCube(world, pos);
	}
}