#version 120

precision highp float;
precision highp int;

uniform mat4 gbufferModelViewInverse;

varying vec4 color;
varying vec2 texcoord;
varying vec3 normal;
varying vec3 viewPos;
varying vec2 lmcoord;

void main() {
    gl_Position = ftransform();
    color = gl_Color;
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmcoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
    normal = normalize(gl_NormalMatrix * gl_Normal);
    viewPos = (gl_ModelViewMatrix * gl_Vertex).xyz;
}
