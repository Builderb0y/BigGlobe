#version 150

uniform sampler2D previousPass;

layout(std140) uniform Uniforms {
	mat4 inverseModelView;
	mat4 inverseProjection;
	float collapse;
};

in vec2 texCoord;

out vec4 fragColor;

const float PI = 3.14159265359;
const float TAU = PI * 2.0;

float square(float f) {
	return f * f;
}

float smoothify(float f) {
	return f * f * (f * -2.0 + 3.0);
}

float smootherify(float f) {
	return ((f * 6.0 - 15.0) * f + 10.0) * (f * f * f);
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

const vec3[] rings = vec3[](
	vec3(-0.7723748152639520, -0.6257217161535689, -0.1091305579560948),
	vec3(-0.4558079648356913, +0.8213886861667926, +0.3428698374449635),
	vec3(+0.6407589983979347, -0.2988057149410549, +0.7072079260660478),
	vec3(+0.1217859973729906, -0.6336616763953198, +0.7639640375775152),
	vec3(+0.5731406611404863, -0.0718927690284453, -0.8162972573204377),
	vec3(+0.1225799366355919, +0.2804134690372744, +0.9520201917590276),
	vec3(-0.9098248429157619, -0.1349507888222423, -0.3924373068523987),
	vec3(+0.4086476798044498, +0.7034281302411686, +0.5815461627212870)
);

struct Voronoise {
	float ringDist;
	int discriminator;
};

Voronoise voronoise(vec3 coord) {
	Voronoise result = Voronoise(1.0, 0);
	for (int index = 0; index < rings.length(); index++) {
		vec3 point = rings[index];
		float dotProduct = dot(coord, point);
		if (dotProduct < 0.0) {
			result.discriminator |= 1 << index;
			dotProduct = -dotProduct;
		}
		float threshold = float(index) * (0.25 / float(rings.length())) + 0.25;
		threshold = unmix(threshold, 1.0, collapse);
		threshold = clamp(threshold, 0.0, 1.0);
		threshold = smootherify(threshold);
		dotProduct -= threshold * 0.5;
		if (dotProduct < result.ringDist) {
			result.ringDist = dotProduct;
		}
	}
	return result;
}

void main() {
	vec4 tmp = inverseProjection * vec4(texCoord * 2.0 - 1.0, 1.0, 1.0);
	vec3 norm = normalize(mat3(inverseModelView) * tmp.xyz);
	Voronoise voronoi = voronoise(norm);
	float flashOffset = (hash11(float(voronoi.discriminator)) * 0.75 + 0.25) - collapse * collapse;
	float flash       = flashOffset > 0.0 ? 0.0 : exp2(flashOffset * 64.0);
	if (voronoi.ringDist < 0.0) {
		vec2 rng = hash21(float(voronoi.discriminator));
		float red = cos(voronoi.ringDist * 64.0) * 0.0625 + 0.25;
		fragColor = vec4(red, 0.0, 0.0, 1.0);
		fragColor.rgb += flash * exp2(voronoi.ringDist * 64.0);
	}
	else {
		vec3 color = texture(previousPass, texCoord).rgb + vec3(flash);
		fragColor = vec4(color, 1.0);
	}
}