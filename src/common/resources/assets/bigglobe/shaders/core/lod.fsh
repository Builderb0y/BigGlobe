#version 330
#extension GL_ARB_separate_shader_objects : require

#ifdef USE_INCLUDE_DIRECTIVE
	#include <bigglobe:lod_vertex.glsl>
	#include <minecraft:fog.glsl>
#else
	#moj_import <bigglobe:lod_vertex.glsl>
	#moj_import <minecraft:fog.glsl>
#endif

uniform sampler2D blockAtlas;
uniform sampler2D lightmap;

layout(std140) uniform ExtraFog {
	float altitude;
	float verticalFogMultiplier;
	float globalFogMultiplier;
};

layout(location = 0) in vec3 pos;
layout(location = 1) in vec2 texcoord;
layout(location = 2) in vec2 lmcoord;
layout(location = 3) in vec4 tint;

layout(location = 0) out vec4 color;

float funnyCurveThingy(float x) {
	return x >= 8.0 ? x : log(exp(x) + 1.0);
}

void main() {
	color = (
		texture(blockAtlas, texcoord) *
		texture(lightmap, lmcoord) *
		tint
	);

	#ifdef ALPHA_CUTOUT
		if (color.a < ALPHA_CUTOUT) {
			discard;
			return;
		}
	#endif

	if (globalFogMultiplier > 0.0) {
		float dist = length(pos);
		float opticalDepth;
		if (verticalFogMultiplier > 0.0) {
			vec3 ray = pos / dist;
			opticalDepth = (dist + (funnyCurveThingy(verticalFogMultiplier * altitude) - funnyCurveThingy(verticalFogMultiplier * (ray.y * dist + altitude))) / (ray.y * verticalFogMultiplier));
		}
		else {
			opticalDepth = dist;
		}
		color.rgb = mix(FogColor.xyz, color.rgb, exp2(opticalDepth * -globalFogMultiplier));
	}
}