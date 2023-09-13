package builderb0y.bigglobe.recipes;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.util.Identifier;

import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public class ScriptedRecipeSerializer implements RecipeSerializer<ScriptedRecipe> {

	public static final AutoCoder<ScriptedRecipe> SCRIPTED_RECIPE_CODER = BigGlobeAutoCodec.AUTO_CODEC.createCoder(ScriptedRecipe.class);

	@Override
	public ScriptedRecipe read(Identifier id, JsonObject json) {
		json = json.deepCopy();
		json.addProperty("id", id.toString());
		try {
			return BigGlobeAutoCodec.AUTO_CODEC.decode(SCRIPTED_RECIPE_CODER, json, JsonOps.INSTANCE);
		}
		catch (DecodeException exception) {
			throw new RuntimeException(exception);
		}
	}

	@Override
	public ScriptedRecipe read(Identifier id, PacketByteBuf buf) {
		NbtCompound nbt = buf.readNbt();
		nbt.putString("id", id.toString());
		try {
			return BigGlobeAutoCodec.AUTO_CODEC.decode(SCRIPTED_RECIPE_CODER, nbt, NbtOps.INSTANCE);
		}
		catch (DecodeException exception) {
			throw new RuntimeException(exception);
		}
	}

	@Override
	public void write(PacketByteBuf buf, ScriptedRecipe recipe) {
		buf.writeNbt((NbtCompound)(BigGlobeAutoCodec.AUTO_CODEC.encode(SCRIPTED_RECIPE_CODER, recipe, NbtOps.INSTANCE)));
	}
}