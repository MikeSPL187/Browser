import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { test } from "node:test";
import vm from "node:vm";

const source = readFileSync(
  new URL("../app/src/gecko/assets/candy_privacy/hide_distractions.js", import.meta.url),
  "utf8",
);

// A small DOM: tags, ids, classes, sizes, and a querySelectorAll that understands the selectors
// the script writes (`#id`, `tag.class:nth-of-type(n)` steps joined by ` > `).
function page() {
  const all = [];
  const element = (tagName, { id = "", classes = [], rect = {}, position = "static", parent = null } = {}) => {
    const node = {
      nodeType: 1,
      tagName: tagName.toUpperCase(),
      id,
      classList: classes,
      position,
      parentElement: parent,
      children: [],
      getBoundingClientRect: () => ({ left: 0, top: 0, width: 300, height: 100, ...rect }),
      contains(other) {
        for (let current = other; current; current = current.parentElement) if (current === this) return true;
        return false;
      },
      get previousElementSibling() {
        if (!this.parentElement) return null;
        const siblings = this.parentElement.children;
        return siblings[siblings.indexOf(this) - 1] || null;
      },
    };
    if (parent) parent.children.push(node);
    all.push(node);
    return node;
  };
  const html = element("html", { rect: { width: 400, height: 800 } });
  const body = element("body", { parent: html, rect: { width: 400, height: 3000 } });
  const stepMatches = (node, step) => {
    if (step.startsWith("#")) return node.id === step.slice(1);
    const match = step.match(/^([a-z0-9]+)((?:\.[A-Za-z0-9_-]+)*)(?::nth-of-type\((\d+)\))?$/);
    if (!match) throw new Error(`unsupported selector step ${step}`);
    const [, tagName, classes, nth] = match;
    if (node.tagName.toLowerCase() !== tagName) return false;
    if (classes && !classes.slice(1).split(".").every((name) => node.classList.includes(name))) return false;
    if (nth) {
      const sameTag = node.parentElement ? node.parentElement.children.filter((child) => child.tagName === node.tagName) : [node];
      if (sameTag.indexOf(node) + 1 !== Number(nth)) return false;
    }
    return true;
  };
  const matches = (node, steps) => {
    let current = node;
    for (let index = steps.length - 1; index >= 0; index--, current = current?.parentElement) {
      if (!current || !stepMatches(current, steps[index])) return false;
    }
    return true;
  };
  const document = {
    documentElement: html,
    body,
    querySelectorAll: (selector) => all.filter((node) => matches(node, selector.split(" > "))),
  };
  const view = {
    innerWidth: 400,
    innerHeight: 800,
    getComputedStyle: (node) => ({ position: node.position }),
  };
  return { element, document, view, body };
}

const context = { globalThis: undefined };
context.globalThis = context;
vm.runInNewContext(source, context);
const hide = context.VolaHideDistractions;

test("a stable id is the whole selector", () => {
  const { element, document, body } = page();
  const banner = element("div", { id: "promo-banner", parent: body });
  element("div", { id: "content", parent: body });

  assert.equal(hide.selectorFor(banner, document), "#promo-banner");
});

test("generated ids and classes never reach a selector", () => {
  for (const name of ["css-1x2y3z", "sc-bdVaJa", "Header_title__a1b2c", "item-12345", "jsx-2349", "ng-star-inserted"]) {
    assert.equal(hide.isStableName(name), false, name);
  }
  for (const name of ["promo", "sidebar-ad", "col-md-6", "h2", "newsletter_signup"]) {
    assert.equal(hide.isStableName(name), true, name);
  }
});

test("a chain of stable classes stops once it finds only the block", () => {
  const { element, document, body } = page();
  const main = element("main", { id: "main", parent: body });
  const aside = element("aside", { classes: ["sidebar", "active", "css-1x2y3z"], parent: main });
  const box = element("div", { classes: ["newsletter", "is-open"], parent: aside });
  element("div", { classes: ["newsletter"], parent: body });

  assert.equal(hide.selectorFor(box, document), "aside.sidebar > div.newsletter");
});

