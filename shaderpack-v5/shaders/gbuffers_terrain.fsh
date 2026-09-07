#version 330 compatibility
/*
 * MCSShaders v4 - terrain fragment.
 *
 * SCOPE CHANGE (v4): the Wither Storm mod now owns sun/moon shading, cloud
 * shadows, storm ground occlusion, fog palettes and the Story Mode tone curve
 * in its own baked core shaders. Duplicating any of that here fought the mod
 * and produced the wrong look, so this pass has ONE job left:
 *
 *     colourful lighting  (warm amber key by day, blue-violet by night)
 *
 * No sun shading. No storm wash. No tone/contrast. Those all live in the mod.
 */
in vec2 texcoord;
in vec2 lmcoord;
in vec4 glcolor;
in vec3 mcsmN;
in float mcsmDay;
in float mcsmShade;

uniform sampler2D gtexture;
uniform sampler2D lightmap;

#define COLORED_LIGHT       1     // [0 1]
#define COLORED_LIGHT_AMT   0.85  // [0.00 0.25 0.50 0.70 0.75 1.00]

vec3 mcsmLightmap(float t) {
    vec3 day   = vec3(1.10, 1.02, 0.92);
    vec3 warm  = vec3(1.18, 0.88, 0.62);
    vec3 night = vec3(0.38, 0.52, 1.05);
    vec3 c = mix(warm, day, smoothstep(0.55, 0.95, t));
    c = mix(night * (0.55 + 0.45 * t), c, smoothstep(0.05, 0.45, t));
    return c;
}

void main() {
    vec4 color = texture(gtexture, texcoord) * glcolor;
    if (color.a <= 0.0) discard;   // MCSM v3: OptiFine-only alpha-ref uniform removed; constant test keeps this program compiling under Iris

#if COLORED_LIGHT
    vec2 lm = texture(lightmap, lmcoord).xy;
    float t = min(lm.x, lm.y);
    // coloured key/fill tint on top of the mod's own grade
    color.rgb *= mix(vec3(1.0), mcsmLightmap(t), COLORED_LIGHT_AMT);
#endif

    // sun-face shading (tree canopies, block sides) — the vivid-world read
    color.rgb *= mcsmShade;

    gl_FragData[0] = vec4(color.rgb, color.a);
}
