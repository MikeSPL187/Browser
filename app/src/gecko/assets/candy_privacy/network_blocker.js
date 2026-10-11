"use strict";

// Vola's own Gecko blocker, request decisions (step 2 of replacing the bundled uBlock Origin).
// Combines the uAssets URL rules (rules.js) and the host lists (host_blocker.js) the way
// System WebView's ContentBlocker does: a URL rule decides first, the host lists after it.
(function exposeCandyNetworkBlocker(global) {
  const LIST_FILES = Object.freeze({
    hostLists: ["blocked_hosts.txt", "uassets_blocked_hosts.txt"],
    indexedLists: ["easylist_blocked_hosts.txt", "hagezi_blocked_hosts.txt"],
    blockedPairs: ["uassets_blocked_host_pairs.txt"],
    allowedPairs: ["easylist_allowed_host_pairs.txt", "uassets_allowed_host_pairs.txt"],
    familyPairs: ["first_party_family_allowed_host_pairs.txt"],
    advanced: "uassets_advanced_filters.txt",
    networkFilters: "easylist_network_filters.txt",
  });

  // URL rules bucketed by target host, so a request checks a handful of rules instead of all.
  function advancedIndex(rules) {
    const byTarget = new Map();
    const anyTarget = [];
    for (const rule of rules) {
      if (!rule.target || rule.target.endsWith(".*")) {
        anyTarget.push(rule);
        continue;
      }
      if (!byTarget.has(rule.target)) byTarget.set(rule.target, []);
      byTarget.get(rule.target).push(rule);
    }
    return function candidates(host) {
      const found = anyTarget.slice();
      let candidate = host;
      while (true) {
        const bucket = byTarget.get(candidate);
        if (bucket) found.push(...bucket);
        const dot = candidate.indexOf(".");
        if (dot < 0) return found;
        candidate = candidate.slice(dot + 1);
      }
    };
  }

  function suffixMatches(host, ruleHost) {
    return typeof ruleHost === "string" && ruleHost.length > 0 &&
      (host === ruleHost || host.endsWith(`.${ruleHost}`));
  }

  // CandyRulePrecedence in Kotlin: pair allow, pair, longer page host, longer request host, allow, id.
  function precedes(candidate, current) {
    const key = (rule) => [
      rule.k === "P" && rule.a === "A" ? 1 : 0,
      rule.k === "P" ? 1 : 0,
      rule.k === "P" ? rule.f.length : 0,
      rule.r.length,
      rule.a === "A" ? 1 : 0,
    ];
    const first = key(candidate);
    const second = key(current);
    for (let index = 0; index < first.length; index++) {
      if (first[index] !== second[index]) return first[index] > second[index];
    }
    return String(candidate.id) < String(current.id);
  }

  // The person's own Vola rules (policy.rules): they win over every bundled list, Strict included.
  function userDecision(userRules, requestHost, pageHost) {
    let winner = null;
    for (const rule of Array.isArray(userRules) ? userRules : []) {
      if (!rule || (rule.a !== "A" && rule.a !== "B") || !suffixMatches(requestHost, rule.r)) continue;
      if (rule.k === "P" && (!pageHost || !suffixMatches(pageHost, rule.f))) continue;
      if (rule.k !== "P" && rule.k !== "H") continue;
      if (winner === null || precedes(rule, winner)) winner = rule;
    }
    return winner === null ? null : { action: winner.a, ruleId: winner.id, builtIn: false };
  }

  function create({
    lists = {},
    advancedText = "",
    networkFiltersText = "",
    rules = global.CandyPrivacyRules,
    hostBlocker = global.CandyHostBlocker,
    networkFilters = global.CandyNetworkFilters,
  } = {}) {
    if (!rules || !hostBlocker || !networkFilters) {
      throw new Error("Vola blocker needs rules.js, host_blocker.js and network_filters.js loaded first");
    }
    const hosts = hostBlocker.create({ ...lists, hostPatternMatches: rules.hostPatternMatches });
    const advancedRules = advancedText ? rules.parseAdvanced(advancedText) : [];
    const candidates = advancedIndex(advancedRules);
    const strictRules = advancedRules.filter((rule) => rule.action !== "A");
    const strictCandidates = advancedIndex(strictRules);
    const urlFilters = networkFiltersText ? networkFilters.parse(networkFiltersText) : [];
    const filterDecision = networkFilters.index(urlFilters, rules);

    // { action: "A" | "B", ruleId?, builtIn } or null for no opinion. A user rule decides first;
    // Strict drops the bundled lists' compatibility exceptions, never the person's own rules.
    // `type` is the webRequest resource type; EasyList rules may name one.
    function decide(requestUrl, pageHost, { strict = false, userRules = [], type = "other" } = {}) {
      let requestHost;
      try {
        const parsed = new URL(requestUrl);
        if (parsed.protocol !== "http:" && parsed.protocol !== "https:") return null;
        requestHost = parsed.hostname.toLowerCase().replace(/\.$/, "");
      } catch (_) {
        return null;
      }
      if (!requestHost) return null;
      const page = typeof pageHost === "string" && pageHost ? pageHost.toLowerCase() : null;
      const user = userDecision(userRules, requestHost, page);
      if (user !== null) return user;
      const advanced = rules.advancedDecision(
        (strict ? strictCandidates : candidates)(requestHost),
        requestUrl,
        page,
      );
      if (advanced === "A") return null;
      // An exception in any list beats a block in any other, as in uBlock Origin.
      const filtered = filterDecision(requestUrl, type, requestHost, page, { strict });
      if (filtered === "A") return null;
      if (advanced === "B" || filtered === "B" || hosts.shouldBlockHosts(requestHost, page, { strict })) {
        return { action: "B", builtIn: true };
      }
      return null;
    }

    return {
      decide,
      indexedSizes: hosts.indexedSizes,
      advancedRuleCount: advancedRules.length,
      networkFilterCount: urlFilters.length,
    };
  }

  // Reads every list through load(fileName) -> Promise<string>; a missing list fails the load.
  async function load(loadText, options = {}) {
    const read = (names) => Promise.all(names.map(loadText));
    const [hostLists, indexedLists, blockedPairs, allowedPairs, familyPairs, advancedText, networkFiltersText] =
      await Promise.all([
        read(LIST_FILES.hostLists),
        read(LIST_FILES.indexedLists),
        read(LIST_FILES.blockedPairs),
        read(LIST_FILES.allowedPairs),
        read(LIST_FILES.familyPairs),
        loadText(LIST_FILES.advanced),
        loadText(LIST_FILES.networkFilters),
      ]);
    return create({
      ...options,
      lists: { hostLists, indexedLists, blockedPairs, allowedPairs, familyPairs },
      advancedText,
      networkFiltersText,
    });
  }

  global.CandyNetworkBlocker = { create, load, userDecision, LIST_FILES };
})(globalThis);
