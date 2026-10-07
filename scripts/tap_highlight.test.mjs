import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { test } from 'node:test';
import vm from 'node:vm';

const source = readFileSync(
  new URL('../app/src/gecko/assets/candy_privacy/tap_highlight.js', import.meta.url),
  'utf8',
);

const selectorMatches = (element, selector) => selector.split(',').some((part) => {
  const rule = part.trim();
  if (rule === ':disabled') return element.disabled === true;
  const match = rule.match(/^([a-z]+)?(?:\[([a-z-]+)(?:=([a-z]+))?\])?$/);
  if (!match) return false;
  const [, tagName, attribute, value] = match;
  if (tagName && element.tagName.toLowerCase() !== tagName) return false;
  if (attribute) {
    if (!(attribute in element.attributes)) return false;
    if (value !== undefined && element.attributes[attribute] !== value) return false;
  }
  return Boolean(tagName || attribute);
});

function harness({ reducedMotion = false } = {}) {
  let clock = 0;
  const timers = [];
  const listeners = {};
  const appended = [];
  const element = (tagName, { attributes = {}, rect, style = {}, parent = null, disabled = false } = {}) => {
    const node = {
      tagName: tagName.toUpperCase(),
      attributes,
      disabled,
      parentElement: parent,
      computed: { backgroundColor: 'rgba(0, 0, 0, 0)', borderRadius: '0px', highlight: '', ...style },
      getBoundingClientRect: () => ({ left: 10, top: 100, width: 200, height: 48, ...rect }),
      matches(selector) { return selectorMatches(this, selector); },
      closest(selector) {
        for (let current = this; current; current = current.parentElement) {
          if (current.matches(selector)) return current;
        }
        return null;
      },
    };
    return node;
  };
  const document = {
    documentElement: {
      appendChild(child) {
        appended.push(child);
        child.remove = () => { child.removed = true; };
      },
    },
    createElement(name) {
      const properties = {};
      return { name, properties, style: { setProperty: (key, value) => { properties[key] = value; } } };
    },
  };
  const context = {
    document,
    innerWidth: 400,
    innerHeight: 800,
    performance: { now: () => clock },
    matchMedia: () => ({ matches: reducedMotion }),
    getComputedStyle: (node) => ({
      backgroundColor: node.computed.backgroundColor,
      borderRadius: node.computed.borderRadius,
      getPropertyValue: (name) => (name === '-webkit-tap-highlight-color' ? node.computed.highlight : ''),
    }),
    setTimeout: (callback, delay) => {
      timers.push({ at: clock + delay, callback });
      return timers.length;
    },
    clearTimeout: (id) => { if (timers[id - 1]) timers[id - 1].cancelled = true; },
    addEventListener: (type, listener) => { listeners[type] = listener; },
  };
  vm.runInNewContext(source, context);
  const advance = (ms) => {
    const until = clock + ms;
    for (;;) {
      const next = timers.filter((timer) => !timer.cancelled && !timer.fired && timer.at <= until)
        .sort((a, b) => a.at - b.at)[0];
      if (!next) break;
      clock = next.at;
      next.fired = true;
      next.callback();
    }
    clock = until;
  };
  const pointer = (type, target, extra = {}) => listeners[type]({
    target, pointerType: 'touch', isPrimary: true, clientX: 50, clientY: 120, ...extra,
  });
  return { element, appended, advance, pointer, listeners };
}

test('a tapped link flashes, stays briefly and fades away', () => {
  const page = harness();
  const body = page.element('body', { style: { backgroundColor: 'rgb(255, 255, 255)' } });
  const link = page.element('a', { attributes: { href: '/watch' }, parent: body, style: { borderRadius: '8px' } });
  const label = page.element('span', { parent: link });

  page.pointer('pointerdown', label);
  page.advance(49);
  assert.equal(page.appended.length, 0, 'no flash before the pan-cancel window');
  page.advance(1);
  const flash = page.appended[0];
  assert.equal(flash.name, 'vola-tap-highlight');
  assert.equal(flash.properties.left, '10px');
  assert.equal(flash.properties.top, '100px');
  assert.equal(flash.properties.width, '200px');
  assert.equal(flash.properties['border-radius'], '8px');
  assert.equal(flash.properties['pointer-events'], 'none');
  assert.equal(flash.properties.background, 'rgba(0, 0, 0, 0.14)');

  page.pointer('pointerup', label);
  page.advance(99);
  assert.equal(flash.properties.opacity, '1');
  page.advance(1);
  assert.equal(flash.properties.opacity, '0');
  page.advance(200);
  assert.equal(flash.removed, true);
});

test('a quick tap still flashes for the minimum time', () => {
  const page = harness();
  const button = page.element('button');
  page.pointer('pointerdown', button);
  page.advance(20);
  page.pointer('pointerup', button);
  assert.equal(page.appended.length, 1);
  page.advance(100);
  assert.equal(page.appended[0].properties.opacity, '0');
});

test('a scroll, a pan or a mouse never flashes', () => {
  const page = harness();
  const link = page.element('a', { attributes: { href: '/' } });

  page.pointer('pointerdown', link);
  page.listeners.pointercancel({});
  page.advance(100);

  page.pointer('pointerdown', link);
  page.pointer('pointermove', link, { clientX: 50, clientY: 140 });
  page.advance(100);

  page.pointer('pointerdown', link, { pointerType: 'mouse' });
  page.advance(100);

  page.pointer('pointerdown', link);
  page.listeners.scroll({});
  page.advance(100);

  assert.equal(page.appended.length, 0);
});

test('plain text, text fields, disabled controls and page-sized wrappers do not flash', () => {
  const page = harness();
  const cases = [
    page.element('p'),
    page.element('input', { attributes: { type: 'text' } }),
    page.element('button', { disabled: true }),
    page.element('a', { attributes: { href: '/' }, rect: { width: 400, height: 700 } }),
    page.element('a', { attributes: { href: '/' }, rect: { width: 0, height: 0 } }),
  ];
  for (const target of cases) {
    page.pointer('pointerdown', target);
    page.advance(60);
    page.pointer('pointerup', target);
    page.advance(400);
  }
  assert.equal(page.appended.length, 0);
});

test('a site that turns the tap highlight off keeps its own feedback', () => {
  const page = harness();
  const button = page.element('button', { style: { highlight: 'rgba(0, 0, 0, 0)' } });
  page.pointer('pointerdown', button);
  page.advance(60);
  assert.equal(page.appended.length, 0);
});

test('dark controls get a light flash and reduced motion skips the fade', () => {
  const page = harness({ reducedMotion: true });
  const button = page.element('button', { style: { backgroundColor: 'rgb(20, 20, 20)' } });
  page.pointer('pointerdown', button);
  page.advance(60);
  const flash = page.appended[0];
  assert.equal(flash.properties.background, 'rgba(255, 255, 255, 0.24)');
  assert.equal(flash.properties.transition, undefined);
  page.pointer('pointerup', button);
  page.advance(100);
  assert.equal(flash.removed, true);
});
