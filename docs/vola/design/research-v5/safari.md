# Safari on iOS 26 / iPadOS 26 (Liquid Glass era): visual and interaction design facts

> Research note, 2026-10-10. Method: Apple HIG pages and WWDC25 transcripts were read in full from developer.apple.com (the only primary domain reachable from this environment). Most third-party pages (MacRumors, 9to5Mac, MacStories, Six Colors, support.apple.com, WebKit, Wikipedia) were blocked by the egress proxy, so facts from them come from search-engine extracts of those pages. They are marked "(via search extract)" and are less reliable than primary sources. Behavior up to iOS 18 is marked **[older]**. iOS 27 shipped in September 2026; its known deltas are flagged **[iOS 27]** so the design system does not copy a state Apple has already revised.

## 1. iPhone layouts: Compact / Bottom / Top, the floating bar, collapse on scroll, swipe between tabs

### Takeaway
iOS 26 Safari ships three layouts: **Compact** (the default), **Bottom** and **Top**. You pick one in Settings › Apps › Safari › Tabs. In Compact, a single floating Liquid Glass capsule holds the address field. A Back button sits to its left and a "•••" page menu sits next to it; Share, Bookmarks, All Tabs and New Tab live behind the "•••" menu. In all layouts the bar shrinks to a small URL pill when you scroll down, and scrolling up or tapping the pill restores it. Swiping horizontally on the bar switches tabs. On the last tab, swiping left opens a new tab. Swiping up opens the tab overview. Apple publishes no exact Safari bar dimensions. The closest numbers are a 21 pt inset for floating bars (third-party), the 44×44 pt minimum hit target and the capsule radius rule (radius = height/2).

