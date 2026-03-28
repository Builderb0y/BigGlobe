package builderb0y.bigglobe.rendering.hyperspace;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.bigglobe.rendering.ScreenTriangleShader;

import static org.lwjgl.opengl.GL20C.*;

@Environment(EnvType.CLIENT)
public class HyperspaceBackgroundShader extends ScreenTriangleShader {

	public int inverseModelView, inverseProjection, cameraPosition, time, collapse;

	public HyperspaceBackgroundShader() {
		try {
			this.compileStage(
				this.fragmentStage,
				//language=glsl
				"""
					#version 150
					
					uniform mat4 inverseModelView;
					uniform mat4 inverseProjection;
					uniform vec3 cameraPosition;
					uniform float collapse;
					uniform float time;
					uniform sampler2D previousPass;
					
					in vec2 texcoord;
					
					out vec4 fragColor;
					
					const float PI = 3.14159265359;
					const float TAU = PI * 2.0;
					
					float square(float f) {
						return f * f;
					}
					
					vec2 square(vec2 v) {
						return v * v;
					}
					
					vec3 square(vec3 v) {
						return v * v;
					}
					
					float smoothify(float f) {
						return f * f * (f * -2.0 + 3.0);
					}
					
					vec3 smoothify(vec3 v) {
						return v * v * (v * -2.0 + 3.0);
					}
					
					float unmix(float a, float b, float f) {
						return (f - a) / (b - a);
					}
					
					float hash11(float p) {
						p = fract(p * 0.1031);
						p *= p + 33.33;
						p *= p + p;
						return fract(p);
					}
					
					vec2 hash21(float p) {
						vec3 p3 = fract(vec3(p) * vec3(0.1031, 0.1013, 0.0973));
						p3 += dot(p3, p3.yzx + 33.33);
						return fract((p3.xx + p3.yz) * p3.zy);
					}
					
					vec3 unitVec(vec2 surface) {
						surface = surface * vec2(2.0, TAU) - vec2(1.0, 0.0);
						float r = sqrt(1.0 - surface.x * surface.x);
						return vec3(vec2(cos(surface.y), sin(surface.y)) * r, surface.x);
					}
					
					vec3 unitVec(float seed) {
						return unitVec(hash21(seed));
					}
					
					float hash13(vec3 p3) {
						p3 = fract(p3 * 0.1031);
						p3 += dot(p3, p3.zyx + 31.32);
						return fract((p3.x + p3.y) * p3.z);
					}
					
					const vec3[] starPlanes = vec3[](
						vec3(1.0, 0.0, 0.0),
						vec3(0.0, 1.0, 0.0),
						vec3(0.0, 0.0, 1.0),
						vec3( 0.5773502692,  0.5773502692, 0.5773502692),
						vec3(-0.5773502692,  0.5773502692, 0.5773502692),
						vec3( 0.5773502692, -0.5773502692, 0.5773502692),
						vec3(-0.5773502692, -0.5773502692, 0.5773502692)
					);
					
					struct Voronoise {
						float ringDist;
						int discriminator;
					};
					
					Voronoise voronoise(vec3 coord) {
						Voronoise result = Voronoise(1.0, 0);
						for (int index = 0; index < 8; index++) {
							vec3 point = unitVec(float(index));
							float dotProduct = dot(coord, point);
							if (dotProduct < 0.0) {
								result.discriminator |= 1 << index;
								dotProduct = -dotProduct;
							}
							if (dotProduct < result.ringDist) {
								result.ringDist = dotProduct;
							}
						}
						return result;
					}
					
					int getStarPlaneIndex(vec3 coord) {
						int result = 0;
						float bestDotProduct = abs(dot(coord, starPlanes[0]));
						for (int index = 1; index < starPlanes.length(); index++) {
							vec3 point = starPlanes[index];
							float dotProduct = abs(dot(coord, point));
							if (dotProduct > bestDotProduct) {
								result = index;
								bestDotProduct = dotProduct;
							}
						}
						return result;
					}
					
					vec2 hash23(vec3 p3) {
						p3 = fract(p3 * vec3(0.1031, 0.1013, 0.0973));
						p3 += dot(p3, p3.yzx + 33.33);
						return fract((p3.xx + p3.yz) * p3.zy);
					}
					
					vec4 hash43(vec3 p) {
						vec4 p4 = fract(p.xyzx * vec4(0.1031, 0.1013, 0.0973, 0.1099));
						p4 += dot(p4, p4.wzxy + 33.33);
						return fract((p4.xxyz + p4.yzzw) * p4.zywx);
					}
					
					float julia(vec2 z, vec2 c) {
						for (int iteration = 0; iteration <= 64; iteration++) {
							z = vec2(z.x * z.x - z.y * z.y, 2.0 * z.x * z.y) + c;
							float z2 = dot(z, z);
							if (z2 > 256.0 * 256.0) {
								//https://iquilezles.org/articles/msetsmooth/
								return float(iteration) - log2(log2(z2)) + 4.0;
							}
						}
						return 0.0;
					}
					
					void main() {
						vec4 tmp = inverseProjection * vec4(texcoord * 2.0 - 1.0, 1.0, 1.0);
						vec3 norm = normalize(mat3(inverseModelView) * tmp.xyz);
						float flash;
						if (collapse > 0.0) {
							Voronoise voronoi = voronoise(norm);
							float flashOffset = (hash11(float(voronoi.discriminator)) * 0.75 + 0.25) - collapse * collapse;
							flash = flashOffset > 0.0 ? 0.0 : exp2(flashOffset * 64.0);
							float threshold = collapse * 1.5 - 0.5;
							threshold = threshold * threshold * threshold * 0.5;
							if (voronoi.ringDist < threshold) {
								vec2 rng = hash21(float(voronoi.discriminator));
								float red = 0.0;
								red = mix(0.25, red, square(voronoi.ringDist));
								red += cos(voronoi.ringDist * 128.0 * smoothify(unmix(1.0 / 3.0, 1.0, collapse))) * 0.0625;
								fragColor = vec4(red, 0.0, 0.0, 1.0);
								fragColor.rgb += flash * exp2((voronoi.ringDist - threshold) * 64.0);
								return;
							}
						}
						else {
							flash = 0.0;
						}
					
						vec3 color = texture(previousPass, texcoord).rgb;
					
						int starIndex = getStarPlaneIndex(norm);
						vec3 axis  = starPlanes[starIndex];
						vec3 axis1 = unitVec(hash23(axis));
						axis1 = normalize(axis1 - axis * dot(axis, axis1));
						vec3 axis2 = cross(axis, axis1);
						vec3 planePos = norm * mat3(axis1, axis2, axis);
					
						//rest in peace, Nameless.
						//I wish I could show this to you now.
						float h = float(starIndex) / 7.0;
						float t = time * (PI / 128.0) + h * TAU;
						vec2  z = planePos.xy * 4.5;
						vec2  c = vec2(cos(t), sin(t)) * (cos(t) * -0.375 + 0.5) * exp2(square(max(collapse * 8.0 - h, 0.0))) + vec2(0.125, 0.0);
						float j = julia(z, c);
						if (j == 0.0) {
							vec2 cameraOffset = cameraPosition * mat2x3(axis1, axis2);
							for (float scale = 4.0; scale <= 6.0; scale += 0.5) {
								vec2 scaledPlanePos = planePos.xy * exp2(scale) + cameraOffset;
								vec2 fractPos = fract(scaledPlanePos);
								vec2 floorPos = scaledPlanePos - fractPos;
								vec4 starData = hash43(vec3(floorPos, scale));
								vec2  starPos = mix(starData.xy, vec2(0.5), starData.z);
								float starIntensity = max(1.0 - 2.0 * distance(fractPos, starPos) / starData.z, 0.0);
								starIntensity = square(square(square(starIntensity)));
								starIntensity *= sin(time + starData.w * TAU) * 0.5 + 0.5;
								vec3 starColor = exp2((starData.z * 8.0 - 7.0) * (vec3(1.0, 2.0, 4.0) / 4.0));
								color += starColor * starIntensity;
							}
						}
						else {
							color = mix(color, vec3(1.0), 1.0 / (exp2(6.0 - j) + 1.0));
						}
						color += flash;
						//float distanceFromOrigin = length(cameraPosition);
						//color *= exp2((dot(norm, cameraPosition) / distanceFromOrigin * -0.5 - 0.5) * distanceFromOrigin * 0.03125);
					
						fragColor = vec4(color, 1.0);
					}
					"""
			);
			this.link();
			this.inverseModelView = glGetUniformLocation(this.program, "inverseModelView");
			this.inverseProjection = glGetUniformLocation(this.program, "inverseProjection");
			this.cameraPosition = glGetUniformLocation(this.program, "cameraPosition");
			this.collapse = glGetUniformLocation(this.program, "collapse");
			this.time = glGetUniformLocation(this.program, "time");
		}
		catch (Throwable throwable) {
			this.close();
			throw AutoCodecUtil.rethrow(throwable);
		}
	}
}