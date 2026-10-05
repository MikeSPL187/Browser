// Vola color generator: M3 2025 TonalSpot roles per workspace seed, a vivid primary and an analogous-hue aura.
//   node gencss.mjs            -> canvas/vola4-colors.css (three example workspaces for the mockups)
//   node gencss.mjs --kotlin   -> app/.../ui/theme/VolaSchemes.kt (every WorkspaceAccent plus private mode)
//   node gencss.mjs --kotlin-themes -> app/.../ui/theme/VolaThemeSchemes.kt (the neutrals of Ice, Dusk, Paper and Mono)
// Both outputs come from roles(), so the app and the mockups can never drift apart.
import { Hct, SchemeTonalSpot, TonalPalette, DynamicScheme, Variant, MaterialDynamicColors as M, hexFromArgb, argbFromHex, Blend } from '@material/material-color-utilities';

// Mockup workspaces (canvas only).
const canvasSeeds = { work: '#006877', anime: '#A23F2B', personal: '#5E4EB7', private: '#4B3FB5' };
// App seeds: the light (tone 40) accent tones from ui/theme/WorkspaceAccents.kt, in WorkspaceAccent order.
const appSeeds = {
  Violet: '#5E4EB7', Blue: '#2F5BD3', Teal: '#006877', Green: '#2B6C3F',
  Amber: '#7C5800', Coral: '#A23F2B', Rose: '#A0305B', Graphite: '#55595F',
};
const privateSeed = canvasSeeds.private;

// Material roles copied straight from the dynamic scheme.
const materialRoles = [
  'primary', 'onPrimary', 'primaryContainer', 'onPrimaryContainer', 'inversePrimary',
  'secondary', 'onSecondary', 'secondaryContainer', 'onSecondaryContainer',
  'tertiary', 'onTertiary', 'tertiaryContainer', 'onTertiaryContainer',
  'error', 'onError', 'errorContainer', 'onErrorContainer',
  'surface', 'onSurface', 'surfaceVariant', 'onSurfaceVariant', 'inverseSurface', 'inverseOnSurface',
  'outline', 'outlineVariant', 'scrim', 'surfaceBright', 'surfaceDim',
  'surfaceContainerLowest', 'surfaceContainerLow', 'surfaceContainer', 'surfaceContainerHigh', 'surfaceContainerHighest',
];
// Vola roles on top of Material: aura, success, warning and the card surface.
const extendedRoles = [
  'aura1', 'aura2', 'aura3', 'ok', 'onOk', 'okContainer', 'onOkContainer',
  'warn', 'onWarn', 'warnContainer', 'onWarnContainer', 'card',
];

const hex = a => hexFromArgb(a).toUpperCase();
// Below this HCT chroma a seed reads as grey.
const MIN_COLORFUL_CHROMA = 16;

