#version 330

// MCSM_visuals: thin companion to the repo's rendertype_clouds.vsh. The vsh
// already multiplies CloudColor once (rgb AND alpha) and bakes the per-face
// brightness, so this stage must NOT re-light or re-tint - doing both is what
// made the old cloud setup render as opaque walls.
//
// What stays here:
//   1. discard for crisp, blocky silhouettes (user rule: alpha < 0.1 gone),
//   2. the storm cloud tint (only while the fog carrier is up),
//   3. the distance fade that vanilla 26.2 does in THIS file (FogCloudsEnd),
//      reproduced from our smuggled carrier value so the storm dissolves the
//      clouds into the wall - and so that with no storm we behave byte-like
//      vanilla (carrier decode returns -1, fade skipped: the game's own
//      FogCloudsEnd path in the .vsh untouched? no - it lives here: when the
//      carrier is not up we fall through to exactly the vanilla multiply).

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:mcsm_visuals.glsl>

in float vertexDistance;
in vec4 vertexColor;
in vec3 mcsmCloudRay;

out vec4 fragColor;

void main() {
    vec4 color = vertexColor;

    if (color.a < 0.1) {
        discard;
    }

    float mcsmP = mcsm_phase(FogSkyEnd, FogColor, FogRenderDistanceEnd);

    if (mcsm_fog_active(mcsmP)) {
        color.rgb *= mcsm_cloud_tint(mcsmP);

        // MCSM 1.9.99 -- the heart mass occludes the cloud deck. Same field the
        // dome blob uses (mcsm_mass_cover), so the deck vanishes exactly where
        // the mass is opaque: the very top of the storm goes black and the
        // clouds stop showing through it. Only inside the 5.10-5.90 window the
        // dome blob itself lives in, so nothing changes in any other phase.
        // 1.9.221 (port): the window matches the infinite blob's 5.00-6.95
        // lifetime, so nothing changes in any other phase.
        if (mcsmP >= 5.00 && mcsmP <= 6.95) {
            vec4 aimC = mcsm_boss_dir(vec3(CameraBlockPos) + CameraOffset);
            if (aimC.w > 0.5) {
                vec3 wdC = normalize(transpose(mat3(ModelViewMat)) * normalize(mcsmCloudRay));
                float cover = mcsm_mass_cover(wdC, aimC.xyz, mcsmP);
                color.rgb *= (1.0 - cover);
                color.a   *= (1.0 - cover * 0.94);
            }
        }
    }

    // 26.2 fades clouds by FogCloudsEnd right here. Our carrier encodes the
    // storm's cloud end when active; with no storm, mcsm_clouds_end()
    // returns the vanilla FogCloudsEnd, so this line IS the vanilla behaviour.
    color.a *= 1.0f - linear_fog_value(vertexDistance, 0.0, mcsm_clouds_end());

    // MCSM v8: same vivid Story Mode grade the sky and terrain use, so the
    // Story Mode cloud deck sits in the identical colour space.
    color.rgb = mcsm_story_grade(color.rgb);

    fragColor = color;
}
