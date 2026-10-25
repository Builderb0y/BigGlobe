package builderb0y.bigglobe.recipes;

import com.mojang.serialization.MapCodec;

import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public class ScriptedRecipeSerializer implements RecipeSerializer<ScriptedRecipe> {

	public static final AutoCoder<ScriptedRecipe> CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(ScriptedRecipe.class);
	public static final MapCodec<ScriptedRecipe> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(CODER);
	public static final StreamCodec<RegistryFriendlyByteBuf, ScriptedRecipe> PACKET_CODEC = new StreamCodec<RegistryFriendlyByteBuf, ScriptedRecipe>() {

		@Override
		public ScriptedRecipe decode(RegistryFriendlyByteBuf buffer) {
			try {
				return BigGlobeAutoCodec.AUTO_CODEC.decode(CODER, buffer.readNbt(), buffer.registryAccess().createSerializationContext(NbtOps.INSTANCE));
			}
			catch (DecodeException exception) {
				throw new RuntimeException(exception);
			}
		}

		@Override
		public void encode(RegistryFriendlyByteBuf buffer, ScriptedRecipe value) {
			buffer.writeNbt(BigGlobeAutoCodec.AUTO_CODEC.encode(CODER, value, buffer.registryAccess().createSerializationContext(NbtOps.INSTANCE)));
		}
	};

	@Override
	public MapCodec<ScriptedRecipe> codec() {
		return CODEC;
	}

	@Override
	public StreamCodec<RegistryFriendlyByteBuf, ScriptedRecipe> streamCodec() {
		return PACKET_CODEC;
	}
}