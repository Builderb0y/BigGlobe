package builderb0y.bigglobe.recipes;

import com.mojang.serialization.Codec;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.RecipeSerializer;

import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public class ScriptedRecipeSerializer implements RecipeSerializer<ScriptedRecipe> {

	public static final AutoCoder<ScriptedRecipe> SCRIPTED_RECIPE_CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(ScriptedRecipe.class);
	public static final Codec<ScriptedRecipe> SCRIPTED_RECIPE_CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(SCRIPTED_RECIPE_CODER).codec();

	@Override
	public Codec<ScriptedRecipe> codec() {
		return SCRIPTED_RECIPE_CODEC;
	}

	@Override
	public ScriptedRecipe read(PacketByteBuf buffer) {
		try {
			return BigGlobeAutoCodec.AUTO_CODEC.decode(SCRIPTED_RECIPE_CODER, buffer.readNbt(), NbtOps.INSTANCE);
		}
		catch (DecodeException exception) {
			throw new RuntimeException(exception);
		}
	}

	@Override
	public void write(PacketByteBuf buffer, ScriptedRecipe recipe) {
		buffer.writeNbt((NbtCompound)(BigGlobeAutoCodec.AUTO_CODEC.encode(SCRIPTED_RECIPE_CODER, recipe, NbtOps.INSTANCE)));
	}
}