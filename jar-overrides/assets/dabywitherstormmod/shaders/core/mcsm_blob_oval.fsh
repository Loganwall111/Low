#version 330

#moj_import <minecraft:dynamictransforms.glsl>

// -----------------------------------------------------------------------------------
// MCSM OVAL BLOB -- one layer of the infinite skybox blob behind the Wither Storm.
//
// The blob is drawn by McsmBlobOval.java as a stack of camera-facing oval quads
// (core / mid / edge / bleed), each one textured by THIS shader. The quad's UV
// square is turned into a perfectly soft radial disc here, so every layer reads
// as a smooth smudge instead of a flat square:
//
//   * shape: 100% procedural smoothstep falloff from the disc centre -- no
//     texture sampling, so there are NO pixels to filter and no GL_NEAREST
//     blockiness (linear filtering is inherent);
//   * colour: the phase palette arrives in vertexColor (exact corrected hexes,
//     lerped per phase by McsmBlobOval -- #161A1D/#2D423F/#6A9A78 in phase 5,
//     #0B0410/#2D1442/#581C6E/#87529C in 5.5-5.9, #1A1226/#462A52/#966173/
//     #D89874 in phase 6);
//   * blending: ALPHA blending (BlendFunction.TRANSLUCENT on the pipeline) --
//     the dark core layer is near-opaque and physically darkens the sky behind
//     it, exactly like an opaque cinematic backdrop, while the outer layers
//     are translucent and let the sky bleed back in around the rim;
//   * fog: this pipeline is the fogless entity emissive path (FOG never
//     touches it) -- no linear or exponential distance fog, ever.
//
// Paired with core/fogless_entity (vertex) + the entity emissive snippet, the
// same layout storm_glow.fsh uses, so it binds on the mod's own pipeline.
// -----------------------------------------------------------------------------------

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    // UV0 spans the quad: offset from its centre, clamped to the unit disc.
    vec2 p = texCoord0 * 2.0 - 1.0;
    float d2 = dot(p, p);
    if (d2 >= 1.0) {
        discard; // outside the disc: the quad's corners never show
    }
    float d = sqrt(d2);

    // Soft full-disc body (smoothstep, zero at the rim so no edge seam ever
    // shows) plus a tighter hot centre so the layer reads as thick smeared
    // light, not a flat decal.
    float body = 1.0 - smoothstep(0.0, 1.0, d);
    float hot  = smoothstep(0.0, 0.35, 1.0 - d);

    // Alpha = layer strength from the Java side (core ~0.96 -> bleed ~0.28),
    // shaped by the smudge. Alpha blending does the rest.
    float alpha = vertexColor.a * (0.62 * body + 0.38 * hot);
    if (alpha <= 0.003) {
        discard;
    }

    vec3 rgb = vertexColor.rgb;
    fragColor = vec4(rgb, alpha) * ColorModulator;
}
