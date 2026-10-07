import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { test } from "node:test";
import vm from "node:vm";

const TOKEN = "a".repeat(32);
const NAME = "CandySystemMediaBridge";

const script = readFileSync(
  new URL(
    "../app/src/main/java/dev/sk2andy/materialbrowser/browser/systemwebview/SystemWebViewMediaBridge.kt",
    import.meta.url,
  ),
  "utf8",
)
  .split('fun script(token: String): String {')[1]
  .split('return """')[1]
  .split('""".trimIndent()')[0]
  .replaceAll("$token", TOKEN)
  .replaceAll("$NAME", NAME);

class FakeElement {
  constructor(children = []) {
    this.children = children;
    this.isConnected = true;
  }

  contains(node) {
    return node === this || this.children.some((child) => child.contains(node));
  }

  querySelectorAll() {
    return this.children.flatMap((child) =>
      child instanceof FakeMedia ? [child] : child.querySelectorAll(),
    );
  }
}

class FakeMedia extends FakeElement {
  constructor({ src, paused = true, ended = false, width = 0, height = 0 }) {
    super();
    this.currentSrc = src;
    this.src = src;
    this.paused = paused;
    this.ended = ended;
    this.currentTime = 0;
    this.duration = 60;
    this.playbackRate = 1;
    this.videoWidth = width;
    this.videoHeight = height;
  }
}

class FakeVideo extends FakeMedia {}

function install() {
  const listeners = new Map();
  const messages = [];
  let now = 0;
  const document = {
    title: "Page",
    fullscreenElement: null,
    addEventListener(type, listener) {
      listeners.set(type, [...(listeners.get(type) || []), listener]);
    },
  };
  const context = {
    document,
    HTMLMediaElement: FakeMedia,
    HTMLVideoElement: FakeVideo,
    performance: { now: () => now },
    [NAME]: { postMessage: (message) => messages.push(JSON.parse(message)) },
  };
  context.globalThis = context;
  vm.runInNewContext(script, context);
  return {
    document,
    messages,
    advance(millis) {
      now += millis;
    },
    dispatch(type, target) {
      (listeners.get(type) || []).forEach((listener) => listener({ target }));
    },
    get last() {
      return messages.at(-1);
    },
  };
}

function fullscreenLandscapeVideo(page) {
  const video = new FakeVideo({ src: "https://example.com/a.mp4", width: 1920, height: 1080 });
  const container = new FakeElement([video]);
  video.paused = false;
  page.dispatch("play", video);
  page.document.fullscreenElement = container;
  page.dispatch("fullscreenchange", page.document);
  return video;
}

test("passive events of other media keep the fullscreen owner", () => {
  const page = install();
  fullscreenLandscapeVideo(page);
  const portrait = new FakeVideo({ src: "https://example.com/b.mp4", width: 640, height: 1280 });
  const audio = new FakeMedia({ src: "https://example.com/c.mp3", paused: false });

  for (const event of ["loadedmetadata", "durationchange", "volumechange", "ratechange"]) {
    page.dispatch(event, portrait);
  }
  audio.paused = true;
  page.dispatch("pause", audio);

  assert.equal(page.last.source, "https://example.com/a.mp4");
  assert.equal(page.last.fullscreen, true);
  assert.equal(page.last.playing, true);
  assert.equal(page.last.videoWidth, 1920);
});

test("another play does not take over a fullscreen owner", () => {
  const page = install();
  fullscreenLandscapeVideo(page);
  const advert = new FakeVideo({ src: "https://example.com/ad.mp4", paused: false });

  page.dispatch("play", advert);
  page.advance(1_000);
  page.dispatch("timeupdate", advert);

  assert.equal(page.last.source, "https://example.com/a.mp4");
});

test("fullscreenchange publishes the element that entered fullscreen", () => {
  const page = install();
  const first = new FakeVideo({ src: "https://example.com/a.mp4", paused: false });
  page.dispatch("play", first);
  first.paused = true;
  page.dispatch("pause", first);
  const second = new FakeVideo({ src: "https://example.com/b.mp4", width: 1280, height: 720 });

  page.document.fullscreenElement = new FakeElement([second]);
  page.dispatch("fullscreenchange", page.document);

  assert.equal(page.last.source, "https://example.com/b.mp4");
  assert.equal(page.last.fullscreen, true);
});

test("owner keeps reporting and a new play takes over outside fullscreen", () => {
  const page = install();
  const first = new FakeMedia({ src: "https://example.com/a.mp3", paused: false });
  page.dispatch("play", first);
  const second = new FakeMedia({ src: "https://example.com/b.mp3" });

  page.dispatch("loadedmetadata", second);
  assert.equal(page.last.source, "https://example.com/a.mp3");

  second.paused = false;
  page.dispatch("play", second);
  assert.equal(page.last.source, "https://example.com/b.mp3");
  assert.equal(page.last.playing, true);
});

test("ended or paused owner releases ownership to the next media", () => {
  const page = install();
  const video = fullscreenLandscapeVideo(page);
  video.paused = true;
  video.ended = true;
  page.dispatch("ended", video);
  assert.equal(page.last.active, false);

  page.document.fullscreenElement = null;
  page.dispatch("fullscreenchange", page.document);
  assert.equal(page.last.fullscreen, false);

  const next = new FakeVideo({ src: "https://example.com/next.mp4" });
  page.dispatch("loadedmetadata", next);
  assert.equal(page.last.source, "https://example.com/next.mp4");
});
