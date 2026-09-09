#version 330 compatibility
/* MCSM v2: passthrough, lightmap mult dropped to match textured pass. */
in vec4 glcolor;
void main() {
    gl_FragData[0] = glcolor;
}
