#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform Uniforms {
	mat4 inverseModelView;
	mat4 inverseProjection;
	vec4 cameraPosition;
	float time;
	float collapse;
};

uniform sampler2D previousPass;

layout(location = 0) in vec2 texcoord;

layout(location = 0) out vec4 fragColor;

const float PI = 3.14159265359;
const float TAU = PI * 2.0;

float square(float f) {
	return f * f;
}

vec4 hash43(vec3 p) {
	vec4 p4 = fract(p.xyzx * vec4(0.1031, 0.1013, 0.0973, 0.1099));
	p4 += dot(p4, p4.wzxy + 33.33);
	return fract((p4.xxyz + p4.yzzw) * p4.zywx);
}

void main() {
	vec3 color = texture(previousPass, texcoord).rgb;
	vec4 tmp = inverseProjection * vec4(texcoord * 2.0 - 1.0, 1.0, 1.0);
	vec3 norm = normalize(mat3(inverseModelView) * tmp.xyz);
	vec2 projection = norm.xz / abs(norm.y);
	for (float scale = 3.0; scale <= 6.0; scale += 0.5) {
		vec2 scaledPlanePos = projection * exp2(scale) + cameraPosition.xz * 0.0625;
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
	fragColor = vec4(color, 1.0);
}