#version 330 compatibility
/* Devouring Storms v6: dark opaque Story Mode water.
 * No reflection math, no water normals, no caustic rays. The shader keeps the
 * surface readable and accepts ordinary shadow/lighting from the pipeline. */

in vec2 texcoord;
in vec2 lmcoord;
in vec4 glcolor;

uniform sampler2D gtexture;
uniform sampler2D lightmap;

#define MCSM_DARK_WATER 1      // [0 1]
#define WATER_OPACITY   0.96   // [0.75 0.85 0.92 0.96 1.00]
#define WATER_DARKNESS  0.78   // [0.40 0.55 0.70 0.78 0.90]

void main() {
    vec4 color = texture(gtexture, texcoord) * glcolor;
    if (color.a <= 0.0) discard;
#if MCSM_DARK_WATER
    vec3 deep = vec3(0.018, 0.044, 0.130);
    vec3 top  = vec3(0.034, 0.090, 0.245);
    float l = clamp(texture(lightmap, lmcoord).y, 0.0, 1.0);
    color.rgb = mix(deep, top, l * 0.55) * WATER_DARKNESS;
    color.a = max(color.a, WATER_OPACITY);
#endif
    gl_FragData[0] = vec4(color.rgb, color.a);
}
