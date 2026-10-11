import assert from "node:assert/strict";
import fs from "node:fs";
import test from "node:test";
import vm from "node:vm";

const context = vm.createContext({ Map, Set, URL, Uint32Array, Uint8Array, Math, String, Error, Promise, atob, TextDecoder, RegExp, Array, Number });
// The background page's order: rules.js, then the host lists, then the request decisions.
for (const script of ["rules.js", "host_blocker.js", "network_filters.js", "network_blocker.js"]) {
  vm.runInContext(
    fs.readFileSync(new URL(`../app/src/gecko/assets/candy_privacy/${script}`, import.meta.url), "utf8"),
    context,
  );
}
const { create, load, LIST_FILES } = context.CandyNetworkBlocker;
const action = (decision) => (decision === null ? null : decision.action);
// Gecko-only lists live in app/src/blocking/gecko; the extension gets both folders.
const readList = async (name) => {
  const shared = new URL(`../app/src/blocking/assets/${name}`, import.meta.url);
  const gecko = new URL(`../app/src/blocking/gecko/${name}`, import.meta.url);
  return fs.readFileSync(fs.existsSync(shared) ? shared : gecko, "utf8");
};

const advancedText = [
  "candy-advanced-filter:2",
  "# Rules: 3",
  ["B", "N", "tracker.example", Buffer.from("/ads/*").toString("base64url"), "-", "-", "*", "-"].join("\t"),
  ["A", "N", "tracker.example", Buffer.from("/ads/allowed.js").toString("base64url"), "-", "-", "*", "-"].join("\t"),
  ["B", "N", "*", Buffer.from("/banner/top.jpg").toString("base64url"), "anime.example", "-", "*", "-"].join("\t"),
].join("\n");

test("a URL rule decides before the host lists, and its allow wins", () => {
  const blocker = create({ lists: { hostLists: ["cdn.example"] }, advancedText });
  assert.equal(action(blocker.decide("https://tracker.example/ads/file.js?x=1", "news.example")), "B");
  assert.equal(action(blocker.decide("https://tracker.example/ads/allowed.js", "news.example")), null);
  assert.equal(action(blocker.decide("https://img.anime.example/banner/top.jpg", "anime.example")), "B");
  assert.equal(action(blocker.decide("https://img.anime.example/banner/top.jpg", "other.example")), null);
  assert.equal(action(blocker.decide("https://cdn.example/a.js", "news.example")), "B");
  assert.equal(action(blocker.decide("https://cdn.example/a.js", "www.cdn.example")), null);
  assert.equal(action(blocker.decide("data:text/plain,x", "news.example")), null);
});

test("strict drops the URL rules' allow exceptions too", () => {
  const blocker = create({ advancedText });
  assert.equal(action(blocker.decide("https://tracker.example/ads/allowed.js", "news.example", { strict: true })), "B");
});

test("the bundled lists load through the extension's file reader", async () => {
  const requested = [];
  const blocker = await load(async (name) => {
    requested.push(name);
    return readList(name);
  });
  assert.equal(requested.length, 10);
  assert.deepEqual(Array.from(blocker.indexedSizes), [100_377, 166_078]);
  // The asset header counts 747 request rules; popup and popunder rules stay out.
  assert.equal(blocker.advancedRuleCount, 747);
  assert.equal(action(blocker.decide("https://stats.doubleclick.net/x.js", "news.example")), "B");
  assert.equal(action(blocker.decide("https://www.example.com/", "news.example")), null);
  assert.equal(action(blocker.decide("https://9anime.vip/banner/top.jpg", "9anime.vip")), "B");
  assert.ok(blocker.networkFilterCount > 10_000);
  // EasyList URL rules: a path on any host, and the resource type the rule names.
  assert.equal(action(blocker.decide("https://js-agent.example/nr-spa-1216.min.js", "news.example", { type: "script" })), "B");
  const started = performance.now();
  for (let i = 0; i < 20_000; i++) blocker.decide(`https://cdn${i}.static.example.org/app.js`, "news.example");
  console.log(`decide: ${((performance.now() - started) * 1000 / 20_000).toFixed(1)} µs per request`);
});

test("a missing list fails the load instead of blocking less", async () => {
  await assert.rejects(load(async (name) => {
    if (name === LIST_FILES.advanced) throw new Error("missing");
    return readList(name);
  }));
});

test("the person's own rules win over the lists, by the Kotlin precedence", () => {
  const blocker = create({ lists: { hostLists: ["ads.example"] }, advancedText });
  const rules = [
    { id: "allow-host", a: "A", k: "H", r: "ads.example", f: null },
    { id: "block-pair", a: "B", k: "P", r: "cdn.ads.example", f: "news.example" },
    { id: "allow-pair", a: "A", k: "P", r: "ads.example", f: "news.example" },
    { id: "block-own", a: "B", k: "H", r: "fonts.example", f: null },
  ];
  // A scoped pair allow beats a more specific pair block.
  assert.deepEqual(
    JSON.parse(JSON.stringify(blocker.decide("https://cdn.ads.example/a.js", "www.news.example", { userRules: rules }))),
    { action: "A", ruleId: "allow-pair", builtIn: false },
  );
  // Elsewhere the host allow lets a listed host through, even on Strict.
  assert.equal(blocker.decide("https://ads.example/a.js", "other.example", { userRules: rules, strict: true }).ruleId,
    "allow-host");
  assert.equal(blocker.decide("https://cdn.fonts.example/f.woff", "other.example", { userRules: rules }).ruleId,
    "block-own");
  assert.equal(action(blocker.decide("https://ads.example/a.js", "other.example")), "B");
});

test("pages on the uAssets popup lists get a window.open that opens nothing", async () => {
  const blocker = await load(readList);
  assert.ok(blocker.windowOpenDefuserCount > 600);
  assert.equal(blocker.defusesWindowOpen("0xxx.ws"), true);
  assert.equal(blocker.defusesWindowOpen("www.0xxx.ws"), true);
  assert.equal(blocker.defusesWindowOpen("123mkv.com"), true);
  assert.equal(blocker.defusesWindowOpen("news.example"), false);
  // Rules that block the new window itself stay with the Kotlin popup filter.
  assert.equal(blocker.defusesWindowOpen("sextgem.com"), false);
  const excluded = create({
    advancedText: [
      "candy-advanced-filter:2",
      "# Rules: 1",
      ["B", "P", "*", Buffer.from("*").toString("base64url"), "movies.example", "kids.movies.example", "*", "W"]
        .join("\t"),
    ].join("\n"),
  });
  assert.equal(excluded.defusesWindowOpen("www.movies.example"), true);
  assert.equal(excluded.defusesWindowOpen("kids.movies.example"), false);
});

test("list rules judge domain= and the party by the frame's own document", () => {
  const networkFiltersText = [
    "candy-network-filter:2",
    "# Rules: 1",
    ["B", "creative", "/creative/", "*", "adframe.example", "-", "*"].join("\t"),
  ].join("\n");
  const blocker = create({ networkFiltersText });
  const url = "https://cdn.example/creative/banner.png";
  assert.equal(action(blocker.decide(url, "news.example", { type: "image" })), null);
  assert.equal(
    action(blocker.decide(url, "news.example", { type: "image", documentHost: "adframe.example" })),
    "B",
  );
});