function roles(seed, dark, contrast) {
  const src = Hct.fromInt(argbFromHex(seed));
  // Grey seeds (Graphite) stay grey: only colorful seeds get the vivid primary and a colored aura.
  const colorful = src.chroma >= MIN_COLORFUL_CHROMA;
  // Tonal spot neutrals, but the primary keeps the seed's chroma so workspace colors stay vivid.
  const base = new SchemeTonalSpot(src, dark, contrast, '2025');
  const s = new DynamicScheme({
    sourceColorHct: src, variant: Variant.TONAL_SPOT, contrastLevel: contrast, isDark: dark, specVersion: '2025',
    primaryPalette: TonalPalette.fromHueAndChroma(src.hue, colorful ? Math.max(src.chroma, 48) : src.chroma),
    secondaryPalette: base.secondaryPalette, tertiaryPalette: base.tertiaryPalette,
    neutralPalette: TonalPalette.fromHueAndChroma(src.hue, 4), neutralVariantPalette: TonalPalette.fromHueAndChroma(src.hue, 8),
    errorPalette: base.errorPalette,
  });
  const v = {};
  for (const r of materialRoles) v[r] = hex(M[r].getArgb(s));
  // Containers stay soft (tonal spot); only the primary itself is vivid.
  if (colorful) for (const r of ['primaryContainer', 'onPrimaryContainer']) v[r] = hex(M[r].getArgb(base));
  // Aura: three analogous stops for a smooth workspace gradient (Zen-like), vivid but light.
  const h = src.hue;
  const au = dark ? [[h, 30, 22], [(h + 38) % 360, 24, 16], [(h + 340) % 360, 14, 10]] : [[h, 26, 91], [(h + 38) % 360, 22, 93], [(h + 340) % 360, 12, 96]];
  au.forEach(([hh, c, t], k) => { v[`aura${k + 1}`] = hex(Hct.from(hh, colorful ? c : Math.min(c, src.chroma), t).toInt()); });
  // Success and warning harmonized with the workspace primary.
  const pri = M.primary.getArgb(s);
  const ok = TonalPalette.fromInt(Blend.harmonize(argbFromHex('#2E7D32'), pri));
  const warn = TonalPalette.fromInt(Blend.harmonize(argbFromHex('#B26A00'), pri));
  v.ok = hex(ok.tone(dark ? 80 : 40)); v.onOk = hex(ok.tone(dark ? 20 : 100));
  v.okContainer = hex(ok.tone(dark ? 30 : 90)); v.onOkContainer = hex(ok.tone(dark ? 90 : 20));
  // Softer error container than the 2025 spec default: alerts stay calm next to workspace colors.
  const errP = TonalPalette.fromInt(M.error.getArgb(s));
  v.errorContainer = hex(errP.tone(dark ? 30 : 92)); v.onErrorContainer = hex(errP.tone(dark ? 90 : 25));
  v.warn = hex(warn.tone(dark ? 80 : 40)); v.onWarn = hex(warn.tone(dark ? 20 : 100));
  v.warnContainer = hex(warn.tone(dark ? 30 : 90)); v.onWarnContainer = hex(warn.tone(dark ? 90 : 20));
  // Dark theme is always pure black (owner decision): the surface ladder starts at #000000.
  if (dark) for (const r of ['surface', 'surfaceDim', 'surfaceContainerLowest']) v[r] = '#000000';
  // Card: white on light themes, a raised grey on the black dark theme.
  v.card = dark ? v.surfaceContainer : v.surfaceContainerLowest;
  return v;
}

// Themes (board W-Themes): each gives the shell its neutrals (surfaces, text, outlines), its aura
// and its card; the accent (the workspace's, or one picked for every workspace) keeps the primary,
// secondary and tertiary roles. Hue and chroma of the neutral and neutral-variant palettes, and the
// aura stops as [hue, chroma, tone] for light and dark.
const themes = {
  Ice: { hue: 222, chroma: 9, variantChroma: 14,
    aura: { light: [[205, 24, 92], [230, 18, 94], [190, 12, 96]], dark: [[215, 26, 20], [240, 20, 14], [195, 12, 9]] } },
  Dusk: { hue: 305, chroma: 12, variantChroma: 18,
    aura: { light: [[295, 24, 90], [330, 20, 92], [270, 12, 95]], dark: [[300, 34, 20], [330, 26, 14], [270, 18, 9]] } },
  Paper: { hue: 92, chroma: 8, variantChroma: 12,
    aura: { light: [[92, 18, 93], [75, 12, 95], [100, 8, 97]], dark: [[88, 14, 18], [70, 10, 13], [100, 6, 9]] } },
  Mono: { hue: 0, chroma: 0, variantChroma: 0,
    aura: { light: [[0, 0, 92], [0, 0, 94], [0, 0, 96]], dark: [[0, 0, 18], [0, 0, 13], [0, 0, 9]] } },
};
// The roles a theme replaces in the accent's scheme.
const themeRoleNames = [
  'surface', 'onSurface', 'surfaceVariant', 'onSurfaceVariant', 'inverseSurface', 'inverseOnSurface',
  'outline', 'outlineVariant', 'scrim', 'surfaceBright', 'surfaceDim',
  'surfaceContainerLowest', 'surfaceContainerLow', 'surfaceContainer', 'surfaceContainerHigh', 'surfaceContainerHighest',
  'aura1', 'aura2', 'aura3', 'card',
];

