#version 330
#extension GL_ARB_separate_shader_objects : require

//layout(location = 0) is missing in vanilla 26.2,
//which causes it to be garbage in the fragment stage.
layout(location = 0) out vec2 texcoord;

void main() {
	//why is it called gl_VertexID in MC 26.2 and earlier,
	//but gl_VertexIndex in MC 26.3 and later?
	//this is not even a valid variable name according to
	//https://registry.khronos.org/OpenGL-Refpages/gl4/html/
	texcoord = vec2((ivec2(gl_VertexIndex) << ivec2(1, 0)) & 2);
	gl_Position = vec4(texcoord * 2.0 - 1.0, 0.0, 1.0);
}