test("look-alike siblings are pinned by position", () => {
  const { element, document, body } = page();
  const list = element("section", { id: "feed", parent: body });
  element("article", { classes: ["card"], parent: list });
  const promoted = element("article", { classes: ["card"], parent: list });
  element("article", { classes: ["card"], parent: list });

  assert.equal(hide.selectorFor(promoted, document), "#feed > article.card:nth-of-type(2)");
});

test("a chain pinned by position climbs to the nearest ancestor with an id", () => {
  const { element, document, body } = page();
  const main = element("main", { id: "main", parent: body });
  const wrapper = element("div", { parent: main });
  const promo = element("div", { parent: wrapper });
  element("div", { parent: body });
  element("div", { parent: element("div", { parent: body }) });

  assert.equal(hide.selectorFor(promo, document), "#main > div:nth-of-type(1) > div");
});

test("without an id above, a chain pinned by position starts at the body", () => {
  const { element, document, body } = page();
  const wrapper = element("div", { parent: body });
  element("p", { parent: wrapper });
  const second = element("p", { parent: wrapper });
  element("div", { parent: body });

  assert.equal(hide.selectorFor(second, document), "body > div > p:nth-of-type(2)");
});

test("the page itself is never a selector", () => {
  const { document, body } = page();

  assert.equal(hide.selectorFor(body, document), null);
  assert.equal(hide.selectorFor(document.documentElement, document), null);
});

test("a tap on a word climbs to the block around it", () => {
  const { element, document, view, body } = page();
  const card = element("div", { parent: body, rect: { width: 360, height: 120 } });
  const line = element("p", { parent: card, rect: { width: 360, height: 20 } });
  const word = element("span", { parent: line, rect: { width: 40, height: 20 } });

  assert.equal(hide.targetFor(word, document, view), card);
});

test("a pinned bubble is a target however small", () => {
  const { element, document, view, body } = page();
  const chat = element("div", { parent: body, position: "fixed", rect: { width: 56, height: 56 } });
  const icon = element("span", { parent: chat, rect: { width: 24, height: 24 } });

  assert.equal(hide.targetFor(icon, document, view), chat);
});

test("a block that fills the screen is not a target", () => {
  const { element, document, view, body } = page();
  const wrapper = element("div", { parent: body, rect: { width: 400, height: 2000 } });
  const word = element("span", { parent: wrapper, rect: { width: 40, height: 20 } });

  assert.equal(hide.targetFor(word, document, view), null);
});

test("«choose larger» takes the nearest bigger box, short of the page", () => {
  const { element, document, view, body } = page();
  const section = element("section", { parent: body, rect: { width: 400, height: 300 } });
  const wrapper = element("div", { parent: section, rect: { width: 360, height: 120 } });
  const card = element("div", { parent: wrapper, rect: { width: 360, height: 120 } });
  const page2 = element("main", { parent: body, rect: { width: 400, height: 2000 } });
  const inner = element("div", { parent: page2, rect: { width: 360, height: 120 } });

  // A wrapper the same size is skipped: it would hide the same thing.
  assert.equal(hide.largerTarget(card, document, view), section);
  // Nothing larger short of the whole page.
  assert.equal(hide.largerTarget(inner, document, view), null);
  assert.equal(hide.largerTarget(section, document, view), null);
  assert.equal(hide.largerTarget(null, document, view), null);
});

test("only plain selectors are applied, at most 32", () => {
  const many = Array.from({ length: 40 }, (_, index) => `div.a${index}`);
  assert.equal(hide.validSelectors(many).length, 32);
  assert.deepEqual(
    hide.validSelectors(["#ad", "div{color:red}", "a[href]", "x".repeat(257), 7, "#main > div.card:nth-of-type(2)"]),
    ["#ad", "#main > div.card:nth-of-type(2)"],
  );
  assert.equal(hide.hiddenStyleText(["#ad", "div.promo"]), "#ad{display:none!important}\ndiv.promo{display:none!important}");
});

