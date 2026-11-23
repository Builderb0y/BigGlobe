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
const float GOLDEN_ANGLE = 2.39996322972865332;

vec2 square(vec2 v) {
	return v * v;
}

vec2 smoothify(vec2 v) {
	return v * v * (3.0 - 2.0 * v);
}

float hash12(vec2 p) {
	vec3 p3 = fract(p.xyx * 0.1031);
	p3 += dot(p3, p3.yzx + 33.33);
	return fract((p3.x + p3.y) * p3.z);
}

vec3 cloudNoise(vec2 coord) {
	vec2 fractCoord = fract(coord);
	vec2 floorCoord = coord - fractCoord;
	vec2 ceilCoord = floorCoord + vec2(1.0);
	vec2 smoothCoord = smoothify(fractCoord);
	vec2 dSmooth = square(fractCoord - fractCoord * fractCoord) * 16.0;

	float corner00 = hash12(vec2(floorCoord.x, floorCoord.y));
	float corner01 = hash12(vec2(floorCoord.x,  ceilCoord.y));
	float corner10 = hash12(vec2( ceilCoord.x, floorCoord.y));
	float corner11 = hash12(vec2( ceilCoord.x,  ceilCoord.y));

	float mixX0 = mix(corner00, corner10, smoothCoord.x);
	float mixX1 = mix(corner01, corner11, smoothCoord.x);
	float mixY0 = mix(corner00, corner01, smoothCoord.y);
	float mixY1 = mix(corner10, corner11, smoothCoord.y);

	float value = mix(mixY0, mixY1, smoothCoord.x);
	vec2 derivative = (vec2(mixY1, mixX1) - vec2(mixY0, mixX0)) * dSmooth;

	return vec3(derivative, value);
}

void main() {
	vec3 color = texture(previousPass, texcoord).rgb;
	vec4 tmp = inverseProjection * vec4(texcoord * 2.0 - 1.0, 1.0, 1.0);
	vec3 norm = normalize(mat3(inverseModelView) * tmp.xyz);
	vec2 projection = norm.xz / abs(norm.y) + cameraPosition.xz * 0.03125;
	vec3 cloud = (
		+ cloudNoise(projection *  1.0 + time * (vec2(cos(GOLDEN_ANGLE * 1.0), sin(GOLDEN_ANGLE * 1.0)) * 0.0625)) * 0.5
		+ cloudNoise(projection *  2.0 + time * (vec2(cos(GOLDEN_ANGLE * 2.0), sin(GOLDEN_ANGLE * 2.0)) * 0.125 )) * 0.25
		+ cloudNoise(projection *  4.0 + time * (vec2(cos(GOLDEN_ANGLE * 3.0), sin(GOLDEN_ANGLE * 3.0)) * 0.25  )) * 0.125
		+ cloudNoise(projection *  8.0 + time * (vec2(cos(GOLDEN_ANGLE * 4.0), sin(GOLDEN_ANGLE * 4.0)) * 0.5   )) * 0.0625
		+ cloudNoise(projection * 16.0 + time * (vec2(cos(GOLDEN_ANGLE * 5.0), sin(GOLDEN_ANGLE * 5.0)) * 1.0   )) * 0.03125
	);
	cloud.z = cloud.z * 3.0 - 2.0;
	if (cloud.z > 0.0) {
		color = mix(
			pow(
				vec3(0.375, 0.4375, 0.5),
				vec3(
					//equivalent to:
					//2.0 - dot(normalize(cloud), normalize(vec3(projection, 1.0)))
					//but with one fewer sqrt operation.
					2.0
					- dot(cloud, vec3(projection, 1.0))
					* inversesqrt(
						dot(cloud, cloud)
						* (dot(projection, projection) + 1.0)
					)
				)
			),
			color,
			exp2(cloud.z * -16.0)
		);
	}
	color *= smoothstep(0.0, 0.25, abs(norm.y));
	fragColor = vec4(color, 1.0);
}