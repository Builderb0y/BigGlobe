package builderb0y.bigglobe.recipes;

import com.mojang.serialization.MapCodec;

import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.RecipeSerializer;

import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public class ScriptedRecipeSerializer implements RecipeSerializer<ScriptedRecipe> {

	public static final AutoCoder<ScriptedRecipe> CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(ScriptedRecipe.class);
	public static final MapCodec<ScriptedRecipe> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(CODER);
	public static final PacketCodec<RegistryByteBuf, ScriptedRecipe> PACKET_CODEC = new PacketCodec<RegistryByteBuf, ScriptedRecipe>() {

		@Override
		public ScriptedRecipe decode(RegistryByteBuf buffer) {
			try {
				return BigGlobeAutoCodec.AUTO_CODEC.decode(CODER, buffer.readNbt(), buffer.getRegistryManager().getOps(NbtOps.INSTANCE));
			}
			catch (DecodeException exception) {
				throw new RuntimeException(exception);
			}
		}

		@Override
		public void encode(RegistryByteBuf buffer, ScriptedRecipe value) {
			buffer.writeNbt(BigGlobeAutoCodec.AUTO_CODEC.encode(CODER, value, buffer.getRegistryManager().getOps(NbtOps.INSTANCE)));
		}
	};

	@Override
	public MapCodec<ScriptedRecipe> codec() {
		return CODEC;
	}

	@Override
	public PacketCodec<RegistryByteBuf, ScriptedRecipe> packetCodec() {
		return PACKET_CODEC;
	}
}