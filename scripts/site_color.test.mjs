import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { test } from 'node:test';
import vm from 'node:vm';

const source = readFileSync(
  new URL('../app/src/gecko/assets/candy_privacy/content_site_color.js', import.meta.url),
  'utf8',
);

// fillStyle as Gecko serializes it: opaque colors as #rrggbb, translucent ones as rgba(); an
// invalid value leaves the previous one.
function canvasColor(value) {
  const named = { white: '#ffffff', black: '#000000', rebeccapurple: '#663399' };
  const text = value.trim().toLowerCase();
  if (named[text]) return named[text];
  if (/^#[0-9a-f]{6}$/.test(text)) return text;
  const short = /^#([0-9a-f]{3})$/.exec(text);
  if (short) return `#${[...short[1]].map((c) => c + c).join('')}`;
  const rgb = /^rgba?\((\d+),\s*(\d+),\s*(\d+)(?:,\s*([\d.]+))?\)$/.exec(text);
  if (!rgb) return null;
  const [r, g, b] = rgb.slice(1, 4).map(Number);
  if (rgb[4] !== undefined && Number(rgb[4]) < 1) return `rgba(${r}, ${g}, ${b}, ${Number(rgb[4])})`;
  return `#${[r, g, b].map((c) => c.toString(16).padStart(2, '0')).join('')}`;
}

// `top` lists the computed backgrounds from the element under the top edge up to its ancestors.
function harness({
  metas = [], dark = false, ready = true, head = true, readyState = 'interactive',
  top = null, body = 'rgba(0, 0, 0, 0)', html = 'rgba(0, 0, 0, 0)',
} = {}) {
  const timers = [];
  const reports = [];
  const listeners = {};
  const mediaListeners = [];
  let observer = null;
  const state = { dark, ready, generation: 1, revision: 1 };
  const meta = ({ name = 'theme-color', content, media = null }) => ({
    localName: 'meta',
    name,
    content,
    media,
    getAttribute(attribute) { return this[attribute]; },
  });
  const nodes = metas.map(meta);
  const context2d = {
    value: '#000000',
    get fillStyle() { return this.value; },
    set fillStyle(next) { const parsed = canvasColor(next); if (parsed) this.value = parsed; },
  };
  const element = (backgroundColor, parentElement = null) => ({ backgroundColor, parentElement });
  const topElement = top && top.reduceRight((parent, color) => element(color, parent), null);
  const document = {
    head: head ? {} : null,
    body: element(body),
    documentElement: element(html),
    elementFromPoint: top ? () => topElement : undefined,
    readyState,
    createElement: () => ({ getContext: () => context2d }),
    querySelectorAll: () => nodes.filter((node) => node.name.toLowerCase() === 'theme-color'),
    addEventListener: (type, listener) => { listeners[type] = listener; },
  };
  const context = {
    document,
    setTimeout: (callback) => { timers.push(callback); return timers.length; },
    innerWidth: 400,
    getComputedStyle: (node) => ({ backgroundColor: node.backgroundColor }),
    addEventListener: (type, listener) => { listeners[type] = listener; },
    matchMedia: (media) => ({
      get matches() { return media.includes('dark') === state.dark; },
      addEventListener: (_type, listener) => mediaListeners.push(listener),
    }),
    MutationObserver: class {
      constructor(callback) { observer = { callback }; }
      observe(target, options) { observer.target = target; observer.options = options; }
    },
    CandyContentTopInset: {
      navigationGeneration: () => state.generation,
      policyRevision: () => state.revision,
      siteColor: (color) => {
        if (!state.ready) return false;
        reports.push(color);
        return true;
      },
    },
  };
  context.globalThis = context;
  vm.runInNewContext(source, context);
  const flush = () => { while (timers.length) timers.shift()(); };
  flush();
  return {
    context, state, reports, nodes, listeners, mediaListeners, flush,
    observer: () => observer,
    add: (spec) => { const node = meta(spec); nodes.push(node); return node; },
  };
}

test('reports the declared theme-color normalized to #rrggbb', () => {
  for (const [content, expected] of [
    ['#1DA1F2', '#1da1f2'], ['#fc0', '#ffcc00'], ['rgb(255, 69, 0)', '#ff4500'],
    ['white', '#ffffff'], ['rgba(10, 20, 30, 0.5)', '#0a141e'],
  ]) {
    const page = harness({ metas: [{ content }] });
    assert.deepEqual(page.reports, [expected], content);
  }
});

test('an invalid, transparent or missing color reports none', () => {
  assert.deepEqual(harness({ metas: [{ content: 'not a color' }] }).reports, [null]);
  assert.deepEqual(harness({ metas: [{ content: 'rgba(0, 0, 0, 0)' }] }).reports, [null]);
  assert.deepEqual(harness().reports, [null]);
});

test('a meta whose media does not match is skipped, and a theme switch reports again', () => {
  const page = harness({
    metas: [
      { content: '#ffffff', media: '(prefers-color-scheme: light)' },
      { content: '#0d1117', media: '(prefers-color-scheme: dark)' },
    ],
  });
  assert.deepEqual(page.reports, ['#ffffff']);
  page.state.dark = true;
  page.mediaListeners.forEach((listener) => listener());
  page.flush();
  assert.deepEqual(page.reports, ['#ffffff', '#0d1117']);
});

test('the same color is reported once per navigation and policy revision', () => {
  const page = harness({ metas: [{ content: '#1f4e5f' }] });
  page.context.__candyReportSiteColor();
  page.flush();
  assert.deepEqual(page.reports, ['#1f4e5f']);
  page.state.generation = 2;
  page.context.__candyReportSiteColor();
  page.flush();
  assert.deepEqual(page.reports, ['#1f4e5f', '#1f4e5f']);
});

test('nothing is reported before the policy arrives; the policy triggers the report', () => {
  const page = harness({ metas: [{ content: '#e50914' }], ready: false });
  assert.deepEqual(page.reports, []);
  page.state.ready = true;
  page.context.__candyReportSiteColor();
  page.flush();
  assert.deepEqual(page.reports, ['#e50914']);
});

test('a meta added or edited later in the head is reported', () => {
  const page = harness();
  assert.deepEqual(page.reports, [null]);
  assert.deepEqual([...page.observer().options.attributeFilter], ['content', 'media', 'name']);
  const node = page.add({ content: '#34c759' });
  page.observer().callback([{ type: 'childList', addedNodes: [node], removedNodes: [] }]);
  page.flush();
  node.content = '#ffcc00';
  page.observer().callback([{ type: 'attributes', target: node }]);
  page.flush();
  page.observer().callback([{ type: 'childList', addedNodes: [{ localName: 'script' }], removedNodes: [] }]);
  page.flush();
  assert.deepEqual(page.reports, [null, '#34c759', '#ffcc00']);
});

test('the head is observed once it exists', () => {
  const page = harness({ head: false });
  assert.equal(page.observer(), null);
  page.context.document.head = {};
  page.listeners.DOMContentLoaded();
  page.flush();
  assert.notEqual(page.observer(), null);
});

test('no color is reported while the page is still loading, a found color is', () => {
  const page = harness({ readyState: 'loading' });
  assert.deepEqual(page.reports, []);
  const node = page.add({ content: '#ff6600' });
  page.observer().callback([{ type: 'childList', addedNodes: [node], removedNodes: [] }]);
  page.flush();
  assert.deepEqual(page.reports, ['#ff6600']);

  const plain = harness({ readyState: 'loading' });
  plain.context.document.readyState = 'interactive';
  plain.listeners.DOMContentLoaded();
  plain.flush();
  assert.deepEqual(plain.reports, [null]);
});

test('without theme-color the first opaque background under the top edge is reported', () => {
  const page = harness({ top: ['rgba(0, 0, 0, 0)', 'rgba(255, 255, 255, 0.5)', 'rgb(29, 161, 242)'] });
  assert.deepEqual(page.reports, ['#1da1f2']);
});

test('the body, then the root background stand in when nothing at the top is opaque', () => {
  assert.deepEqual(harness({ top: ['rgba(0, 0, 0, 0)'], body: 'rgb(13, 17, 23)' }).reports, ['#0d1117']);
  assert.deepEqual(harness({ html: 'rgb(255, 255, 255)' }).reports, ['#ffffff']);
  assert.deepEqual(harness({ top: ['rgba(0, 0, 0, 0)'] }).reports, [null]);
});

test('theme-color wins over the top of the page, and the top is not read while loading', () => {
  assert.deepEqual(
    harness({ metas: [{ content: '#e50914' }], top: ['rgb(255, 255, 255)'] }).reports,
    ['#e50914'],
  );
  const loading = harness({ readyState: 'loading', top: ['rgb(29, 161, 242)'] });
  assert.deepEqual(loading.reports, []);
  loading.context.document.readyState = 'complete';
  loading.listeners.load();
  loading.flush();
  assert.deepEqual(loading.reports, ['#1da1f2']);
});