test("choosing mode outlines a block, hides it on the second tap and tells Vola", async () => {
  const { element, document, view, body } = page();
  const appended = [];
  document.documentElement.appendChild = (child) => { appended.push(child); child.isConnected = true; };
  document.createElement = (name) => {
    const properties = {};
    return {
      name,
      dataset: {},
      properties,
      setAttribute() {},
      remove() { this.removed = true; },
      style: { setProperty: (key, value) => { properties[key] = value; }, removeProperty: (key) => { delete properties[key]; } },
    };
  };
  const listeners = {};
  const sent = [];
  let onMessage;
  const runtime = {
    onMessage: { addListener: (listener) => { onMessage = listener; } },
    sendMessage: (message) => {
      sent.push(message);
      return Promise.resolve(message.type === "content-policy-request" ? { type: "content-policy", ready: false } : undefined);
    },
  };
  const window = {
    ...view,
    document,
    browser: { runtime },
    setTimeout: (callback) => callback(),
    matchMedia: () => ({ matches: false }),
    addEventListener: (type, listener) => { listeners[type] = listener; },
    removeEventListener: (type) => { delete listeners[type]; },
  };
  window.globalThis = window;
  window.top = window;
  window.self = window;
  vm.runInNewContext(source, window);

  const banner = element("div", { classes: ["promo"], parent: body, rect: { width: 360, height: 90 } });
  const word = element("span", { parent: banner, rect: { width: 40, height: 20 } });
  word.style = banner.style = { setProperty() {}, removeProperty() {} };
  onMessage({ type: "content-policy", ready: true, revision: 3, navigationGeneration: 7, animationsEnabled: false, hiddenElements: ["#cookie-wall"] });
  const sheet = appended.find((node) => node.name === "style");
  assert.equal(sheet.textContent, "#cookie-wall{display:none!important}");

  onMessage({ type: "distractions-pick", active: true, highlightColor: "#336699" });
  const tap = (target, isTrusted = true) => {
    const event = { type: "click", target, isTrusted, cancelable: true, stopImmediatePropagation() { this.stopped = true; }, preventDefault() { this.prevented = true; } };
    listeners.click(event);
    return event;
  };
  // The page's own script cannot choose: a synthetic click is swallowed and outlines nothing.
  const forged = tap(word, false);
  assert.ok(forged.stopped, "the page never sees its own synthetic click either");
  assert.equal(appended.find((node) => node.name === "div"), undefined);
  listeners.touchstart({ type: "touchstart", isTrusted: false, touches: [{ clientX: 0, clientY: 0 }], stopImmediatePropagation() {} });
  // The engine moved the click onto a nearby link; the touch began on the banner.
  const link = element("a", { parent: body, rect: { width: 360, height: 40 } });
  document.elementFromPoint = (x, y) => (x === 30 && y === 120 ? word : null);
  listeners.touchstart({ type: "touchstart", isTrusted: true, touches: [{ clientX: 30, clientY: 120 }], stopImmediatePropagation() {} });
  const first = tap(link);
  assert.ok(first.stopped && first.prevented, "the page never sees the tap");
  const outline = appended.find((node) => node.name === "div");
  assert.equal(outline.properties.width, "360px");
  assert.equal(outline.properties.border, "2px solid #336699");
  // Vola hears that a block is outlined, so the panel can say it to TalkBack.
  assert.deepEqual(
    JSON.parse(JSON.stringify(sent.at(-1))),
    { type: "distraction-outlined", outlined: true, larger: false, navigationGeneration: 7 },
  );

  // «Cancel» on Vola's choice card lets the block go and says so.
  onMessage({ type: "distractions-act", action: "cancel" });
  assert.ok(outline.removed);
  assert.deepEqual(
    JSON.parse(JSON.stringify(sent.at(-1))),
    { type: "distraction-outlined", outlined: false, larger: false, navigationGeneration: 7 },
  );
  // «Hide» with nothing outlined does nothing.
  const sentBefore = sent.length;
  onMessage({ type: "distractions-act", action: "hide" });
  assert.equal(sent.length, sentBefore);

  tap(word);
  const again = appended.filter((node) => node.name === "div").at(-1);
  assert.notEqual(again, outline);
  tap(word);
  assert.ok(again.removed);
  assert.equal(sheet.textContent, "#cookie-wall{display:none!important}\ndiv.promo{display:none!important}");
  assert.deepEqual(
    JSON.parse(JSON.stringify(sent.at(-1))),
    { type: "distraction-hidden", selector: "div.promo", navigationGeneration: 7 },
  );

  onMessage({ type: "distractions-pick", active: false });
  assert.equal(listeners.click, undefined);
});

