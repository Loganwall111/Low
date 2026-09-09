#version 330 compatibility

/* Fallback root program.
 *
 * Iris/OptiFine resolve EVERY gbuffers_* program that a pack does not supply
 * back to gbuffers_basic. Shipping a pack without it means most geometry has
 * no program at all -- which is why the pack appeared to do nothing.
 */

out vec2 texcoord;
out vec4 glcolor;

void main() {
    gl_Position = ftransform();
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    glcolor = gl_Color;
}
