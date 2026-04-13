layout(std140) uniform ModelOffset {
	vec4 modelOffset;
};

in uvec3 rawVertexData;

Vertex decodeVertex() {
	Vertex vertex;
	vertex.pos.xz = vec2((uvec2(rawVertexData.x) >> uvec2(0u, 8u)) & uvec2(255u)) * (float(REGION_SIZE) / 128.0) - (0.5 * float(REGION_SIZE));
	vertex.pos.y = float(int(rawVertexData.x) >> 16) * (4096.0 / 32768.0);
	vertex.pos = vertex.pos * modelOffset.w + modelOffset.xyz;
	vertex.texcoord = vec2(float(rawVertexData.y & 65535u), float(rawVertexData.y >> 16u)) * (1.0 / 65536.0);
	vertex.tint = vec4((uvec4(rawVertexData.z) >> uvec4(0u, 6u, 12u, 18u)) & uvec4(63u)) * (1.0 / 64.0) + (0.5 / 64.0);
	vertex.lmcoord = vec2((uvec2(rawVertexData.z) >> uvec2(24u, 28u)) & uvec2(15u)) * (1.0 / 16.0) + (0.5 / 16.0);
	return vertex;
}