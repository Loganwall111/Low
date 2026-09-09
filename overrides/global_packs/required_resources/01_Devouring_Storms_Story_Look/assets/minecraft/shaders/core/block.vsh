#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:chunksection.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:sample_lightmap.glsl>

// ============================================================================
//  MCSM visuals - terrain.vsh  [spec file "rendertype_solid.vsh", rebased on
//  the REAL 26.2 terrain.vsh: no Normal attribute any more -- chunk world
//  placement comes from ChunkSection + Globals camera vectors, and the
//  faked-shadow dot product runs in the fragment stage off a reconstructed
//  per-face geometric normal (see terrain.fsh).]
// ============================================================================

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

uniform sampler2D Sampler2;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;
out vec3 mcsmWorldPos;

void main() {
    vec3 pos = Position + (ChunkPosition - CameraBlockPos) + CameraOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    sphericalVertexDistance = fog_spherical_distance(pos);
    cylindricalVertexDistance = fog_cylindrical_distance(pos);
    // MCSM 1.9.84 -- COLOURED LIGHTING MOVED INTO THE MOD.
    // This was previously only in the Iris pack (gbuffers_terrain COLORED_LIGHT),
    // so with shaders OFF the world was flatly lit and the user reported "no
    // coloured lighting". Ported verbatim from the pack's mcsmLightmap() so it
    // now works in vanilla play, which is what the user asked for.
    // UV2.y is the sky-light coordinate; low = torch/night, high = daylight.
    vec4 lmSample = sample_lightmap(Sampler2, UV2);
    float mcsmT = clamp(float(UV2.y) / 240.0, 0.0, 1.0);
    vec3 mcsmDay   = vec3(1.06, 0.99, 0.90);
    vec3 mcsmWarm  = vec3(1.10, 0.86, 0.66);
    vec3 mcsmNight = vec3(0.42, 0.55, 1.00);
    vec3 mcsmTint  = mix(mcsmWarm, mcsmDay, smoothstep(0.55, 0.95, mcsmT));
    mcsmTint = mix(mcsmNight * (0.55 + 0.45 * mcsmT), mcsmTint,
                   smoothstep(0.05, 0.45, mcsmT));
    // 0.50 matches the pack's COLORED_LIGHT_AMT default.
    lmSample.rgb *= mix(vec3(1.0), mcsmTint, 0.50);
    vertexColor = Color * lmSample;
    texCoord0 = UV0;

    // MCSM: world position per vertex; the fragment stage derives the exact
    // axis-aligned face normal from its screen derivatives.
    mcsmWorldPos = pos;
}
