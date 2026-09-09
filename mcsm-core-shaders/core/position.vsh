#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

// ============================================================================
//  MCSM visuals - sky.vsh
//  Companion to sky.fsh (REQUIRED pair: the extra varying must exist on both
//  sides). In 1.21.2+ there is no fog.fsh: the old standalone fog quad is the
//  sky pass, so sky.fsh owns both the skybox and the horizon fog color.
// ============================================================================

in vec3 Position;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec3 mcsmCamRay;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);

    // The sky pass ModelViewMat is rotation-only and positions are camera-space
    // frustum corners, so Position itself is the view ray.
    mcsmCamRay = Position;
}
