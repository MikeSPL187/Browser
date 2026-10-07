import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { test } from "node:test";
import vm from "node:vm";

const read = (path) => readFileSync(new URL(`../${path}`, import.meta.url), "utf8");

const geckoExtractor = read("app/src/gecko/assets/candy_privacy/content.js")
  .split("function extractCandyReaderPayload() {")[1]
  .split("\nbrowser.runtime.onMessage.addListener(")[0];
const webViewExtractor = read("app/src/main/java/dev/sk2andy/materialbrowser/reader/ReaderExtraction.kt")
  .split('val javascript: String = """')[1]
  .split('""".trimIndent()')[0];

/** Matches the selector lists the extractors use: tags, tag[attr], tag[attr="value"], #id. */
function matches(element, selectorList) {
  return selectorList.split(",").map((part) => part.trim()).some((selector) => {
    if (selector.startsWith("#")) return element.attributes.id === selector.slice(1);
    const match = /^([a-z0-9]*)(?:\[([a-z-]+)(?:="([^"]*)")?\])?$/i.exec(selector);
    if (!match) throw new Error(`Unsupported selector ${selector}`);
    const [, tag, attribute, value] = match;
    if (tag && element.tagName !== tag.toUpperCase()) return false;
    if (!attribute) return true;
    if (!(attribute in element.attributes)) return false;
    return value === undefined || element.attributes[attribute] === value;
  });
}

class FakeText {
  constructor(text) {
    this.text = text;
    this.parentElement = null;
  }

  get textContent() {
    return this.text;
  }

  cloneNode() {
    return new FakeText(this.text);
  }
}

class FakeElement {
  constructor(tag, attributes = {}, children = []) {
    this.tagName = tag.toUpperCase();
    this.attributes = attributes;
    this.parentElement = null;
    this.childNodes = [];
    children.forEach((child) => this.append(typeof child === "string" ? new FakeText(child) : child));
  }

  append(child) {
    child.parentElement = this;
    this.childNodes.push(child);
  }

  get id() {
    return this.attributes.id || "";
  }

  get href() {
    return this.attributes.href || "";
  }

  get content() {
    return this.attributes.content;
  }

  get textContent() {
    return this.childNodes.map((child) => child.textContent).join("");
  }

  // Detached clones have no layout, so innerText falls back to the text like textContent.
  get innerText() {
    return this.textContent;
  }

  get descendants() {
    return this.childNodes.filter((child) => child instanceof FakeElement)
      .flatMap((child) => [child, ...child.descendants]);
  }

  querySelectorAll(selector) {
    return this.descendants.filter((element) => matches(element, selector));
  }

  querySelector(selector) {
    return this.querySelectorAll(selector)[0] || null;
  }

  closest(selector) {
    for (let element = this; element; element = element.parentElement) {
      if (matches(element, selector)) return element;
    }
    return null;
  }

  contains(node) {
    for (let current = node; current; current = current.parentElement) {
      if (current === this) return true;
    }
    return false;
  }

  cloneNode(deep) {
    return new FakeElement(
      this.tagName,
      { ...this.attributes },
      deep ? this.childNodes.map((child) => child.cloneNode(true)) : [],
    );
  }

  remove() {
    if (!this.parentElement) return;
    this.parentElement.childNodes = this.parentElement.childNodes.filter((child) => child !== this);
    this.parentElement = null;
  }
}

const h = (tag, attributes, ...children) => new FakeElement(tag, attributes, children);

function pageContext(article) {
  const body = h("body", {}, article);
  const html = h("html", {}, body);
  const document = {
    body,
    title: "Nested article",
    querySelector: (selector) => html.querySelector(selector),
  };
  return vm.createContext({
    document,
    location: { href: "https://news.example/nested", hostname: "news.example" },
    getComputedStyle: () => ({}),
  });
}

function extractWithGecko(article) {
  const context = pageContext(article);
  vm.runInContext(`function extractCandyReaderPayload() {${geckoExtractor}`, context);
  return vm.runInContext("extractCandyReaderPayload()", context);
}

function extractWithWebView(article) {
  return JSON.parse(vm.runInContext(webViewExtractor, pageContext(article)));
}

const nestedArticle = () => h(
  "article",
  {},
  h("h1", {}, "Nested blocks"),
  h("p", {}, "An opening paragraph that sets the scene."),
  h("blockquote", {}, h("p", {}, "A quoted paragraph that must appear once.")),
  h(
    "ul",
    {},
    h(
      "li",
      {},
      "Outer item",
      h("ul", {}, h("li", {}, "Inner item with ", h("a", { href: "https://docs.example/inner" }, "a link"))),
    ),
    h("li", {}, h("p", {}, "Item with a paragraph")),
  ),
);

const expectedBlocks = [
  { kind: "heading", level: 1, text: "Nested blocks", links: [] },
  { kind: "paragraph", level: 0, text: "An opening paragraph that sets the scene.", links: [] },
  { kind: "quote", level: 0, text: "A quoted paragraph that must appear once.", links: [] },
  { kind: "listitem", level: 0, text: "Outer item", links: [] },
  {
    kind: "listitem",
    level: 0,
    text: "Inner item with a link",
    links: [{ label: "a link", url: "https://docs.example/inner" }],
  },
  { kind: "listitem", level: 0, text: "Item with a paragraph", links: [] },
];

for (const [engine, extract] of [["Gecko", extractWithGecko], ["System WebView", extractWithWebView]]) {
  test(`${engine} reader extraction emits nested quotes and list items once`, () => {
    const payload = extract(nestedArticle());

    assert.equal(payload.title, "Nested article");
    assert.deepEqual(JSON.parse(JSON.stringify(payload.blocks)), expectedBlocks);
  });

  test(`${engine} reader extraction skips blocks inside a quote, even in a nested list`, () => {
    const article = h(
      "article",
      {},
      h(
        "blockquote",
        {},
        h("ul", {}, h("li", {}, "Quoted outer", h("ol", {}, h("li", {}, "Quoted inner")))),
      ),
    );

    assert.deepEqual(
      JSON.parse(JSON.stringify(extract(article).blocks)),
      [{ kind: "quote", level: 0, text: "Quoted outerQuoted inner", links: [] }],
    );
  });
}
