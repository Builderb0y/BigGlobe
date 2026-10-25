package builderb0y.bigglobe.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.versions.BlockStateVersions;

@AddPseudoField("effect_duration")
@AddPseudoField("suspicious_stew_effect")
public class NetherFlowerBlock extends FlowerBlock {

	public static final MapCodec<NetherFlowerBlock> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(NetherFlowerBlock.class);

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public MapCodec codec() {
		return CODEC;
	}

	public NetherFlowerBlock(Holder<MobEffect> suspicious_stew_effect, float effect_duration, Properties settings) {
		super(suspicious_stew_effect, effect_duration, settings);
	}

	public Holder<MobEffect> suspicious_stew_effect() {
		return this.getSuspiciousEffects().effects().get(0).effect();
	}

	public float effect_duration() {
		return this.getSuspiciousEffects().effects().get(0).duration() / 20.0F;
	}

	@Override
	public boolean mayPlaceOn(BlockState floor, BlockGetter world, BlockPos pos) {
		return BlockStateVersions.isOpaqueFullCube(floor, world, pos);
	}
}