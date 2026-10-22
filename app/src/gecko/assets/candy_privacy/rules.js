"use strict";

(function exposeCandyPrivacyRules(global) {
  const COMMON_SECOND_LEVEL = new Set([
    "ac", "co", "com", "edu", "firm", "gen", "go", "gov", "ind", "mil", "ne",
    "net", "nom", "or", "org",
  ]);
  const MULTI_LABEL_SUFFIXES = new Set([
    "ac.uk", "co.uk", "gov.uk", "ltd.uk", "me.uk", "net.uk", "org.uk", "plc.uk",
    "asn.au", "com.au", "conf.au", "csiro.au", "edu.au", "gov.au", "id.au", "net.au",
    "org.au", "oz.au", "ac.nz", "co.nz", "geek.nz", "gen.nz", "govt.nz", "health.nz",
    "iwi.nz", "maori.nz", "mil.nz", "net.nz", "org.nz", "parliament.nz", "school.nz",
    "com.ar", "edu.ar", "gob.ar", "gov.ar", "int.ar", "mil.ar", "net.ar", "org.ar",
    "com.br", "net.br", "org.br", "com.cn", "net.cn", "org.cn", "co.in", "firm.in",
    "gen.in", "ind.in", "net.in", "org.in", "co.jp", "ne.jp", "or.jp", "com.mx",
    "com.tr", "co.za", "appspot.com", "blogspot.com", "github.io", "netlify.app",
    "pages.dev", "vercel.app",
  ]);
  const HIGH_CONFIDENCE_SELECTORS = [
    ":is(img, iframe, embed)[src*=\"/ads_banner.\" i]",
    ":is(img, iframe, embed)[src*=\"_ads_banner.\" i]",
    "object[data*=\"/ads_banner.\" i]",
    "object[data*=\"_ads_banner.\" i]",
    "#ad_banner + #AlternateMessage",
  ];
  const SENSITIVE_HOSTS = ["accounts.google.*", "mail.google.*", "maps.google.*"];
  const SIMPLE_SELECTOR = /^([.#])([A-Za-z_][A-Za-z0-9_-]*)$/;
  const GENERIC_POLICY_CACHE_SIZE = 128;
  const MAX_GENERIC_NAMES = 2048;
  const MAX_GENERIC_COMPLEX = 1024;
  // Shared by every cosmetic rule without host exclusions: one frozen array, not 40 000 empty ones.
  const NO_HOSTS = Object.freeze([]);

  function lines(text) {
    return text.split(/\r?\n/).map((line) => line.trim())
      .filter((line) => line && !line.startsWith("#"));
  }

  function decode(value) {
    const binary = atob(value.replace(/-/g, "+").replace(/_/g, "/"));
    return new TextDecoder("utf-8", { fatal: true }).decode(
      Uint8Array.from(binary, (character) => character.charCodeAt(0)),
    );
  }

  function declaredCount(text, prefix) {
    const line = text.split(/\r?\n/).map((value) => value.trim())
      .find((value) => value.startsWith(prefix));
    const count = line ? Number(line.slice(prefix.length).trim()) : NaN;
    if (!Number.isSafeInteger(count) || count < 0) throw new Error(`Missing ${prefix}`);
    return count;
  }

  function hostMatches(host, expected) {
    return host === expected || host.endsWith(`.${expected}`);
  }

  function isRegistrySuffix(value) {
    const labels = value.split(".");
    if (MULTI_LABEL_SUFFIXES.has(value)) return true;
    if (labels.length === 1) return /^[a-z]{2,63}$/.test(value) || /^xn--[a-z0-9-]+$/.test(value);
    return labels.length === 2 && labels[1].length === 2 && COMMON_SECOND_LEVEL.has(labels[0]);
  }

  function hostPatternMatches(host, pattern) {
    if (pattern === "*") return true;
    if (!pattern.endsWith(".*")) return hostMatches(host, pattern);
    const prefix = pattern.slice(0, -1);
    const suffix = host.startsWith(prefix) ? host.slice(prefix.length) :
      host.includes(`.${prefix}`) ? host.slice(host.indexOf(`.${prefix}`) + prefix.length + 1) : "";
    return Boolean(suffix) && isRegistrySuffix(suffix);
  }

  // A list of domains, one per line (financial_sites.txt): whether a host is one of them or under one.
  function parseSiteList(text) {
    const sites = new Set(lines(text).map((line) => line.toLowerCase()));
    return function listed(host) {
      if (typeof host !== "string" || !host) return false;
      for (let candidate = host; ; candidate = candidate.slice(candidate.indexOf(".") + 1)) {
        if (sites.has(candidate)) return true;
        if (!candidate.includes(".")) return false;
      }
    };
  }

  function isUsK12Suffix(labels) {
    return labels.length === 3 && labels[0] === "k12" && labels[1].length === 2 && labels[2] === "us";
  }

  function registrableDomain(host) {
    const labels = host.split(".");
    if (labels.length < 2) return null;
    const lastTwo = labels.slice(-2).join(".");
    const suffixLength = labels.length >= 4 && isUsK12Suffix(labels.slice(-3)) ? 3 :
      MULTI_LABEL_SUFFIXES.has(lastTwo) ||
        (labels[labels.length - 1].length === 2 && COMMON_SECOND_LEVEL.has(labels[labels.length - 2])) ? 2 : 1;
    return labels.length > suffixLength ? labels.slice(-(suffixLength + 1)).join(".") : null;
  }

  function urlPatternMatches(value, pattern) {
    const anchoredStart = pattern.startsWith("|");
    const anchoredEnd = pattern.endsWith("|") && pattern.length > 1;
    const body = pattern.replace(/^\|/, "").replace(/\|$/, "");
    if (!body) return false;
    const matchAt = (start) => {
      let valueIndex = start;
      let patternIndex = 0;
      let starPatternIndex = -1;
      let starValueIndex = -1;
      while (valueIndex < value.length) {
        if (patternIndex === body.length) return !anchoredEnd;
        if (body[patternIndex] === "*") {
          starPatternIndex = patternIndex++;
          starValueIndex = valueIndex;
        } else {
          const token = body[patternIndex];
          const character = value[valueIndex];
          const separator = token === "^" &&
            !/[\p{L}\p{N}]/u.test(character) && !["_", "-", ".", "%"].includes(character);
          if (separator || (token !== "^" && token.toLowerCase() === character.toLowerCase())) {
            patternIndex++;
            valueIndex++;
          } else if (starPatternIndex >= 0) {
            patternIndex = starPatternIndex + 1;
            valueIndex = ++starValueIndex;
          } else {
            return false;
          }
        }
      }
      while (body[patternIndex] === "*") patternIndex++;
      const matched = patternIndex === body.length ||
        (patternIndex === body.length - 1 && body[patternIndex] === "^");
      return matched && (!anchoredEnd || valueIndex === value.length);
    };
    if (anchoredStart) return matchAt(0);
    for (let start = 0; start <= value.length; start++) if (matchAt(start)) return true;
    return false;
  }

  function parseAdvanced(text) {
    if (!text.startsWith("candy-advanced-filter:2")) throw new Error("Invalid advanced header");
    const rows = lines(text).slice(1);
    if (rows.length !== declaredCount(text, "# Rules:")) throw new Error("Advanced count mismatch");
    return rows.map((row) => {
      const fields = row.split("\t");
      if (fields.length !== 8) throw new Error("Invalid advanced rule");
      return {
        action: fields[0], scope: fields[1], target: fields[2] === "*" ? null : fields[2],
        pattern: decode(fields[3]), pages: fields[4] === "-" ? [] : fields[4].split(","),
        excluded: fields[5] === "-" ? [] : fields[5].split(","), party: fields[6],
      };
    }).filter((rule) => rule.scope === "N");
  }

  // uAssets `$popup` rules marked "W": on these pages window.open opens nothing (System WebView's
  // CandyWindowOpenDefuserScript).
  function parseWindowOpenDefusers(text) {
    if (!text.startsWith("candy-advanced-filter:2")) throw new Error("Invalid advanced header");
    return lines(text).slice(1).map((row) => row.split("\t"))
      .filter((fields) => fields.length === 8 && fields[0] === "B" && fields[1] === "P" && fields[7] === "W")
      .map((fields) => ({
        pages: fields[4] === "-" ? [] : fields[4].split(","),
        excluded: fields[5] === "-" ? [] : fields[5].split(","),
      }))
      .filter((rule) => rule.pages.length);
  }

  function defusesWindowOpen(defusers, pageHost) {
    return Boolean(pageHost) && defusers.some((rule) =>
      rule.pages.some((pattern) => hostPatternMatches(pageHost, pattern)) &&
      !rule.excluded.some((pattern) => hostPatternMatches(pageHost, pattern)));
  }

  function advancedDecision(rules, rawUrl, pageHost) {
    let target;
    try { target = new URL(rawUrl); } catch (_) { return null; }
    const targetHost = target.hostname.toLowerCase().replace(/\.$/, "");
    const targetParty = registrableDomain(targetHost);
    const pageParty = pageHost ? registrableDomain(pageHost) : null;
    const thirdParty = pageHost ? targetParty !== pageParty : null;
    let blocked = false;
    for (const rule of rules) {
      if (rule.target && !hostPatternMatches(targetHost, rule.target)) continue;
      if (rule.pages.length && (!pageHost || !rule.pages.some((value) => hostPatternMatches(pageHost, value)))) continue;
      if (pageHost && rule.excluded.some((value) => hostPatternMatches(pageHost, value))) continue;
      if (rule.party !== "*" && thirdParty === null) continue;
      if (rule.party === "1" && thirdParty) continue;
      if (rule.party === "3" && !thirdParty) continue;
      if (!urlPatternMatches(target.pathname + target.search, rule.pattern)) continue;
      if (rule.action === "A") return "A";
      blocked = true;
    }
    return blocked ? "B" : null;
  }

  function parseCosmetic(text, expectedHeader) {
    if (!text.startsWith(expectedHeader)) throw new Error("Invalid cosmetic header");
    const rows = lines(text).slice(1);
    const rules = rows.map((row) => {
      const fields = row.split("\t");
      if (fields.length !== 4) throw new Error("Invalid cosmetic rule");
      return {
        action: fields[0], host: fields[1],
        excluded: fields[2] === "-" ? NO_HOSTS : fields[2].split(","),
        selector: fields[3] === "-" ? "" : decode(fields[3]),
      };
    });
    const hideCount = rules.filter((rule) => rule.action === "H").length;
    const allowCount = rules.filter((rule) => rule.action === "A").length;
    const disabledCount = rules.filter((rule) => rule.action === "D").length;
    if (hideCount !== declaredCount(text, "# Hide rules:") ||
        allowCount !== declaredCount(text, "# Exception rules:") ||
        disabledCount !== declaredCount(text, "# Generic hide exceptions:")) {
      throw new Error("Cosmetic count mismatch");
    }
    return rules;
  }

  function parseProcedural(text) {
    if (!text.startsWith("candy-procedural-cosmetic:1")) throw new Error("Invalid procedural header");
    const rows = lines(text).slice(1);
    if (rows.length !== declaredCount(text, "# Rules:")) throw new Error("Procedural count mismatch");
    return rows.map((row) => {
      const fields = row.split("\t");
      if (fields.length !== 5) throw new Error("Invalid procedural rule");
      return {
        action: fields[0], host: fields[1], selector: decode(fields[2]),
        text: fields[3] === "-" ? "" : decode(fields[3]), ignoreCase: fields[4] === "i",
      };
    });
  }

  function parseCandyDefaults(text) {
    if (!text.startsWith("candy-rules:1")) throw new Error("Invalid Candy defaults header");
    return lines(text).slice(1).map((row) => {
      const fields = row.split("\t");
      if (fields.length !== 9 || fields[0] !== "rule" || fields[1] !== "css") {
        throw new Error("Invalid Candy default rule");
      }
      return { host: fields[3], selector: decode(fields[4]), group: fields[7] };
    });
  }

  // Strings stored as one joined string and their offsets: no string object per entry. `find`
  // needs the values sorted (plain code-unit order, as Array.prototype.sort leaves them).
  function packedStrings(values) {
    const starts = new Uint32Array(values.length + 1);
    for (let index = 0; index < values.length; index++) starts[index + 1] = starts[index] + values[index].length;
    const text = values.join("");
    const compare = (value, index) => {
      const start = starts[index];
      const length = starts[index + 1] - start;
      const common = Math.min(value.length, length);
      for (let offset = 0; offset < common; offset++) {
        const difference = value.charCodeAt(offset) - text.charCodeAt(start + offset);
        if (difference !== 0) return difference;
      }
      return value.length - length;
    };
    const size = values.length;
    return {
      size,
      at: (index) => text.slice(starts[index], starts[index + 1]),
      find(value) {
        let low = 0;
        let high = size - 1;
        while (low <= high) {
          const middle = (low + high) >>> 1;
          const comparison = compare(value, middle);
          if (comparison < 0) high = middle - 1;
          else if (comparison > 0) low = middle + 1;
          else return middle;
        }
        return -1;
      },
    };
  }

  // Plain scoped hide rules (no exclusions; 23 000 of the 25 000 scoped ones) as sorted hosts,
  // each with a range of ids into one table of distinct selectors.
  function packPlainRules(selectorsByHost) {
    const hosts = Array.from(selectorsByHost.keys()).sort();
    const hostStarts = new Uint32Array(hosts.length + 1);
    const selectorIds = new Map();
    const selectors = [];
    const ids = [];
    hosts.forEach((host, index) => {
      for (const selector of selectorsByHost.get(host)) {
        let id = selectorIds.get(selector);
        if (id === undefined) {
          id = selectors.length;
          selectorIds.set(selector, id);
          selectors.push(selector);
        }
        ids.push(id);
      }
      hostStarts[index + 1] = ids.length;
    });
    return { hosts: packedStrings(hosts), hostStarts, ids: Uint32Array.from(ids), selectors: packedStrings(selectors) };
  }

  // Scoped cosmetic rules bucketed by host, so a frame looks at its own hosts' rules instead of
  // all 40 000. Generic rules (host "*") stay out of the buckets: plain `.class` and `#id` ones go
  // to sets a page looks up with the names it actually uses, the rest to a short list.
  // The index lives as long as the extension does, so plain hide rules are packed (packPlainRules)
  // and their rule objects rebuilt only for the hosts a frame asks about.
  function indexCosmetics(cosmeticRules) {
    const byHost = new Map();
    const plainByHost = new Map();
    const everyHost = [];
    const generic = { classes: new Set(), ids: new Set(), complex: [], complexSet: new Set(), conditional: new Map() };
    const unconditional = new Set();
    for (const rule of cosmeticRules) {
      if (rule.host === "*") {
        if (rule.action === "A") everyHost.push(rule);
        if (rule.action === "H") addGeneric(generic, unconditional, rule);
      } else if (rule.host.endsWith(".*")) {
        everyHost.push(rule);
      } else {
        const buckets = rule.action === "H" && !rule.excluded.length ? plainByHost : byHost;
        if (!buckets.has(rule.host)) buckets.set(rule.host, []);
        buckets.get(rule.host).push(rule.action === "H" && !rule.excluded.length ? rule.selector : rule);
      }
    }
    // A selector with a copy that excludes no host is never dropped by the copies that do.
    for (const selector of unconditional) generic.conditional.delete(selector);
    const plain = packPlainRules(plainByHost);
    const candidates = (host) => {
      const found = everyHost.slice();
      let candidate = host;
      while (true) {
        const bucket = byHost.get(candidate);
        if (bucket) found.push(...bucket);
        const hostIndex = plain.hosts.find(candidate);
        for (let index = hostIndex < 0 ? 0 : plain.hostStarts[hostIndex];
          hostIndex >= 0 && index < plain.hostStarts[hostIndex + 1]; index++) {
          found.push({ action: "H", host: candidate, excluded: NO_HOSTS, selector: plain.selectors.at(plain.ids[index]) });
        }
        const dot = candidate.indexOf(".");
        if (dot < 0) return found;
        candidate = candidate.slice(dot + 1);
      }
    };
    const policies = new Map();
    return {
      size: cosmeticRules.length,
      candidates,
      generic,
      // null when generic hiding is off on this host, else the generic selectors it must keep.
      genericPolicy(host) {
        if (policies.has(host)) return policies.get(host);
        const policy = resolveGenericPolicy(generic, candidates(host), host);
        if (policies.size >= GENERIC_POLICY_CACHE_SIZE) policies.delete(policies.keys().next().value);
        policies.set(host, policy);
        return policy;
      },
    };
  }

  function addGeneric(generic, unconditional, rule) {
    const simple = SIMPLE_SELECTOR.exec(rule.selector);
    if (simple) {
      (simple[1] === "." ? generic.classes : generic.ids).add(simple[2]);
    } else if (!generic.complexSet.has(rule.selector)) {
      generic.complexSet.add(rule.selector);
      generic.complex.push(rule.selector);
    }
    if (!rule.excluded.length) {
      unconditional.add(rule.selector);
    } else {
      if (!generic.conditional.has(rule.selector)) generic.conditional.set(rule.selector, []);
      generic.conditional.get(rule.selector).push(rule.excluded);
    }
  }

  function isGenericSelector(generic, selector) {
    const simple = SIMPLE_SELECTOR.exec(selector);
    if (!simple) return generic.complexSet.has(selector);
    return (simple[1] === "." ? generic.classes : generic.ids).has(simple[2]);
  }

  function resolveGenericPolicy(generic, rules, host) {
    if (SENSITIVE_HOSTS.some((pattern) => hostPatternMatches(host, pattern))) return null;
    const disabled = rules.some((rule) => rule.action === "D" && hostPatternMatches(host, rule.host) &&
      !rule.excluded.some((pattern) => hostPatternMatches(host, pattern)));
    if (disabled) return null;
    const denied = new Set();
    for (const rule of rules) {
      if (rule.action === "A" && isGenericSelector(generic, rule.selector) &&
          (rule.host === "*" || hostPatternMatches(host, rule.host))) {
        denied.add(rule.selector);
      }
    }
    // A selector some copies exclude on a host is kept there only when no copy applies to it.
    for (const [selector, exclusions] of generic.conditional) {
      if (exclusions.every((excluded) => excluded.some((pattern) => hostPatternMatches(host, pattern)))) {
        denied.add(selector);
      }
    }
    return { denied };
  }

  // The generic `.class` / `#id` selectors a frame's names hit, minus the ones its host keeps.
  function genericSelectors(cosmetics, host, names) {
    const policy = cosmetics.genericPolicy(host);
    if (!policy) return [];
    const found = [];
    const add = (prefix, set, values) => {
      for (const value of values.slice(0, MAX_GENERIC_NAMES)) {
        if (typeof value !== "string" || !set.has(value)) continue;
        const selector = prefix + value;
        if (!policy.denied.has(selector)) found.push(selector);
      }
    };
    add(".", cosmetics.generic.classes, Array.isArray(names.classes) ? names.classes : []);
    add("#", cosmetics.generic.ids, Array.isArray(names.ids) ? names.ids : []);
    return found;
  }

  function scopedSelectors(cosmetics, host) {
    if (SENSITIVE_HOSTS.some((pattern) => hostPatternMatches(host, pattern))) return [];
    const cosmeticRules = Array.isArray(cosmetics) ? cosmetics : cosmetics.candidates(host);
    const allowed = new Set(cosmeticRules.filter((rule) =>
      rule.action === "A" && (rule.host === "*" || hostPatternMatches(host, rule.host)),
    ).map((rule) => rule.selector));
    return cosmeticRules.filter((rule) =>
      rule.action === "H" && rule.host !== "*" && hostPatternMatches(host, rule.host) &&
      !rule.excluded.some((pattern) => hostPatternMatches(host, pattern)) && !allowed.has(rule.selector),
    ).map((rule) => rule.selector);
  }

  function cosmeticPayload(staticRules, policy, frameHost) {
    if (!frameHost || policy.pausedHosts.some((host) => hostMatches(policy.pageHost || frameHost, host))) {
      return { selectors: [], procedural: [] };
    }
    const selectors = [];
    // `listsExempt` (a bank or payment frame) keeps the bundled lists out, never the person's rules.
    const bundled = Boolean(policy.blockAds) && policy.listsExempt !== true;
    if (bundled) {
      selectors.push(...HIGH_CONFIDENCE_SELECTORS);
      selectors.push(...scopedSelectors(staticRules.cosmetics, frameHost));
      selectors.push(...staticRules.candyDefaults.filter((rule) =>
        rule.group === "Built-in ads" && hostPatternMatches(frameHost, rule.host),
      ).map((rule) => rule.selector));
    }
    if (policy.blockAds) {
      selectors.push(...policy.cosmetics.filter((rule) =>
        hostMatches(frameHost, rule.h),
      ).map((rule) => rule.s));
    }
    if (policy.hideConsent && !policy.cookieBannerRemovalDisabled) {
      selectors.push(...staticRules.candyDefaults.filter((rule) =>
        rule.group === "Built-in cookies" && hostPatternMatches(frameHost, rule.host),
      ).map((rule) => rule.selector));
    }
    const unique = [...new Set(selectors)].slice(0, 4096);
    const procedural = bundled ? staticRules.procedural.filter((rule) =>
      hostPatternMatches(frameHost, rule.host),
    ).slice(0, 64) : [];
    // `generic` asks the frame to report its class and id names; the complex generic selectors
    // (attribute and descendant ones, a few hundred) come with the payload.
    const genericPolicy = bundled && !Array.isArray(staticRules.cosmetics) &&
      staticRules.cosmetics.genericPolicy ? staticRules.cosmetics.genericPolicy(frameHost) : null;
    const genericSelectorList = genericPolicy ? staticRules.cosmetics.generic.complex.filter((selector) =>
      !genericPolicy.denied.has(selector),
    ).slice(0, MAX_GENERIC_COMPLEX) : [];
    return { selectors: unique, procedural, generic: genericPolicy !== null, genericSelectors: genericSelectorList };
  }

  global.CandyPrivacyRules = {
    advancedDecision,
    cosmeticPayload,
    defusesWindowOpen,
    genericSelectors,
    hostMatches,
    hostPatternMatches,
    indexCosmetics,
    parseAdvanced,
    parseCandyDefaults,
    parseCosmetic,
    parseProcedural,
    parseSiteList,
    parseWindowOpenDefusers,
    registrableDomain,
    urlPatternMatches,
  };
})(globalThis);
