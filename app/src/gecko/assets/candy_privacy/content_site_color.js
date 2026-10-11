"use strict";

// Reports the page's <meta name="theme-color"> for the v5 bar color (docs/vola/design/system-v5.md,
// section 1.3, source 1). Runs in the top frame only. A meta whose `media` does not match is
// skipped, and media changes (a light/dark switch) and meta edits are reported again. Alpha is
// ignored; a fully transparent color counts as none.
(() => {
  const maxMediaWatchers = 8;
  const maxColorLength = 64;
  const mediaWatchers = new Map();
  let headObserver = null;
  let scheduled = false;
  let reportedKey = null;
  let parser = null;

  function parsedColor(value) {
    if (typeof value !== "string") return null;
    const text = value.trim();
    if (!text || text.length > maxColorLength) return null;
    parser ||= document.createElement("canvas").getContext("2d");
    if (!parser) return null;
    // An invalid color leaves fillStyle unchanged, so two different starting values tell it apart.
    parser.fillStyle = "#000000";
    parser.fillStyle = text;
    const first = parser.fillStyle;
    parser.fillStyle = "#ffffff";
    parser.fillStyle = text;
    if (parser.fillStyle !== first) return null;
    if (/^#[0-9a-f]{6}$/i.test(first)) return first.toLowerCase();
    const rgba = /^rgba?\(\s*(\d+),\s*(\d+),\s*(\d+)(?:,\s*([\d.]+))?\s*\)$/.exec(first);
    if (!rgba || (rgba[4] !== undefined && Number(rgba[4]) <= 0)) return null;
    return `#${rgba.slice(1, 4).map((channel) =>
      Math.min(255, Number(channel)).toString(16).padStart(2, "0")).join("")}`;
  }

  function mediaMatches(media) {
    if (!globalThis.matchMedia) return true;
    let query = mediaWatchers.get(media);
    if (!query) {
      query = globalThis.matchMedia(media);
      if (mediaWatchers.size < maxMediaWatchers) {
        query.addEventListener?.("change", schedule);
        mediaWatchers.set(media, query);
      }
    }
    return query.matches;
  }

  function activeThemeColor() {
    for (const meta of document.querySelectorAll?.('meta[name="theme-color" i]') || []) {
      const media = meta.getAttribute("media");
      if (media && !mediaMatches(media)) continue;
      const color = parsedColor(meta.getAttribute("content"));
      if (color) return color;
    }
    return null;
  }

  function touchesMeta(record) {
    if (record.type === "attributes") return record.target?.localName === "meta";
    for (const node of [...record.addedNodes, ...record.removedNodes]) {
      if (node.localName === "meta" || node.querySelector?.("meta")) return true;
    }
    return false;
  }

  function observeHead() {
    if (headObserver || !document.head || typeof MutationObserver !== "function") return;
    headObserver = new MutationObserver((records) => {
      if (records.some(touchesMeta)) schedule();
    });
    headObserver.observe(document.head, {
      childList: true,
      subtree: true,
      attributes: true,
      attributeFilter: ["content", "media", "name"],
    });
  }

  function report() {
    scheduled = false;
    const bridge = globalThis.CandyContentTopInset;
    if (!bridge?.siteColor) return;
    observeHead();
    const color = activeThemeColor();
    // While the head is still parsing, "no color yet" is not news: the browser keeps the previous
    // page's color until this page reports, so reporting none early would flash the neutral bar.
    if (color === null && document.readyState === "loading") return;
    const key = `${bridge.navigationGeneration()}:${bridge.policyRevision()}:${color}`;
    if (key === reportedKey) return;
    if (bridge.siteColor(color)) reportedKey = key;
  }

  function schedule() {
    if (scheduled) return;
    scheduled = true;
    setTimeout(report, 0);
  }

  globalThis.__candyReportSiteColor = schedule;
  document.addEventListener("DOMContentLoaded", schedule, { once: true });
  globalThis.addEventListener("pageshow", schedule);
  schedule();
})();
