"use strict";

// Tells the v5 bar whether the page reads as an article, so the capsule can offer «Read»
// (system-v5.md, section 5.1). The test follows Mozilla's own «probably readerable» one: long,
// visible paragraphs that are not page furniture, about one article's worth. It looks where Reader
// view reads (the first article, else main, else the body) and skips what Reader drops (nav, aside,
// footer, form), so «Read» is offered only where Reader has something to show. Nothing about the
// page leaves it but that answer.
(() => {
  const minParagraphLength = 140;
  const enoughScore = 20;
  const maxParagraphs = 600;
  const maxAncestors = 6;
  // After a load or an in-page navigation the article may still be arriving.
  const lateLooksMillis = [500, 1500];
  const furnitureWords = new Set([
    "nav", "navbar", "navigation", "menu", "footer", "header", "comment", "comments", "sidebar",
    "related", "share", "sharing", "social", "promo", "banner", "cookie", "cookies", "subscribe",
    "newsletter", "advert", "ad", "ads", "sponsor", "sponsored", "breadcrumb", "breadcrumbs",
    "pagination",
  ]);
  const storyWords = new Set(["article", "content", "main", "post", "story", "entry", "body", "text"]);
  // What Reader view (extractCandyReaderPayload) removes before it reads the page.
  const droppedByReader = "nav, aside, footer, form";

  /** Whether [paragraphs] ({ length, furniture, visible }) add up to an article. */
  function readable(paragraphs) {
    let score = 0;
    for (const paragraph of paragraphs) {
      if (!paragraph.visible || paragraph.furniture || paragraph.length < minParagraphLength) continue;
      score += Math.sqrt(paragraph.length - minParagraphLength);
      if (score > enoughScore) return true;
    }
    return false;
  }

  /** Whether a class and id label names page furniture: a whole word of it, unless a story word too. */
  function furnitureLabel(label) {
    const words = String(label).toLowerCase().split(/[^a-z0-9]+/).filter(Boolean);
    return words.some((word) => furnitureWords.has(word)) && !words.some((word) => storyWords.has(word));
  }

  function label(element) {
    const className = typeof element.className === "string" ? element.className : "";
    return `${className} ${element.id || ""}`;
  }

  function isFurniture(node, root) {
    const dropped = node.closest?.(droppedByReader);
    if (dropped && root.contains?.(dropped)) return true;
    let element = node.parentElement;
    for (let depth = 0; element && element !== root && depth < maxAncestors; element = element.parentElement, depth++) {
      if (furnitureLabel(label(element))) return true;
    }
    return false;
  }

  // Only the paragraph's own flags: a dialog over the page marks the whole page aria-hidden, and
  // the article under it is still an article.
  function isVisible(node) {
    if (node.hidden || node.getAttribute?.("aria-hidden") === "true") return false;
    if (typeof node.checkVisibility === "function") {
      return node.checkVisibility({ checkOpacity: true, checkVisibilityCSS: true });
    }
    if (typeof getComputedStyle !== "function") return true;
    const style = getComputedStyle(node);
    return style.display !== "none" && style.visibility !== "hidden";
  }

  function paragraphs(doc) {
    const root = doc.querySelector?.("article") || doc.querySelector?.("main") || doc.body;
    const nodes = root?.querySelectorAll?.("p") || [];
    const result = [];
    for (let index = 0; index < nodes.length && index < maxParagraphs; index++) {
      const node = nodes[index];
      const length = (node.textContent || "").trim().length;
      // Short ones never count, so they skip the costlier checks.
      if (length < minParagraphLength) continue;
      result.push({ length, furniture: isFurniture(node, root), visible: isVisible(node) });
    }
    return result;
  }

  // The page is measured once per document (navigation generation) and once more at each late
  // look; a new policy revision only sends the answer again.
  let measured = null;
  let reportedKey = null;

  function report(remeasure = false) {
    const bridge = globalThis.CandyContentTopInset;
    if (!bridge?.readerAvailable || document.readyState !== "complete") return;
    const generation = bridge.navigationGeneration();
    const fresh = measured?.generation !== generation;
    if (fresh || remeasure) {
      measured = { generation, available: readable(paragraphs(document)) };
    }
    if (fresh) {
      for (const delay of lateLooksMillis) {
        setTimeout(() => {
          if (globalThis.CandyContentTopInset?.navigationGeneration() === generation) report(true);
        }, delay);
      }
    }
    const key = `${generation}:${bridge.policyRevision()}:${measured.available}`;
    if (key === reportedKey) return;
    if (bridge.readerAvailable(measured.available)) reportedKey = key;
  }

  globalThis.VolaReaderProbe = Object.freeze({ readable, furnitureLabel, paragraphs });
  globalThis.__candyReportReaderAvailable = () => report();
  globalThis.addEventListener?.("load", () => report(true), { once: true });
  globalThis.addEventListener?.("pageshow", () => report(true));
})();
