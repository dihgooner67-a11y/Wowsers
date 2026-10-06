#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float Style;    // palette: 0 b/w, 1 red, 2 purple, 3 fire, 4 blue, 5 warm gold, 6 event horizon, 7 FUGA ink (orange paper, black ink)
uniform float Amount;   // how much of the view is turned into the palette
uniform float Invert;   // 1 = shadows become bright (the "negative" impact frame)
uniform float Bloom;    // glow strength on bright areas
uniform float Time;
uniform float LensX;    // black hole lens: centre (uv), strength, horizon radius (vertical-uv units)
uniform float LensY;
uniform float LensStr;
uniform float LensR;

in vec2 texCoord;
out vec4 fragColor;

vec3 pal(float s, float t) {
    vec3 a; vec3 b; vec3 c;
    if (s < 0.5)      { a = vec3(0.0);              b = vec3(0.45);             c = vec3(1.0); }
    else if (s < 1.5) { a = vec3(0.03, 0.0, 0.01);  b = vec3(0.85, 0.05, 0.12); c = vec3(1.0, 0.82, 0.84); }
    else if (s < 2.5) { a = vec3(0.02, 0.0, 0.07);  b = vec3(0.55, 0.12, 1.0);  c = vec3(1.0, 0.86, 1.0); }
    else if (s < 3.5) { a = vec3(0.06, 0.0, 0.0);   b = vec3(1.0, 0.36, 0.0);   c = vec3(1.0, 0.95, 0.5); }
    else if (s < 4.5) { a = vec3(0.0, 0.0, 0.06);   b = vec3(0.12, 0.5, 1.0);   c = vec3(0.9, 1.0, 1.0); }
    else if (s < 5.5) { a = vec3(0.10, 0.05, 0.02); b = vec3(0.85, 0.65, 0.3);  c = vec3(1.0, 0.95, 0.8); }
    else              { a = vec3(0.0);              b = vec3(1.0, 0.36, 0.04);  c = vec3(1.0, 0.93, 0.6); }
    return t < 0.5 ? mix(a, b, t * 2.0) : mix(b, c, (t - 0.5) * 2.0);
}

vec3 bloomAt(vec2 uv) {
    vec3 acc = vec3(0.0);
    float wsum = 0.0;
    for (int i = 0; i < 12; i++) {
        float ang = 6.2831853 * float(i) / 12.0;
        for (int r = 1; r <= 4; r++) {
            vec2 off = vec2(cos(ang), sin(ang)) * float(r * r) * 2.2 / InSize;
            vec3 c = texture(DiffuseSampler, uv + off).rgb;
            float l = dot(c, vec3(0.299, 0.587, 0.114));
            float w = smoothstep(0.6, 1.0, l) / float(r);
            acc += c * w;
            wsum += 1.0 / float(r);
        }
    }
    return acc / wsum;
}

// Bend the view around the black hole: sample from nearer the hole (deflection ~ 1/r), black inside the horizon.
vec2 lensUV(vec2 uv, out float mask, out float ring) {
    mask = 1.0;
    ring = 0.0;
    if (LensStr <= 0.0) return uv;
    float aspect = InSize.x / InSize.y;
    vec2 d = uv - vec2(LensX, LensY);
    d.x *= aspect;
    float r = max(length(d), 0.0001);
    vec2 dir = d / r;
    float bend = min(LensStr * LensR * LensR / r, 0.45);
    vec2 off = dir * bend;
    off.x /= aspect;
    mask = smoothstep(LensR * 0.93, LensR, r);
    ring = exp(-pow((r - LensR * 1.07) / (LensR * 0.06), 2.0)) * LensStr;
    return clamp(uv - off, vec2(0.001), vec2(0.999));
}

float lumAt(vec2 uv) {
    return dot(texture(DiffuseSampler, uv).rgb, vec3(0.299, 0.587, 0.114));
}

// FUGA look: the real scene redrawn as black ink on orange paper. Block outlines and texture lines become ink
// (Sobel edges), shadows fill in solid black and mid-tones turn into halftone dots, so it follows whatever is around you.
float inkAt(vec2 uv) {
    vec2 px = 1.6 / InSize;
    float tl = lumAt(uv + vec2(-px.x,  px.y));
    float t  = lumAt(uv + vec2( 0.0,   px.y));
    float tr = lumAt(uv + vec2( px.x,  px.y));
    float l  = lumAt(uv + vec2(-px.x,  0.0));
    float r  = lumAt(uv + vec2( px.x,  0.0));
    float bl = lumAt(uv + vec2(-px.x, -px.y));
    float b  = lumAt(uv + vec2( 0.0,  -px.y));
    float br = lumAt(uv + vec2( px.x, -px.y));
    float gx = (tr + 2.0 * r + br) - (tl + 2.0 * l + bl);
    float gy = (tl + 2.0 * t + tr) - (bl + 2.0 * b + br);
    float edge = smoothstep(0.16, 0.30, length(vec2(gx, gy)));
    float lum = lumAt(uv);
    vec2 cell = fract(uv * InSize / 5.0) - 0.5;
    float dotRadius = mix(0.0, 0.55, clamp((0.58 - lum) / 0.36, 0.0, 1.0));
    float halftone = step(length(cell), dotRadius);
    float tone = lum < 0.22 ? 1.0 : (lum < 0.58 ? halftone : 0.0);
    return max(edge, tone);
}

void main() {
    float mask;
    float ring;
    vec2 uv = lensUV(texCoord, mask, ring);
    vec4 src = texture(DiffuseSampler, uv);
    vec3 col;
    if (Style > 6.5) {
        float ink = inkAt(uv);
        if (Invert > 0.5) ink = 1.0 - ink;
        vec3 toned = mix(vec3(1.0, 0.38, 0.03), vec3(0.015, 0.0, 0.0), ink);
        col = mix(src.rgb, toned, Amount);
    } else {
        float lum = dot(src.rgb, vec3(0.299, 0.587, 0.114));
        lum = clamp((lum - 0.04) * 1.4, 0.0, 1.0);
        lum = mix(lum, 1.0 - lum, Invert);
        col = mix(src.rgb, pal(Style, lum), Amount);
        col += bloomAt(uv) * Bloom * 0.9;
    }
    col *= 0.96 + 0.04 * sin(Time * 2.0);
    col *= mask;
    col += vec3(1.0, 0.78, 0.45) * ring * 0.7;
    fragColor = vec4(col, 1.0);
}
