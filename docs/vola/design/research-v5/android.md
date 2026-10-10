# Android-платформа для Safari-подобного интерфейса AAA-уровня (Compose + GeckoView, Android 12–16, 2025–2026)

> Методика: первичные источники прочитаны напрямую. Исходники AndroidX (токены движения) взяты через raw.githubusercontent.com. Документация Haze и исходники GeckoView получены sparse-клоном репозиториев `chrisbanes/haze` (HEAD от 2026-10-10, последний тег 2.0.1) и `mozilla-firefox/firefox` (HEAD от 2026-10-10; в проекте Vola GeckoView `157.0.20261005135250`). Страницы developer.android.com загружены через curl. Страница source.android.com через прокси была недоступна (403), mozilla.github.io и chrisbanes.github.io не резолвились. Утверждения из общих знаний без источника вынесены в «Выводы» или «Пробелы».

## 1. Движение Material 3 Expressive: MotionScheme, пружины, морфинг формы, LoadingIndicator

### Главное
`MotionScheme.standard()` и `MotionScheme.expressive()` различаются только **пространственными** пружинами: у standard затухание 0.9 и жёсткость 1400/700/300 (fast/default/slow), у expressive — затухание 0.6/0.8/0.8 и жёсткость 800/380/200. Пружины **эффектов** (цвет, прозрачность) в обеих схемах одинаковые: затухание 1.0, жёсткость 3800/1600/800. В material3 1.5 (сейчас 1.5.0-beta01) MotionScheme выведен из экспериментальных, а `LoadingIndicator` и `MaterialShapes` остаются экспериментальными.

