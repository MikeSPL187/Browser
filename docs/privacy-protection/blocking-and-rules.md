# Blocking and Candy Rules

## Blocking pipeline

| Layer | Source | Role |
| --- | --- | --- |
| Process snapshot | `BundledBlockingSnapshotProvider` | Build immutable bundled matchers once per app process on background workers |
| Bundled network lists | `ContentBlocker` + `RequestBlocker` | Exact/subdomain host and scoped pair blocking with allow exceptions |
| Advanced URL lists | `AdvancedFilterRules` | Host-bucketed URL-path/wildcard rules plus scoped popup/popunder decisions |
| Procedural cosmetics | `ProceduralCosmeticRules` | Bounded literal-text hiding and element removal for scoped upstream rules |
| Bundled cosmetic lists | `EasyListCosmeticRules`, `BundledCandyRules` | Resolve scoped and bounded generic standard selectors with exceptions |
| Consent handling | `ConsentBlockerScript` + curated request rules | Hide consent UI, stop known modal CMP runtimes and apply bounded declarative site rules |
| User/import/subscription rules | `CandyRule*`, `CandyRuleRepository` | Validate, normalize, persist and compile per-profile matchers |
| WebKit runtime interception | WebKit browser controller | Combine site settings, bundled lists and Candy Rule decisions for the active WebKit renderer |
| Gecko runtime interception | uBlock Origin + Candy Cookie Defaults | Mozilla-signed default extension owns ad filtering; Candy keeps only its small curated consent fallback |

Scoped cosmetic document-start rules run in every frame whose origin matches the registered page or
user-rule origin. Generic cosmetics use a Candy-owned token scanner in the top document and its
same-origin frames; cross-origin frames remain outside that boundary and rely on request blocking.

Bundled advanced rules support bounded host-anchored paths, `*` wildcards, `^` separators,
positive/negative `domain` scopes, first/third-party scopes, allow exceptions, `$popup`, and
`$popunder`.
Popup rules inspect HTTP(S) main-frame targets created by `onCreateWindow`; automatic non-gesture
windows remain rejected by existing WebView policy. A same-site first target stays monitored for a
bounded five-second window so a delayed cross-site advertising hop cannot bypass policy.
Site pause, profile, and private-tab ownership come from actual opener. Cross-site windows without a
matching rule honor the user gesture and open normally; explicit popup rules still block matching
targets. A per-domain **Always block pop-ups** override rejects every `onCreateWindow` request from
the registrable opener domain before Candy creates a popup tab. This strict override is independent
of site pause, filter allow rules, and federated-login compatibility, and never offers an **Open**
action. Regular tabs persist it per profile; private tabs keep it in memory until the private session
ends. Popunder rules use a bounded five-second correlation window between the surviving child and a
redirected opener, then close the listed opener. Regex filters, redirects, `$important`, arbitrary
JavaScript, and trusted uBO scriptlets fail closed.

The EasyList and uAssets cosmetic compilers include their supported global standard-CSS subsets.
Global and scoped `#@#` exceptions cancel matching selectors across merged sources; supported
`$ghide` exceptions disable only global selectors for matching sites. Kotlin resolves those host
semantics before the Candy-owned runtime receives a bounded deny policy. Simple global ID/class
selectors are token-indexed and injected only when matching DOM tokens occur. Complex global
selectors use one bounded stylesheet. The immutable prefix-compressed selector payload is built on
the cosmetic worker, cached once per top document, and never expanded into thousands of
`insertRule` calls during navigation. Scanner batches process at most 512 nodes or 4 ms, retain at
most 8,192 pending nodes, and inject at most 1,024 selectors / 96 KiB per document.
The data-only WebView bridge requires an unexposed per-WebView token, accepts canonical hosts rather
than arbitrary URLs, and keeps at most 64 resolved host policies; invalid calls fail closed.
Five Candy-owned native CSS rules additionally cover high-confidence leftovers. Four collapse media
with a bounded `ads_banner` filename token; one collapses an `#AlternateMessage` fallback only when
it directly follows `#ad_banner`. They add no request matching or DOM scanning. Ambiguous editorial
examples, banner documentation, and unrelated alternate-content elements remain visible.

Candy's host-scoped consent defaults remove Reddit's current data-protection consent dialog and its
temporary body scroll lock on WebKit. Gecko delegates consent handling to its installed Firefox
extension and does not run Candy consent selectors or request rules in parallel.

Network host matching combines Candy's curated hosts, the complete supported EasyList/EasyPrivacy
template graph, uAssets, and a deduplicated HaGeZi Pro delta. These lists (hosts, host pairs,
`uassets_advanced_filters.txt` and the cosmetic rules) live in `app/src/blocking/assets/` and ship once per build:
System WebView reads them at the asset root, the Gecko build inside the `candy_privacy` extension
(`rules/`); `BlockingListAssets` opens either. Sorted byte indexes keep the larger
bundles off the per-request allocation path. Curated owner-family exceptions may allow a blocked
service only on a PSL-validated family such as `google.*`; lookalike suffixes do not match. User
Candy allow rules and site pause still take precedence over bundled blocking.

