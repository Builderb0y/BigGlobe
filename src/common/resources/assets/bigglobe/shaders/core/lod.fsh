#version 150

#moj_import <bigglobe:lod_vertex.glsl>
#moj_import <minecraft:fog.glsl>

uniform sampler2D blockAtlas;
uniform sampler2D lightmap;

layout(std140) uniform ExtraFog {
	float altitude;
	float verticalFogMultiplier;
	float globalFogMultiplier;
};

in Vertex vertexData;

out vec4 color;

float funnyCurveThingy(float x) {
	return x >= 8.0 ? x : log(exp(x) + 1.0);
}

void main() {
	color = (
		texture(blockAtlas, vertexData.texcoord) *
		texture(lightmap, vertexData.lmcoord) *
		vertexData.tint
	);

	#ifdef ALPHA_CUTOUT
		if (color.a < ALPHA_CUTOUT) {
			discard;
			return;
		}
	#endif

	if (globalFogMultiplier > 0.0) {
		float dist = length(vertexData.pos);
		float opticalDepth;
		if (verticalFogMultiplier > 0.0) {
			vec3 ray = vertexData.pos / dist;
			opticalDepth = (dist + (funnyCurveThingy(verticalFogMultiplier * altitude) - funnyCurveThingy(verticalFogMultiplier * (ray.y * dist + altitude))) / (ray.y * verticalFogMultiplier));
		}
		else {
			opticalDepth = dist;
		}
		color.rgb = mix(FogColor.xyz, color.rgb, exp2(opticalDepth * -globalFogMultiplier));
	}
}