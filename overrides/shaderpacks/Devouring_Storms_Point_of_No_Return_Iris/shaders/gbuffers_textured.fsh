#version 330 compatibility
/* MCSM v2: passthrough on purpose. No lightmap multiply — clouds and
   particles keep the exact core-pack lighting (user: shader must not
   take the clouds). */
in vec2 texcoord;
in vec2 lmcoord;
in vec4 glcolor;
uniform sampler2D gtexture;
void main() {
    vec4 color = texture(gtexture, texcoord) * glcolor;
    if (color.a <= 0.0) discard;   // MCSM v3: OptiFine-only alpha-ref uniform removed; constant test keeps this program compiling under Iris
    gl_FragData[0] = color;
}
