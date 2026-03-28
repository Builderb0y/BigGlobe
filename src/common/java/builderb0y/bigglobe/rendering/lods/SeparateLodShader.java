package builderb0y.bigglobe.rendering.lods;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import static org.lwjgl.opengl.GL32C.*;

@Environment(EnvType.CLIENT)
public class SeparateLodShader extends VanillaLodShader {

	public int modelOffset;

	public SeparateLodShader() {
		this.compileStage(
			this.fragmentStage,
			//language=glsl
			"""
				#version 150
				
				uniform sampler2D blockAtlas;
				uniform sampler2D lightmap;
				uniform vec3 fogColor;
				uniform vec3 fogParams;
				
				#define altitude fogParams.x
				#define verticalMultiplier fogParams.y
				#define globalMultiplier fogParams.z
				
				in vec2 texcoord;
				in vec2 lmcoord;
				in vec4 tint;
				in vec3 pos;
				
				out vec4 color;
				
				void main() {
					color = (
						//texture(lightmap, gl_FragCoord.xy / vec2(1536.0, 896.0))
						texture(blockAtlas, texcoord) *
						texture(lightmap, lmcoord) *
						tint
					);
					if (color.a < 0.1) discard;
				
					if (globalMultiplier < 0.0) {
						float dist = length(pos);
						float opticalDepth;
						if (verticalMultiplier < 0.0) {
							vec3 ray = pos / dist;
							if (abs(ray.y) < 0.00001) {
								opticalDepth = dist * exp(min(altitude * verticalMultiplier, 32.0));
							}
							else {
								opticalDepth = ((exp(min(dist * ray.y * verticalMultiplier, 32.0)) - 1.0) * exp(min(altitude * verticalMultiplier, 32.0))) / (ray.y * verticalMultiplier);
							}
						}
						else {
							opticalDepth = dist;
						}
						color.rgb = mix(fogColor, color.rgb, exp2(opticalDepth * globalMultiplier));
					}
				}
				"""
		);
		this.compileStage(
			this.vertexStage,
			//language=glsl
			"#version 150\n" +
			"#define MIN_LOD " + LodQuadTree.MIN_LEVEL + '\n' +
			"""
				uniform mat4 modelViewProjectionMatrix;
				uniform vec4 modelOffset;
				
				in uvec2 horizontalPosition;
				in int verticalPosition;
				in uvec2 texcoordData;
				in uvec3 colorData;
				in uint lightData;
				
				out vec2 texcoord;
				out vec2 lmcoord;
				out vec4 tint;
				out vec3 pos;
				
				void main() {
					vec3 modelPos;
					modelPos.xz = vec2(horizontalPosition) * (float(1 << MIN_LOD) / 128.0) - 64.0 * (float(1 << MIN_LOD) / 128.0);
					modelPos.y = float(verticalPosition) * (4096.0 / 32768.0);
					uint decodedColor = (colorData.z << 16u) | (colorData.y << 8u) | colorData.x;
					tint = vec4((uvec4(decodedColor) >> uvec4(0u, 6u, 12u, 18u)) & uvec4(63u)) * (1.0 / 64.0) + (0.5 / 64.0);
					lmcoord = vec2((uvec2(lightData) >> uvec2(0u, 4u)) & 15u) * (1.0 / 16.0) + (0.5 / 16.0);
					texcoord = texcoordData * (1.0 / 65536.0);
					pos = modelPos * modelOffset.w + modelOffset.xyz;
					gl_Position = modelViewProjectionMatrix * vec4(pos, 1.0);
				}
				"""
		);
		this.link();

		this.modelViewProjectionMatrix = glGetUniformLocation(this.program, "modelViewProjectionMatrix");
		this.blockAtlas = glGetUniformLocation(this.program, "blockAtlas");
		this.lightmap = glGetUniformLocation(this.program, "lightmap");
		this.modelOffset = glGetUniformLocation(this.program, "modelOffset");
		this.fogColor = glGetUniformLocation(this.program, "fogColor");
		this.fogParams = glGetUniformLocation(this.program, "fogParams");
	}
}