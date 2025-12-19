package builderb0y.bigglobe.rendering.lods;

import org.lwjgl.opengl.*;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.SpriteAtlasTexture;

import builderb0y.bigglobe.rendering.GLException;
import builderb0y.bigglobe.rendering.Shader;
import builderb0y.bigglobe.versions.RenderVersions;

import static org.lwjgl.opengl.GL32C.*;

public class VanillaLodShader extends Shader {

	public int modelViewProjectionMatrix, fogColor, fogParams;
	public int blockAtlas, lightmap;

	public void bindTextures() {
		glActiveTexture(GL_TEXTURE0);
		GLException.check();
		AbstractTexture texture = MinecraftClient.getInstance().getTextureManager().getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
		glBindTexture(GL_TEXTURE_2D, RenderVersions.glID(texture));
		GLException.check();
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
		GLException.check();
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		GLException.check();
		#if MC_VERSION >= MC_1_21_11
			GL33C.glBindSampler(0, 0);
			GLException.check();
		#endif

		glActiveTexture(GL_TEXTURE2);
		GLException.check();
		glBindTexture(GL_TEXTURE_2D, RenderVersions.glID(MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager()));
		GLException.check();
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		GLException.check();
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		GLException.check();
		#if MC_VERSION >= MC_1_21_11
			GL33C.glBindSampler(2, 0);
			GLException.check();
		#endif

		glUniform1i(this.blockAtlas, 0);
		glUniform1i(this.lightmap, 2);
	}
}