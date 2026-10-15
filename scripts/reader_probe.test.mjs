import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { test } from "node:test";
import vm from "node:vm";

const source = readFileSync(
  new URL("../app/src/gecko/assets/candy_privacy/reader_probe.js", import.meta.url),
  "utf8",
);

function probe() {
  const context = vm.createContext({ document: { readyState: "loading" } });
  vm.runInContext(source, context);
  return context.VolaReaderProbe;
}

const paragraph = (length, extra = {}) => ({ length, furniture: false, visible: true, ...extra });

test("a few long paragraphs read as an article", () => {
  assert.equal(probe().readable([paragraph(400), paragraph(380), paragraph(420)]), true);
});

test("short text, hidden text and page furniture do not", () => {
  const { readable } = probe();
  assert.equal(readable([]), false);
  assert.equal(readable(Array.from({ length: 40 }, () => paragraph(120))), false);
  assert.equal(readable([paragraph(400, { visible: false }), paragraph(400, { visible: false })]), false);
  assert.equal(readable([paragraph(500, { furniture: true }), paragraph(500, { furniture: true })]), false);
});

test("one long paragraph alone is not quite an article", () => {
  assert.equal(probe().readable([paragraph(500)]), false);
});

test("furniture is a whole word of the label, and a story word keeps the text", () => {
  const { furnitureLabel } = probe();
  assert.equal(furnitureLabel("related-posts"), true);
  assert.equal(furnitureLabel("comments__list"), true);
  assert.equal(furnitureLabel("site-nav menu"), true);
  assert.equal(furnitureLabel("commentary"), false);
  assert.equal(furnitureLabel("shared-reading"), false);
  assert.equal(furnitureLabel("article-body"), false);
  assert.equal(furnitureLabel("footer-content"), false);
});

/** A tiny page: [root] is the element Reader reads; each paragraph says where it sits. */
function page({ rootTag = "body", paragraphs }) {
  const root = { localName: rootTag, contains: () => true };
  const nodes = paragraphs.map(({ length, inDropped = false, parentClass = "", ariaHidden = false, shown = true }) => ({
    textContent: "x".repeat(length),
    hidden: false,
    getAttribute: (name) => (name === "aria-hidden" && ariaHidden ? "true" : null),
    checkVisibility: () => shown,
    closest: () => (inDropped ? {} : null),
    parentElement: { className: parentClass, id: "", parentElement: root },
  }));
  root.querySelectorAll = (selector) => (selector === "p" ? nodes : []);
  return {
    readyState: "complete",
    querySelector: (selector) => (selector === rootTag ? root : null),
    body: rootTag === "body" ? root : { querySelectorAll: () => [] },
  };
}

test("only what Reader view keeps counts", () => {
  const { paragraphs, readable } = probe();
  const article = (extra) => page({ rootTag: "article", paragraphs: [400, 380, 420].map((length) => ({ length, ...extra })) });
  assert.equal(readable(paragraphs(article({}))), true);
  // Inside nav/aside/footer/form Reader drops the text, so it is no article for «Read» either.
  assert.equal(readable(paragraphs(article({ inDropped: true }))), false);
  assert.equal(readable(paragraphs(article({ parentClass: "related-posts" }))), false);
  // Hidden by its own styles (or inside a hidden block, which checkVisibility sees).
  assert.equal(readable(paragraphs(article({ shown: false }))), false);
  // A paragraph hidden from TalkBack itself does not count (a dialog's aria-hidden on the page
  // wrapper is not looked at).
  assert.equal(readable(paragraphs(article({ ariaHidden: true }))), false);
});

test("a new document is measured again a moment later, a new policy revision is not", async () => {
  const timers = [];
  const sent = [];
  let generation = 1;
  let revision = 1;
  let doc = page({ paragraphs: [] });
  const context = vm.createContext({
    get document() { return doc; },
    setTimeout: (callback, delay) => timers.push({ callback, delay }),
    CandyContentTopInset: {
      readerAvailable: (available) => { sent.push(available); return true; },
      navigationGeneration: () => generation,
      policyRevision: () => revision,
    },
  });
  vm.runInContext(source, context);
  context.__candyReportReaderAvailable();
  assert.deepEqual(sent, [false]);
  assert.deepEqual(timers.map(({ delay }) => delay), [500, 1500]);
  // The article arrives after the first look; the late look sees it.
  doc = page({ paragraphs: [{ length: 400 }, { length: 380 }, { length: 420 }] });
  timers.shift().callback();
  assert.deepEqual(sent, [false, true]);
  // A new revision of the same document resends without looking again or scheduling more.
  revision = 2;
  context.__candyReportReaderAvailable();
  assert.deepEqual(sent, [false, true, true]);
  assert.equal(timers.length, 1);
  // An in-page navigation is a new generation: looked at now and again later.
  generation = 2;
  doc = page({ paragraphs: [] });
  context.__candyReportReaderAvailable();
  assert.deepEqual(sent, [false, true, true, false]);
  assert.equal(timers.length, 3);
});
