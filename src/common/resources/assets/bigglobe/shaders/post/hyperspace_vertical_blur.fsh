#version 150

uniform sampler2D previousPass;

layout(std140) uniform Uniforms {
	ivec2 resolution;
};

out vec4 outColor;

vec4 square(vec4 color) {
	return color * color;
}

void main() {
	ivec2 coord = ivec2(gl_FragCoord.xy);
	int start = max(coord.y - 4, 0);
	int end = min(coord.y + 4, resolution.y - 1);
	vec4 color = vec4(0.0);
	for (coord.y = start; coord.y <= end; coord.y++) {
		//assume previous pass has alpha of 1.0 everywhere.
		color += square(texelFetch(previousPass, coord, 0));
	}
	outColor = sqrt(color / color.w);
}