#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform Uniforms {
	float collapse;
};

uniform sampler2D previousPass;

layout(location = 0) in vec2 texcoord;

layout(location = 0) out vec4 fragColor;

vec3 square(vec3 v) {
	return v * v;
}

float interleavedGradientNoise(vec2 coord) {
	return fract(dot(coord, vec2(3.555713358, 0.3092692451)));
}

void main() {
	vec2 end = mix(texcoord, vec2(0.5), collapse);
	float ign = interleavedGradientNoise(floor(gl_FragCoord.xy));
	vec3 color = vec3(0.0);
	for (int step = 0; step < 16; step++) {
		float fraction = (float(step) + ign) * 0.0625;
		color += square(texture(previousPass, mix(texcoord, end, fraction)).rgb);
	}
	color = sqrt(color * 0.0625);
	fragColor = vec4(color, 1.0);
}