import assert from "node:assert/strict";
import fs from "node:fs";
import test from "node:test";
import vm from "node:vm";

const context = vm.createContext({ Map, Object, Set, URL });
// The background page loads rules.js first; privacy_signals.js takes its registrable domains.
for (const script of ["rules.js", "privacy_signals.js"]) {
  vm.runInContext(
    fs.readFileSync(new URL(`../app/src/gecko/assets/candy_privacy/${script}`, import.meta.url), "utf8"),
    context,
  );
}
const signals = context.CandyPrivacySignals;

test("request headers replace spoofed values and keep unrelated headers", () => {
  const headers = signals.requestHeaders(
    [
      { name: "Accept", value: "text/html" },
      { name: "dNt", value: "0" },
      { name: "SEC-GPC", value: "0" },
    ],
    { doNotTrackEnabled: true, globalPrivacyControlEnabled: true },
  );

  assert.deepEqual(JSON.parse(JSON.stringify(headers)), [
    { name: "Accept", value: "text/html" },
    { name: "DNT", value: "1" },
    { name: "Sec-GPC", value: "1" },
  ]);
});

test("strict site protection drops the referrer to other sites only", () => {
  const policy = { pageHost: "news.example", trimCrossSiteReferrer: true };
  const headers = [{ name: "Referer", value: "https://news.example/story" }];
  const plain = (value) => JSON.parse(JSON.stringify(value));

  assert.deepEqual(plain(signals.requestHeaders(headers, policy, "https://ads.tracker.test/pixel")), []);
  assert.deepEqual(plain(signals.requestHeaders(headers, policy, "https://cdn.news.example/a.js")), plain(headers));
  assert.deepEqual(plain(signals.requestHeaders(headers, policy, "https://news.example/b")), plain(headers));
  assert.deepEqual(
    plain(signals.requestHeaders(headers, { ...policy, trimCrossSiteReferrer: false }, "https://ads.tracker.test/")),
    plain(headers),
  );
  assert.deepEqual(plain(signals.requestHeaders(headers, policy, "not a url")), plain(headers));
});

test("strict site protection keeps the referrer between subdomains of one site", () => {
  const policy = { pageHost: "www.example.co.uk", trimCrossSiteReferrer: true };
  const headers = [{ name: "Referer", value: "https://www.example.co.uk/" }];
  const plain = (value) => JSON.parse(JSON.stringify(value));

  assert.deepEqual(plain(signals.requestHeaders(headers, policy, "https://img.example.co.uk/a.png")), plain(headers));
  assert.deepEqual(plain(signals.requestHeaders(headers, policy, "https://other.co.uk/a.png")), []);
  assert.deepEqual(
    plain(signals.requestHeaders(headers, { pageHost: "192.168.1.5", trimCrossSiteReferrer: true }, "http://10.0.1.5/")),
    [],
  );
});

test("strict site protection judges a request by the document that made it", () => {
  // A link from a strict page: the tab already shows the destination, the origin is the strict page.
  const policy = { pageHost: "shop.test", trimCrossSiteReferrer: true };
  const headers = [{ name: "Referer", value: "https://news.example/story" }];
  const plain = (value) => JSON.parse(JSON.stringify(value));

  assert.deepEqual(
    plain(signals.requestHeaders(headers, policy, "https://shop.test/", "https://news.example/story")),
    [],
  );
  assert.deepEqual(
    plain(signals.requestHeaders(headers, policy, "https://cdn.shop.test/a.js", "https://www.shop.test/")),
    plain(headers),
  );
  assert.deepEqual(
    plain(signals.requestHeaders(headers, policy, "https://shop.test/", "about:blank")),
    plain(headers),
  );
});

test("disabled request signals remove stale privacy headers", () => {
  const headers = signals.requestHeaders(
    [{ name: "DNT", value: "1" }, { name: "Sec-GPC", value: "1" }],
    { doNotTrackEnabled: false, globalPrivacyControlEnabled: false },
  );

  assert.deepEqual(JSON.parse(JSON.stringify(headers)), []);
});

test("document signals expose enabled values and neutral disabled values", () => {
  const navigator = {};
  assert.equal(signals.installDocumentSignals(
    { navigator },
    { doNotTrackEnabled: true, globalPrivacyControlEnabled: true },
  ), true);
  assert.equal(navigator.doNotTrack, "1");
  assert.equal(navigator.globalPrivacyControl, true);
  assert.equal(Object.getOwnPropertyDescriptor(navigator, "doNotTrack").get, undefined);
  assert.equal(Object.getOwnPropertyDescriptor(navigator, "doNotTrack").writable, false);
  assert.equal(Object.getOwnPropertyDescriptor(navigator, "globalPrivacyControl").get, undefined);
  assert.equal(Object.getOwnPropertyDescriptor(navigator, "globalPrivacyControl").writable, false);

  signals.installDocumentSignals(
    { navigator },
    { doNotTrackEnabled: false, globalPrivacyControlEnabled: false },
  );
  assert.equal(navigator.doNotTrack, null);
  assert.equal(navigator.globalPrivacyControl, false);
});

test("registered document-start code uses primitive descriptors and rejects stale revisions", () => {
  const navigator = {};
  const pageContext = vm.createContext({ navigator });
  vm.runInContext(
    signals.registrationCode(
      { doNotTrackEnabled: true, globalPrivacyControlEnabled: true },
      4,
    ),
    pageContext,
  );

  const dnt = Object.getOwnPropertyDescriptor(navigator, "doNotTrack");
  const gpc = Object.getOwnPropertyDescriptor(navigator, "globalPrivacyControl");
  assert.equal(dnt.value, "1");
  assert.equal(dnt.get, undefined);
  assert.equal(dnt.set, undefined);
  assert.equal(dnt.writable, false);
  assert.equal(typeof dnt.value, "string");
  assert.equal(gpc.value, true);
  assert.equal(gpc.get, undefined);
  assert.equal(gpc.set, undefined);
  assert.equal(gpc.writable, false);
  assert.equal(typeof gpc.value, "boolean");

  vm.runInContext(
    signals.registrationCode(
      { doNotTrackEnabled: false, globalPrivacyControlEnabled: false },
      5,
    ),
    pageContext,
  );
  vm.runInContext(
    signals.registrationCode(
      { doNotTrackEnabled: true, globalPrivacyControlEnabled: true },
      4,
    ),
    pageContext,
  );
  assert.equal(navigator.doNotTrack, null);
  assert.equal(navigator.globalPrivacyControl, false);
  assert.equal(navigator.__candyPrivacySignalRevision, 5);
});
