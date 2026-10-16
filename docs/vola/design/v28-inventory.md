# V28a — инвентаризация экранов телефона перед «v5 по умолчанию»

Шаг 1 плана V28, утверждённого владельцем 2026-10-16 ([план в #80](https://github.com/MikeSPL187/Browser/issues/80#issuecomment-6208731550)).
Вопрос: что увидит пользователь телефона, если включить `ui.v5`, — v5 или прежний интерфейс (v4).
Источник — код `main` на 2026-10-16: где экран читает флаг (`rememberUiV5Flag`, `rememberDrawsV5`,
`rememberSettingsUiV5`, `LocalSettingsSkin`). Это разбор кода, а не прогон; обход экранов v5
(`walk[84]`–`walk[93]`) подтверждает разделы А1–А6.

Обозначения: **v5** — свой экран v5; **v5-скин** — прежний экран, нарисованный компонентами v5;
**v4** — прежний интерфейс; **нейтр.** — не оболочка браузера (содержимое, системные окна).

## А. Уже v5 на телефоне (под `ui.v5`)

| # | Экран | Код | Шаг |
|---|---|---|---|
| А1 | Нижняя панель, сжатая полоса, свайп вкладок, цвет сайта | `ui/v5/V5BottomBar.kt`, `BrowserAddressChromeV5.kt`, `V5SiteTint` | V7, V8 |
| А2 | Редактор адреса и подсказки | `ui/v5/V5AddressEditor.kt`, `V5SuggestionLayout` | V9a |
| А3 | Обзор вкладок, группы, профили | `V5TabOverview.kt`, `V5TabGroups.kt`, `V5ProfileSheet.kt`, `V5ProfileEditSheet.kt` | V10–V12 |
| А4 | Новая вкладка, частная новая вкладка, закреплённые | `V5NewTab.kt`, `V5PrivateNewTab.kt`, `V5PinnedAddSheet.kt` | V13, V14 |
| А5 | Меню страницы | `V5PageMenu.kt` | V15b |
| А6 | Главная настроек; страницы настроек на `SettingsPage` | `SettingsHomeV5.kt`; `V5SettingsSkin.kt` (v5-скин), слои `SettingsLayer` | V15c, V15d |
| А7 | Тянущееся обновление | `BrowserPullRefreshV5.kt`, `V5PullRefreshIndicator.kt` | V16 |
| А8 | Панель защиты сайта | `V5SiteProtection.kt` | V17 |
| А9 | Отчёт о приватности | `V5PrivacyReport.kt` | V18 |
| А10 | Режим чтения и «Слушать» | `ReaderStudioScreen.kt` (тёмная тема v5), `V5ListenRow.kt` | V19 |
| А11 | «Скрыть отвлекающее» | `V5DistractionsPanel.kt` | V20 |
| А12 | Предпросмотр ссылки | `V5LinkGlance.kt` | V21 |
| А13 | Загрузки: кольцо и строка над панелью | `V5DownloadRing.kt`, `V5DownloadNotice.kt` | V22 |

## Б. Под `ui.v5` на телефоне всё ещё v4 — пробелы до шага 2

Каждый пункт до «v5 по умолчанию» получает одно из трёх: **сделать v5** (свой PR), **v5-скин**
(прежняя логика, компоненты v5) или **убрать** (принцип 2). Рекомендация — в последней колонке.

| # | Что видит пользователь | Код (v4) | Когда показывается под v5 | Рекомендация |
|---|---|---|---|---|
| Б1 | Знакомство и урок жестов | `FirstRunOverlay.kt`, `FirstRunScreens.kt`, `GestureOnboardingScreen.kt` | первый запуск | **сделать v5**: первое, что видит человек; урок — жесты v5 (свайп по панели, обзор), не v4 |
| Б2 | «Библиотека»: история, избранное, загрузки | `HistoryActivity`/`HistoryScreen.kt`, `FavoritesActivity`/`FavoritesScreen.kt`, `DownloadsActivity`/`DownloadsScreen.kt`, `LibraryComponents.kt` | из меню v5 («История», «Избранное», «Загрузки») | **сделать v5**: списки на `V5List`, поиск как в обзоре; три экрана — один PR на экран |
| Б3 | «Пароли» (экран 18) | `PasswordsActivity`, `ui/passwords/` | из настроек | **v5-скин** сейчас; полный v5 — вместе с V24 (проверка паролей) |
| Б4 | «Все действия» — прежнее меню | `BrowserMainMenu.kt` (зовёт `BrowserAddressChromeV5.kt:150`) | кнопка в меню v5 | **убрать** после разбора: каждое нужное действие — в меню v5 или его «Дополнительно» (принцип 3), остальное — сказать «нет» |
| Б5 | Поиск на странице | `FindInPageBar.kt` | меню v5 → «Найти на странице» | **сделать v5**: строка над панелью, как `V5ListenRow` |
| Б6 | Редактор адреса на пустой новой вкладке, ответ команды в строке | `BrowserAddressChrome.kt:230` (`showInteractiveBlankStart`, `commandFeedback`) | новая вкладка с открытым вводом; команды адресной строки | **сделать v5**: редактор v5 и над новой вкладкой; ответ команды — строкой v5 |
| Б7 | Панель при закреплении к краю, морфинг в обзор, развёрнутая панель предпросмотра | `BrowserAddressChrome.kt:460` (`effectiveAddressBarDockPlacement`, `tabOverviewOpening`, `linkPeekAddressBarExpanded`) | док к краю (жест v4), переход в обзор | док — **убрать** под v5 (жест v4, у v5 своё место панели); переход в обзор — **сделать v5** (панель v5 до конца морфинга) |
| Б8 | Полные сведения о сайте, сертификат, разрешения | `PrivacyXRaySheet.kt`, `SiteInfoOverview.kt`, `SiteCertificatePage.kt`, `PermissionRadarSheet.kt` | «Подробнее» в панели защиты v5 | **v5-скин** (`V5Sheet` + `V5List`), содержимое прежнее |
| Б9 | Запросы сайта: разрешения, вход, alert/confirm, внешнее приложение, капча, федеративный вход, безопасность загрузки | `PermissionPromptSheet.kt`, `HttpAuthPromptDialog.kt`, `BrowserWebPromptDialog.kt`, `ExternalAppPromptDialog.kt`, `CaptchaCompatibilityPromptDialog.kt`, `FederatedLoginPromptDialog.kt`, `DownloadSafetySheet.kt`, `DownloadManagerChooserDialog.kt` | по запросу страницы | **v5-скин**: одна форма запроса на `V5Sheet` (принцип 1), один PR на всю группу |
| Б10 | Перевод страницы | прежний экран перевода | меню v5 → «Перевести» | **сделать v5** в V23 (порт F #175 есть) |
| Б11 | Перенос вкладки в другой профиль | `MoveTabToProfileDialog.kt` (зовёт `BrowserScreen.kt`) | «Переместить в профиль» | **v5-скин** на `V5Sheet` |
| Б12 | Страницы настроек не на `SettingsPage`: расширения, фильтры, юзерскрипты, «капсулы» сайтов, обои профиля, иконки | `FirefoxExtensionManagerOverlay.kt`, `FilterStudioScreen.kt`, `UserscriptManagementScreen.kt`, `ToppingCatalogScreen.kt`, `SiteCapsuleScreen.kt`, `ProfileWallpaperEditorScreen.kt`, `Capsule*Activity` | из «Дополнительно» настроек | **v5-скин** для расширений, фильтров, юзерскриптов; «капсулы», обои и иконки — решить в Г |
| Б13 | Блокировка частных вкладок и профиля | `PrivateTabsLockScreen.kt`, `ProfileLockedOverlay.kt` | частные вкладки под замком, закрытый профиль | **сделать v5**: экран замка — один для обоих |
| Б14 | «Что нового», запрос обновления, журнал сбоев | `ReleaseNotesScreen.kt`, `AppUpdatePrompt.kt`, диалог журнала (#126) | после обновления, по сбою | **v5-скин** на `V5Sheet` |

## В. Нейтральное — не оболочка браузера

Не мешает «v5 по умолчанию»; цвета и шрифт переводятся на токены v5 отдельным PR, если обход найдёт
разнобой.

- Страница опасного сайта, ошибки загрузки, пустые состояния — `DangerousSitePage.kt`, `PageErrorFeedback.kt`, `VolaStatePage.kt`.
- Полноэкранное видео, картинка в картинке, трансляция — `FullscreenVideoOverlay.kt`, `CastControls.kt`.
- Сканер QR, голосовой поиск — `QrCodeScanner.kt`, `AddressVoiceSearch.kt`.
- Экран запуска — `LaunchScreen.kt`; виджет поиска — `CandySearchWidgetProvider`.

## Г. Только v4 — уходит на шаге 4 (решения владельца по функциям)

Функции, у которых в v5 нет места. По плану v5 старая функция возвращается, только если она лучше
решения ориентиров; по принципу 2 рекомендую не переносить, кроме отмеченных.

| Функция v4 | Код | Рекомендация |
|---|---|---|
| Оформление «Рама»/«Воздух», рамка страницы | `BrowserContentFrameRules.kt`, `BrowserPageFrames.kt`, `CandyChromeSurface.kt` | убрать: v5 рисует страницу от края до края |
| Пространства (заменены профилями) | `WorkspaceSheets.kt`, `WorkspaceSwipe.kt`, `WorkspaceIdentityEditor.kt`, `ProfileSwitcher.kt` | убрать: данные уже профили (F, V11) |
| Обзор v4: сетка и «Hero», док обзора, лист действий вкладки | `TabOverview.kt`, `TabOverviewDock.kt`, `TabOverviewHeader.kt`, `TabActionsSheet.kt` | убрать на телефоне (в v5 — долгое нажатие на карточку, V12b); прыжок «Hero» (#315) уходит вместе с ним |
| Закрепление панели к краю, компактный режим, список вкладок компактного режима | `AddressBarDock*.kt`, `CompactModeHint.kt`, `CompactTabList.kt` | убрать |
| Полоса вкладок широкой панели | `WideAddressTabStrip.kt` | остаётся для планшета (шаг 5) |
| «Отложить» вкладки | `SnoozeScreens.kt` | решить владельцу: в Safari нет; оставить в «Дополнительно» или убрать |
| «След» (граф переходов) | `CandyTrailScreen.kt` (открывается только из обзора v4) | решить владельцу: под v5 уже недоступен; в ориентирах нет |
| Split View на телефоне | `SplitViewLayout.kt` | решить владельцу: на телефоне Safari его не даёт; оставить для планшета |
| «Капсулы» сайтов, обои профиля, пакеты иконок | `SiteCapsule*`, `ProfileWallpaperEditor*`, `CapsuleIconPackPicker*` | решить владельцу: оформление v4 |

## Д. Порядок до шага 2

1. Б1 знакомство → Б2 «Библиотека» (три PR) → Б5 поиск на странице → Б6, Б7 панель до конца.
2. Б8, Б9, Б11, Б14 — v5-скин листов (по PR на группу).
3. Б4 «Все действия» — разбор действий, затем убрать.
4. Б3, Б10 — с V24 и V23; Б12, Б13 — v5-скин и экран замка.
5. Г — решения владельца по функциям списком в #80, после чего шаг 4 удаляет код v4.

Когда Б1–Б9 и Б11–Б14 закрыты, переходим к шагу 2 (флаг по умолчанию). Б3 и Б10 могут идти
на v5-скине.
