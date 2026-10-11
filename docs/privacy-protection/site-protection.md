# Site protection levels (v5, V17 groundwork)

The model and engine parts behind the v5 site protection panel. No screen yet: part 1 defines the
port, the rules, storage and the Gecko pieces; part 2 wires them into the browser.

## Levels (owner's decision 2026-10-11, after Brave)

| Level | Stored as | What it does |
| --- | --- | --- |
| Off | the site exception (site pause, `site_exceptions`) | No blocking, no cookie-banner removal, no de-AMP, no fingerprinting protection, no engine tracking protection |
| Standard | default, nothing stored | The global protection settings, plus the site's sign-in and captcha cookie exceptions |
| Strict | `strictProtectionOverride` in `site_privacy_overrides` | Ads and trackers blocked and cookie banners removed whatever the global switches say, compatibility allow lists ignored, third-party cookies blocked with no sign-in or captcha exception, cross-site referrers trimmed |

The site's own switches hold on Standard and Strict: cookie banners (`cookieBannerRemovalDisabledOverride`),
de-AMP (`deAmpDisabledOverride`) and fingerprinting protection (`fingerprintingProtectionDisabledOverride`).
They are how a broken site is fixed without lowering its level. Private tabs keep every change in
memory only.

## Code

| Part | Where |
| --- | --- |
| Port and model | `browser/protection/SiteProtection.kt`: `SiteProtectionLevel`, `SiteProtectionPolicy`, `SiteProtectionCounts`, `SiteProtectionPanelState`, `SiteProtectionPort` |
| Rules | `browser/protection/SiteProtectionRules.kt`: level, resolved policy, override transforms, fingerprinting exception hosts, panel counts |
| Storage | `SitePrivacyOverrides` (`blocking/PrivacyXRay.kt`), persisted per profile by `BrowserSessionStore` |
| Gecko fingerprinting | `GeckoFingerprintingExceptions` writes `privacy.fingerprintingProtection.granularOverrides` (`-AllTargets` for the site's pages and the frames in them) through `CandyGeckoPrefsBridge.commitRuntimePref`; `AndroidBrowserEngineFactory.setFingerprintingProtectionExceptions` |
| Gecko counts | `GeckoContentBlockingCounts` reads `ContentBlockingController.getLog`: blocked sites per kind (trackers, fingerprinters, cryptominers, social trackers, tracking cookies); `AndroidBrowserEngineSessionPort.protectionCounts` |
| System WebView | No per-site fingerprinting control (`perSiteFingerprintingProtection = false`) and no engine log (`protectionCounts` answers null); levels, cookie banners and de-AMP work through Vola's blocker |

Gecko keeps one fingerprinting exception list for the whole runtime: a site excepted in one
profile or private tab is excepted in all of them while the exception lasts. The panel says so.

For the panel (V17):

- Gecko's override schema takes only domains whose top level is 2–6 Latin letters. `.рф`
  (`xn--p1ai`), longer zones such as `.digital` and IP addresses cannot be excepted: hide the
  fingerprinting switch there (`GeckoFingerprintingExceptions.supports`) or say why it is missing.
- Counts are per registrable domain (`a.tracker.example` and `b.tracker.example` are one site).
  Partitioned cookies are not counted as blocked. The panel shows one total (`system-v5.md`, section 10).
- Off is the site pause and clears the Strict mark: after the pause is lifted the site is back on
  Standard. The panel text must say so.

On Gecko, network ad blocking is the bundled uBlock Origin, which Vola cannot steer per site (no API
for other extensions, no managed storage on Android): levels do not reach it yet. Options and the
plan-v5 decision to replace it — `docs/vola/research/ublock-origin.md`.

## Wiring (part 2)

| Effect | Where |
| --- | --- |
| The port | `BrowserSiteProtection` implements `SiteProtectionPort` and now holds the site pause (`pause`, `resume`), moved out of `BrowserController`, which keeps one-line delegates |
| Policy per page | `BrowserController.sitePolicyFor` resolves `SiteProtectionPolicy`; `geckoPrivacyPolicyFor` takes cookie-banner handling (`SiteProtectionRules.handlesCookieConsent`), third-party cookie exceptions and Strict referrer trimming from it, and on System WebView the ad blocking too |
| Referer on Strict | `GeckoPrivacyPolicy.trimCrossSiteReferrer` → `privacy_signals.js` drops `Referer` on requests to another site (not the page's own host, its parents or children) |
| de-AMP per site | AMP links to a site whose de-AMP switch is off stay AMP (`deAmpsTo`) |
| Fingerprinting | `permanentSitePrivacyOverrides` and private-tab changes push the exception list to the engine; an engine started later gets it at start |
| Counts | `SiteProtectionPort.counts` adds the engine's log to the blocker's requests |

Limits: Strict cannot block third-party cookies on a site while the global switch lets them through
(Gecko keeps per-site allow exceptions only); uBlock Origin is untouched until Vola's own blocker
replaces it on Gecko.
