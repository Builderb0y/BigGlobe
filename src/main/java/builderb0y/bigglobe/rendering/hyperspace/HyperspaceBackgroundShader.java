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
				
				float noise11(float coord) {
					float  fractCoord = fract(coord);
					float  floorCoord = coord - fractCoord;
					float   ceilCoord = floorCoord + 1.0;
					float smoothCoord = smoothify(fractCoord);
					return mix(hash11(floorCoord), hash11(ceilCoord), smoothCoord);
				}
				
				vec2 hash21(float p) {
					vec3 p3 = fract(vec3(p) * vec3(0.1031, 0.1030, 0.0973));
					p3 += dot(p3, p3.yzx + 33.33);
					return fract((p3.xx + p3.yz) * p3.zy);
				}
				
				vec3 unitVec(float seed) {
					vec2 surface = hash21(seed) * vec2(2.0, TAU) - vec2(1.0, 0.0);
					float r = sqrt(1.0 - surface.x * surface.x);
					return vec3(vec2(cos(surface.y), sin(surface.y)) * r, surface.x);
				}
				
				float hash13(vec3 p3) {
					p3 = fract(p3 * 0.1031);
					p3 += dot(p3, p3.zyx + 31.32);
					return fract((p3.x + p3.y) * p3.z);
				}
				
				vec2 hash23(vec3 p3) {
					p3 = fract(p3 * vec3(0.1031, 0.1030, 0.0973));
					p3 += dot(p3, p3.yzx + 33.33);
					return fract((p3.xx + p3.yz) * p3.zy);
				}
				
				vec3 hash33(vec3 p3) {
					p3 = fract(p3 * vec3(0.1031, 0.1030, 0.0973));
					p3 += dot(p3, p3.yxz + 33.33);
					return fract((p3.xxy + p3.yxx) * p3.zyx);
				}
				
				vec4 voronoise(vec3 coord) {
					vec4 closest = vec4(0.0);
					for (int index = 0; index < 64; index++) {
						vec3 point = unitVec(float(index));
						float dotProduct = dot(coord, point);
						if (dotProduct > closest.w) {
							closest = vec4(point, dotProduct);
						}
					}
					closest.w = 0.0;
					for (int index = 0; index < 64; index++) {
						vec3 point = unitVec(float(index));
						vec3 cellOffset = point - closest.xyz;
						vec3 coordOffset = coord - closest.xyz;
						closest.w = max(closest.w, dot(cellOffset, coordOffset) / dot(cellOffset, cellOffset));
					}
					closest.w *= 2.0;
					return closest;
				}
				
				vec2 noise23(vec3 coord) {
					vec3  fractCoord = fract(coord);
					vec3  floorCoord = coord - fractCoord;
					vec3   ceilCoord = floorCoord + vec3(1.0);
					vec3 smoothCoord = smoothify(fractCoord);
				
					return mix(
						mix(
							mix(
								hash23(vec3(floorCoord.x, floorCoord.y, floorCoord.z)),
								hash23(vec3(floorCoord.x, floorCoord.y,  ceilCoord.z)),
								smoothCoord.z
							),
							mix(
								hash23(vec3(floorCoord.x,  ceilCoord.y, floorCoord.z)),
								hash23(vec3(floorCoord.x,  ceilCoord.y,  ceilCoord.z)),
								smoothCoord.z
							),
							smoothCoord.y
						),
						mix(
							mix(
								hash23(vec3( ceilCoord.x, floorCoord.y, floorCoord.z)),
								hash23(vec3( ceilCoord.x, floorCoord.y,  ceilCoord.z)),
								smoothCoord.z
							),
							mix(
								hash23(vec3( ceilCoord.x,  ceilCoord.y, floorCoord.z)),
								hash23(vec3( ceilCoord.x,  ceilCoord.y,  ceilCoord.z)),
								smoothCoord.z
							),
							smoothCoord.y
						),
						smoothCoord.x
					);
				}
				
				vec2 backgroundNoise(vec3 norm) {
					vec2 noise = noise23(norm);
					return vec2(abs(noise.x - 0.5), noise.y);
				}
				
				vec3 smoothHue(float h) {
					return sqrt(normalize(square(cos(h * TAU - vec3(0.0, 1.0, 2.0) * (TAU / 3.0)) * 0.5 + 0.5)));
				}
				
				vec4 hash42(vec2 p) {
					vec4 p4 = fract(p.xyxy * vec4(0.1031, 0.1030, 0.0973, 0.1099));
					p4 += dot(p4, p4.wzxy + 33.33);
					return fract((p4.xxyz + p4.yzzw) * p4.zywx);
				}
				
				void main() {
					vec4 tmp = inverseProjection * vec4(texcoord * 2.0 - 1.0, 1.0, 1.0);
					vec3 norm = normalize(mat3(inverseModelView) * tmp.xyz);
					vec3 offsetNorm;
					float flash;
					if (collapse > 0.0) {
						vec4 voronoi = voronoise(norm);
						float flashOffset = (hash13(voronoi.xyz) * 0.75 + 0.25) - collapse * collapse;
						flash = flashOffset > 0.0 ? 0.0 : exp2(flashOffset * 64.0);
						float threshold = collapse * 1.5 - 0.5;
						threshold = 1.0 - threshold * threshold * threshold;
						threshold *= threshold;
						if (voronoi.w > threshold) {
							vec2 rng = hash23(voronoi.xyz);
							float red = 0.0;
							red += noise11(4.0 * time * rng.x) * 0.5;
							red = mix(red, 0.25, square(voronoi.w));
							red += cos((1.0 - voronoi.w) * exp2(mix(3.0, 6.0, rng.y)) * smoothify(unmix(1.0 / 3.0, 1.0, collapse))) * 0.0625;
							fragColor = vec4(red, 0.0, 0.0, 1.0);
							fragColor.rgb += flash * exp2((threshold - voronoi.w) * 16.0);
							return;
						}
						else {
							vec3 collapseOffset = (hash33(voronoi.xyz) * 2.0 - 1.0) * (collapse * collapse);
							norm += collapseOffset;
							offsetNorm = norm + cameraPosition * 0.015625;
							norm = normalize(norm);
						}
					}
					else {
						offsetNorm = norm + cameraPosition * 0.015625;
						flash = 0.0;
					}
					vec2 noise = vec2(0.0);
					noise += backgroundNoise(offsetNorm *  2.0) * 0.5;
					noise += backgroundNoise(offsetNorm *  4.0) * 0.25;
					noise += backgroundNoise(offsetNorm *  8.0) * 0.125;
					noise += backgroundNoise(offsetNorm * 16.0) * 0.0625;
					noise += backgroundNoise(offsetNorm * 32.0) * 0.03125;
					noise += backgroundNoise(offsetNorm * 64.0) * 0.015625;

					vec3 color = mix(smoothHue(noise.y * 0.5 + 0.375), vec3(1.0), noise.x) * noise.x;

					vec3 starSum = vec3(0.0);

					for (int planeIndex = 1; planeIndex <= 16; planeIndex++) {
						vec3 axis1 = unitVec( float(planeIndex));
						vec3 axis2 = unitVec(-float(planeIndex));
						axis2 = normalize(axis2 - axis1 * dot(axis1, axis2));
						vec2 planePos = vec2(dot(norm, axis1), dot(norm, axis2));
						float rotation = time * 0.015625;
						vec2 cs = vec2(cos(rotation), sin(rotation));
						planePos *= mat2(cs.x, cs.y, -cs.y, cs.x);
						float planeIntensity = 1.0 - dot(planePos, planePos);
						vec2 scaledPlanePos = planePos * 16.0;
						vec2 fractPos = fract(scaledPlanePos);
						vec2 floorPos = scaledPlanePos - fractPos;
						vec4 starData = hash42(floorPos);
						vec2  starPos = mix(starData.xy, vec2(0.5), starData.z);
						float starIntensity = max(1.0 - 2.0 * distance(fractPos, starPos) / starData.z, 0.0);
						starIntensity = square(square(square(starIntensity)));
						starIntensity *= sin(time + starData.w * TAU) * 0.5 + 0.5;
						vec3 starColor = exp2((starData.z * 8.0 - 7.0) * (vec3(1.0, 2.0, 4.0) / 4.0));
						starSum += starColor * starIntensity * planeIntensity;

						//rest in peace, Nameless.
						//I wish I could show this to you now.
						float t = time * (PI / 256.0) + float(planeIndex) * (PI / 16.0);
						vec2 z = planePos * 4.0;
						vec2 c = vec2(cos(t), sin(t)) * (cos(t) * -0.375 + 0.5) + vec2(0.125, 0.0);
						int fractalIterations = 0;
						for (int iteration = 0; iteration <= 32; iteration++) {
							z = vec2(z.x * z.x - z.y * z.y, 2.0 * z.x * z.y) + c;
							if (dot(z, z) >= 4.0) {
								fractalIterations = iteration;
								break;
							}
						}
						starSum = mix(starSum, vec3(1.0), float(fractalIterations) * 0.03125);
					}
					starSum *= noise.x;

					color += starSum * 4.0 + flash;
					float distanceFromOrigin = length(cameraPosition);
					color *= exp2((dot(norm, cameraPosition) / distanceFromOrigin * -0.5 - 0.5) * distanceFromOrigin * 0.03125);

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