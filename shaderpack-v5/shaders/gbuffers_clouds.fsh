#version 330 compatibility
/* 1.9.152: soft volumetric cloud plane. Story Mode decks are painted by the
   sky pass; this softens the leftover vanilla cloud sticker into a pale
   white mass so looking straight up never reads as blocky MC cubes. */
in vec2 texcoord;
in vec2 lmcoord;
in vec4 glcolor;
uniform sampler2D gtexture;
void main() {
    vec4 color = texture(gtexture, texcoord) * glcolor;
    if (color.a <= 0.01) discard;
    vec3 soft = mix(vec3(0.86, 0.90, 0.98), color.rgb, 0.05); // almost pure soft mass
    color.rgb = soft;
    color.a *= 0.55; // softer decks, less cube read
    gl_FragData[0] = color;
}
