#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform Uniforms {
	mat4 inverseModelView;
	mat4 inverseProjection;
	vec4 cameraPosition;
	vec4 beamOrigin;
	float beamDirectionSeed;
	float beamTime;
	float time;
	float collapse;
};

layout(location = 0) in vec2 texcoord;

layout(location = 0) out vec4 fragColor;

const float PI = 3.14159265359;
const float TAU = PI * 2.0;

float square(float f) {
	return f * f;
}

vec3 square(vec3 v) {
	return v * v;
}

float smoothify(float f) {
	return f * f * (3.0 - 2.0 * f);
}

vec3 smoothify(vec3 v) {
	return v * v * (3.0 - 2.0 * v);
}

float hash11(float p) {
	p = fract(p * 0.1031);
	p *= p + 33.33;
	p *= p + p;
	return fract(p);
}

float noise11(float coord) {
	float fractCoord = fract(coord);
	float floorCoord = coord - fractCoord;
	float ceilCoord = floorCoord + 1.0;
	fractCoord = smoothify(fractCoord);
	return mix(
		hash11(floorCoord),
		hash11( ceilCoord),
		fractCoord
	);
}

vec4 hash43(vec3 p) {
	vec4 p4 = fract(p.xyzx * vec4(0.1061, 0.1030, 0.0973, 0.1099));
	p4 += dot(p4, p4.wzxy + 33.33);
	return fract((p4.xxyz + p4.yzzw) * p4.zywx);
}

vec3 hashWithTime33(vec3 coord) {
	vec4 hash = hash43(coord);
	return sin(hash.xyz * TAU + time * 0.5 * hash.w);
}

vec3 noise33(vec3 coord) {
	vec3 fractCoord = fract(coord);
	vec3 floorCoord = coord - fractCoord;
	vec3 ceilCoord = floorCoord + vec3(1.0);
	fractCoord = smoothify(fractCoord);
	return mix(
		mix(
			mix(
				hashWithTime33(vec3(floorCoord.x, floorCoord.y, floorCoord.z)),
				hashWithTime33(vec3(floorCoord.x, floorCoord.y,  ceilCoord.z)),
				fractCoord.z
			),
			mix(
				hashWithTime33(vec3(floorCoord.x,  ceilCoord.y, floorCoord.z)),
				hashWithTime33(vec3(floorCoord.x,  ceilCoord.y,  ceilCoord.z)),
				fractCoord.z
			),
			fractCoord.y
		),
		mix(
			mix(
				hashWithTime33(vec3( ceilCoord.x, floorCoord.y, floorCoord.z)),
				hashWithTime33(vec3( ceilCoord.x, floorCoord.y,  ceilCoord.z)),
				fractCoord.z
			),
			mix(
				hashWithTime33(vec3( ceilCoord.x,  ceilCoord.y, floorCoord.z)),
				hashWithTime33(vec3( ceilCoord.x,  ceilCoord.y,  ceilCoord.z)),
				fractCoord.z
			),
			fractCoord.y
		),
		fractCoord.x
	);
}

float interleavedGradientNoise(vec2 coord) {
	return fract(dot(coord, vec2(3.555713358, 0.3092692451)));
}

vec3 smoothHue(float h) {
	return sqrt(normalize(square(cos(h * TAU - vec3(0.0, 1.0, 2.0) * (TAU / 3.0)) * 0.5 + 0.5)));
}

vec3 mixLinear(vec3 a, vec3 b, float f) {
	return sqrt(mix(a * a, b * b, f));
}

float sigmoid(float f) {
	return f / sqrt(f * f + 1.0) * 0.5 + 0.5;
}

void main() {
	vec4 tmp = inverseProjection * vec4(texcoord * 2.0 - 1.0, 1.0, 1.0);
	vec3 norm = normalize(mat3(inverseModelView) * tmp.xyz);
	float light;
	if (beamTime >= 0.0) {
		light = min(dot(normalize(cameraPosition.xyz - beamOrigin.xyz), norm), 0.0);
		light *= light;
		light *= light;
		light *= (
			square(max(1.0 - beamTime * 2.0, 0.0)) *
			(noise11(time * 16.0) * 0.25 + 0.75)
		);
	}
	else {
		light = 0.0;
	}
	vec4 accum = vec4(0.0, 0.0, 0.0, 1.0);
	float stepSize = 64.0;
	float dist = interleavedGradientNoise(floor(gl_FragCoord.xy)) * stepSize;
	for (int step = 0; step < 4; step++, dist += stepSize) {
		vec3 pos = norm * dist + cameraPosition.xyz;
		vec3 noise = (
			+ (noise33(pos * (1.0 / 32.0)) * 0.5    )
			+ (noise33(pos * (1.0 / 16.0)) * 0.25   )
			+ (noise33(pos * (1.0 /  8.0)) * 0.125  )
			+ (noise33(pos * (1.0 /  4.0)) * 0.0625 )
			+ (noise33(pos * (1.0 /  2.0)) * 0.03125)
		);
		float density = sigmoid(4.0 * noise.x + 2.0 * collapse);
		float altDensity = 1.0 / (exp2(8.0 - 16.0 * noise.x - (sin(time * PI) * 4.0 + 8.0) * collapse * collapse) + 1.0);
		float brightness = noise.y * 0.125 + 0.125;
		float hue = noise.z * 0.25 + 0.66666666;
		hue = mix(hue, 1.0, collapse * (2.0 - collapse));
		vec3 color = smoothHue(hue);
		color = mixLinear(vec3(brightness), color, altDensity);
		color = mixLinear(color, vec3(1.0), light);
		accum = mix(accum, vec4(color, 0.0), density * vec4(accum.aaa, 1.0));
	}
	fragColor = vec4(accum.rgb, 1.0);
}