### GeckoView extension-owned filtering

Android Gecko sessions provision Mozilla-signed uBlock Origin as their ad and tracker blocker.
Candy does not send bundled ad lists, custom ad rules, popup or popunder filter lists to Gecko and
does not initialize the native `ContentBlocker` snapshot in the Gecko controller. The private
`candy-privacy-host` parses only the 4.9 KB `candy_default_rules.txt` consent defaults today.
Vola's own Gecko blocker replaces uBlock Origin in steps (`docs/vola/research/ublock-origin.md`).
It is behind the developer option «Vola's own Gecko blocker» (`DeveloperSettings.volaGeckoBlocker`,
off by default; uBlock Origin keeps running alongside until the switch-over step):

- The extension packs the network lists; `host_blocker.js` ports `RequestBlocker` with the same
  decision order, `network_blocker.js` adds the uAssets URL rules (bucketed by target host) and the
  person's own Vola rules with `CandyRulePrecedence`. `network_filters.js` applies the EasyList and
  EasyPrivacy URL rules the host compiler leaves out (`easylist_network_filters.txt`, 12 745 rules
  from `scripts/compile_network_filters.py`): paths, `||`/`|`/`^`/`*`, resource types (Gecko's
  webRequest reports them), party and `domain=`, and `@@` exceptions. Each rule carries one URL
  token, so a request only tries the rules whose token its URL contains; `$redirect`, `$csp`,
  `$important`, `$removeparam`, regular expressions and `match-case` stay out. Order: own rule →
  URL rules (an exception in any list wins, as in uBlock Origin) → host lists.
- With the option on, `geckoPrivacyPolicyFor` sends the site's `blockAdsAndTrackers`, the active
  Candy rules and `strictBlocking` (Strict level). Off sends nothing to block; a paused site is
  skipped as before; Strict drops the lists' compatibility exceptions (allow pairs, owner families,
  uAssets allow rules), never the person's own rules or the first-party escape.
- Main-frame requests are never blocked. Blocks report as built-in events, so the site panel
  counts them. The first requests after start wait up to 5 s for the lists to load; a list that
  fails to load lets requests through rather than holding the page.
- Ad cosmetics: the EasyList and uAssets scoped hiding rules and the uAssets procedural rules
  (`app/src/blocking/assets/`, packed into the extension like the network lists) load with them.
  `CandyPrivacyRules.indexCosmetics` buckets them by host, so a frame checks its own hosts' rules
  (about 0.8 ms per frame in node, 3.5 times faster than the full scan). A frame of a page that
  blocks ads waits up to 5 s for them.
- Generic cosmetics (host `*`, about 13 900 selectors): the 13 100 plain `.class` / `#id` ones
  sit in two sets. A frame whose payload says `generic: true` walks its DOM in 4 ms chunks per
  animation frame, reports the class and id names it meets (each once, at most 20 000) with a
  `genericCosmetics` message, and hides the selectors the background answers (at most 1 024). The
  ~700 complex generic selectors come with the payload. Exceptions (`#@#`), `generichide` and
  per-host exclusions (`~site##.ad`) are resolved per host and cached (128 hosts); a lookup of
  1 850 names takes about 0.1 ms in node.
- Popups: the uAssets `$popup` / `$popunder` rules (`uassets_advanced_filters.txt`). The 98 that
  judge the new window (opener page, target) go to `PopupListFilter` in Kotlin, which
  `BrowserController` asks at its popup and popunder decisions when the flag is on and the opener's
  site blocks ads; the list is parsed off the main thread, a popup opened before it is ready waits
  up to 300 ms, then goes through. The 629 marked for the window.open defuser come to the frame
  with its cosmetics (`defuseWindowOpen`): the page's `window.open` opens nothing, defined in the
  page's own scope through `exportFunction`, as `CandyWindowOpenDefuserScript` does on WebView.
  Popups to hosts on the host lists are not blocked on Gecko yet (WebView checks them too).
- Tests: `scripts/host_blocker.test.mjs`, `scripts/network_blocker.test.mjs`,
  `scripts/candy_privacy_rules.test.mjs` (index matches the full scan on real lists). The host
also retains session binding, Reader extraction, PiP playback continuity and narrowly scoped
SSO/CAPTCHA compatibility observation.

Gecko Safe Browsing remains enabled. The global third-party-cookie switch changes
the process runtime between `ACCEPT_FIRST_PARTY` and `ACCEPT_ALL` for normal and private sessions.
The host also observes only the recognized SSO and CAPTCHA request hosts, including requests that
arrive before a just-published policy update; native blocking state, HTTPS host/path, current-page,
policy-revision direction, and public-suffix validation drives the existing consent flow.
GeckoView 155 has no site-scoped override for hard `ACCEPT_FIRST_PARTY` rejection. After consent,
Candy therefore switches the shared runtime to `ACCEPT_ALL` only while the matching page is the
selected session. Normal and private modes remain separate. Candy restores `ACCEPT_FIRST_PARTY`
before cross-host main-frame navigation and on deactivation, revocation, or close. Inactive
same-mode sibling sessions share the runtime. Candy marks them inactive through Gecko's session
lifecycle during that bounded compatibility window, though GeckoView does not specify that as a
complete network suspension.
Compatibility observations are returned to native code in bounded batches. The built-in host is
hidden from the user extension manager and is explicitly enabled in private browsing; ordinary
installed Firefox extensions retain their separate permission policy.

