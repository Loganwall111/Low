#version 330 compatibility
/*
 * MCSM v2 — terrain vertex: adds a sun-shadow term.
 * The old composite-stage grading never reached the screen (composite wrote
 * colortex1, final read colortex0), so with this pack on the core pack's
 * terrain.vsh was replaced by a bare pass and ALL shading vanished — that is
 * the "shadows don't render under the shader" bug. Fix: shade here, grade in final.
 */
uniform float sunAngle;
out vec2 texcoord;
out vec2 lmcoord;
out vec4 glcolor;
out vec3 mcsmN;
out float mcsmDay;

void main() {
    gl_Position = ftransform();
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmcoord  = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
    glcolor  = gl_Color;

    // OptiFine reports sunAngle in degrees, Iris in radians — accept both.
    float ang = sunAngle > 15.0 ? radians(sunAngle) : sunAngle;
    float elev = cos(ang);                       // ~+1 at noon, <0 at night
    mcsmDay = clamp(elev * 2.4, 0.0, 1.0);
    mcsmN = gl_Normal;                           // terrain is never rotated
}
