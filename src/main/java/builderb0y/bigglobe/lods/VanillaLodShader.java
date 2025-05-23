package builderb0y.bigglobe.lods;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.SpriteAtlasTexture;

import static org.lwjgl.opengl.GL32C.*;

public class VanillaLodShader extends LodShader {

	public int modelViewProjectionMatrix, fogColor, fogParams;
	public int blockAtlas, lightmap;

	public void bindTextures() {
		glActiveTexture(GL_TEXTURE0);
		GLException.check();
		glBindTexture(GL_TEXTURE_2D, AbstractLodRenderer.glID(MinecraftClient.getInstance().getTextureManager().getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)));
		GLException.check();
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
		GLException.check();
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		GLException.check();

		glActiveTexture(GL_TEXTURE2);
		GLException.check();
		glBindTexture(GL_TEXTURE_2D, AbstractLodRenderer.glID(MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager()));
		GLException.check();
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		GLException.check();
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		GLException.check();

		glUniform1i(this.blockAtlas, 0);
		glUniform1i(this.lightmap, 2);
	}
}