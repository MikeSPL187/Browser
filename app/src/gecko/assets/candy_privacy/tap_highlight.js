"use strict";

// Tap highlight (#123, H6): GeckoView shows nothing when a link or a button is tapped, Chrome
// flashes the element. A short translucent flash over the tapped control, in an element that
// takes no input and is removed once it fades. Nothing leaves the page and nothing is stored.
(() => {
  const interactive = [
    "a[href]", "area[href]", "button", "summary", "label", "select",
    "input[type=button]", "input[type=submit]", "input[type=reset]", "input[type=image]",
    "input[type=checkbox]", "input[type=radio]", "input[type=file]", "input[type=color]",
    "[role=button]", "[role=link]", "[role=tab]", "[role=menuitem]", "[role=option]",
    "[role=checkbox]", "[role=radio]", "[role=switch]", "[onclick]",
  ].join(",");
  // A pan cancels the pointer within a few frames; waiting that long keeps scrolls flash-free.
  const showDelayMs = 50;
  const minVisibleMs = 100;
  const fadeMs = 180;
  const slopCssPx = 10;
  const maxViewportShare = 0.4;
  const maxAncestors = 8;
  const tag = "vola-tap-highlight";

  let pending = null;

  const now = () => globalThis.performance?.now?.() ?? Date.now();
  const reducedMotion = () => {
    try { return globalThis.matchMedia?.("(prefers-reduced-motion: reduce)")?.matches === true; }
    catch (_error) { return false; }
  };

  const transparent = (color) => !color || color === "transparent" ||
    /^rgba\(\s*\d+\s*,\s*\d+\s*,\s*\d+\s*,\s*0(\.0+)?\s*\)$/.test(color);

  // Light pages get a dark flash, dark pages a light one.
  const tint = (element) => {
    let node = element;
    for (let depth = 0; node && depth < maxAncestors; depth++, node = node.parentElement) {
      const color = getComputedStyle(node).backgroundColor;
      if (transparent(color)) continue;
      const channels = color.match(/\d+(\.\d+)?/g)?.slice(0, 3).map(Number) ?? [255, 255, 255];
      const luminance = 0.299 * channels[0] + 0.587 * channels[1] + 0.114 * channels[2];
      return luminance < 128 ? "rgba(255, 255, 255, 0.24)" : "rgba(0, 0, 0, 0.14)";
    }
    return "rgba(0, 0, 0, 0.14)";
  };

  const target = (event) => {
    const start = event.target;
    if (!start || typeof start.closest !== "function") return null;
    const element = start.closest(interactive);
    if (!element || element.matches(":disabled")) return null;
    const style = getComputedStyle(element);
    // A site that turns the WebKit tap highlight off draws its own feedback.
    const highlight = style.getPropertyValue("-webkit-tap-highlight-color");
    if (highlight !== "" && transparent(highlight)) return null;
    const rect = element.getBoundingClientRect();
    if (!(rect.width >= 1 && rect.height >= 1)) return null;
    const viewport = (globalThis.innerWidth || 0) * (globalThis.innerHeight || 0);
    if (viewport > 0 && rect.width * rect.height > maxViewportShare * viewport) return null;
    return { element, rect, radius: style.borderRadius || "0px" };
  };

  const show = (tap) => {
    if (tap.host) return;
    const host = document.createElement(tag);
    const set = (name, value) => host.style.setProperty(name, value, "important");
    set("all", "initial");
    set("position", "fixed");
    set("left", `${tap.target.rect.left}px`);
    set("top", `${tap.target.rect.top}px`);
    set("width", `${tap.target.rect.width}px`);
    set("height", `${tap.target.rect.height}px`);
    set("border-radius", tap.target.radius);
    set("background", tint(tap.target.element));
    set("pointer-events", "none");
    set("z-index", "2147483647");
    set("opacity", "1");
    if (!reducedMotion()) set("transition", `opacity ${fadeMs}ms ease-out`);
    (document.documentElement || document.body)?.appendChild(host);
    tap.host = host;
    tap.shownAt = now();
  };

  const remove = (tap) => {
    clearTimeout(tap.timer);
    tap.host?.remove();
    tap.host = null;
  };

  const release = (tap) => {
    if (pending === tap) pending = null;
    if (!tap.host) show(tap);
    const host = tap.host;
    const fade = () => {
      if (reducedMotion()) { remove(tap); return; }
      host.style.setProperty("opacity", "0", "important");
      tap.timer = setTimeout(() => remove(tap), fadeMs + 20);
    };
    tap.timer = setTimeout(fade, Math.max(0, minVisibleMs - (now() - tap.shownAt)));
  };

  const cancel = () => {
    if (!pending) return;
    remove(pending);
    pending = null;
  };

  const onDown = (event) => {
    try {
      cancel();
      if (event.pointerType !== "touch" || event.isPrimary === false) return;
      const found = target(event);
      if (!found) return;
      const tap = { target: found, x: event.clientX, y: event.clientY, host: null, shownAt: 0, timer: 0 };
      tap.timer = setTimeout(() => { if (pending === tap) show(tap); }, showDelayMs);
      pending = tap;
    } catch (_error) {
      pending = null;
    }
  };

  const onMove = (event) => {
    if (!pending || event.isPrimary === false) return;
    if (Math.hypot(event.clientX - pending.x, event.clientY - pending.y) > slopCssPx) cancel();
  };

  const onUp = (event) => {
    if (!pending || event.isPrimary === false) return;
    try { release(pending); } catch (_error) { cancel(); }
  };

  const options = { capture: true, passive: true };
  addEventListener("pointerdown", onDown, options);
  addEventListener("pointermove", onMove, options);
  addEventListener("pointerup", onUp, options);
  addEventListener("pointercancel", cancel, options);
  addEventListener("scroll", cancel, options);
  addEventListener("pagehide", cancel, options);
})();
