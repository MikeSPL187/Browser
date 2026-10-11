"use strict";

// Vola's own Gecko blocker, host lists (step 1 of replacing the bundled uBlock Origin).
// The same lists and decision order as System WebView's RequestBlocker.kt, so both engines block
// the same hosts. Large lists stay one string with line offsets: no string per host in memory.
(function exposeCandyHostBlocker(global) {
  const HOST_RULE = /^[a-z0-9.-]+$/;
  const WEB_SCHEMES = new Set(["http:", "https:"]);

  function trimDots(value) {
    return value.replace(/^\.+|\.+$/g, "");
  }

  function isHostRule(value) {
    return value.length > 0 && HOST_RULE.test(value);
  }

  function ruleLines(text) {
    return String(text || "").split(/\r?\n/).map((line) => line.trim())
      .filter((line) => line && !line.startsWith("#"));
  }

  function hostSet(texts) {
    const hosts = new Set();
    for (const text of texts) {
      for (const line of ruleLines(text)) {
        let rule = line.toLowerCase();
        if (rule.startsWith("||")) rule = rule.slice(2);
        if (rule.endsWith("^")) rule = rule.slice(0, -1);
        rule = trimDots(rule);
        if (isHostRule(rule)) hosts.add(rule);
      }
    }
    return hosts;
  }

  // A strictly sorted host list, searched in place (SortedHostIndex in Kotlin).
  function sortedHostIndex(text) {
    const source = String(text || "");
    const starts = [];
    const ends = [];
    let start = 0;
    while (start < source.length) {
      let end = source.indexOf("\n", start);
      if (end < 0) end = source.length;
      const contentEnd = end > start && source.charCodeAt(end - 1) === 13 ? end - 1 : end;
      if (contentEnd > start && source.charCodeAt(start) !== 35) {
        if (starts.length > 0 &&
          compareRange(source, starts[starts.length - 1], ends[ends.length - 1], start, contentEnd) >= 0) {
          throw new Error("Host index must be strictly sorted");
        }
        starts.push(start);
        ends.push(contentEnd);
      }
      start = end + 1;
    }
    const startOffsets = Uint32Array.from(starts);
    const endOffsets = Uint32Array.from(ends);

    function compareHost(host, index) {
      const lineStart = startOffsets[index];
      const length = endOffsets[index] - lineStart;
      const common = Math.min(host.length, length);
      for (let offset = 0; offset < common; offset++) {
        const difference = host.charCodeAt(offset) - source.charCodeAt(lineStart + offset);
        if (difference !== 0) return difference;
      }
      return host.length - length;
    }

    return {
      size: startOffsets.length,
      has(host) {
        let low = 0;
        let high = startOffsets.length - 1;
        while (low <= high) {
          const middle = (low + high) >>> 1;
          const comparison = compareHost(host, middle);
          if (comparison < 0) high = middle - 1;
          else if (comparison > 0) low = middle + 1;
          else return true;
        }
        return false;
      },
    };
  }

  function compareRange(source, firstStart, firstEnd, secondStart, secondEnd) {
    const firstLength = firstEnd - firstStart;
    const secondLength = secondEnd - secondStart;
    const common = Math.min(firstLength, secondLength);
    for (let offset = 0; offset < common; offset++) {
      const difference = source.charCodeAt(firstStart + offset) - source.charCodeAt(secondStart + offset);
      if (difference !== 0) return difference;
    }
    return firstLength - secondLength;
  }

  function hostPairs(texts, acceptsPage) {
    const byRequestHost = new Map();
    for (const text of texts) {
      for (const line of ruleLines(text)) {
        const tab = line.indexOf("\t");
        if (tab < 0) continue;
        const requestHost = trimDots(line.slice(0, tab).toLowerCase());
        const page = acceptsPage(line.slice(tab + 1).toLowerCase());
        if (!isHostRule(requestHost) || page === null) continue;
        if (!byRequestHost.has(requestHost)) byRequestHost.set(requestHost, []);
        byRequestHost.get(requestHost).push(page);
      }
    }
    return byRequestHost;
  }

  function pageHostOrAny(value) {
    const host = trimDots(value);
    return host === "*" || isHostRule(host) ? host : null;
  }

  // CosmeticHostPattern.canonicalize in Kotlin: a host, or "label.label.*" for any public suffix.
  function pageHostPattern(value) {
    const candidate = value.trim().replace(/\.+$/, "");
    if (!candidate.endsWith(".*")) return isHostRule(trimDots(candidate)) ? trimDots(candidate) : null;
    const labels = candidate.slice(0, -2).split(".");
    return labels.length > 0 && labels.every((label) => /^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$/.test(label)) ?
      candidate : null;
  }

  function matchesHostOrSubdomain(host, ruleHost) {
    return host === ruleHost || host.endsWith(`.${ruleHost}`);
  }

  function isSameHostOrSubdomain(first, second) {
    return first === second || first.endsWith(`.${second}`) || second.endsWith(`.${first}`);
  }

  // Calls test(candidate) for the host and each parent domain, stopping at the first true.
  function anyParent(host, test) {
    let candidate = host;
    while (true) {
      if (test(candidate)) return true;
      const dot = candidate.indexOf(".");
      if (dot < 0) return false;
      candidate = candidate.slice(dot + 1);
    }
  }

  function normalizeHost(host) {
    if (typeof host !== "string") return null;
    const normalized = trimDots(host.toLowerCase());
    return isHostRule(normalized) ? normalized : null;
  }

  function hostOfWebUrl(url) {
    try {
      const parsed = new URL(url);
      return WEB_SCHEMES.has(parsed.protocol) ? parsed.hostname : null;
    } catch (_) {
      return null;
    }
  }

  function hostOfUrl(url) {
    try {
      return new URL(url).hostname || null;
    } catch (_) {
      return null;
    }
  }

  function create({
    hostLists = [],
    indexedLists = [],
    blockedPairs = [],
    allowedPairs = [],
    familyPairs = [],
    hostPatternMatches = global.CandyPrivacyRules?.hostPatternMatches,
  } = {}) {
    const blockedHosts = hostSet(hostLists);
    const indexes = indexedLists.map(sortedHostIndex);
    const blockedPageHosts = hostPairs(blockedPairs, pageHostOrAny);
    const allowedPageHosts = hostPairs(allowedPairs, pageHostOrAny);
    const familyPatterns = typeof hostPatternMatches === "function" ?
      hostPairs(familyPairs, pageHostPattern) : new Map();

    function isListed(candidate) {
      return blockedHosts.has(candidate) || indexes.some((index) => index.has(candidate));
    }

    function shouldBlockHosts(requestHost, pageHost) {
      const request = normalizeHost(requestHost);
      if (request === null) return false;
      const page = normalizeHost(pageHost);
      // The current site keeps working even when a list names its own host.
      if (page !== null && isSameHostOrSubdomain(request, page)) return false;
      if (anyParent(request, (candidate) => (allowedPageHosts.get(candidate) || []).some((allowed) =>
        allowed === "*" || (page !== null && matchesHostOrSubdomain(page, allowed))))) return false;
      if (page !== null && anyParent(request, (candidate) =>
        (familyPatterns.get(candidate) || []).some((pattern) => hostPatternMatches(page, pattern)))) return false;
      if (page !== null && anyParent(request, (candidate) =>
        (blockedPageHosts.get(candidate) || []).some((blocked) => matchesHostOrSubdomain(page, blocked)))) return true;
      return anyParent(request, isListed);
    }

    return {
      indexedSizes: indexes.map((index) => index.size),
      shouldBlockHosts,
      shouldBlock(requestUrl, pageUrl) {
        const requestHost = hostOfWebUrl(requestUrl);
        return requestHost !== null && shouldBlockHosts(requestHost, hostOfUrl(pageUrl));
      },
    };
  }

  global.CandyHostBlocker = { create, sortedHostIndex };
})(globalThis);
