# Site protection levels (v5, V17 groundwork)

The model and engine parts behind the v5 site protection panel. No screen yet: part 1 defines the
port, the rules, storage and the Gecko pieces; part 2 wires them into `BrowserController` and the
blocker.

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
