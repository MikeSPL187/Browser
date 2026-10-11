import assert from "node:assert/strict";
import fs from "node:fs";
import test from "node:test";
import vm from "node:vm";

const context = vm.createContext({ Map, Set, URL, Uint8Array, TextDecoder, atob, RegExp, Array, Number, String, Error });
for (const script of ["rules.js", "network_filters.js"]) {
  vm.runInContext(
    fs.readFileSync(new URL(`../app/src/gecko/assets/candy_privacy/${script}`, import.meta.url), "utf8"),
    context,
  );
}
const { parse, index, patternToRegExp } = context.CandyNetworkFilters;
const rules = context.CandyPrivacyRules;
const asset = (rows) => ["candy-network-filter:2", `# Rules: ${rows.length}`,
  ...rows.map((fields) => fields.join("\t"))].join("\n");

test("ABP anchors and separators become the regular expression they mean", () => {
  const domain = patternToRegExp("||ads.example/banner");
  assert.equal(domain.test("https://ads.example/banner.png"), true);
  assert.equal(domain.test("https://cdn.ads.example/banner"), true);
  assert.equal(domain.test("https://badads.example/banner"), false);
  assert.equal(domain.test("https://news.example/?u=ads.example/banner"), false);
  const start = patternToRegExp("|https://track.");
  assert.equal(start.test("https://track.example/x"), true);
  assert.equal(start.test("http://x.example/?https://track."), false);
  const separator = patternToRegExp("/ads^");
  assert.equal(separator.test("https://x.example/ads?x=1"), true);
  assert.equal(separator.test("https://x.example/ads"), true);
  assert.equal(separator.test("https://x.example/adsl"), false);
  assert.equal(patternToRegExp("/a.b+(c)").test("https://x.example/a.b+(c)"), true);
});

test("types, party, page scope and exceptions decide together", () => {
  const decide = index(parse(asset([
    ["B", "banner", "/ads/banner.", "image,imageset", "-", "-", "*"],
    ["B", "pixel", "/pixel.gif", "*", "-", "-", "3"],
    ["B", "promo", "/promo/", "*", "news.example", "sport.news.example", "*"],
    ["A", "banner", "/ads/banner.ok", "*", "-", "-", "*"],
  ])), rules);
  assert.equal(decide("https://x.example/ads/banner.png", "image", "x.example", "news.example"), "B");
  assert.equal(decide("https://x.example/ads/banner.png", "script", "x.example", "news.example"), null);
  assert.equal(decide("https://x.example/ads/banner.ok.png", "image", "x.example", "news.example"), "A");
  assert.equal(decide("https://x.example/ads/banner.ok.png", "image", "x.example", "news.example", { strict: true }), "B");
  assert.equal(decide("https://cdn.other.example/pixel.gif", "image", "cdn.other.example", "news.example"), "B");
  assert.equal(decide("https://cdn.news.example/pixel.gif", "image", "cdn.news.example", "www.news.example"), null);
  assert.equal(decide("https://cdn.example/promo/a.js", "script", "cdn.example", "www.news.example"), "B");
  assert.equal(decide("https://cdn.example/promo/a.js", "script", "cdn.example", "sport.news.example"), null);
  assert.equal(decide("https://cdn.example/promo/a.js", "script", "cdn.example", "other.example"), null);
});

test("the bundled EasyList filters load and block typical ad URLs", () => {
  const text = fs.readFileSync(new URL("../app/src/blocking/gecko/easylist_network_filters.txt", import.meta.url), "utf8");
  const filters = parse(text);
  assert.ok(filters.length > 10_000);
  const decide = index(filters, rules);
  const started = performance.now();
  for (let i = 0; i < 20_000; i++) {
    decide(`https://cdn${i}.example.org/static/app.${i}.js?v=${i}`, "script", `cdn${i}.example.org`, "news.example");
  }
  console.log(`EasyList URL filters: ${((performance.now() - started) * 1000 / 20_000).toFixed(1)} µs per request`);
  assert.equal(decide("https://www.example.org/static/app.js", "script", "www.example.org", "news.example"), null);
  // EasyPrivacy: New Relic's browser agent and a stats beacon, from any host.
  assert.equal(decide("https://js-agent.example/nr-spa-1216.min.js", "script", "js-agent.example", "news.example"), "B");
  assert.equal(decide("https://news.example/aap/stats?id=1", "xmlhttprequest", "news.example", "news.example"), "B");
});

test("a pattern edge that is only `*` adds nothing to scan", () => {
  assert.equal(patternToRegExp("*/ads/*").source, patternToRegExp("/ads/").source);
  assert.equal(patternToRegExp("|https://x.example/*").source.endsWith(".*"), false);
  assert.equal(patternToRegExp("/ads/*|").source.endsWith(".*$"), true);
});