function themeRoles(theme, dark, contrast) {
  const t = themes[theme];
  const src = Hct.from(t.hue, Math.max(t.chroma, 1), 50);
  const base = new SchemeTonalSpot(src, dark, contrast, '2025');
  const s = new DynamicScheme({
    sourceColorHct: src, variant: Variant.TONAL_SPOT, contrastLevel: contrast, isDark: dark, specVersion: '2025',
    primaryPalette: base.primaryPalette, secondaryPalette: base.secondaryPalette, tertiaryPalette: base.tertiaryPalette,
    neutralPalette: TonalPalette.fromHueAndChroma(t.hue, t.chroma),
    neutralVariantPalette: TonalPalette.fromHueAndChroma(t.hue, t.variantChroma),
    errorPalette: base.errorPalette,
  });
  const v = {};
  for (const r of themeRoleNames.filter(r => !r.startsWith('aura') && r !== 'card')) v[r] = hex(M[r].getArgb(s));
  (dark ? t.aura.dark : t.aura.light).forEach(([hh, c, tone], k) => { v[`aura${k + 1}`] = hex(Hct.from(hh, c, tone).toInt()); });
  // Dark theme is always pure black (owner decision); the theme tints the containers above it.
  if (dark) for (const r of ['surface', 'surfaceDim', 'surfaceContainerLowest']) v[r] = '#000000';
  v.card = dark ? v.surfaceContainer : v.surfaceContainerLowest;
  return v;
}

// Text/background pairs that must reach WCAG 4.5:1 in every scheme. ContrastTest.kt checks the same list.
const contrastPairs = [
  ['onPrimary', 'primary'], ['onPrimaryContainer', 'primaryContainer'],
  ['onSecondary', 'secondary'], ['onSecondaryContainer', 'secondaryContainer'],
  ['onTertiary', 'tertiary'], ['onTertiaryContainer', 'tertiaryContainer'],
  ['onError', 'error'], ['onErrorContainer', 'errorContainer'],
  ['onOk', 'ok'], ['onOkContainer', 'okContainer'], ['onWarn', 'warn'], ['onWarnContainer', 'warnContainer'],
  ['inverseOnSurface', 'inverseSurface'], ['inversePrimary', 'inverseSurface'],
  ...['surface', 'surfaceBright', 'surfaceDim', 'surfaceContainerLowest', 'surfaceContainerLow', 'surfaceContainer',
    'surfaceContainerHigh', 'surfaceContainerHighest', 'surfaceVariant', 'card', 'aura1', 'aura2', 'aura3']
    .flatMap(bg => [['onSurface', bg], ['onSurfaceVariant', bg]]),
  ...['surface', 'surfaceContainerLowest', 'card']
    .flatMap(bg => [['primary', bg], ['error', bg], ['ok', bg], ['warn', bg]]),
];

function luminance(h) {
  const c = [1, 3, 5].map(i => parseInt(h.slice(i, i + 2), 16) / 255)
    .map(x => (x <= 0.03928 ? x / 12.92 : ((x + 0.055) / 1.055) ** 2.4));
  return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2];
}
function contrast(a, b) {
  const [la, lb] = [luminance(a), luminance(b)];
  return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
}
function checkContrast(name, v) {
  const low = contrastPairs.filter(([fg, bg]) => contrast(v[fg], v[bg]) < 4.5)
    .map(([fg, bg]) => `${fg}/${bg} ${contrast(v[fg], v[bg]).toFixed(2)}`);
  if (low.length) throw new Error(`${name}: contrast below 4.5:1: ${low.join(', ')}`);
}

function css() {
  const map = {
    pri: 'primary', 'on-pri': 'onPrimary', 'pri-c': 'primaryContainer', 'on-pri-c': 'onPrimaryContainer',
    sec: 'secondary', 'sec-c': 'secondaryContainer', 'on-sec-c': 'onSecondaryContainer',
    ter: 'tertiary', 'ter-c': 'tertiaryContainer', 'on-ter-c': 'onTertiaryContainer',
    sf: 'surface', 'sf-dim': 'surfaceDim', 'sf-lowest': 'surfaceContainerLowest', 'sf-low': 'surfaceContainerLow',
    'sf-c': 'surfaceContainer', 'sf-high': 'surfaceContainerHigh', 'sf-highest': 'surfaceContainerHighest',
    'on-sf': 'onSurface', 'on-sf-v': 'onSurfaceVariant', ol: 'outline', 'ol-v': 'outlineVariant',
    'inv-sf': 'inverseSurface', 'inv-on-sf': 'inverseOnSurface', err: 'error', 'err-c': 'errorContainer', 'on-err-c': 'onErrorContainer',
    'aura-1': 'aura1', 'aura-2': 'aura2', 'aura-3': 'aura3',
    ok: 'ok', 'ok-c': 'okContainer', 'on-ok-c': 'onOkContainer',
    warn: 'warn', 'warn-c': 'warnContainer', 'on-warn-c': 'onWarnContainer',
  };
  const block = (sel, v) => `${sel} {\n` + Object.entries(map).map(([k, r]) => `  --${k}: ${v[r]};`).join('\n') + '\n}\n';
  let out = '/* Generated by tools/gencss.mjs from workspace seeds (Material color utilities, spec 2025). Do not edit by hand. */\n';
  for (const [w, seed] of Object.entries(canvasSeeds)) {
    if (w !== 'private') out += block(`.w-${w}.t-light`, roles(seed, false, 0));
    out += block(`.w-${w}.t-dark`, roles(seed, true, 0));
    if (w !== 'private') out += block(`.w-${w}.t-light.hc`, roles(seed, false, 1));
  }
  return out;
}

