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
