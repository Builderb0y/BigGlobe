#version 150

#moj_import <bigglobe:lod_vertex.glsl>
#moj_import <bigglobe:lod_decode_vertex_flat.glsl>

uniform Matrices {
	mat4 modelViewProjectionMatrix;
};

out Vertex vertexData;

void main() {
	vertexData = decodeVertex();
	gl_Position = modelViewProjectionMatrix * vec4(vertexData.pos, 1.0);
}