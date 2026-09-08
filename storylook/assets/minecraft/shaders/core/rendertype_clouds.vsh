#version 330

// MCSM_visuals: the repo rendertype_clouds.vsh verbatim except
// #version 150 -> 330 (26.2 core contract; its CloudInfo UBO, CloudFaces
// buffer and fog_spherical_distance use already match vanilla 26.2 exactly).
// Extrudes the cloud slab 2.5x in Y, bakes CloudColor once - the paired .fsh
// must NOT re-tint (that double-multiply is what made the clouds opaque).

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

const int FLAG_MASK_DIR      = 7;
const int FLAG_INSIDE_FACE   = 1 << 4;
const int FLAG_USE_TOP_COLOR = 1 << 5;
const int FLAG_EXTRA_Z       = 1 << 6;
const int FLAG_EXTRA_X       = 1 << 7;

layout(std140) uniform CloudInfo {
    vec4 CloudColor;
    vec3 CloudOffset;
    vec3 CellSize;
};

uniform isamplerBuffer CloudFaces;

const float CloudFadeAlpha   = 0;   // 0 = a full 0 alpha fade
const float CloudHeight      = 2.5;   // vertical scaling
const float CloudYOffset     = 0.0;   // Y offset
const float BrightnessBottom = 1.0;
const float BrightnessTop    = 1.0;
const float BrightnessNorth  = 1.0;
const float BrightnessSouth  = 1.0;
const float BrightnessWest   = 1.0;
const float BrightnessEast   = 1.0;

out float vertexDistance;
out vec4 vertexColor;
// MCSM 1.9.99 -- view-space ray to this cloud vertex. The .fsh turns it into a
// world direction so the storm's heart mass can hide the deck at the very top
// ("you can't even see the clouds at the very top of the storm").
out vec3 mcsmCloudRay;

const vec3[] vertices = vec3[](
    vec3(1,0,0),vec3(1,0,1),vec3(0,0,1),vec3(0,0,0),   // Bottom
    vec3(0,1,0),vec3(0,1,1),vec3(1,1,1),vec3(1,1,0),   // Top
    vec3(0,0,0),vec3(0,1,0),vec3(1,1,0),vec3(1,0,0),   // North
    vec3(1,0,1),vec3(1,1,1),vec3(0,1,1),vec3(0,0,1),   // South
    vec3(0,0,1),vec3(0,1,1),vec3(0,1,0),vec3(0,0,0),   // West
    vec3(1,0,0),vec3(1,1,0),vec3(1,1,1),vec3(1,0,1)    // East
);

float lerp(float d, float e, float f) {
    return e + d * (f - e);
}

void main() {
    int quadVertex = gl_VertexID % 4;
    int index = (gl_VertexID / 4) * 3;

    int cellX = texelFetch(CloudFaces, index).r;
    int cellZ = texelFetch(CloudFaces, index + 1).r;
    int dirAndFlags = texelFetch(CloudFaces, index + 2).r;
    int direction = dirAndFlags & FLAG_MASK_DIR;
    bool isInsideFace = (dirAndFlags & FLAG_INSIDE_FACE) != 0;
    bool useTopColor = (dirAndFlags & FLAG_USE_TOP_COLOR) != 0;

    cellX = (cellX << 1) | ((dirAndFlags & FLAG_EXTRA_X) >> 7);
    cellZ = (cellZ << 1) | ((dirAndFlags & FLAG_EXTRA_Z) >> 6);

    vec3 faceVertex = vertices[(direction * 4) + (isInsideFace ? 3 - quadVertex : quadVertex)];

    // World position with scaling & offset
    vec3 scaledVertex = faceVertex * CellSize;
    scaledVertex.y *= CloudHeight; // vertical scaling
    vec3 pos = scaledVertex + (vec3(cellX, 0, cellZ) * CellSize) + CloudOffset + vec3(0, CloudYOffset, 0);

    vec4 viewPos = ModelViewMat * vec4(pos, 1.0);
    gl_Position = ProjMat * viewPos;
    mcsmCloudRay = viewPos.xyz;

    // Fog distance
    vertexDistance = fog_spherical_distance(pos);

    float brightness = 1.0; // appearsShaded=false
    if (useTopColor || direction == 1) brightness = BrightnessTop;
    else if (direction == 0) brightness = BrightnessBottom;
    else if (direction == 2) brightness = BrightnessNorth;
    else if (direction == 3) brightness = BrightnessSouth;
    else if (direction == 4) brightness = BrightnessWest;
    else if (direction == 5) brightness = BrightnessEast;

    vec3 rgb = vec3(brightness);
    float baseA = CloudColor.a;
    float finalA = baseA;

    // (fadeEnabled = true)
    float vertexY = pos.y - CloudOffset.y;  // local vertex height
    float normalizedY = clamp(vertexY / CloudHeight, 0.0, 1.0);

    float dir = clamp(CloudOffset.y / CloudHeight, -1.0, 1.0); // relYToCenter
    float fadeBelow = lerp(normalizedY, 1.0, CloudFadeAlpha);
    float fadeAbove = lerp(1.0 - normalizedY, 1.0, CloudFadeAlpha);
    float mixFactor = (dir + 1.0) * 0.5;
    float fade = mix(fadeBelow, fadeAbove, mixFactor);

    finalA = baseA * (0.8 - fade);

    vertexColor = vec4(rgb, finalA) * CloudColor;
}