function kotlin() {
  const variants = [['light', false, 0], ['dark', true, 0], ['lightHighContrast', false, 1], ['darkHighContrast', true, 1]];
  const argb = h => `0xFF${h.slice(1)}`;
  const tokens = (name, v, indent) => {
    checkContrast(name, v);
    const pad = ' '.repeat(indent);
    return `VolaSchemeTokens(\n` + [...materialRoles, ...extendedRoles].map(r => `${pad}    ${r} = ${argb(v[r])},`).join('\n') + `\n${pad})`;
  };
  const set = (name, seed) => `    val ${name} = VolaSchemeSet(\n        seed = ${argb(seed)},\n` +
    variants.map(([k, dark, c]) => `        ${k} = ${tokens(`${name}.${k}`, roles(seed, dark, c), 8)},`).join('\n') + '\n    )\n';
  let out = '// Generated by gencss.mjs, do not edit.\n' +
    '// Source: docs/vola/design/tools/gencss.mjs (Material Color Utilities 0.4.0, TonalSpot, spec 2025).\n' +
    '// Regenerate: see docs/vola/design/tools/README.md.\n' +
    'package dev.sk2andy.materialbrowser.ui.theme\n\n' +
    'import dev.sk2andy.materialbrowser.browser.WorkspaceAccent\n\n' +
    '/** Color schemes of every workspace accent and of private mode, generated from their seed colors. */\n' +
    'internal object VolaSchemes {\n';
  for (const [name, seed] of Object.entries(appSeeds)) out += set(name, seed) + '\n';
  out += set('Private', privateSeed) + '\n';
  out += '    fun forAccent(accent: WorkspaceAccent): VolaSchemeSet = when (accent) {\n' +
    Object.keys(appSeeds).map(n => `        WorkspaceAccent.${n} -> ${n}`).join('\n') + '\n    }\n}\n';
  return out;
}

function kotlinThemes() {
  const variants = [['light', false, 0], ['dark', true, 0], ['lightHighContrast', false, 1], ['darkHighContrast', true, 1]];
  const argb = h => `0xFF${h.slice(1)}`;
  const tokens = (name, v, indent) => {
    const pad = ' '.repeat(indent);
    return `VolaThemeTokens(\n` + themeRoleNames.map(r => `${pad}    ${r} = ${argb(v[r])},`).join('\n') + `\n${pad})`;
  };
  // Every accent must stay readable on every theme: check the combined schemes.
  for (const theme of Object.keys(themes)) for (const [k, dark, c] of variants) {
    const neutrals = themeRoles(theme, dark, c);
    for (const [accent, seed] of Object.entries(appSeeds)) checkContrast(`${theme}.${accent}.${k}`, { ...roles(seed, dark, c), ...neutrals });
  }
  const set = (name) => `    val ${name} = VolaThemeSet(\n` +
    variants.map(([k, dark, c]) => `        ${k} = ${tokens(`${name}.${k}`, themeRoles(name, dark, c), 8)},`).join('\n') + '\n    )\n';
  let out = '// Generated by gencss.mjs --kotlin-themes, do not edit.\n' +
    '// Source: docs/vola/design/tools/gencss.mjs (Material Color Utilities 0.4.0, TonalSpot, spec 2025).\n' +
    '// Regenerate: see docs/vola/design/tools/README.md.\n' +
    'package dev.sk2andy.materialbrowser.ui.theme\n\n' +
    '/** The neutrals, aura and card of each theme (board W-Themes); the accent keeps its own roles. */\n' +
    'internal object VolaThemeSchemes {\n';
  for (const name of Object.keys(themes)) out += set(name) + '\n';
  return out.replace(/\n$/, '') + '}\n';
}

const mode = process.argv.includes('--kotlin-themes') ? 'themes' : process.argv.includes('--kotlin') ? 'kotlin' : 'css';
process.stdout.write(mode === 'themes' ? kotlinThemes() : mode === 'kotlin' ? kotlin() : css());
