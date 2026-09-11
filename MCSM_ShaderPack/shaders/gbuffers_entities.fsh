#version 120

// ============================================================================
// MCSM gbuffers_entities.fsh — Story Mode entity lighting + the Wither Storm's
// luminescent turquoise teeth aura (emissive, pulsing) + magenta accents,
// plus the MATTE charcoal-indigo body pass: near-black sheets grade toward
// dark blue-violet charcoal and catch ONE restrained indigo edge light.
// No specular lobe, no sweeping glint band: matte MCSM flesh, never glossy.
// Colored lighting (warm sun / amber torch / lavender shadow) is ON by
// default via MCSM_LIGHTING, matching gbuffers_terrain.
// ============================================================================

#define EMISSIVE_TEETH_GLOW // Bright cyan (#00E5FF) bloom on Wither Storm teeth
#define MCSM_LIGHTING // warm/cool Story Mode colored lighting, ON by default

precision highp float;
precision highp int;

uniform sampler2D gtexture;
uniform float frameTimeCounter;

varying vec4 color;
varying vec2 texcoord;
varying vec2 lmcoord;
varying vec3 normal;
varying vec3 viewPos;

void main() {
    vec4 col = texture2D(gtexture, texcoord);
    col *= color;
    if (col.a < 0.1) {
        discard;
    }

    // Teeth arrive vertex-tinted cyan-white (0.45, 1.0, 1.0): keep the red
    // gate wide enough to catch them, tight enough to miss white cloth.
    float isTurquoise = step(0.65, col.g) * step(0.75, col.b) * (1.0 - step(0.55, col.r));
    float isMagenta   = step(0.60, col.r) * step(0.60, col.b) * (1.0 - step(0.50, col.g));

#ifdef MCSM_LIGHTING
    // Warm Story Mode sunlight, cool lavender ambient shadow, warm amber torchlight.
    float blockLight = clamp((lmcoord.x - 0.03) * 1.05, 0.0, 1.0);
    float skyLight   = clamp((lmcoord.y - 0.03) * 1.05, 0.0, 1.0);
    vec3 sunLightColor = vec3(1.12, 1.02, 0.90);
    vec3 shadowAmbientColor = vec3(0.70, 0.62, 0.88);
    vec3 torchColor = vec3(1.20, 0.78, 0.38);
    vec3 lightTerm = mix(shadowAmbientColor * 0.72, sunLightColor, pow(skyLight, 1.25))
                   + torchColor * pow(blockLight, 1.35) * 1.35;
    // Emissive teeth and accents bypass the lightmap so they burn at night.
    float lit = 1.0 - max(isTurquoise, isMagenta);
    col.rgb *= mix(vec3(1.0), lightTerm, lit);
#endif

#ifdef EMISSIVE_TEETH_GLOW
    if (isTurquoise > 0.5) {
        // Turquoise aura on the teeth: emissive core + breathing glow
        float pulse = 0.90 + 0.10 * sin(frameTimeCounter * 4.0);
        float halo = 0.30 + 0.20 * sin(frameTimeCounter * 2.3);
        col.rgb = vec3(0.0, 0.92, 1.0) * (3.5 * pulse + halo);
    } else if (isMagenta > 0.5) {
        float pulse = 0.92 + 0.08 * sin(frameTimeCounter * 3.0);
        col.rgb = vec3(0.85, 0.12, 0.95) * 3.0 * pulse;
    }
#endif

    // ---- MATTE CHARCOAL-INDIGO BODY: dark sheets stay flat, edges catch light ----
    float luma = dot(col.rgb, vec3(0.299, 0.587, 0.114));
    float isCharcoal = (1.0 - smoothstep(0.02, 0.16, luma)) * step(0.15, col.a);
    if (isCharcoal > 0.01) {
        vec3 n = normalize(normal);
        vec3 v = normalize(-viewPos);
        // Base lift: near-black -> matte charcoal-indigo.
        col.rgb = max(col.rgb, vec3(0.028, 0.030, 0.062));
        col.rgb *= vec3(0.86, 0.90, 1.10);
        // One restrained indigo edge light so the silhouette keeps its crisp rim.
        float fres = pow(1.0 - clamp(dot(n, v), 0.0, 1.0), 3.0);
        col.rgb += vec3(0.10, 0.12, 0.26) * fres * isCharcoal;
    }

    gl_FragColor = col;
}
