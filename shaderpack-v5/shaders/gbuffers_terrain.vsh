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
out float mcsmShade;

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
    // hard-ish sun term so tree canopies and block faces throw real shade
    // under Iris (the pack previously only graded colour, never shaded)
    vec3 L = normalize(vec3(0.35, max(elev, 0.05), 0.55));
    float ndotl = clamp(dot(normalize(mcsmN), L), 0.0, 1.0);
    // keep a floor so caves/undersides aren't pure black
    // 1.9.166: harder block-face key so trees/ground throw real MCSM shade
    float crisp = mix(ndotl, step(0.05, ndotl), 0.55);
    mcsmShade = mix(0.38, 1.12, crisp) * mix(0.72, 1.0, mcsmDay) + (1.0 - mcsmDay) * 0.52;
    mcsmShade = clamp(mcsmShade, 0.32, 1.20);
}