The first external load is fail-closed until the extension assets, private permission, policy, and
session binding are all acknowledged. Initialization, policy, and bootstrap binding each have a
15-second bound. A timeout, asset failure, or post-initialization native-port disconnect emits a
failed navigation with a Candy Privacy error instead of loading without protection. A restart caused
by initially applying the private permission may reconnect within the same bounded initialization
window. Session binding uses two acknowledgements: native first authenticates the exact extension,
session and token generation and returns a primitive response; only after the extension page receives
that response does its background port confirm the binding. Candy posts the real page navigation only
after this confirmation. Navigating synchronously from the message delegate can tear down Gecko's
extension-page actor before the native response is delivered. Policy revisions may overtake an
in-flight bootstrap, but an authenticated older revision can only bind the same live generation; the
external-load gate still waits for both that session binding and the latest policy acknowledgement.

WebKit retains Candy's existing bounded cosmetic, procedural, popup, popunder, and consent handling.
Those rules do not execute in Gecko page content.

Bundled network, URL, popup, and procedural assets start parsing as soon as the first
`ContentBlocker` is created. The immutable snapshot is application-scoped, survives Activity
recreation, and is reused by every tab. A blank WebView and browser chrome may appear immediately;
the first external load or persisted WebView-state restore waits for snapshot readiness. The latest
pending navigation per tab wins, while stop, close, snooze, blank navigation, WebView recreation,
and controller destruction cancel stale starts. After process death the snapshot is rebuilt in the
background before restored pages can issue requests. Internal `about:blank` callbacks from a newly
created waiting WebView are ignored so they cannot overwrite a persisted restore state.

WebView request callbacks pass their already parsed request/page hosts into `ContentBlocker`.
Advanced rules inspect path/query only when a host or page bucket has candidates; the legacy
fallback reuses the same hosts instead of parsing both URLs again.

## Candy Rule precedence

| Higher priority | Lower priority |
| --- | --- |
| Scoped pair allow | Scoped pair block |
| More specific page/request host | Less specific host |
| Allow at equal specificity | Block at equal specificity |
| Stable rule ID tie-break | — |

## Guardrails

- Never intercept a main-frame request with a Candy network rule.
- Preserve first-party escape for broad host rules; precise generated URL-path rules may block
  first-party resources after their explicit allow exceptions are checked.
- Validate hosts, public suffixes, selectors, profile IDs and HTTPS subscription sources atomically.
- Keep persistent matcher free of ephemeral private rules; private matcher may include them only in memory.
- Support only declared Candy/ABP subsets. Reject unsupported syntax instead of approximating it.
- Curated consent-runtime hosts apply provider-wide only while cookie-banner removal is enabled;
  site protection pause and per-site consent overrides remain escape hatches.

## Main files

| Concern | File |
| --- | --- |
| Runtime blocker | [`ContentBlocker.kt`](../../app/src/main/java/dev/sk2andy/materialbrowser/blocking/ContentBlocker.kt) |
| Gecko host contract/runtime | [`CandyPrivacyHostContract.kt`](../../app/src/main/java/dev/sk2andy/materialbrowser/browser/gecko/CandyPrivacyHostContract.kt), [`GeckoPrivacyHostRuntime.kt`](../../app/src/main/java/dev/sk2andy/materialbrowser/browser/gecko/GeckoPrivacyHostRuntime.kt) |
| Async process snapshot | [`BundledBlockingSnapshot.kt`](../../app/src/main/java/dev/sk2andy/materialbrowser/blocking/BundledBlockingSnapshot.kt) |
| First-load/restore gate | [`BlockingStartGate.kt`](../../app/src/main/java/dev/sk2andy/materialbrowser/browser/BlockingStartGate.kt) |
| Host lookup | [`RequestBlocker.kt`](../../app/src/main/java/dev/sk2andy/materialbrowser/blocking/RequestBlocker.kt) |
| URL/popup lookup | [`AdvancedFilterRules.kt`](../../app/src/main/java/dev/sk2andy/materialbrowser/blocking/AdvancedFilterRules.kt) |
| Procedural runtime | [`ProceduralCosmeticRules.kt`](../../app/src/main/java/dev/sk2andy/materialbrowser/blocking/ProceduralCosmeticRules.kt) |
| Generic cosmetic runtime | [`GenericCosmeticRuntime.kt`](../../app/src/main/java/dev/sk2andy/materialbrowser/blocking/GenericCosmeticRuntime.kt) |
| Rule validation/matching | [`CandyRule.kt`](../../app/src/main/java/dev/sk2andy/materialbrowser/blocking/CandyRule.kt) |
| Import/export/subscriptions | [`CandyRuleFormat.kt`](../../app/src/main/java/dev/sk2andy/materialbrowser/blocking/CandyRuleFormat.kt) |
