"use strict";

// EasyList/EasyPrivacy URL filters for Vola's own Gecko blocker (scripts/compile_network_filters.py).
// Rules are indexed by one URL token each; a request tries only the rules whose token occurs in
// its URL, and an ABP pattern becomes a regular expression the first time it is tried.
(function exposeCandyNetworkFilters(global) {
  const HEADER = "candy-network-filter:2";
  const TOKEN_RUN = /[a-z0-9%]+/g;
  const SEPARATOR = "(?:[^a-z0-9_.%-]|$)";

  function declaredCount(text, prefix) {
    const line = text.split(/\r?\n/).find((value) => value.startsWith(prefix));
    const count = line ? Number(line.slice(prefix.length).trim()) : NaN;
    if (!Number.isSafeInteger(count) || count < 0) throw new Error(`Missing ${prefix}`);
    return count;
  }

  // ABP pattern -> RegExp over the lower-case URL: `||` anchors at a host label, `|` at an end,
  // `^` is a separator (or the end), `*` anything.
  function patternToRegExp(pattern) {
    let body = pattern;
    let prefix = "";
    let suffix = "";
    if (body.startsWith("||")) {
      body = body.slice(2);
      prefix = "^[a-z][a-z0-9+.-]*:\\/\\/(?:[^\\/?#]*\\.)?";
    } else if (body.startsWith("|")) {
      body = body.slice(1);
      prefix = "^";
    }
    if (body.endsWith("|")) {
      body = body.slice(0, -1);
      suffix = "$";
    }
    // An unanchored edge that is only `*` matches anyway: no `.*` to scan.
    if (!prefix) body = body.replace(/^\*+/, "");
    if (!suffix) body = body.replace(/\*+$/, "");
    const source = Array.from(body, (character) => {
      if (character === "*") return ".*";
      if (character === "^") return SEPARATOR;
      return character.replace(/[.+?${}()[\]\\/|]/g, "\\$&");
    }).join("");
    return new RegExp(prefix + source + suffix);
  }

  function parse(text) {
    if (!text.startsWith(HEADER)) throw new Error("Invalid network filter header");
    const rows = text.split(/\r?\n/).slice(1).filter((line) => line && !line.startsWith("#"));
    if (rows.length !== declaredCount(text, "# Rules:")) throw new Error("Network filter count mismatch");
    return rows.map((row) => {
      const fields = row.split("\t");
      if (fields.length !== 7 || (fields[0] !== "A" && fields[0] !== "B")) {
        throw new Error("Invalid network filter");
      }
      return {
        action: fields[0],
        token: fields[1] === "-" ? null : fields[1],
        pattern: fields[2],
        types: fields[3] === "*" ? null : new Set(fields[3].split(",")),
        pages: fields[4] === "-" ? [] : fields[4].split(","),
        excluded: fields[5] === "-" ? [] : fields[5].split(","),
        party: fields[6],
        regExp: null,
      };
    });
  }

  function index(filters, { hostPatternMatches, registrableDomain }) {
    const byToken = new Map();
    const tokenless = [];
    for (const filter of filters) {
      if (filter.token === null) {
        tokenless.push(filter);
      } else {
        if (!byToken.has(filter.token)) byToken.set(filter.token, []);
        byToken.get(filter.token).push(filter);
      }
    }

    function applies(filter, url, type, requestHost, pageHost) {
      if (filter.types !== null && !filter.types.has(type)) return false;
      if (filter.pages.length && (!pageHost || !filter.pages.some((page) => hostPatternMatches(pageHost, page)))) {
        return false;
      }
      if (pageHost && filter.excluded.some((page) => hostPatternMatches(pageHost, page))) return false;
      if (filter.party !== "*") {
        if (!pageHost) return false;
        const thirdParty = registrableDomain(requestHost) !== registrableDomain(pageHost);
        if (filter.party === "3" ? !thirdParty : thirdParty) return false;
      }
      if (filter.regExp === null) filter.regExp = patternToRegExp(filter.pattern);
      return filter.regExp.test(url);
    }

    // "A" when an exception matches, "B" when only a block does, null otherwise; `strict` skips the
    // lists' exceptions like the rest of the Strict level.
    return function decide(rawUrl, type, requestHost, pageHost, { strict = false } = {}) {
      const url = String(rawUrl).toLowerCase();
      const seen = new Set();
      let blocked = false;
      const visit = (bucket) => {
        for (const filter of bucket) {
          if (blocked && filter.action === "B") continue;
          if (strict && filter.action === "A") continue;
          if (!applies(filter, url, type, requestHost, pageHost)) continue;
          if (filter.action === "A") return true;
          blocked = true;
        }
        return false;
      };
      if (visit(tokenless)) return "A";
      for (const [token] of url.matchAll(TOKEN_RUN)) {
        if (seen.has(token)) continue;
        seen.add(token);
        const bucket = byToken.get(token);
        if (bucket && visit(bucket)) return "A";
      }
      return blocked ? "B" : null;
    };
  }

  global.CandyNetworkFilters = { parse, index, patternToRegExp };
})(globalThis);
