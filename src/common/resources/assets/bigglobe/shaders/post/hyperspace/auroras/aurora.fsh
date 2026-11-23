#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform Uniforms {
	mat4 inverseModelView;
	mat4 inverseProjection;
	vec4 cameraPosition;
	float time;
	float collapse;
};

layout(location = 0) in vec2 texcoord;

layout(location = 0) out vec4 fragColor;

const float PI = 3.14159265359;
const float TAU = PI * 2.0;
const float GOLDEN_ANGLE = 2.39996322972865332;

float square(float f) {
	return f * f;
}

vec2 square(vec2 v) {
	return v * v;
}

vec3 square(vec3 v) {
	return v * v;
}

float cube(float f) {
	return f * f * f;
}

float smoothify(float f) {
	return f * f * (3.0 - 2.0 * f);
}

vec2 smoothify(vec2 v) {
	return v * v * (3.0 - 2.0 * v);
}

vec3 smoothify(vec3 v) {
	return v * v * (3.0 - 2.0 * v);
}

float hash12(vec2 p) {
	vec3 p3 = fract(p.xyx * 0.1031);
	p3 += dot(p3, p3.yzx + 33.33);
	return fract((p3.x + p3.y) * p3.z);
}

float noise12(vec2 coord) {
	vec2 fractCoord = fract(coord);
	vec2 floorCoord = coord - fractCoord;
	vec2 ceilCoord = floorCoord + vec2(1.0);
	fractCoord = smoothify(fractCoord);
	return mix(
		mix(
			hash12(vec2(floorCoord.x, floorCoord.y)),
			hash12(vec2(floorCoord.x, ceilCoord.y)),
			fractCoord.y
		),
		mix(
			hash12(vec2(ceilCoord.x, floorCoord.y)),
			hash12(vec2(ceilCoord.x, ceilCoord.y)),
			fractCoord.y
		),
		fractCoord.x
	);
}

vec2 hash23(vec3 p3) {
	p3 = fract(p3 * vec3(0.1031, 0.1060, 0.0973));
	p3 += dot(p3, p3.yzx + 33.33);
	return fract((p3.xx + p3.yz) * p3.zy);
}

vec3 smoothHue(float h) {
	return sqrt(normalize(square(cos(h * TAU - vec3(0.0, 1.0, 2.0) * (TAU / 3.0)) * 0.5 + 0.5)));
}

float interleavedGradientNoise(vec2 coord) {
	return fract(dot(coord, vec2(3.555713358, 0.3092692451)));
}

vec3 wave(vec2 coord, float t) {
	//maxima code:
	//	smooth(x) := x^2 * (3 - 2 * x);
	//	r: smooth(sqrt(x^2 + y^2) / t) * t;
	//	wave: -sin(48 * (r - t)) * sqrt(t) * (1 - t) ^ 3 * exp(16 * (r - t));
	//	optimize([ combine(diff(wave, x)), wave, combine(diff(wave, y)) ]);
	//hand-optimized slightly after that.

	//float var1 = -t;
	float var3 = dot(coord, coord);
	if (var3 >= t * t) return vec3(0.0);
	float var2 = 1.0 / t;
	float var4 = sqrt(var3);
	float var5 = 3.0 - 2.0 * var2 * var4;
	float var6 = var2 * var3 * var5 - t;
	float var7 = exp(16.0 * var6);
	float var8 = cube(1.0 - t);
	float var9 = sqrt(t);
	float var10 = var2 * var2;
	float common1 = 2.0 * (var2 * var5 - var10 * var4);
	float var11 = coord.x * common1;
	float var12 = 48.0 * var6;
	float var13 = cos(var12);
	float var14 = sin(var12);
	float var15 = coord.y * common1;
	float common2 = var7 * var8 * var9;
	float common3 = common2 * (16.0 * var14 + 48.0 * var13);
	return -vec3(var11 * common3, var14 * common2, var15 * common3);
}

vec3 multiWave(vec2 projection, vec2 offset) {
	vec2 floorProjection = floor(projection + offset);
	float timeOffset = hash12(floorProjection - offset);
	vec2 origin = hash23(vec3(floorProjection - offset, floor(time * 0.125 + timeOffset)));
	vec2 relative = fract(projection + offset) - origin;
	vec2 space = min(origin, 1.0 - origin);
	relative /= min(space.x, space.y);
	return wave(relative, fract(time * 0.125 + timeOffset));
}

void main() {
	vec4 tmp = inverseProjection * vec4(texcoord * 2.0 - 1.0, 1.0, 1.0);
	vec3 norm = normalize(mat3(inverseModelView) * tmp.xyz);
	vec2 projection = norm.xz / abs(norm.y);
	vec2 ray = projection;
	projection += cameraPosition.xz * 0.015625;
	ray += multiWave(projection, vec2(0.0,  0.0 )).xz;
	ray += multiWave(projection, vec2(0.0,  0.25)).xz;
	ray += multiWave(projection, vec2(0.0,  0.5 )).xz;
	ray += multiWave(projection, vec2(0.0,  0.75)).xz;
	ray += multiWave(projection, vec2(0.25, 0.0 )).xz;
	ray += multiWave(projection, vec2(0.25, 0.25)).xz;
	ray += multiWave(projection, vec2(0.25, 0.5 )).xz;
	ray += multiWave(projection, vec2(0.25, 0.75)).xz;
	ray += multiWave(projection, vec2(0.5,  0.0 )).xz;
	ray += multiWave(projection, vec2(0.5,  0.25)).xz;
	ray += multiWave(projection, vec2(0.5,  0.5 )).xz;
	ray += multiWave(projection, vec2(0.5,  0.75)).xz;
	ray += multiWave(projection, vec2(0.75, 0.0 )).xz;
	ray += multiWave(projection, vec2(0.75, 0.25)).xz;
	ray += multiWave(projection, vec2(0.75, 0.5 )).xz;
	ray += multiWave(projection, vec2(0.75, 0.75)).xz;
	float fraction = interleavedGradientNoise(floor(gl_FragCoord.xy));
	vec3 color = vec3(0.0);
	for (int loop = 0; loop < 16; loop++) {
		float progress = (fraction + float(loop)) * 0.0625 + 1.0;
		vec2 ray = projection + ray * progress;
		float density = 1.0 - abs(noise12(vec2(ray.x * 0.25 + time * 0.03125, ray.y)) * 8.0 - 4.0);
		if (density > 0.0) {
			density *= square(noise12(vec2(ray.x * 8.0, ray.y * 8.0 + time * 0.5)) * 2.0 - 1.0);
			color += smoothHue(mix(noise12(vec2(ray * 1.5 + time * 0.1875)) * 0.5 + 0.375, 1.0, collapse * (2.0 - collapse))) * density * (0.5 - 0.5 * square(progress * 2.0 - 3.0));
		}
	}
	fragColor = vec4(color, 1.0);
}