function backgroundHarness(policy) {
  const sent = [];
  const posted = [];
  const background = readFileSync(
    new URL("../app/src/gecko/assets/candy_privacy/background.js", import.meta.url),
    "utf8",
  );
  const start = background.indexOf("function forwardDistractionPick");
  const end = background.indexOf("function postReaderResult");
  const context = vm.createContext({
    PROTOCOL_VERSION: 2,
    policiesByToken: new Map([["token", policy]]),
    tokenByTab: new Map([[7, "token"]]),
    nativePort: { postMessage: (message) => posted.push(message) },
    browser: { tabs: { sendMessage: (...args) => { sent.push(args); return Promise.resolve(); } } },
  });
  vm.runInContext(background.slice(start, end), context);
  return { context, sent, posted };
}

test("the background starts choosing on the bound tab's top frame only", () => {
  const harness = backgroundHarness({ revision: 4, navigationGeneration: 9 });

  harness.context.forwardDistractionPick({ token: "token", active: true, highlightColor: "#336699" });
  harness.context.forwardDistractionPick({ token: "other", active: true });

  assert.equal(harness.sent.length, 1);
  assert.equal(harness.sent[0][0], 7);
  assert.deepEqual(JSON.parse(JSON.stringify(harness.sent[0][1])), { type: "distractions-pick", active: true, highlightColor: "#336699" });
  assert.equal(harness.sent[0][2].frameId, 0);
});

test("the background passes the choice card's answer to the bound tab's top frame only", () => {
  const harness = backgroundHarness({ revision: 4, navigationGeneration: 9 });

  harness.context.forwardDistractionAct({ token: "token", action: "larger" });
  harness.context.forwardDistractionAct({ token: "token", action: "remove-everything" });
  harness.context.forwardDistractionAct({ token: "other", action: "hide" });

  assert.equal(harness.sent.length, 1);
  assert.equal(harness.sent[0][0], 7);
  assert.deepEqual(JSON.parse(JSON.stringify(harness.sent[0][1])), { type: "distractions-act", action: "larger" });
  assert.equal(harness.sent[0][2].frameId, 0);
});

test("the background passes on a hidden block for the current page of the top frame only", () => {
  const harness = backgroundHarness({ revision: 4, navigationGeneration: 9 });
  const hidden = { type: "distraction-hidden", selector: "div.promo", navigationGeneration: 9 };

  harness.context.relayDistractionHidden(hidden, { frameId: 0, tab: { id: 7 } });
  harness.context.relayDistractionHidden(hidden, { frameId: 3, tab: { id: 7 } });
  harness.context.relayDistractionHidden({ ...hidden, navigationGeneration: 8 }, { frameId: 0, tab: { id: 7 } });
  harness.context.relayDistractionHidden(hidden, { frameId: 0, tab: { id: 8 } });

  assert.equal(harness.posted.length, 1);
  assert.deepEqual(JSON.parse(JSON.stringify(harness.posted[0])), {
    type: "distraction-hidden",
    protocolVersion: 2,
    token: "token",
    revision: 4,
    navigationGeneration: 9,
    selector: "div.promo",
  });
});

test("the background passes on an outline for the current page of the top frame only", () => {
  const harness = backgroundHarness({ revision: 4, navigationGeneration: 9 });
  const outlined = { type: "distraction-outlined", outlined: true, larger: true, navigationGeneration: 9 };

  harness.context.relayDistractionHidden(outlined, { frameId: 0, tab: { id: 7 } });
  harness.context.relayDistractionHidden(outlined, { frameId: 3, tab: { id: 7 } });
  harness.context.relayDistractionHidden({ ...outlined, navigationGeneration: 8 }, { frameId: 0, tab: { id: 7 } });

  assert.equal(harness.posted.length, 1);
  assert.deepEqual(JSON.parse(JSON.stringify(harness.posted[0])), {
    type: "distraction-outlined",
    protocolVersion: 2,
    token: "token",
    revision: 4,
    navigationGeneration: 9,
    outlined: true,
    larger: true,
  });
});
