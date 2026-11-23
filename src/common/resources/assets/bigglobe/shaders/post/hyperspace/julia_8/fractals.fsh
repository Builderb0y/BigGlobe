#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D previousPass;

layout(std140) uniform Uniforms {
	mat4 inverseModelView;
	mat4 inverseProjection;
	vec4 cameraPosition;
	float time;
	float collapse;
};

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

const float PI = 3.14159265359;
const float TAU = PI * 2.0;

float square(float f) {
	return f * f;
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
	for (int iteration = 0; iteration < 32; iteration++) {
		z = vec2(z.x * z.x - z.y * z.y, 2.0 * z.x * z.y);
		z = vec2(z.x * z.x - z.y * z.y, 2.0 * z.x * z.y);
		z = vec2(z.x * z.x - z.y * z.y, 2.0 * z.x * z.y);
		z += c;
		float z2 = dot(z, z);
		if (z2 > 16.0 * 16.0) {
			//https://iquilezles.org/articles/msetsmooth/
			return float(iteration) - log2(log2(z2) / log2(16.0 * 16.0)) / log2(8.0);
		}
	}
	return 0.0;
}

void main() {
	vec4 tmp = inverseProjection * vec4(texCoord * 2.0 - 1.0, 1.0, 1.0);
	vec3 norm = normalize(mat3(inverseModelView) * tmp.xyz);

	vec3 color = texture(previousPass, texCoord).rgb;

	float xzLength = length(norm.xz);
	vec2 planePos = norm.xz * (atan(xzLength / norm.y) / xzLength);

	//rest in peace, Nameless.
	//I wish I could show this to you now.
	float t = time * (1.0 / 64.0);
	vec2 sc = vec2(cos(t), sin(t));
	float r = cos(7.0 * t) * (-3.0 / 128.0) + (3.0 / 64.0);
	r = (64.0 * r) / mix(1.0, 64.0, r);
	r += collapse * collapse * 8.0;
	vec2 z = planePos;
	vec2 c = sc * r;
	float iterations = julia(z, c);
	if (iterations == 0.0) {
		for (float scale = 4.0; scale <= 6.0; scale += 0.5) {
			vec2 scaledPlanePos = planePos * exp2(scale) + cameraPosition.xz * sign(norm.y);
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
		color = mix(color, vec3(1.0), 1.0 / (exp2(4.0 - iterations * 2.0) + 1.0));
	}

	fragColor = vec4(color, 1.0);
}