### Cited Findings
- Three layouts: Compact (default), Bottom, Top, chosen in Settings › Apps › Safari › Tabs. Bottom and Top behave much like iOS 18 but in Liquid Glass styling. — [iDownloadBlog](https://www.idownloadblog.com/2025/06/12/ios-26-safari-tab-bar-designs/), [Tom's Guide](https://www.tomsguide.com/phones/iphones/ios-26-safari-lets-you-pick-your-own-tab-design-heres-how-to-do-it), [MacRumors](https://www.macrumors.com/2025/06/10/apple-ios-26-safari-design/) (via search extract)
- Compact: the URL bar sits at the bottom, "detached from the edge", between the Back button and the ••• button. — [9to5Mac](https://9to5mac.com/2025/09/15/iphone-ios-26-safari-new-compact-design/) (via search extract)
- Compact: "The Back button sits to the left of the pill-shaped address bar, and a three-dot menu next to it holds Share, All Tabs, Bookmarks, New Tab, and more." Other reports place the ••• button on the left of the URL field. — [MacRumors iOS 26 beta 2](https://www.macrumors.com/2025/06/23/ios-26-beta-2-safari-design-change/), [iDownloadBlog](https://www.idownloadblog.com/2025/07/07/get-back-old-safari-address-bar-ios-26/) (via search extract)
- Beta 1 of Compact had no Forward button. From beta 2, tapping Back in Compact "splits it into separate back and forward buttons". Top and Bottom always showed both. — [MacRumors](https://www.macrumors.com/2025/06/23/ios-26-beta-2-safari-design-change/) (via search extract)
- Bottom layout: Share, Bookmarks and Tabs buttons are always visible. The Top and Bottom bars are "slimmer bars that don't span the full width of the screen". In all three modes the bars float instead of docking and are translucent. — [MacRumors guide](https://www.macrumors.com/guide/ios-26-safari-features/), [allthings.how](https://allthings.how/safaris-compact-tabs-removed-on-macos-26-and-ipados-26-what-changed/) (via search extract)
- Collapse: "Compact and Bottom layouts shrink into a small pill as you scroll down a page." Top also collapses its address bar into a pill. Scroll up or tap the pill to restore the full UI. The collapsed state shows "just the webpage address". — [MacRumors](https://www.macrumors.com/2025/06/23/ios-26-beta-2-safari-design-change/), [9to5Mac iPadOS 26.4](https://9to5mac.com/2026/03/26/macos-tahoe-26-4-and-ipados-26-4-add-compact-tab-bar-in-safari/) (via search extract)
- The system pattern Safari mirrors: a tab bar can "minimize on scroll" with `tabBarMinimizeBehavior = .onScrollDown` and "re-expands when scrolling in the opposite direction". With an accessory, the minimized bar moves the accessory inline, and you exit minimized state "by tapping a tab or scrolling to the top". — [WWDC25 "Build a UIKit app with the new design"](https://developer.apple.com/videos/play/wwdc2025/284/), [WWDC25 "Build a SwiftUI app with the new design"](https://developer.apple.com/videos/play/wwdc2025/323/), [HIG Tab bars](https://developer.apple.com/design/human-interface-guidelines/tab-bars)
- Gestures in Compact: a horizontal swipe over the address bar switches to the previous or next tab. Swiping up on the address bar opens the tab overview. Caveat: the Home indicator sits right below the bar, so an imprecise swipe up can leave the app and a horizontal swipe on the indicator switches apps. — [PhoneArena](https://www.phonearena.com/news/Safari-on-iOS-26-is-a-mess-but-you-can-fix-some-of-it_id175463) (via search extract)
- New tab past the last tab: swiping left across the toolbar opens a new tab **only when you are on the last tab**. On an earlier tab the same swipe just moves to the next tab. Commenters disagree whether this predates iOS 26. **[older: the gesture existed in iOS 15–18 bottom bar]** — [MacRumors forums](https://forums.macrumors.com/threads/safari-changes-on-ios-26-go-beyond-the-address-bar.2459238/), [MacObserver](https://www.macobserver.com/tips/ios-26-safari-tabs/) (via search extract)
- Floating bar inset: "The bar is inset from the screen edges (21pt on left, right, and bottom)" (third-party iOS 26 guide about the floating tab bar). — [LearnUI](https://www.learnui.design/blog/ios-design-guidelines-templates.html) (via search extract)
- Hit target: "a button needs a hit region of at least 44x44 pt". About 12 pt of padding around bezeled elements and about 24 pt around non-bezeled elements. — [HIG Buttons](https://developer.apple.com/design/human-interface-guidelines/buttons), [HIG Accessibility](https://developer.apple.com/design/human-interface-guidelines/accessibility)
- Near device edges on phone: "use a capsule with extra margin to create space near the screen edge". On iPad/Mac, use a concentric shape aligned with the window edge. — [WWDC25 "Get to know the new design system"](https://developer.apple.com/videos/play/wwdc2025/356/)
- Toolbar items use "system-provided symbols without borders"; the bar is the container. Primary action uses `.prominent` (tinted) on the trailing side. Bar items are monochrome by default. — [HIG Toolbars](https://developer.apple.com/design/human-interface-guidelines/toolbars), [WWDC25 323](https://developer.apple.com/videos/play/wwdc2025/323/)
- Criticism: the Compact default "minimize[s] the controls … even further", Forward is missing and Share/Bookmarks/Tabs cost an extra tap. Reviewers suggest the Top layout, plus the 26.1 opacity option for legibility. — [PhoneArena](https://www.phonearena.com/news/Safari-on-iOS-26-is-a-mess-but-you-can-fix-some-of-it_id175463) (via search extract)

### Inferences
- Compact control order, left to right: [Back (splits into Back|Forward on tap)] [••• page menu] [capsule: page-menu/reader glyph · domain · reload] (see Gaps). An Android clone can follow this: one capsule plus one or two circular glass buttons, each ≥44 dp, inset about 21 dp from the screen sides and bottom (above the gesture inset).
- Estimate, not measured: the expanded capsule is about 44–50 pt tall, which follows from the 44 pt hit target plus glass padding. The collapsed pill shows only the domain in smaller text (about Footnote/Caption size). Verify with Apple's iOS 26 Figma kit before you fix any tokens.
- Collapse/expand is direction-driven: scroll down collapses, any upward scroll or a tap expands, and reaching the top expands too.

### Gaps
- No primary or measured source found for the exact Safari capsule height, corner radius, button diameter, glyph size or collapsed-pill height. The Apple Design Resources Figma kit (figma.com/community/file/1527721578857867021) holds these but could not be opened here.
- The exact position of the reader/"page menu" glyph inside the capsule and of the reload button in iOS 26.x final is not confirmed by a fetched source.
- No source describes the transition curve of the collapse (scroll-linked or a spring after a threshold).

## 2. Address field focus: morph, suggestions, keyboard

### Takeaway
Apple's general iOS 26 rule is that a bottom search control "animates into a search field above the keyboard". Safari's Compact field follows this pattern. No fetched source documents Safari 26's suggestion order (Top Hit, Switch to Tab and so on).

### Cited Findings
- Bottom-toolbar search: "When someone taps it, it animates into a search field above the keyboard so they can begin typing." With little room, the system may minimize search into a toolbar button that opens "a full-width search field … above the keyboard". — [HIG Search fields](https://developer.apple.com/design/human-interface-guidelines/search-fields), [WWDC25 323](https://developer.apple.com/videos/play/wwdc2025/323/)
- Liquid Glass "dynamically morphs between the controls in each context … maintains the concept of having a singular floating plane that the controls live on", and menus "pop open" from the tapped button. — [WWDC25 "Meet Liquid Glass"](https://developer.apple.com/videos/play/wwdc2025/219/)
- HIG: "Provide the most relevant search results first … consider categorizing them." — [HIG Search fields](https://developer.apple.com/design/human-interface-guidelines/search-fields)

### Inferences
- Model the focus transition as the same glass capsule morphing: it widens to full width, rises to sit on the keyboard, the side buttons dissolve into it, and Cancel or Close appears. The suggestions list fills the space above it. Apple's long-standing Safari order **[older, iOS 15–18, from general knowledge, not verified for 26]** is Top Hit, then Safari Suggestions, Switch to Tab / open tabs, Bookmarks & History, search-engine suggestions, then "On This Page".

### Gaps
- No fetched source confirms the iOS 26 suggestion order, the "Switch to Tab" row styling, or whether suggestions render on a glass sheet or an opaque list.

## 3. Tab overview, tab groups, profiles, search in tabs, close/pin

### Takeaway
The tab overview uses Liquid Glass controls. The tab-group and profile switcher is a drop-down at the top, other tab tools sit behind "•••", and a horizontal swipe switches between tab groups. Beta 2 moved "+" back to the bottom-left as in iOS 18. Precise grid metrics were not found.

### Cited Findings
- Profile switching is "a dropdown menu at the top of the display", with other tab-management tools behind "···". "Swiping from left to right allows you to quickly swap between your tab groups." — [MacRumors iOS 26 Safari guide](https://www.macrumors.com/guide/ios-26-safari-features/) (via search extract)
- Beta 1 moved the "+" new-tab button to the top, and beta 2 restored it to the bottom-left "as in iOS 18". Commenters still complained about new-tab and "Done" controls at the top left and right of the tab overview. — [MacObserver](https://www.macobserver.com/news/ios-26-beta-2-restores-safaris-new-tab-button-to-the-bottom-bar/), [MacRumors forums](https://forums.macrumors.com/threads/ios-26-beta-2-fixes-frustrating-safari-design.2459543/) (via search extract)
- "Safari's buttons, address bar, and tab view use the translucent Liquid Glass style … everything is rounder." — [MacRumors](https://www.macrumors.com/guide/ios-26-safari-features/) (via search extract)
- **[iOS 27]** Safari can "bundle your tabs into topics" automatically. — [WWDC26 recap, TapSmart](https://www.tapsmart.com/news/wwdc26-recap/) (via search extract)
- Concentric nesting rule for cards in a container: "artwork in a card … needs to be concentric", meaning inner radius = outer radius − padding. — [WWDC25 356](https://developer.apple.com/videos/play/wwdc2025/356/)

### Inferences
- **[older, iOS 15–18 general knowledge, unverified for 26]** On iPhone portrait the overview is a 2-column grid of portrait thumbnails with the title under or over each card. Swiping a card left closes it, and X closes it. Long-press offers Pin/Move to Tab Group/Close Other Tabs. Pull down reveals a "Search Tabs" field. Treat this as a hypothesis to verify on a device.
- Card radii should follow concentricity: if the device corner is about 55 pt and the grid margin is about 16 pt, the card radius comes out around 20–28 pt (estimate).

### Gaps
- Column count, card aspect ratio, card radius, spacing, and close/pin gestures were not confirmed by any fetched iOS 26 source.
- It is unconfirmed whether "Search Tabs" still uses pull-down in iOS 26.

## 4. Reader, Listen to Page, Hide Distracting Items, Privacy Report, Private Browsing, page tinting

### Takeaway
These features live in the **Page Menu**, which opens from the glyph in the Smart Search field. Hide Distracting Items is a tap-to-select flow that shows a crossed-out eye badge next to the Page Menu button. Private Browsing can be locked with Face ID, Touch ID or the passcode. Page tinting in Safari 26 effectively **ignores `theme-color`**: Safari samples the page's CSS background, or a fixed or sticky element near the edge. A user toggle, "Allow Website Tinting", controls it.

### Cited Findings
- Hide Distracting Items flow: from the Smart Search field tap the Page Menu button, then Hide Distracting Items, tap an element, then tap Hide. Show Hidden Items restores them. "An eye with a line through it appears next to the Page Menu button" while items are hidden. It works best on items that don't change regularly. — [Apple Support 120682](https://support.apple.com/en-us/120682), [Apple Support iPhone guide](https://support.apple.com/guide/iphone/hide-distractions-when-browsing-iph7778f2888/ios) (via search extract) **[introduced iOS 18, still present in 26.4]**
- Hide animation: on iPhone/iPad "you tap an element, then tap Hide, and then see the animation" (type unspecified). — [Intego](https://www.intego.com/mac-security-blog/how-to-use-distraction-control-in-safari-to-remove-unwanted-web-page-elements/) (via search extract)
- Listen to Page can be used while in Reader view. — [HowToGeek](https://www.howtogeek.com/every-safari-user-should-change-these-settings-now/) (via search extract)
- Locked Private Browsing: Settings › Apps › Safari › "Require Face ID / Touch ID / Passcode to Unlock Private Browsing". It locks when not in use, possibly not immediately. Off by default on iPhone, on by default on macOS. — [Apple Support 105028](https://support.apple.com/en-us/105028), [iPhone guide](https://support.apple.com/en-in/guide/iphone/iphb01fc3c85/ios) (via search extract)
- Private visual difference: on iPad "the search field background is black instead of white". On Mac, a dark Smart Search field with white text. No iPhone 26 confirmation found. — [Apple Support](https://support.apple.com/en-in/guide/iphone/iphb01fc3c85/ios) (via search extract)
- Privacy Report summarizes trackers prevented by Intelligent Tracking Prevention on the current page and over the past 30 days. — [Apple Support](https://support.apple.com/en-in/guide/iphone/iphb01fc3c85/ios), [TechRadar](https://www.techradar.com/phones/iphone/5-smart-privacy-features-on-iphone-you-need-to-know-about) (via search extract)
- Tinting setting: iOS: Settings › Apps › Safari › Tabs › "Allow Website Tinting". macOS: "Show color in tab bar". — [andesco/safari-color-tinting](https://github.com/andesco/safari-color-tinting) (via search extract)
- Tint source in Safari 26: Safari "no longer uses" `theme-color` even though it still parses it. It tints from the background-color of a fixed or sticky element just below the top viewport edge, otherwise from the page background. Where nothing samples, it falls back to white, so html/body need an opaque background. At scroll position 0 it samples CSS background-color, not images or video. Some reports say it re-samples only on load. — [andesco](https://github.com/andesco/safari-color-tinting), [1ar.io](https://1ar.io/updates/safari-26-liquid-glass-web/), [Ben Frain](https://benfrain.com/ios26-safari-theme-color-tab-tinting-with-fixed-position-elements/), [grooovinger](https://grooovinger.com/notes/2026-02-27-safari-26-header-background) (via search extract; developer write-ups, not Apple docs)
- **[older]** Safari 15 introduced `theme-color` tinting, and it could be overridden via the meta tag. — [Use Your Loaf](https://useyourloaf.com/blog/safari-15-theme-color/) (via search extract)
- Legibility complaint: during the iOS 26 beta, Safari tinting toolbar colour from the page made bookmark text unreadable on some sites. — [MacRumors forums](https://forums.macrumors.com/threads/safari-changing-colour-of-tool-bar-text-unreadable.2458598/) (via search extract)
- Scroll edge effect (system): a "soft" blur-and-fade of content under floating bars is the iOS default. It switches to subtle dimming when dark content passes under. "Hard" style is for pinned headers, mostly on macOS. Use one effect per view. — [WWDC25 219](https://developer.apple.com/videos/play/wwdc2025/219/), [WWDC25 356](https://developer.apple.com/videos/play/wwdc2025/356/)

### Inferences
- For Vola, taking `theme-color` first and then falling back to sampling the top or bottom edge colour is a reasonable superset. Safari 26 has moved to sampling, so sampling should drive the tint. A user toggle like "Allow Website Tinting" is worth copying, and so is a white/neutral fallback.
- Badge pattern: when a page-level mode is active (hidden items, reader), show a small status glyph next to the page-menu button.

### Gaps
- No fetched source describes the Hide Distracting Items animation. It is widely remembered as a particle "disintegrate" effect from iOS 18, but that is unverified here.
- Listen to Page controls (where the mini player lives, speed/skip) are not documented in fetched sources for iOS 26.
- No iOS 26-specific Reader appearance changes were found.
- Fingerprinting protection specifics for Safari 26 were not found in fetched sources.

## 5. Typography (SF Pro, iOS Dynamic Type at the default "Large" size)

### Takeaway
Apple's official iOS text styles at the default size are listed below. Use these values to calibrate Vola's Inter scale, with body at 17. Tracking is size-specific: −0.43 pt at 17 pt, positive from 24 pt up. iOS 26 typography is "bolder and left-aligned" in alerts and onboarding.

### Cited Findings
- Default (Large) Dynamic Type, iOS/iPadOS. Style: weight, size/leading (pt), emphasized weight. — [HIG Typography](https://developer.apple.com/design/human-interface-guidelines/typography)
  - Large Title: Regular 34/41, Bold
  - Title 1: Regular 28/34, Bold
  - Title 2: Regular 22/28, Bold
  - Title 3: Regular 20/25, Semibold
  - Headline: **Semibold** 17/22, Semibold
  - Body: Regular 17/22, Semibold
  - Callout: Regular 16/21, Semibold
  - Subhead: Regular 15/20, Semibold
  - Footnote: Regular 13/18, Semibold
  - Caption 1: Regular 12/16, Semibold
  - Caption 2: Regular 11/13, Semibold
- Larger steps for scaling: xLarge Body 19/24, xxLarge Body 21/26, xxxLarge Body 23/29. Every style moves about +2 pt per step. — [HIG Typography](https://developer.apple.com/design/human-interface-guidelines/typography)
- SF Pro tracking (pt): 11 → +0.06, 12 → 0, 13 → −0.08, 15 → −0.23, 16 → −0.31, **17 → −0.43**, 20 → −0.45, 22 → −0.26, 24 → +0.07, 28 → +0.38, 34 → +0.40. In 1/1000 em: 17 pt = −26, 13 pt = −6, 34 pt = +12. — [HIG Typography](https://developer.apple.com/design/human-interface-guidelines/typography)
- iOS 26: "Typography has been refined to strengthen clarity and structure, now bolder and left-aligned to improve readability in key moments like alerts and onboarding." — [WWDC25 356](https://developer.apple.com/videos/play/wwdc2025/356/)
- Toolbar titles: keep them under 15 characters. Prefer symbols over text in bars. — [HIG Toolbars](https://developer.apple.com/design/human-interface-guidelines/toolbars), [WWDC25 356](https://developer.apple.com/videos/play/wwdc2025/356/)

### Inferences
- Inter is metrically larger than SF Pro at the same size. To match Safari's optical size, Inter body may need about 16–17 sp. Inter's own recommended negative tracking at text sizes is close to SF's −0.4 pt at 17.
- Bar text guess (not measured): the domain in the expanded field reads like Body or Callout (16–17 pt), and the collapsed pill like Footnote/Caption (12–13 pt).

### Gaps
- No source states which text style the Safari 26 address field uses, or its weight in the collapsed state.

## 6. Liquid Glass material: construction, variants, tint, shadows, shapes and concentricity

### Takeaway
Liquid Glass is a layered "meta-material". It lenses and refracts the content behind it, has specular highlights that move with motion and interaction, and has an adaptive shadow and adaptive tint and dynamic range. Small bars flip light/dark with the content behind them; large surfaces don't. There are two variants, **Regular** (default, adaptive) and **Clear** (more transparent, needs a dimming layer of about 35% over bright content). Shapes follow three types: fixed radius, **capsule** (radius = height/2) and **concentric** (child radius = parent radius − padding). Glass belongs only to the navigation layer, never glass on glass.

### Cited Findings
- Lensing: the material "dynamically bends, shapes, and concentrates light in real time", unlike older materials that "scattered light". Elements "materialize in and out by gradually modulating the light bending and lensing" instead of fading. — [WWDC25 219](https://developer.apple.com/videos/play/wwdc2025/219/)
- Layers adapt continuously: "As text scrolls underneath, shadows become more prominent". "The amount of tint and the dynamic range shift". The glass "can also independently switch between light and dark". — [WWDC25 219](https://developer.apple.com/videos/play/wwdc2025/219/)
- Size-dependent thickness: when glass grows (for example a menu from a toolbar button), it simulates "a thicker, more substantial material", with "deeper, richer shadows, … more pronounced lensing and refraction … softer scattering of light". — [WWDC25 219](https://developer.apple.com/videos/play/wwdc2025/219/)
- Highlights layer: light sources produce highlights that "respond to geometry". The lights move on lock/unlock and "in some cases … respond to device motion". — [WWDC25 219](https://developer.apple.com/videos/play/wwdc2025/219/)
- Shadow: opacity increases over text and decreases over a solid light background. — [WWDC25 219](https://developer.apple.com/videos/play/wwdc2025/219/)
- Touch feedback: the material "illuminates from within … Starting right under your fingertips, the glow spreads throughout the element and onto any Liquid Glass elements nearby". It has "gel-like flexibility". Elements "lift up into Liquid Glass temporarily" when interacted with. — [WWDC25 219](https://developer.apple.com/videos/play/wwdc2025/219/)
- Light/dark flipping: "Small elements like navbars and tabbars … flip from light to dark based on the background." "Bigger elements, like menus or sidebars … don't flip". Symbols and glyphs on glass flip to mirror it. — [WWDC25 219](https://developer.apple.com/videos/play/wwdc2025/219/)
- Variants: Regular "blurs and adjusts the luminosity of background content" and most system components use it. Clear is "highly translucent", for media backgrounds only. Over bright content, "consider adding a dark dimming layer of 35% opacity". Never mix variants. — [HIG Materials](https://developer.apple.com/design/human-interface-guidelines/materials), [WWDC25 219](https://developer.apple.com/videos/play/wwdc2025/219/)
- Tinting: "Selecting a color generates a range of tones that are mapped to content brightness underneath". Tint is not a solid opaque fill. Tint only primary actions, because "when every element is tinted, nothing stands out". — [WWDC25 219](https://developer.apple.com/videos/play/wwdc2025/219/)
- No glass on glass: use fills, transparency and vibrancy for elements on top of glass. Don't use glass in the content layer. — [WWDC25 219](https://developer.apple.com/videos/play/wwdc2025/219/), [HIG Materials](https://developer.apple.com/design/human-interface-guidelines/materials)
- Accessibility modifiers: Reduce Transparency makes glass "frostier". Increase Contrast gives "predominantly black or white" with "a contrasting border". Reduce Motion "decreases the intensity of some effects and disables any elastic properties". — [WWDC25 219](https://developer.apple.com/videos/play/wwdc2025/219/)
- iOS 26.1 user setting: Settings › Display & Brightness › Liquid Glass › **Clear** (default) / **Tinted**. Tinted "increases opacity and adds more contrast". Six Colors found the change modest. — [MacTrast](https://www.mactrast.com/2025/10/fourth-ios-26-1-ipados-26-1-and-macos-tahoe-26-1-betas-allow-users-to-control-liquid-glass-shine/amp/), [Six Colors](https://sixcolors.com/post/2025/11/soaping-up-liquid-glass-less-transparency-more-contrast/) (via search extract)
- Standard (non-glass) materials for the content layer: ultraThin, thin, regular (default), thick. — [HIG Materials](https://developer.apple.com/design/human-interface-guidelines/materials)
- Shapes: "fixed shapes have a constant corner radius. Capsules use a radius that's half the height of the container. And concentric shapes calculate their radius by subtracting padding from the parent's." Use a concentric shape "with a fallback radius" for components that can stand alone. Avoid corners that feel "pinched or flared". — [WWDC25 356](https://developer.apple.com/videos/play/wwdc2025/356/)
- Bordered buttons are capsules by default in iOS 26. Bar-item groups share one glass background. Text buttons go in their own containers, never grouped with symbols. Done is a tinted (blue) checkmark. — [WWDC25 323](https://developer.apple.com/videos/play/wwdc2025/323/), [WWDC25 356](https://developer.apple.com/videos/play/wwdc2025/356/)
- Default custom glass shape is a capsule (UIKit `UIGlassEffect`, SwiftUI `glassEffect`). `cornerConfiguration` customizes it. Glass "appears using a special materialize animation". — [WWDC25 284](https://developer.apple.com/videos/play/wwdc2025/284/), [WWDC25 323](https://developer.apple.com/videos/play/wwdc2025/323/)
- Prefer circular or capsule buttons. Capsules suit horizontal rows and rounded rectangles suit vertical stacks. — [HIG Buttons](https://developer.apple.com/design/human-interface-guidelines/buttons)
- **[iOS 27]** Apple reduced default transparency and "added a darkened edge around Liquid Glass elements, along with brighter specular highlights". It added a transparency slider from "ultra clear to fully tinted" and changed sidebar corners on iPadOS/macOS. — [MacRumors](https://www.macrumors.com/2026/06/10/how-liquid-glass-is-changing-in-ios-27/), [Wikipedia](https://en.wikipedia.org/wiki/Liquid_Glass) (via search extract)
- NN/g criticized iOS 26 Liquid Glass usability (legibility, cramped tab bars). — [NN/g](https://www.nngroup.com/articles/liquid-glass/) (via search extract)

### Inferences
- Android recipe (approximation, since Apple's shader isn't public): RenderEffect blur over the content under the bar (API 31+), a luminance-adaptive tint layer, a 1 px inner specular stroke with a gradient highlight, and an adaptive shadow (stronger over text). Flip glyph colour by sampled background luminance. On older APIs, or with Reduce Transparency, use a frosted opaque fallback. Follow iOS 27 and default to *less* transparency with a darker edge.
- Token rules to adopt: `radius.capsule = height/2`, `radius.concentric(parent, padding) = parent − padding` with a fallback, a dimming scrim of 0.35 for clear glass on bright media, and one soft scroll-edge fade under each floating bar.

### Gaps
- Apple publishes no blur radius, refraction strength, tint alpha or shadow values for Liquid Glass.

## 7. Motion and haptics

### Takeaway
Apple's spring vocabulary is duration plus bounce. The defaults are `spring(response: 0.5, dampingFraction: 0.825)` and `spring(duration: 0.5, bounce: 0)`, with presets smooth (bounce 0), snappy (small bounce, about 0.15) and bouncy (about 0.3). Interactive drags use `interactiveSpring(response: 0.15, dampingFraction: 0.86, blendDuration: 0.25)`. Liquid Glass motion is "gel-like": it morphs instead of fading, menus pop from their source, and a Reduce Motion setting removes the elasticity. Haptics come in three families: Impact (light, medium, heavy, rigid, soft), Selection, and Notification.

### Cited Findings
- `static func spring(response: Double = 0.5, dampingFraction: Double = 0.825, blendDuration: TimeInterval = 0)`. — [SwiftUI docs](https://developer.apple.com/documentation/swiftui/animation/spring(response:dampingfraction:blendduration:))
- `static func spring(duration: TimeInterval = 0.5, bounce: Double = 0.0, blendDuration: Double = 0)`. — [SwiftUI docs](https://developer.apple.com/documentation/swiftui/animation/spring(duration:bounce:blendduration:))
- `smooth/snappy/bouncy(duration: TimeInterval = 0.5, extraBounce: Double = 0.0)`: smooth has "no bounce", snappy a "small amount of bounce", bouncy a "higher amount of bounce". — [SwiftUI smooth](https://developer.apple.com/documentation/swiftui/animation/smooth(duration:extrabounce:)), [snappy](https://developer.apple.com/documentation/swiftui/animation/snappy(duration:extrabounce:)), [bouncy](https://developer.apple.com/documentation/swiftui/animation/bouncy(duration:extrabounce:))
- `interactiveSpring(response: 0.15, dampingFraction: 0.86, blendDuration: 0.25)`, "intended for driving interactive animations". — [SwiftUI docs](https://developer.apple.com/documentation/swiftui/animation/interactivespring(response:dampingfraction:blendduration:))
- WWDC23 examples: 0.15 bounce is "barely bouncy" and 0.3 is "noticeably bouncy", both at 0.5 s. Bounce ranges from −1 to 1. A third-party table lists snappy as 0.3 s/0.15 (unconfirmed by Apple). — [WWDC23 "Animate with springs"](https://developer.apple.com/videos/play/wwdc2023/10158/) (via search extract)
- Liquid Glass motion: "instantly flexing and energizing with light" on touch. Controls "continually shape shift" between contexts. Menus: "the bubble simply pops open". Action sheets now "spring from the action itself". — [WWDC25 219](https://developer.apple.com/videos/play/wwdc2025/219/), [WWDC25 356](https://developer.apple.com/videos/play/wwdc2025/356/)
- Sheets: dragging up makes glass "recede, becoming more opaque and gently growing in size". Modal tasks pair glass with a dimming layer. — [WWDC25 356](https://developer.apple.com/videos/play/wwdc2025/356/)
- Motion principles: be brief and precise, avoid motion on frequent interactions, let people cancel motion, and make motion optional. Touch interaction gets "greater emphasis" than trackpad. — [HIG Motion](https://developer.apple.com/design/human-interface-guidelines/motion)
- Haptics: Impact light, medium, heavy, rigid, soft ("a tap when a view snaps into place"). Selection ("values of a UI element are changing"). Notification (success/warning/error outcome). — [HIG Playing haptics](https://developer.apple.com/design/human-interface-guidelines/playing-haptics)

### Inferences
- Mapping to Compose: `dampingRatio = 1 − bounce` (for bounce ≥ 0) and stiffness = (2π/duration)². So duration 0.5 / bounce 0 gives dampingRatio 1.0 and stiffness ≈ 158. Snappy (about 0.3 s/0.15) gives about 0.85 and ≈ 439. Bouncy (0.5 s/0.3) gives 0.7 and ≈ 158. Response 0.5/damping 0.825 gives stiffness ≈ 158 and dampingRatio 0.825.
- Likely haptic mapping for a browser (not documented for Safari): Selection tick per tab step on swipe-to-switch, light/soft Impact when the bar snaps or a new tab is created past the last tab, and the system long-press haptic for context menus.

### Gaps
- No source documents the specific springs or haptics Safari uses for tab swipe, bar collapse or long-press.

## 8. iPadOS 26 Safari layout

### Takeaway
iPadOS 26.0 shipped only the **Separate** layout: a Liquid Glass address bar and toolbar above a separate tab strip, plus an inset glass sidebar with tab groups, iCloud Tabs and Saved. iPadOS 26.4 (March 2026) restored the **Compact Tab Bar** option, where address and tabs share one row.

### Cited Findings
- The Compact option was missing in iPadOS 26 / macOS 26.0. iPad got "a distinct address/toolbar above a separate tab strip" with Liquid Glass styling. — [allthings.how](https://allthings.how/safaris-compact-tabs-removed-on-macos-26-and-ipados-26-what-changed/) (via search extract)
- iPadOS 26.4 / macOS 26.4 brought Compact back. Settings › Apps › Safari › Tabs offers "Compact Tab Bar" and "Separate Tab Bar". — [9to5Mac](https://9to5mac.com/2026/03/26/macos-tahoe-26-4-and-ipados-26-4-add-compact-tab-bar-in-safari/), [MacRumors](https://www.macrumors.com/how-to/safaris-compact-tab-bar-back-on-mac-and-ipad/) (via search extract)
- The sidebar on iPad/Mac was overhauled with new iCloud Tabs and Saved sections. Tab groups live there. With the sidebar minimized on an 11" iPad the groups feel hidden. — [MacRumors](https://www.macrumors.com/guide/ios-26-safari-features/), [ByteBits](https://kalebcadle.substack.com/p/ipados-26-changes-the-way-i-use-safari) (via search extract)
- System: sidebars are "inset and built with Liquid Glass", and content and scroll views extend behind them ("background extension effect"). Glass controls "nest perfectly into the rounded corners of windows". Light from nearby colourful content "spill[s] onto its surface". Sidebar and tab bar form "a single navigational element that fluidly scales". — [WWDC25 356](https://developer.apple.com/videos/play/wwdc2025/356/), [WWDC25 219](https://developer.apple.com/videos/play/wwdc2025/219/)
- iPad search placement: top trailing in the toolbar. — [WWDC25 323](https://developer.apple.com/videos/play/wwdc2025/323/)
- **[older]** Safari 15 iPad Compact Tab Bar plus "Show Color in Tab Bar". — [AppleInsider](https://appleinsider.com/articles/21/10/19/safaris-color-tab-bar-restricted-to-compact-view-in-macos-monterey-ipados-151) (via search extract)
- **[iOS 27]** Changed sidebar corners on iPadOS. — [Wikipedia](https://en.wikipedia.org/wiki/Liquid_Glass) (via search extract)

### Inferences
- For Vola on Expanded width: a top glass toolbar with Back/Forward and sidebar toggle on the leading edge, a centered address field, Share/New Tab/Tabs on the trailing edge, a tab strip below, and an inset floating sidebar for profiles and tab groups. Offer an option that merges the toolbar and tab strip, iPad-Compact style. Use concentric radii relative to the window corners.

### Gaps
- No measured iPad toolbar height, tab-strip tab width rules or sidebar width were found.
