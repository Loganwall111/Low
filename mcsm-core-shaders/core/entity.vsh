#version 330

#if defined(PER_FACE_LIGHTING) || !defined(NO_CARDINAL_LIGHTING)
#moj_import <minecraft:light.glsl>
#endif
#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:sample_lightmap.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:mcsm_visuals.glsl>

// ============================================================================
//  MCSM visuals - entity.vsh   [spec file #3, rebased on real 26.2 entity.vsh]
//  Dynamic vertex tracking for back-of-storm attachments.
//  In 26.2 translucent entities are a define-variant of this shared program
//  (PER_FACE_LIGHTING / EMISSIVE / NO_OVERLAY / DISSOLVE...), so the override
//  is un-bypassable: there is no separate entity_translucent file any more.
//
//  MESH CONTRACT: attachment vertices mark themselves with a maxed lightmap
//  sentinel UV2 >= (3900, 3900). Even with our shaders stripped the sentinel
//  clamps to a full-bright lightmap corner, so attachments stay glowing.
// ============================================================================

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

#ifndef NO_OVERLAY
uniform sampler2D Sampler1;
#endif

#ifndef EMISSIVE
uniform sampler2D Sampler2;
#endif

out float sphericalVertexDistance;
out float cylindricalVertexDistance;

#ifdef PER_FACE_LIGHTING
out vec4 vertexPerFaceColorBack;
out vec4 vertexPerFaceColorFront;
#else
out vec4 vertexColor;
#endif

#ifndef EMISSIVE
out vec4 lightMapColor;
#endif

#ifndef NO_OVERLAY
out vec4 overlayColor;
#endif

out vec2 texCoord0;
flat out int mcsmAttach;
out vec3 mcsmLocalPos;
// Specular rim carrier for the storm body/head/tentacle entity pass.
out float mcsmRim;

void main() {
    vec3 pos = Position;
    mcsmLocalPos = Position;

    mcsmAttach = (UV2.x > 3900 && UV2.y > 3900) ? 1 : 0;

    // Cinematic purple edge sheen. Work in view space so the view direction
    // is the player's actual camera direction for every pose and phase.
    vec3 mcsmViewNormal = normalize(mat3(ModelViewMat) * Normal);
    vec3 mcsmViewPosition = (ModelViewMat * vec4(pos, 1.0)).xyz;
    vec3 viewDir = normalize(-mcsmViewPosition);
    float rim = 1.0 - max(dot(viewDir, mcsmViewNormal), 0.0);
    rim = pow(rim, 3.5);
    mcsmRim = rim;

    float mcsmP = mcsm_phase(FogSkyEnd, FogColor, FogRenderDistanceEnd);
    bool mcsmStormy = mcsm_active(mcsmP);

    if (mcsmAttach == 1 && mcsmStormy) {
        // Vortex writhe in model space BEFORE the transform, so the offset
        // rides the boss pose matrix upstream: the attachment follows the
        // storm automatically, no BossPos plumbing required.
        float clock = mcsm_clock(GameTime);
        float ang = pos.y * 0.06 + sin(clock * 0.35) * 0.6;
        float ca = cos(ang);
        float sa = sin(ang);
        pos.xz = vec2(pos.x * ca - pos.z * sa, pos.x * sa + pos.z * ca);
        pos.xz *= 1.0 + 0.12 * sin(clock * 0.9 + pos.y * 0.25);
    }

    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    if (mcsmAttach == 1 && mcsmStormy) {
        // Spec animation math, kept verbatim and w-scaled so the 0.5 offset
        // means half a block in world units, not half a screen:
        //     gl_Position.xyz += sin(witherstorm_GameTime * 3.0) * 0.5;
        if (gl_Position.w > 0.001) {
            gl_Position.xyz += sin(witherstorm_GameTime * 3.0) * 0.5 * gl_Position.w;
        }
    }

    sphericalVertexDistance = fog_spherical_distance(pos);
    cylindricalVertexDistance = fog_cylindrical_distance(pos);

#ifdef PER_FACE_LIGHTING
    if (mcsmAttach == 1) {
        // Energy lattice is emissive: flat colour, no directional shading,
        // both faces identical.
        vertexPerFaceColorBack  = Color;
        vertexPerFaceColorFront = Color;
    } else {
        vec2 light = minecraft_compute_light(Light0_Direction, Light1_Direction, Normal);
        vertexPerFaceColorBack  = minecraft_mix_light_separate(-light, Color);
        vertexPerFaceColorFront = minecraft_mix_light_separate(light, Color);
    }
#elif defined(NO_CARDINAL_LIGHTING)
    vertexColor = Color;
#else
    vertexColor = mcsmAttach == 1 ? Color : minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color);
#endif

#ifndef EMISSIVE
    lightMapColor = mcsmAttach == 1 ? vec4(1.0) : sample_lightmap(Sampler2, UV2);
#endif

#ifndef NO_OVERLAY
    overlayColor = texelFetch(Sampler1, UV1, 0);
#endif

    texCoord0 = UV0;

#ifdef APPLY_TEXTURE_MATRIX
    texCoord0 = (TextureMat * vec4(UV0, 0.0, 1.0)).xy;
#endif
}
