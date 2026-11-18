#version 330
#extension GL_ARB_separate_shader_objects : require

#ifdef USE_INCLUDE_DIRECTIVE
	#include <bigglobe:lod_vertex.glsl>
	#include <bigglobe:lod_decode_vertex_flat.glsl>
#else
	#moj_import <bigglobe:lod_vertex.glsl>
	#moj_import <bigglobe:lod_decode_vertex_flat.glsl>
#endif

uniform Matrices {
	mat4 modelViewProjectionMatrix;
};

layout(location = 0) out vec3 pos;
layout(location = 1) out vec2 texcoord;
layout(location = 2) out vec2 lmcoord;
layout(location = 3) out vec4 tint;

void main() {
	Vertex vertex = decodeVertex();
	pos = vertex.pos;
	texcoord = vertex.texcoord;
	lmcoord = vertex.lmcoord;
	tint = vertex.tint;
	gl_Position = modelViewProjectionMatrix * vec4(vertex.pos, 1.0);
}