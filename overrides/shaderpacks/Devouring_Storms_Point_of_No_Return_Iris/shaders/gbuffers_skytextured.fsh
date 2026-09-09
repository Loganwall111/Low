#version 330 compatibility

// Sun and moon. Kept close to vanilla so the celestial bodies still read,
// with a mild warm push on the sun to match the Story Mode palette.

in vec2 texcoord;
in vec4 glcolor;
uniform sampler2D gtexture;

void main() {
    vec4 c = texture(gtexture, texcoord) * glcolor;
    c.rgb *= vec3(1.04, 0.99, 0.94);
    gl_FragData[0] = c;
}