### Источники и находки
- Токены Expressive (дословно): `SpringDefaultSpatialDamping = 0.8f`, `SpringDefaultSpatialStiffness = 380.0f`, `SpringFastSpatialDamping = 0.6f`, `SpringFastSpatialStiffness = 800.0f`, `SpringSlowSpatialDamping = 0.8f`, `SpringSlowSpatialStiffness = 200.0f`; эффекты — `Damping = 1.0f` и жёсткость `1600` (default), `3800` (fast), `800` (slow) — [ExpressiveMotionTokens.kt, androidx-main](https://raw.githubusercontent.com/androidx/androidx/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/tokens/ExpressiveMotionTokens.kt)
- Токены Standard (дословно): пространственные `Damping = 0.9f` у всех, жёсткость `700` (default), `1400` (fast), `300` (slow); эффекты — `1.0f` и `1600/3800/800` — [StandardMotionTokens.kt, androidx-main](https://raw.githubusercontent.com/androidx/androidx/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/tokens/StandardMotionTokens.kt)
- Для сравнения со стандартными константами Compose: `Spring.DampingRatioLowBouncy = 0.75`, `DampingRatioNoBouncy = 1f`, `StiffnessMedium = 1500f` — [Spring API (через поисковую выдачу)](https://developer.android.com/reference/kotlin/androidx/compose/animation/core/Spring)
- material3 **1.4.0** стабилен с 24.09.2025: «Material 3 components are now using the new MotionScheme to define their motion»; вышли SearchBar/ExpandedFullScreenSearchBar/SearchBarState, `Text` с autoSize, SecureTextField. API с `ExperimentalMaterial3ExpressiveApi` из стабильной 1.4 убрали: «please switch to 1.5.0-alpha» — [Compose Material3 release notes](https://developer.android.com/jetpack/androidx/releases/compose-material3)
- Линия **1.5**: последняя версия — `1.5.0-beta01` от 07.10.2026; стабильная — 1.4.0. По ходу альф 1.5 из экспериментальных вышли: «Graduate motion scheme from experimental» (около 1.5.0-alpha15), Expressive FloatingToolbar (около alpha22), TopAppBar/FlexibleBottomAppBar (около alpha23), ButtonGroup, SplitButton, ToggleButtons, FAB Menu, слайдеры, TopAppBarScrollBehavior (принимает `ScrollableState`). Одновременно отменили стабилизацию: «Revert MaterialShapes and LoadingIndicator promotions to stable» (около alpha19). `LocalMotionScheme` удалён, вместо него `MaterialTheme.motionScheme` или `MotionTheme.LocalMotionScheme` для Modifier.Node — [Compose Material3 release notes](https://developer.android.com/jetpack/androidx/releases/compose-material3)
- `MotionScheme.standard()` и `MotionScheme.expressive()` — фабрики в companion object (раньше назывались standardMotionScheme/expressiveMotionScheme). Схема задаётся через `MaterialTheme`. BottomSheet использует fast-effects при скрытии и default-spatial при раскрытии и учитывает `motionScheme` при вложенной прокрутке и перетаскивании — [Compose Material3 release notes](https://developer.android.com/jetpack/androidx/releases/compose-material3)
- `PullToRefreshDefaults.loadingIndicatorColor` и `loadingIndicatorContainerColor` снова экспериментальные. API `PolygonShape` в 1.5.0-beta01 переделан: `CornerRounding` вынесен на верхний уровень, появились `transform {}` и `copy()` — [Compose Material3 release notes](https://developer.android.com/jetpack/androidx/releases/compose-material3)
- В Vola сейчас `compose-material3 = "1.5.0-alpha29"`, `compose-bom = "2026.09.00"` — `gradle/libs.versions.toml` (репозиторий проекта).

### Выводы
- Фирменное «Safari-ощущение» (плавно и почти без перелёта) ближе к **standard** (ζ = 0.9). Expressive с ζ = 0.6 у fast заметно пружинит, это стиль Pixel. Практичный вариант: свои токены в `ui/theme/motion`. Например, для перемещения панелей ζ ≈ 0.85–0.9 и жёсткость ≈ 380–700, для прозрачности и цвета — пружины эффектов (ζ = 1.0, жёсткость 1600/3800). Expressive (ζ = 0.6–0.8) — только для «фирменных моментов».
- Переход на 1.5.0-beta01 безопасен для MotionScheme, FloatingToolbar и FlexibleBottomAppBar. От `LoadingIndicator` и `MaterialShapes`/морфинга формы нужно ждать изменений API (opt-in остаётся).
- Схемы различаются только пространственными пружинами, поэтому для режима «системные анимации отключены» достаточно оставить пружины эффектов (только прозрачность), как требует CLAUDE.md.

### Пробелы
- Точные параметры морфинга в `LoadingIndicator` (набор полигонов, период) и объём `MaterialShapes` в 1.5.0-beta01 не проверены: страница API не загружалась целиком.
- Номера альф 1.5 для отдельных стабилизаций восстановлены по положению строк в release notes (±1 альфа).

## 2. Размытие и стекло на Android: RenderEffect, межоконное размытие, Haze, SurfaceView и запасные варианты

### Главное
Размыть **то, что лежит под панелью**, можно только если эти пиксели нарисованы в том же слое Compose/RenderNode. `SurfaceView` (GeckoView по умолчанию) рисует в отдельной поверхности, поэтому ни Haze, ни `RenderEffect` её содержимое не видят. Межоконное размытие Android 12+ работает между окнами, а не внутри одного окна, и может отключиться в любой момент (экономия батареи, туннелирование мультимедиа). Надёжная основа — полупрозрачная тонированная панель с цветом из `theme-color` или с выборкой цвета. Настоящее размытие — только над UI самого приложения: обзор вкладок, листы, стартовая страница.

### Источники и находки
- `RenderEffect.createBlurEffect(radiusX, radiusY, TileMode)` (API 31) размывает **содержимое RenderNode, на котором установлен**, то есть делает размытие переднего плана. Есть варианты с `inputEffect`, `createBitmapEffect`, `createChainEffect` — [RenderEffect](https://developer.android.com/reference/android/graphics/RenderEffect); в Android 12 появилось как `view.setRenderEffect(RenderEffect.createBlurEffect(...))` — [Android 12 features](https://developer.android.com/about/versions/12/features)
- Межоконное размытие: `WindowManager.LayoutParams.setBlurBehindRadius(int)` «Blurs the screen behind the window… Requires FLAG_BLUR_BEHIND to be set». `Window.setBackgroundBlurRadius(int)` «blurs only within the bounds of the window». «Cross-window blur might not be supported by some devices due to GPU limitations. It can also be disabled at runtime, e.g. during battery saving mode, when multimedia tunneling is used or when minimal post processing is requested» — [WindowManager.LayoutParams](https://developer.android.com/reference/android/view/WindowManager.LayoutParams); `WindowManager.isCrossWindowBlurEnabled()` (+ слушатель изменений) — [WindowManager](https://developer.android.com/reference/android/view/WindowManager)
- **Haze** (chrisbanes, версия 2.0.1): на Android 13+ (API 33+) — «optimal environment for all use cases». На 12/12L (31/32) — обходные пути: прогрессивное размытие в Quality рисуется многократно, в Balanced — через маску. На Android 12 и ниже RenderNode не перерисовывается при изменении контента, поэтому Haze принудительно инвалидирует слои через глобальный `ViewTreeObserver` pre-draw, и это вызывает лишние инвалидации. На Android 11 и ниже по умолчанию **scrim** (полупрозрачный слой). Экспериментальный RenderScript медленный, работает в фоновом потоке и отстаёт на кадр и больше — [Haze docs/blur/platforms.md](https://github.com/chrisbanes/haze/blob/main/docs/blur/platforms.md)
- Haze и SurfaceView: «A `SurfaceView` owns a separate Android surface, so its pixels are not present when Haze captures the Compose source layer». Совет — переключиться на `TextureView` (CameraX `COMPATIBLE`, ExoPlayer TextureView): «The same constraint applies to video players and other platform views» — [Haze docs/scenarios/camera.md](https://github.com/chrisbanes/haze/blob/main/docs/scenarios/camera.md)
- Режимы Haze 2.x: `HazePerformanceMode.Default` (= `Balanced` = `Fixed(0.5f)`), `Performance` (`Fixed(0f)`), `Quality` (`Fixed(1f)`). Совет: «Reduce the affected area», использовать маску вместо прогрессивного размытия, если нужно лишь затухание. Стабильный источник позволяет повторно использовать уже обработанный результат, а прокрутка требует свежего ввода — [Haze docs/performance.md](https://github.com/chrisbanes/haze/blob/main/docs/performance.md)
- Цифры Haze (Pixel 8a, Android 17 / SDK 37.2, 60 Гц; P90 времени кадра CPU / P90 перерасхода, мс). Blur: стабильный источник 4.60/−6.85 (Quality), 4.67/−6.93 (Balanced), 4.58/−7.02 (Performance); меняющийся источник 5.49/−4.91, 5.35/−5.50, 5.47/−5.70. Glass, меняющийся источник: 4.45/−6.95 (Quality), 4.27/−8.08 (Balanced), 4.08/−8.99 (Performance). Performance расходует примерно на 9% меньше пиковой памяти GPU (≈100 МБ против ≈110 МБ). Оговорка авторов: время CPU «does not measure GPU shader cost» — [Haze docs/benchmark-results.md](https://github.com/chrisbanes/haze/blob/main/docs/benchmark-results.md)
- Нативный `HazeInput.Backdrop` фильтрует пиксели, уже нарисованные в окне. Требует «hardware-accelerated **Android 37.2** window», по умолчанию выключен (`HazeFeatureFlags.isPlatformBackdropEnabled`). Не может «cross a dialog, popup, or window boundary». На Pixel 8a P90 времени кадра CPU был на **23–46% выше**, чем у Sources — [Haze docs/performance.md](https://github.com/chrisbanes/haze/blob/main/docs/performance.md), [docs/core-concepts.md](https://github.com/chrisbanes/haze/blob/main/docs/core-concepts.md)
- `Modifier.blur` в Compose размывает сам узел, а Haze — фон под узлом — [Haze docs/blur/faq.md](https://github.com/chrisbanes/haze/blob/main/docs/blur/faq.md)

### Выводы
- Для Vola на Android 12–16 настоящее стекло **поверх веб-страницы** с GeckoView на SurfaceView невозможно. Варианты: (а) переключить GeckoView на `BACKEND_TEXTURE_VIEW` (см. раздел 3: хуже по производительности; плюс копия кадра в GPU-текстуру и, вероятно, больше памяти); (б) имитировать стекло: полупрозрачная заливка `theme-color` или усреднённого цвета верхнего/нижнего края страницы (например, по `capturePixels` в уменьшенном масштабе по событию, а не каждый кадр) с тонкой границей и тенью. Вариант (б) соответствует решению владельца «панель над страницей — в цвете сайта».
- Нативный Backdrop из Haze на Android 17 (SDK 37.2) тоже не поможет со SurfaceView: он фильтрует пиксели окна приложения, а SurfaceView компонуется SurfaceFlinger'ом отдельно. Это вывод, не проверено.
- Haze уместен для UI самого приложения (обзор вкладок поверх миниатюр-битмапов, листы, стартовая страница с обоями) на API 31+. На API ≤ 30 — только scrim. minSdk Vola проверить: если 26–30, нужен запасной путь.
- Обязателен слушатель `isCrossWindowBlurEnabled` и запасной путь без размытия для режима экономии батареи. Это прямо требует документация.

### Пробелы
- Конкретная стоимость `RenderEffect.createBlurEffect` в мс GPU по радиусам в первичных источниках Android не найдена. Цифры Haze измеряют CPU, а не шейдеры.
- Документация AOSP по межоконному размытию (source.android.com/docs/core/display/window-blurs) была недоступна через прокси (403).

## 3. Особенности GeckoView: поверхность, снимки, динамическая панель, theme-color, overscroll

### Главное
GeckoView по умолчанию рисует через `SurfaceView` («best performance at the price of not being able to animate GeckoView»). API называется `setViewBackend(BACKEND_SURFACE_VIEW | BACKEND_TEXTURE_VIEW)`; `setSurfaceViewEnabled` в текущем API нет. Схлопывающуюся нижнюю панель поддерживают `setDynamicToolbarMaxHeight`, `setVerticalClipping` и `ContentDelegate.onShowDynamicToolbar`. Снимки вкладок делают `capturePixels()`, `captureFullPage()` и `ScreenshotBuilder` (масштаб, переиспользование битмапа). Отдельного колбэка для `<meta name="theme-color">` нет: только `onWebAppManifest` (theme_color из манифеста), поэтому meta theme-color берётся через WebExtension/content script.

### Источники и находки
- «`BACKEND_SURFACE_VIEW` … offers the best performance at the price of not being able to animate GeckoView». «`BACKEND_TEXTURE_VIEW` … offers worse performance … but allows you to animate GeckoView or to paint a GeckoView on top of another GeckoView». «By default, GeckoView will use a SurfaceView» (`setViewBackend(int)`) — [GeckoView.java, mozilla-firefox/firefox main](https://github.com/mozilla-firefox/firefox/blob/main/mobile/android/geckoview/src/main/java/org/mozilla/geckoview/GeckoView.java)
- `setDynamicToolbarMaxHeight(int height)`: «Set the maximum height of the dynamic toolbar(s). If there are two or more dynamic toolbars, the height value should be the total amount». `setVerticalClipping(int clippingHeight)`: «Update the amount of vertical space that is clipped or visibly obscured in the bottom portion of the view. Tells gecko where to put bottom fixed elements so they are fully visible… in screen pixels», только на UI-потоке. Изменение max height сбрасывает clipping в 0 — [GeckoView.java](https://github.com/mozilla-firefox/firefox/blob/main/mobile/android/geckoview/src/main/java/org/mozilla/geckoview/GeckoView.java)
- `ContentDelegate.onShowDynamicToolbar(GeckoSession)`: «The app should display its dynamic toolbar, fully expanded to the height that was previously specified via setDynamicToolbarMaxHeight» — [GeckoSession.java](https://github.com/mozilla-firefox/firefox/blob/main/mobile/android/geckoview/src/main/java/org/mozilla/geckoview/GeckoSession.java)
- `capturePixels()`: «Returned Bitmap will have the same dimensions as the Surface… If the GeckoSession#isCompositorReady is false the GeckoResult will complete with an IllegalStateException… must be called on the UI thread»; реализовано как `screenshot().capture()`. `captureFullPage()` захватывает и области вне экрана. В `GeckoDisplay.ScreenshotBuilder` есть режимы SCALE/ASPECT/FULL/RECYCLE (выходной размер, переиспользование битмапа) — [GeckoDisplay.java](https://github.com/mozilla-firefox/firefox/blob/main/mobile/android/geckoview/src/main/java/org/mozilla/geckoview/GeckoDisplay.java)
- `ContentDelegate.onWebAppManifest(session, JSONObject)`: «The various colors (theme_color, background_color, etc.) present in the manifest have been transformed into #AARRGGBB format». Других колбэков с theme-color в GeckoSession нет: поиск `themeColor|theme_color` находит только манифест — [GeckoSession.java](https://github.com/mozilla-firefox/firefox/blob/main/mobile/android/geckoview/src/main/java/org/mozilla/geckoview/GeckoSession.java). В Vola meta theme-color уже читается встроенным расширением (`candy_privacy`, поле `themeColor` в `GeckoPrivacyHostRuntime.kt`).
- Другие полезные колбэки ContentDelegate: `onFirstContentfulPaint`, `onPaintStatusReset` (вместе показывают, когда на экране есть контент, например чтобы убрать заглушку или снимок), `onMetaViewportFitChange` (viewport-fit=cover для safe area), `onPreviewImage` — [GeckoSession.java](https://github.com/mozilla-firefox/firefox/blob/main/mobile/android/geckoview/src/main/java/org/mozilla/geckoview/GeckoSession.java)
- Overscroll и pull-to-refresh: `GeckoSession.getOverscrollEdgeEffect()`; `PanZoomController.onTouchEventForDetailResult(MotionEvent)` возвращает `InputResultDetail` (`INPUT_RESULT_UNHANDLED / HANDLED / HANDLED_CONTENT / IGNORED`, `SCROLLABLE_FLAG_TOP/RIGHT/BOTTOM/LEFT`, `OVERSCROLL_FLAG_HORIZONTAL/VERTICAL`). Предупреждение: «highly recommended to only call this with ACTION_DOWN… Returning a GeckoResult for every touch event will generate a lot of allocations» — [PanZoomController.java](https://github.com/mozilla-firefox/firefox/blob/main/mobile/android/geckoview/src/main/java/org/mozilla/geckoview/PanZoomController.java)

### Выводы
- Схлопывающаяся нижняя панель в стиле Safari: держать GeckoView на всю высоту, вызвать `setDynamicToolbarMaxHeight(высота панели + нижний inset)` и при сдвиге панели передавать `setVerticalClipping(-смещение)`, чтобы `position: fixed; bottom: 0` у страницы оставались видимы. По `onShowDynamicToolbar` возвращать панель. Так GeckoView не меняет размер в каждом кадре, а relayout SurfaceView дорогой (вывод по документации; Fenix устроен так же, но в источниках это не проверено).
- Анимировать сам GeckoView (масштаб в обзор вкладок, «карточка» в предиктивном «назад») на SurfaceView нельзя. Стандартный приём: на время перехода заменить вид снимком `capturePixels` через ScreenshotBuilder в уменьшенном размере с RECYCLE. TextureView — только если владелец примет потерю производительности.
- Снимок вкладки асинхронный и требует готового композитора. Снимать при уходе со вкладки, хранить уменьшенный битмап (для приватных вкладок — только в памяти, по правилу 5 CLAUDE.md).
- Pull-to-refresh: на ACTION_DOWN запросить `InputResultDetail`; если страница не обработала жест и не может прокрутиться вверх (нет `SCROLLABLE_FLAG_TOP`), включить свой индикатор с тактильным `GESTURE_THRESHOLD_ACTIVATE`.

### Пробелы
- Стоимость `capturePixels` в мс по устройствам в официальных источниках не найдена: она зависит от размера поверхности, копия GPU→CPU. Нужен Macrobenchmark в CI.
- Официальных цифр разницы SurfaceView и TextureView в GeckoView (fps, память, батарея) нет: только формулировка «worse performance».

## 4. Предиктивный «назад», обязательный edge-to-edge (Android 15/16), конфликты жестов

### Главное
При targetSdk 36 на Android 16 системные анимации предиктивного «назад» включены по умолчанию, а `onBackPressed`/`KEYCODE_BACK` больше не приходят. Отказаться от edge-to-edge нельзя: `windowOptOutEdgeToEdgeEnforcement` отключён. Нижнюю зону домашнего жеста исключить нельзя вообще. Исключения для бокового «назад» ограничены **200 dp по вертикали** на край.

### Источники и находки
- Android 16, targetSdk 36: «predictive back system animations (back-to-home, cross-task, and cross-activity) are enabled by default. Additionally, `onBackPressed` is not called and `KeyEvent.KEYCODE_BACK` is not dispatched anymore». Временный отказ — `android:enableOnBackInvokedCallback="false"` — [Android 16 behavior changes](https://developer.android.com/about/versions/16/behavior-changes-16)
- Android 15: опция разработчика для анимаций убрана; системные анимации показываются приложениям, которые включили предиктивный «назад» целиком или для отдельной activity. В Android 16 есть `PRIORITY_SYSTEM_NAVIGATION_OBSERVER`: колбэк-наблюдатель не поглощает событие. В Compose — `PredictiveBackHandler { progress: Flow<BackEventCompat> -> … }` (activity-compose ≥ 1.8.0), отмена жеста приходит как `CancellationException` — [Predictive back guide](https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture)
- Edge-to-edge: «Edge-to-edge is enforced on Android 15 (API level 35) and higher once your app targets SDK 35». В Android 16 при targetSdk 36 атрибут `windowOptOutEdgeToEdgeEnforcement` «is deprecated and disabled» — [Edge-to-edge](https://developer.android.com/develop/ui/views/layout/edge-to-edge), [Android 16 behavior changes](https://developer.android.com/about/versions/16/behavior-changes-16)
- Панели системы: `enableEdgeToEdge()` делает их прозрачными, кроме трёхкнопочной навигации (там полупрозрачный scrim, `setNavigationBarContrastEnforced(false)` его снимает). «Don't create a translucent gesture navigation bar» — [Edge-to-edge](https://developer.android.com/develop/ui/views/layout/edge-to-edge)
- Android 16, targetSdk 36: на экранах с sw ≥ 600dp ограничения ориентации, изменения размера и соотношения сторон игнорируются. Отказ через `PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY` временный, в API 37 не работает — [Android 16 behavior changes](https://developer.android.com/about/versions/16/behavior-changes-16)
- Жесты: «Apps can't opt out of these gestures [home/quick switch]… Android 10 introduces `WindowInsets.getMandatorySystemGestureInsets()`». Для «назад» есть `View.setSystemGestureExclusionRects()` (API 29) — [Gesture navigation](https://developer.android.com/develop/ui/views/touch-and-input/gestures/gesturenav); «the system will put a limit of 200dp on the vertical extent of the exclusions it takes into account. The limit does not apply while the navigation bar is stickily hidden» — [View.setSystemGestureExclusionRects](https://developer.android.com/reference/android/view/View)

### Выводы
- Свайп по нижней адресной строке влево и вправо для смены вкладок (как в Safari) **совместим** с жестовой навигацией, если строка лежит выше `mandatorySystemGestureInsets.bottom`. Её отступ — `navigationBars` плюс зазор. Свайп вверх по строке для обзора вкладок конфликтует с домашним жестом, если начинается в зоне insets. Горизонтальные свайпы у краёв экрана конфликтуют с «назад»: исключать не больше 200 dp по высоте, лучше в зоне адресной строки.
- Предиктивный «назад» внутри приложения (закрытие листа, обзора вкладок, возврат по истории вкладки с карточкой-снимком) делается через `PredictiveBackHandler` с пружинами из раздела 1. Навигация «назад» по истории GeckoSession должна идти через `OnBackPressedCallback`, а не через `onBackPressed`.

### Пробелы
- Точные параметры системной анимации cross-activity (масштаб 0.9 и т. п.) в первичных источниках не найдены.
- Поля `BackEventCompat` (`progress`, `touchX/Y`, `swipeEdge`) по справочнику не сверены: это общие знания.

## 5. Тактильная отдача: HapticFeedbackConstants и HapticFeedbackType в Compose

### Главное
Набор Android 14 (API 34) — `GESTURE_THRESHOLD_ACTIVATE/DEACTIVATE`, `SEGMENT_TICK`, `SEGMENT_FREQUENT_TICK`, `TOGGLE_ON/OFF`, `DRAG_START`, `NO_HAPTICS` — даёт почти iOS-набор. В Compose `HapticFeedbackType` (ui 1.8+) есть 13 значений, но **нет** `GestureStart`, `GestureThresholdDeactivate`, `DragStart`, `ClockTick`. Для них нужен `View.performHapticFeedback` с проверкой SDK.

### Источники и находки
- API-уровни констант: `LONG_PRESS` 3, `VIRTUAL_KEY` 5, `KEYBOARD_TAP` 8, `CLOCK_TICK` 21, `CONTEXT_CLICK` 23, `KEYBOARD_PRESS`/`KEYBOARD_RELEASE`/`VIRTUAL_KEY_RELEASE`/`TEXT_HANDLE_MOVE` 27, `CONFIRM`/`REJECT`/`GESTURE_START`/`GESTURE_END` 30, `GESTURE_THRESHOLD_ACTIVATE`/`GESTURE_THRESHOLD_DEACTIVATE`/`SEGMENT_TICK`/`SEGMENT_FREQUENT_TICK`/`TOGGLE_ON`/`TOGGLE_OFF`/`DRAG_START`/`NO_HAPTICS` 34. `FLAG_IGNORE_GLOBAL_SETTING` устарел в 33: «only privileged apps can ignore user settings». Описание THRESHOLD: «swipe/drag-style gesture, such as pull-to-refresh, where the gesture action is "eligible" at a certain threshold of movement» — [HapticFeedbackConstants](https://developer.android.com/reference/android/view/HapticFeedbackConstants)
- Compose `HapticFeedbackType`: `LongPress`, `TextHandleMove` (ui 1.0.0); `Confirm`, `ContextClick`, `GestureEnd`, `GestureThresholdActivate`, `Reject`, `SegmentFrequentTick`, `SegmentTick`, `ToggleOff`, `ToggleOn` (1.8.0); `KeyboardTap` (1.9.0); `VirtualKey` — [HapticFeedbackType](https://developer.android.com/reference/kotlin/androidx/compose/ui/hapticfeedback/HapticFeedbackType)

### Выводы
- Сопоставление для «фирменных моментов»: порог pull-to-refresh или свайпа вкладки — `GESTURE_THRESHOLD_ACTIVATE` (возврат за порог — `…_DEACTIVATE` через View); перелистывание вкладок в обзоре — `SEGMENT_TICK`; закрытие вкладки свайпом — `CONFIRM`; ошибка или запрет — `REJECT`; переключатели блокировщика — `TOGGLE_ON/OFF`; перетаскивание вкладки — `DRAG_START`. На API 31–33 нужен запасной вариант (например, `CLOCK_TICK`/`CONTEXT_CLICK`/`LONG_PRESS`). Обёртку держать в одном токен-слое `ui/theme`. Как Compose обрабатывает новые типы на старых API, на странице не описано.

### Пробелы
- Поведение Compose `HapticFeedbackType.SegmentTick` и др. на API < 34 (молча ничего не делает или заменяет эффектом) в справочнике не описано.

## 6. Типографика: Inter Variable, нелинейное масштабирование шрифта до 200%, sp и dp

### Главное
С Android 14 шрифт масштабируется до 200% **нелинейно**: крупный текст растёт медленнее мелкого. Тексты задаются в sp, отступы и высоты — никогда в sp.

### Источники и находки
- «Starting in Android 14, the system supports font scaling up to 200%… the system applies a nonlinear scaling curve… large text doesn't scale at the same rate as smaller text». «Don't use sp units for padding or define view heights assuming implicit padding: with nonlinear font scaling sp dimensions might not be proportional, so 4sp + 20sp might not equal 24sp» — [Android 14 features](https://developer.android.com/about/versions/14/features)
- Android 16 при targetSdk 36 игнорирует `elegantTextHeight` («UI fonts» выводятся из употребления). Затронуты арабское, тайское, тамильское и другие письма — [Android 16 behavior changes](https://developer.android.com/about/versions/16/behavior-changes-16)

### Выводы
- Основной текст 17 sp по шкале Safari при 200% не удвоится (нелинейная кривая). Высоты адресной строки и строк списков задавать в dp с `heightIn(min = …)`, чтобы при крупном шрифте они росли, а не обрезали текст. Проверять превью при `fontScale = 2f`.
- Inter Variable не содержит арабского, тайского и т. п.: для них Android подставит системный шрифт. Связь с `elegantTextHeight` важна для высоты строк.

### Пробелы
- Размер файла Inter Variable (TTF/WOFF2, с курсивом и без) по первичному источнику не подтверждён: GitHub API rsms/inter не вернул ассеты. Нужно измерить при добавлении в `res/font` и учесть в бюджете размера APK.

## 7. Доступность: цели касания, контраст, действия TalkBack

### Главное
Минимальная цель касания — 48×48 dp. Контраст текста — 4.5:1 (меньше 18 sp, или жирный меньше 14 sp) и 3:1 для остального.

### Источники и находки
- «touch target size, of at least 48dpx48dp. Larger is even better». «If the text is smaller than 18sp, or if the text is bold and smaller than 14sp… color contrast ratio of at least 4.5:1. For all other text… at least 3:1» — [Make apps more accessible](https://developer.android.com/guide/topics/ui/accessibility/apps)

### Выводы
- Функции, доступные только жестами (свайп вкладок по адресной строке, свайп для закрытия, перетаскивание), нужно продублировать через `Modifier.semantics { customActions = listOf(CustomAccessibilityAction("Следующая вкладка") { … }) }`. Строки — в `values/` и `values-ru/`.
- Полупрозрачные панели в цвете `theme-color`: контраст текста и иконок нужно считать по фактическому смешанному цвету и при нехватке переключать светлый/тёмный передний план или усиливать непрозрачность заливки.

### Пробелы
- Страница Compose про `customActions` и рекомендации TalkBack для жестов не загружена: API указан по общим знаниям.

## 8. Производительность: бюджет кадра, Baseline Profiles, плавность над GeckoView

### Главное
Baseline Profiles ускоряют выполнение кода примерно на 30% с первого запуска. Haze при 60 Гц на Pixel 8a укладывается в 4–5.5 мс CPU на кадр с запасом 5–10 мс, но при 120 Гц бюджет вдвое меньше.

### Источники и находки
- «Baseline Profiles improve code execution speed by about 30% from the first launch» — [Baseline Profiles overview](https://developer.android.com/topic/performance/baselineprofiles/overview)
- Для измерения Haze рекомендует Macrobenchmark: frame overrun (положительное значение — пропуск) и CPU frame duration (не измеряет шейдеры GPU). Тестировать прокрутку, переходы и **первое появление** эффекта; «smooth scrolling does not rule out a pause when a Glass surface is first created» — [Haze docs/performance.md](https://github.com/chrisbanes/haze/blob/main/docs/performance.md)
- GeckoView на SurfaceView даёт лучшую производительность, TextureView — хуже — [GeckoView.java](https://github.com/mozilla-firefox/firefox/blob/main/mobile/android/geckoview/src/main/java/org/mozilla/geckoview/GeckoView.java)

### Выводы
- Бюджет кадра: 16.7 мс при 60 Гц, 11.1 мс при 90 Гц, 8.3 мс при 120 Гц. Это арифметика, а не цифра из источника. Цифры Haze (P90 ≈ 4–5.5 мс CPU при 60 Гц) значат, что на 120 Гц стекло над постоянно меняющимся источником займёт около половины бюджета, не считая GPU.
- Плавная прокрутка над GeckoView: прокрутку ведёт композитор Gecko в своей поверхности, поэтому Compose не должен перерисовываться в каждом кадре прокрутки. Смещение панели читать в фазе layout/draw (`Modifier.offset { }`/`graphicsLayer { }`), а не через рекомпозицию. `setVerticalClipping` вызывать только при изменении значения. Не вызывать `onTouchEventForDetailResult` на каждое касание (это прямо предупреждено в javadoc).
- Baseline Profile должен покрыть старт, открытие обзора вкладок и ввод в адресную строку. Macrobenchmark в CI — для бюджетов из `plan-v5.md` §7.

### Пробелы
- Официальных цифр о влиянии SurfaceView GeckoView на jank Compose-оверлея нет. Нужен собственный Macrobenchmark на устройстве.
