"use strict";

// Tells the v5 bar whether the page reads as an article, so the capsule can offer «Read»
// (system-v5.md, section 5.1). Runs in the top frame once the page has loaded and again a moment
// later for articles that arrive late. The test is cheap and leans to «no»: it adds up the long,
// visible paragraphs that sit outside page furniture (navigation, comments, footers, promotions)
// and asks for about one real article's worth. Nothing about the page leaves it but that answer.
(() => {
  const minParagraphLength = 140;
  const enoughScore = 20;
  const maxParagraphs = 600;
  const maxAncestors = 6;
  const lateLookMillis = 1500;
  const furniture =
    /(^|[\s_-])(nav|menu|footer|header|comment|sidebar|related|share|social|promo|banner|cookie|subscribe|newsletter|advert|sponsor|breadcrumb|pagination)/i;
  const story = /(^|[\s_-])(article|content|main|post|story|entry|body|text)/i;
  const furnitureTags = new Set(["nav", "footer", "aside", "form"]);

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

  function names(element) {
    const className = typeof element.className === "string" ? element.className : "";
    return `${className} ${element.id || ""}`;
  }

  function isFurniture(node) {
    let element = node.parentElement;
    for (let depth = 0; element && depth < maxAncestors; element = element.parentElement, depth++) {
      if (furnitureTags.has(element.localName)) return true;
      const label = names(element);
      if (furniture.test(label) && !story.test(label)) return true;
    }
    return false;
  }

  function isVisible(node) {
    if (node.closest?.("[hidden], [aria-hidden='true']")) return false;
    if (typeof getComputedStyle !== "function") return true;
    const style = getComputedStyle(node);
    return style.display !== "none" && style.visibility !== "hidden";
  }

  function paragraphs(doc) {
    const nodes = doc.querySelectorAll?.("p, pre") || [];
    const result = [];
    for (let index = 0; index < nodes.length && index < maxParagraphs; index++) {
      const node = nodes[index];
      const length = (node.textContent || "").trim().length;
      // Short ones never count, so they skip the costlier checks.
      if (length < minParagraphLength) continue;
      result.push({ length, furniture: isFurniture(node), visible: isVisible(node) });
    }
    return result;
  }

  let reportedKey = null;

  function report() {
    const bridge = globalThis.CandyContentTopInset;
    if (!bridge?.readerAvailable || document.readyState !== "complete") return;
    const available = readable(paragraphs(document));
    const key = `${bridge.navigationGeneration()}:${bridge.policyRevision()}:${available}`;
    if (key === reportedKey) return;
    if (bridge.readerAvailable(available)) reportedKey = key;
  }

  globalThis.VolaReaderProbe = Object.freeze({ readable });
  globalThis.__candyReportReaderAvailable = report;
  globalThis.addEventListener?.("load", () => {
    report();
    setTimeout(report, lateLookMillis);
  }, { once: true });
  globalThis.addEventListener?.("pageshow", report);
})();
