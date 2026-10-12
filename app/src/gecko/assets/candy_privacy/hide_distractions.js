"use strict";

// Hide distracting items (v5 V20, after Safari). In choosing mode a tap outlines the block under
// the finger and a second tap on it hides it; the page's own taps, links and forms get nothing
// meanwhile, while scrolling still works. The hidden block's CSS selector goes to Vola, which
// keeps it for the site and sends the site's list back with every page policy; the page then
// hides those blocks with one style sheet. Top frame only; nothing else leaves the page.
(() => {
  const maxSelectors = 32;
  const maxSelectorLength = 256;
  const maxDepth = 12;
  const maxTargetClimb = 16;
  // A target smaller than this is a word or an icon: the tap climbs to the block around it.
  const minTargetWidthShare = 0.3;
  const minTargetWidthPx = 120;
  const minTargetHeightPx = 32;
  // A block that fills the screen is the page itself, not a distraction.
  const maxViewportShare = 0.9;
  const fadeMs = 220;
  // A click this soon after a touch belongs to it.
  const touchClickMs = 1000;
  const defaultHighlight = "#0a84ff";
  const tag = "vola-hide-distractions";
  // State and decoration classes change while the page runs, so a selector never relies on them.
  const changingClasses = new Set([
    "active", "open", "opened", "closed", "show", "shown", "hide", "hidden", "visible", "invisible",
    "hover", "focus", "focused", "selected", "current", "expanded", "collapsed", "sticky", "fixed",
    "stuck", "loaded", "loading", "animated", "animating", "in", "fade", "enter", "leave",
  ]);
  const generatedPrefixes = ["sc-", "css-", "jsx-", "emotion-", "svelte-", "ng-", "_ngcontent", "_nghost"];

  // A class or id written by a person: no long digit runs, no hash-like mixed segments, no
  // CSS-in-JS prefixes. Only these characters ever reach a selector, so nothing needs escaping.
  const isStableName = (name) => {
    if (typeof name !== "string" || name.length === 0 || name.length > 48) return false;
    if (!/^-?[A-Za-z_][A-Za-z0-9_-]*$/.test(name) || /\d{3,}/.test(name)) return false;
    const lower = name.toLowerCase();
    if (generatedPrefixes.some((prefix) => lower.startsWith(prefix))) return false;
    return !name.split(/[-_]+/).some((segment) =>
      segment.length >= 4 && /\d/.test(segment) && /[A-Za-z]/.test(segment) &&
        (segment.match(/\d/g) || []).length >= 2,
    );
  };
  const isStableClass = (name) => isStableName(name) && !changingClasses.has(name.toLowerCase()) &&
    !/^(is|has)[-_A-Z]/.test(name) && !name.startsWith(tag);

  const isOnly = (doc, selector, element) => {
    try {
      const found = doc.querySelectorAll(selector);
      return found.length === 1 && found[0] === element;
    } catch (_error) {
      return false;
    }
  };

  const nthOfType = (element) => {
    let index = 1;
    for (let sibling = element.previousElementSibling; sibling; sibling = sibling.previousElementSibling) {
      if (sibling.tagName === element.tagName) index++;
    }
    return index;
  };

  const stepOf = (element) => element.tagName.toLowerCase() +
    Array.from(element.classList || []).filter(isStableClass).slice(0, 2).map((name) => `.${name}`).join("");

  // A selector that finds only [element]: its id, or a child chain of tags and stable classes,
  // pinned by position where siblings look alike. A chain that needed a position also climbs to
  // an ancestor with an id or to the body, so another page layout cannot match it by chance.
  const selectorFor = (element, doc) => {
    if (!element || !element.tagName || element === doc.documentElement || element === doc.body) return null;
    const done = (selector) => (selector.length <= maxSelectorLength && isOnly(doc, selector, element) ? selector : null);
    if (isStableName(element.id) && isOnly(doc, `#${element.id}`, element)) return done(`#${element.id}`);
    const parts = [];
    let positional = false;
    let node = element;
    for (let depth = 0; node && node !== doc.documentElement && depth < maxDepth; depth++, node = node.parentElement) {
      if (node !== element && isStableName(node.id) && isOnly(doc, `#${node.id}`, node)) {
        return done([`#${node.id}`, ...parts].join(" > "));
      }
      if (node === doc.body) return done(["body", ...parts].join(" > "));
      const step = stepOf(node);
      parts.unshift(step);
      if (positional) continue;
      if (isOnly(doc, parts.join(" > "), element)) return done(parts.join(" > "));
      parts[0] = `${step}:nth-of-type(${nthOfType(node)})`;
      positional = isOnly(doc, parts.join(" > "), element);
      // The position did not single it out here either; a level higher may, without it.
      if (!positional) parts[0] = step;
    }
    return positional ? done(parts.join(" > ")) : null;
  };

  const isPinned = (element, view) => {
    try {
      const position = view.getComputedStyle(element).position;
      return position === "fixed" || position === "sticky";
    } catch (_error) {
      return false;
    }
  };

  // The block a tap on [element] means: a pinned bar or bubble at once, otherwise the first box
  // wide and tall enough to be more than a word. Nothing when that is the whole page.
  const targetFor = (element, doc, view) => {
    const viewportArea = view.innerWidth * view.innerHeight;
    const minWidth = Math.min(view.innerWidth * minTargetWidthShare, minTargetWidthPx);
    let node = element && element.nodeType === 1 ? element : element?.parentElement;
    for (let depth = 0; node && depth < maxTargetClimb; depth++, node = node.parentElement) {
      if (node === doc.body || node === doc.documentElement) return null;
      const rect = node.getBoundingClientRect();
      const large = rect.width >= minWidth && rect.height >= minTargetHeightPx;
      if (!large && !isPinned(node, view)) continue;
      return rect.width * rect.height > viewportArea * maxViewportShare ? null : node;
    }
    return null;
  };

  const validSelectors = (list) => (Array.isArray(list) ? list : []).filter((selector) =>
    typeof selector === "string" && selector.length > 0 && selector.length <= maxSelectorLength &&
      /^[A-Za-z0-9_\-#.>:() ]+$/.test(selector),
  ).slice(0, maxSelectors);

  const hiddenStyleText = (selectors) => selectors.map((selector) => `${selector}{display:none!important}`).join("\n");

  globalThis.VolaHideDistractions = { hiddenStyleText, isStableName, selectorFor, targetFor, validSelectors };

  if (typeof browser === "undefined" || globalThis.top !== globalThis.self) return;

  let hidden = [];
  let policy = { revision: 0, navigationGeneration: 0, animationsEnabled: true };
  let style = null;
  let picking = null;

  const applyHidden = () => {
    const text = hiddenStyleText(hidden);
    if (!style && !text) return;
    if (!style) {
      style = document.createElement("style");
      style.dataset.volaPrivacy = "hidden-distractions";
    }
    style.textContent = text;
    if (!style.isConnected) (document.documentElement || document).appendChild(style);
  };

  const acceptPolicy = (message) => {
    if (!message || message.type !== "content-policy" || message.ready !== true) return;
    policy = {
      revision: message.revision,
      navigationGeneration: message.navigationGeneration,
      animationsEnabled: message.animationsEnabled !== false,
    };
    const next = validSelectors(message.hiddenElements);
    // Blocks hidden while choosing stay hidden until the policy that carries them arrives.
    const kept = picking ? hidden.filter((selector) => !next.includes(selector)) : [];
    hidden = [...next, ...kept].slice(0, maxSelectors);
    applyHidden();
  };

  const reducedMotion = () => {
    try { return globalThis.matchMedia?.("(prefers-reduced-motion: reduce)")?.matches === true; }
    catch (_error) { return false; }
  };

  const outline = (state, target) => {
    state.target = target;
    if (!target) {
      state.box?.remove();
      state.box = null;
      return;
    }
    if (!state.box) {
      state.box = document.createElement("div");
      state.box.setAttribute(tag, "");
      state.box.setAttribute("aria-hidden", "true");
      const boxStyle = state.box.style;
      boxStyle.setProperty("position", "fixed", "important");
      boxStyle.setProperty("pointer-events", "none", "important");
      boxStyle.setProperty("z-index", "2147483647", "important");
      boxStyle.setProperty("box-sizing", "border-box", "important");
      boxStyle.setProperty("border-radius", "8px", "important");
      boxStyle.setProperty("border", `2px solid ${state.color}`, "important");
      boxStyle.setProperty("background", `${state.color}26`, "important");
      boxStyle.setProperty("margin", "0", "important");
      document.documentElement.appendChild(state.box);
    }
    place(state);
  };

  const place = (state) => {
    if (!state.box || !state.target) return;
    const rect = state.target.getBoundingClientRect();
    state.box.style.setProperty("left", `${rect.left}px`, "important");
    state.box.style.setProperty("top", `${rect.top}px`, "important");
    state.box.style.setProperty("width", `${rect.width}px`, "important");
    state.box.style.setProperty("height", `${rect.height}px`, "important");
  };

  const hide = (state, target) => {
    const selector = selectorFor(target, document);
    outline(state, null);
    if (!selector) return;
    const commit = () => {
      if (!hidden.includes(selector)) hidden = [...hidden, selector].slice(-maxSelectors);
      applyHidden();
      target.style.removeProperty("transition");
      target.style.removeProperty("opacity");
      target.style.removeProperty("transform");
      browser.runtime.sendMessage({
        type: "distraction-hidden",
        selector,
        navigationGeneration: policy.navigationGeneration,
      }).catch(() => {});
    };
    if (!policy.animationsEnabled || reducedMotion()) {
      commit();
      return;
    }
    target.style.setProperty("transition", `opacity ${fadeMs}ms ease-out, transform ${fadeMs}ms ease-out`, "important");
    target.style.setProperty("opacity", "0", "important");
    target.style.setProperty("transform", "scale(0.96)", "important");
    setTimeout(commit, fadeMs);
  };

  const swallow = (event) => {
    event.stopImmediatePropagation();
    if (event.cancelable && !event.type.startsWith("touch") && !event.type.startsWith("pointer")) {
      event.preventDefault();
    }
  };

  // The engine moves a tap's click onto a nearby link or button (Gecko's `ui.mouse.radius`); the
  // block under the finger is where the touch began, so the click is read from there.
  const onTouchStart = (event) => {
    swallow(event);
    const touch = event.touches?.[0];
    if (picking && touch) picking.touch = { x: touch.clientX, y: touch.clientY, at: Date.now() };
  };

  const tappedBy = (state, event) => {
    const touch = state.touch;
    state.touch = null;
    if (!touch || Date.now() - touch.at > touchClickMs) return event.target;
    return document.elementFromPoint(touch.x, touch.y) || event.target;
  };

  const onClick = (event) => {
    swallow(event);
    const state = picking;
    if (!state) return;
    const tapped = tappedBy(state, event);
    if (state.target && tapped && (state.target === tapped || state.target.contains(tapped))) {
      hide(state, state.target);
      return;
    }
    outline(state, targetFor(tapped, document, globalThis));
  };

  const onMove = () => { if (picking) place(picking); };

  const swallowed = ["mousedown", "mouseup", "pointerdown", "pointerup", "touchend",
    "dblclick", "auxclick", "contextmenu", "submit"];

  const startPicking = (color) => {
    if (picking) {
      picking.color = color;
      return;
    }
    picking = { color, target: null, box: null, touch: null };
    globalThis.addEventListener("click", onClick, { capture: true });
    globalThis.addEventListener("touchstart", onTouchStart, { capture: true, passive: true });
    swallowed.forEach((type) => globalThis.addEventListener(type, swallow, { capture: true, passive: type.startsWith("touch") }));
    globalThis.addEventListener("scroll", onMove, { capture: true, passive: true });
    globalThis.addEventListener("resize", onMove, { passive: true });
  };

  const stopPicking = () => {
    if (!picking) return;
    outline(picking, null);
    picking = null;
    globalThis.removeEventListener("click", onClick, { capture: true });
    globalThis.removeEventListener("touchstart", onTouchStart, { capture: true });
    swallowed.forEach((type) => globalThis.removeEventListener(type, swallow, { capture: true }));
    globalThis.removeEventListener("scroll", onMove, { capture: true });
    globalThis.removeEventListener("resize", onMove);
  };

  browser.runtime.onMessage.addListener((message) => {
    if (message?.type === "content-policy") {
      acceptPolicy(message);
    } else if (message?.type === "distractions-pick") {
      if (message.active === true) {
        const color = /^#[0-9a-f]{6}$/i.test(message.highlightColor) ? message.highlightColor : defaultHighlight;
        startPicking(color);
      } else {
        stopPicking();
      }
    }
  });

  browser.runtime.sendMessage({ type: "content-policy-request" }).then(acceptPolicy).catch(() => {});
})();
