package builderb0y.bigglobe.rendering.hyperspace;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.rendering.ScreenTriangleShader;

import static org.lwjgl.opengl.GL20C.*;

public class HorizontalBlurPass extends ScreenTriangleShader {

	public int resolution, previousPass;

	public HorizontalBlurPass() {
		try {
			this.compileStage(
				this.fragmentStage,
				//language=glsl
				"""
				#version 150

				uniform ivec2 resolution;
				uniform sampler2D previousPass;

				out vec4 outColor;

				vec4 square(vec4 color) {
					return color * color;
				}

				void main() {
					ivec2 coord = ivec2(gl_FragCoord.xy);
					//assume previous pass has alpha of 1.0 everywhere.
					vec4 color = square(texelFetch(previousPass, coord, 0));
					for (int offsetX = 1; offsetX <= 4; offsetX++) {
						ivec2 newCoord = coord + ivec2(offsetX, 0);
						if (newCoord.x < resolution.x) {
							color += square(texelFetch(previousPass, newCoord, 0));
						}
					}
					for (int offsetX = 1; offsetX <= 4; offsetX++) {
						ivec2 newCoord = coord - ivec2(offsetX, 0);
						if (newCoord.x >= 0) {
							color += square(texelFetch(previousPass, newCoord, 0));
						}
					}
					outColor = sqrt(color / color.w);
				}
				"""
			);
			this.link();
			this.resolution = glGetUniformLocation(this.program, "resolution");
			this.previousPass = glGetUniformLocation(this.program, "previousPass");
		}
		catch (Throwable throwable) {
			this.close();
			throw AutoCodecUtil.rethrow(throwable);
		}
	}
}