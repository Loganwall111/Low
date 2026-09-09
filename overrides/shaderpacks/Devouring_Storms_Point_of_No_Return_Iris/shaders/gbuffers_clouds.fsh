#version 330 compatibility
/* Devouring Storms v5: the pack OWNS the clouds now (user order: the pack
   must never revert the clouds to vanilla). Exact passthrough - the Story
   Mode cloud decks are painted by the sky pass (gbuffers_skybasic), and the
   vanilla cloud plane keeps its core-pack lighting and colour untouched. */
in vec2 texcoord;
in vec2 lmcoord;
in vec4 glcolor;
uniform sampler2D gtexture;
void main() {
    vec4 color = texture(gtexture, texcoord) * glcolor;
    if (color.a <= 0.0) discard;
    gl_FragData[0] = color;
}
