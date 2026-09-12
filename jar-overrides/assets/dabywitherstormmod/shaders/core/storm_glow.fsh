#version 330

#moj_import <minecraft:dynamictransforms.glsl>

// -----------------------------------------------------------------------------------
// STORM GLOW -- the light around the Wither Storm's teeth and eyes.
//
// Each glow is ONE camera-facing quad; this shader turns it into a round pool of light
// whose brightness falls off smoothly from the centre. Doing the falloff per PIXEL is
// the whole point: the old glow was a 20-segment triangle fan with vertex alpha, so the
// gradient was linear and polygonal -- that is what read as a flat 2D decal stuck on
// the head. A gaussian body + a tight hot core + a white-hot centre is what real
// over-exposed light looks like.
//
// Paired with the ENTITY_EMISSIVE snippet (Sampler0 only, EMISSIVE) so it runs on the
// same vertex shader as everything else: core/fogless_entity with NO_OVERLAY +
// NO_CARDINAL_LIGHTING. The shader must never declare a uniform the pipeline's bind
// group layout lacks -- that crashes on Vulkan even where OpenGL shrugs.
//
// Deliberately UNFOGGED: this is emitted light, so it punches through the murk instead
// of being washed out by it (same call as the storm's night glow). Fog can't simply be
// mixed in here anyway -- the blend is ADDITIVE (ONE, ONE), so mixing toward the fog
// colour would add a bright square across the whole quad.
//
// 1.9.215 R2 -- phase palette, not forced blue. The mod pushes the phase colour
// through the VERTEX colour every tick (McsmTeethPhaseTint -> DabyWSClientConfig
// .eyeColorR/G/B): pure white in phase 5 (the aura phase), cyan-white elsewhere,
// greenish-blue in phase 6, green-white in phase 7. The old shader threw that away
// with a hard re-hue to blue AND scaled the pool by the bound texture's luminance --
// the eye glow binds a dark atlas tile, so the eyes dimmed to near-black. Now the
// texture only SHAPES the light (alpha + a floor-capped luminance gate), and the
// pool colour is the phase palette at full strength.
// -----------------------------------------------------------------------------------

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    // UV0 spans the whole glow quad, so this is just the offset from its centre.
    vec2 p = texCoord0 * 2.0 - 1.0;
    float d2 = dot(p, p);
    if (d2 >= 1.0) {
        discard; // outside the disc: keeps the quad's corners from showing
    }
    float d = sqrt(d2);
    float edge = 1.0 - d; // 1 at the centre, 0 exactly on the rim

    // Soft body (gaussian, forced to zero at the rim so there is never a visible circle)
    // plus a gentle core. Kept SOFT on purpose: a tight, steep core turns each quad into a
    // hard little bead, and a row of those reads as beads rather than as one light.
    float halo = exp(-d2 * 2.0) * edge;
    float core = edge * edge;

    // The phase palette lives in the vertex colour; the texture only shapes the
    // light. A dark atlas tile must never dim the pool to near-black again.
    vec4 tex = texture(Sampler0, texCoord0);
    float texLum = dot(tex.rgb, vec3(0.2126, 0.7152, 0.0722));
    float texShape = clamp(0.30 + 0.70 * max(tex.a, texLum), 0.0, 1.0);

    vec3 tint = vertexColor.rgb;
    // Never let a dim tint pull the pool down; brighten low-luma colours up to
    // 1.7x, leave bright ones alone. (Phase 5's pure white is untouched.)
    float lum = max(dot(tint, vec3(0.2126, 0.7152, 0.0722)), 0.12);
    vec3 rgb = tint * clamp(1.0 / max(lum, 0.55), 1.0, 1.7);

    // The brightest part washes toward white so the centre reads as over-exposed
    // light: phase 5 = pure white teeth with a white aura, other phases keep
    // their colour everywhere except the hot heart.
    rgb = mix(rgb, vec3(1.0), core * 0.25);

    // Slight floor so even a dim Java-side alpha still reads as a glow, never black.
    float a = clamp(vertexColor.a * 1.15 + 0.07, 0.0, 1.0);
    float intensity = clamp((halo * 0.90 + core * 0.35) * 1.15, 0.0, 1.0) * a * texShape;
    if (intensity <= 0.003) {
        discard;
    }

    // ADDITIVE blending is (ONE, ONE) -- the alpha channel is ignored by the blender,
    // so the falloff has to be premultiplied into the colour itself.
    fragColor = vec4(rgb * intensity, intensity) * ColorModulator;
}
