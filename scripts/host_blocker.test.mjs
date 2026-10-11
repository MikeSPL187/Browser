import assert from "node:assert/strict";
import fs from "node:fs";
import test from "node:test";
import vm from "node:vm";

// The same cases as RequestBlockerTest.kt: both engines must block the same hosts.
const context = vm.createContext({ Map, Set, URL, Uint32Array, Math, String, Error, atob, TextDecoder });
for (const script of ["rules.js", "host_blocker.js"]) {
  vm.runInContext(
    fs.readFileSync(new URL(`../app/src/gecko/assets/candy_privacy/${script}`, import.meta.url), "utf8"),
    context,
  );
}
const { create, sortedHostIndex } = context.CandyHostBlocker;
const blocker = create({ hostLists: ["doubleclick.net\ntracker.example"] });

test("blocks the exact host and its subdomains", () => {
  assert.equal(blocker.shouldBlock("https://doubleclick.net/ad.js", "https://news.example"), true);
  assert.equal(blocker.shouldBlock("https://stats.tracker.example/pixel", "https://news.example"), true);
  assert.equal(blocker.shouldBlock("HTTPS://doubleclick.net/ad.js", "https://news.example"), true);
});

test("keeps the current site working and ignores what is not a web host", () => {
  assert.equal(blocker.shouldBlockHosts("stats.tracker.example", "news.example"), true);
  assert.equal(blocker.shouldBlockHosts("stats.tracker.example", "tracker.example"), false);
  assert.equal(blocker.shouldBlock("https://tracker.example/app.js", "https://www.tracker.example/a"), false);
  assert.equal(blocker.shouldBlockHosts("not a host/path", "news.example"), false);
  assert.equal(blocker.shouldBlock("https://notdoubleclick.net/app.js", "https://news.example"), false);
  assert.equal(blocker.shouldBlock("not a url", "https://news.example"), false);
  assert.equal(blocker.shouldBlock("data:text/plain,hello", "https://news.example"), false);
});

test("sorted index matches exact hosts and rejects an unsorted list", () => {
  const index = sortedHostIndex("# generated\r\nads.example\r\nalpha.example\nbeta.example\n");
  assert.equal(index.size, 3);
  assert.equal(index.has("ads.example"), true);
  assert.equal(index.has("alpha.example"), true);
  assert.equal(index.has("sub.ads.example"), false);
  assert.equal(index.has("other.example"), false);
  assert.throws(() => sortedHostIndex("b.example\na.example\n"));
});

test("indexed lists match parent domains across several indexes", () => {
  const indexed = create({ indexedLists: ["easy.example\n", "ads.example\nhagezi.example\n"] });
  assert.equal(indexed.shouldBlockHosts("cdn.ads.example", "news.example"), true);
  assert.equal(indexed.shouldBlockHosts("cdn.hagezi.example", "news.example"), true);
  assert.equal(indexed.shouldBlockHosts("notads.example", "news.example"), false);
  assert.equal(indexed.shouldBlockHosts("hagezi.example.com", "news.example"), false);
});

test("normalizes adblock host anchors and ignores unsupported rules", () => {
  const anchored = create({ hostLists: ["||Ads.Example.^\n||bad.example/path^\n*.wildcard.example"] });
  assert.equal(anchored.shouldBlock("https://cdn.ads.example/file.js", "https://news.example"), true);
  assert.equal(anchored.shouldBlock("https://bad.example/path", "https://news.example"), false);
  assert.equal(anchored.shouldBlock("https://x.wildcard.example/file.js", "https://news.example"), false);
});

test("allow pairs apply only on their page and win over blocks", () => {
  const exceptions = create({
    hostLists: ["adsninja.ca\ntracker.example\nads.example"],
    blockedPairs: ["ads.example\tnews.example"],
    allowedPairs: ["adsninja.ca\thowtogeek.com\ntracker.example\tlocal.news.example\nads.example\tnews.example"],
  });
  assert.equal(exceptions.shouldBlock("https://cdn.adsninja.ca/c.js", "https://www.howtogeek.com/a"), false);
  assert.equal(exceptions.shouldBlock("https://cdn.adsninja.ca/c.js", "https://unrelated.example/a"), true);
  assert.equal(exceptions.shouldBlock("https://tracker.example/pixel", "https://news.example/a"), true);
  assert.equal(exceptions.shouldBlock("https://ads.example/b.js", "https://news.example/a"), false);
  assert.equal(exceptions.shouldBlock("https://ads.example/b.js", "https://other.example/a"), true);
});

test("a host pair blocks only on its first party", () => {
  const pairs = create({ blockedPairs: ["ads.example\tnews.example"] });
  assert.equal(pairs.shouldBlock("https://cdn.ads.example/b.js", "https://www.news.example/a"), true);
  assert.equal(pairs.shouldBlock("https://cdn.ads.example/b.js", "https://notnews.example/a"), false);
});

test("the owner family exception allows real Google registries and rejects lookalikes", () => {
  const family = create({
    hostLists: ["adservice.google.com"],
    allowedPairs: ["adservice.google.com\tpartner.example"],
    familyPairs: ["adservice.google.com\tgoogle.*"],
  });
  for (const page of ["google.com", "google.de", "maps.google.com", "partner.example"]) {
    assert.equal(family.shouldBlockHosts("adservice.google.com", page), false, page);
  }
  for (const page of ["news.example", "google.evil.com", "google.com.evil"]) {
    assert.equal(family.shouldBlockHosts("adservice.google.com", page), true, page);
  }
});

test("the bundled lists load as the WebView blocker reads them", () => {
  const read = (name) => fs.readFileSync(new URL(`../app/src/blocking/assets/${name}`, import.meta.url), "utf8");
  const started = performance.now();
  const bundled = create({
    hostLists: [read("blocked_hosts.txt"), read("uassets_blocked_hosts.txt")],
    indexedLists: [read("easylist_blocked_hosts.txt"), read("hagezi_blocked_hosts.txt")],
    blockedPairs: [read("uassets_blocked_host_pairs.txt")],
    allowedPairs: [read("easylist_allowed_host_pairs.txt"), read("uassets_allowed_host_pairs.txt")],
    familyPairs: [read("first_party_family_allowed_host_pairs.txt")],
  });
  const builtMs = performance.now() - started;
  // The sizes CandyDefaultRulesAssetInstrumentedTest pins for the Kotlin index.
  assert.deepEqual(Array.from(bundled.indexedSizes), [100_377, 166_078]);
  assert.equal(bundled.shouldBlockHosts("0.myikas.com", "news.example"), true);
  assert.equal(bundled.shouldBlockHosts("analyticsengine.s3.amazonaws.com", "news.example"), true);
  assert.equal(bundled.shouldBlockHosts("stats.doubleclick.net", "news.example"), true);
  assert.equal(bundled.shouldBlockHosts("adservice.google.com", "www.google.com"), false);
  assert.equal(bundled.shouldBlockHosts("example.com", "news.example"), false);
  const lookups = 20_000;
  const lookupStarted = performance.now();
  for (let i = 0; i < lookups; i++) bundled.shouldBlockHosts(`cdn${i}.static.example.org`, "news.example");
  const perLookupUs = (performance.now() - lookupStarted) * 1000 / lookups;
  console.log(`host lists: built in ${builtMs.toFixed(0)} ms, ${perLookupUs.toFixed(1)} µs per lookup`);
});
