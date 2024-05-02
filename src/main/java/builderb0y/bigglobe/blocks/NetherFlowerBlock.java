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

	public NetherFlowerBlock(#if MC_VERSION >= MC_1_20_5 RegistryEntry<StatusEffect> #else StatusEffect #endif suspicious_stew_effect, float effect_duration, Settings settings) {
		super(suspicious_stew_effect, effect_duration, settings);
	}

	public #if MC_VERSION >= MC_1_20_5 RegistryEntry<StatusEffect> #else StatusEffect #endif suspicious_stew_effect() {
		return this.getStewEffects().effects().get(0).effect();
	}

	public float effect_duration() {
		return this.getStewEffects().effects().get(0).duration() / 20.0F;
	}

	@Override
	public boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
		return floor.isOpaqueFullCube(world, pos);
	}
}