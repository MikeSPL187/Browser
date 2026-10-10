# Chrome для Android (2025–2026): дизайн, Material 3 Expressive и функциональные паттерны

> Методика: в этой сессии прямое чтение страниц (WebFetch/curl) было заблокировано сетевой политикой (DNS ENOTFOUND / proxy 403). Все находки получены из сводок поисковой выдачи (WebSearch), полный текст статей не прочитан. Для ключевых фактов указаны первичные или крупные профильные источники. Где источники расходятся или устарели, это отмечено. Дата исследования: 2026-10-10.

## 1. Адресная строка внизу (2025) и нижняя навигационная панель (2026)

### Takeaway
С середины 2025 года в Chrome для Android можно перенести адресную строку вниз: долгим нажатием на строку или в «Настройки → Адресная строка». Вниз переезжает вся панель: омнибокс, переключатель вкладок и меню. При фокусе строка уходит наверх вместе с подсказками, и это критикуют как «месиво». В 2026 году (Chrome 150) Google начал выкатывать вторую нижнюю панель (Home / Gemini / New tab / Tab switcher), и вместе с полосой групп вкладок она даёт до трёх рядов хрома. Обозреватели считают это перегрузом интерфейса.

### Cited Findings
- Ограниченный выпуск в stable начался в апреле 2025 года (Chrome 135, замечен на двух устройствах). В том же релизе переделали настройки — [9to5Google, 2025-04-15](https://9to5google.com/2025/04/15/chrome-bottom-address-bar-android-135/)
- Официальный анонс и расширение выкатки — 24 июня 2025, «для всех в ближайшие недели» — [9to5Google, 2025-06-24](https://9to5google.com/2025/06/24/chrome-bottom-address-bar-android/); [TechCrunch](https://techcrunch.com/2025/06/24/chrome-for-android-now-lets-you-move-the-address-bar-to-the-bottom-too); [blog.google](https://blog.google/products-and-platforms/products/chrome/address-bar-position-change/)
- Широкая выкатка — середина июля 2025 — [Chrome Unboxed](https://chromeunboxed.com/chromes-bottom-address-bar-is-finally-rolling-out-widely-on-android/); [Android Authority](https://www.androidauthority.com/chrome-for-android-bottom-address-bar-3577732)
- Как переключить: долгое нажатие на адресную строку → «Move address bar to the bottom / to the top», либо ⋮ → Settings → Address bar → Top/Bottom. При включённой синхронизации настроек позиция синхронизируется между Android-устройствами — [Google Chrome Help](https://support.google.com/chrome/answer/14181646?hl=en&co=GENIE.Platform%3DAndroid); [Google Chrome Help (16737545)](https://support.google.com/chrome/answer/16737545?hl=en)
- Вниз переезжает вся панель: адресная строка вместе с переключателем вкладок, меню ⋮ и другими ярлыками. Верхнее положение по-прежнему доступно, переезд не обязателен — [9to5Google, 2025-06-24](https://9to5google.com/2025/06/24/chrome-bottom-address-bar-android/); [gHacks](https://www.ghacks.net/2025/06/25/google-chrome-on-android-finally-lets-you-move-the-address-bar-to-the-bottom/)
- Критика первой реализации: при нажатии на нижнюю строку она «снова уходит на самый верх экрана вместе с подсказками» — [Android Authority, hands-on «it's a mess»](https://www.androidauthority.com/chrome-android-bottom-url-bar-hands-on-3571842/)
- Есть и обратный сценарий: строка «падает» вниз над клавиатурой. Опубликован обход через флаг `chrome://flags` «bottom toolbar v2» (неофициально) — [PiunikaWeb, 2025-12-12](https://piunikaweb.com/2025/12/12/chrome-android-address-bar-moves-to-bottom-fix/)
- Непоследовательность: кнопка «новая вкладка» в переключателе вкладок осталась наверху, хотя адресная строка внизу — [gHacks](https://www.ghacks.net/2025/06/25/google-chrome-on-android-finally-lets-you-move-the-address-bar-to-the-bottom/)
- Chrome для iOS получил нижнюю строку раньше: в октябре 2023 года по 9to5Google, украинский источник называет октябрь 2024. Даты расходятся — [Ubergizmo/сводка](https://www.ubergizmo.com/2025/06/chrome-android-address-bar-bottom); [mezha.ua](https://mezha.ua/en/news/chrome-dlya-android-maye-adresniy-ryadok-vnizu-302915/amp/)
- **2026, Chrome 150: новая нижняя панель.** Сверху остаются URL, «Поделиться», кнопка «Назад» (теперь отдельно слева от омнибокса) и меню ⋮. Снизу — «New Tab», управление вкладками и кнопка Gemini — [9to5Google, 2026-08-06](https://9to5google.com/2026/08/06/google-chrome-android-navigation-bar-gemini-button/); [9to5Google, 2026-08-31](https://9to5google.com/2026/08/31/chrome-redesign-too-much-ui/)
- По скриншотам на нижней панели четыре кнопки: Home, Gemini, New tab, Tab switcher. Home можно отключить — [PiunikaWeb, 2026-07-14](https://piunikaweb.com/2026/07/14/gemini-chrome-for-android-redesigned-bottom-nav-bar/); [Gadget Hacks](https://android.gadgethacks.com/news/chrome-android-bottom-navigation-bar-redesign-explained/)
- Ярлык панели инструментов (настраиваемая кнопка) переехал внутрь адресной строки, освободив место для «Назад» слева — [9to5Google, 2026-08-31](https://9to5google.com/2026/08/31/chrome-redesign-too-much-ui/)
- Источники расходятся: по Android Authority, кнопка Gemini появляется, только если навигационная панель перенесена вниз — [Android Authority](https://www.androidauthority.com/chrome-android-navigation-bar-gemini-3687175/)
- С открытыми группами вкладок интерфейс складывается в три ряда, это «выглядит комично» и съедает экран — [Android Headlines, 2026-09](https://www.androidheadlines.com/2026/09/chrome-new-bottom-bar-redesign-mobile-app.html); [9to5Google «too much chrome»](https://9to5google.com/2026/08/31/chrome-redesign-too-much-ui/)
- Выкатка медленная, на серверной стороне. 9to5Google видел новый интерфейс только на одном устройстве из нескольких — [9to5Google, 2026-08-06](https://9to5google.com/2026/08/06/google-chrome-android-navigation-bar-gemini-button/)
- По сообщениям, похожую нижнюю панель («Duet») Google в 2020 году убрал, потому что «массовые пользователи» находили её «дезориентирующей» — [сводка по Android Headlines / 9to5Google, 2026](https://www.androidheadlines.com/2026/09/chrome-new-bottom-bar-redesign-mobile-app.html)

### Inferences
- Для Vola (Safari-first, строка внизу по умолчанию) у Chrome стоит взять: (а) переключение позиции долгим нажатием на строку, (б) дублирующий пункт в настройках, (в) синхронизацию позиции. Пользователь Chrome будет искать переключатель именно там.
- Главная ошибка Chrome — прыжок строки наверх при фокусе и «новая вкладка» наверху в обзоре вкладок. В Vola строка и подсказки должны оставаться у клавиатуры (как в Safari), а «+» в обзоре — внизу, рядом с большим пальцем.
- Реакция на Chrome 150 показывает: два ряда управления плюс полоса групп — это перегруз. Для Vola нужен один нижний ряд и сворачивание при прокрутке, в духе принципов Zen и Safari.

### Gaps
- Не удалось подтвердить точное поведение нижней строки при прокрутке (скрывается ли вместе со страницей, как в верхнем режиме) и где показывается полоса вкладок/групп на телефоне в нижнем режиме: страницы не загрузились, в сводках этого нет.
- Нет официальных размеров (высота панели, отступы).

## 2. Переключатель вкладок: сетка, группы, поиск, «Закрыть все», инкогнито

### Takeaway
Обзор вкладок Chrome для Android — сетка карточек в два столбца на телефоне. Сверху сегментированный переключатель «Вкладки / Инкогнито / Группы». У групп 9 цветов и названия, есть поиск по вкладкам и группам и «Закрыть все» в меню ⋮. Редизайн M3 Expressive (август–октябрь 2025, Chrome 139–141) затронул в основном оформление: «+» в скруглённом квадрате с Dynamic Color, контейнер-переключатель секций, рамка группы в её цвете. Функции не менялись.

### Cited Findings
- M3 Expressive в Tab Grid: кнопка «+» (новая вкладка) — в скруглённом квадрате с фоном Dynamic Color. Переключатель «Вкладки / Инкогнито / Группы» стал отдельным контейнером, текущий раздел выделен скруглённым квадратом. Группы окрашены в выбранный цвет. Размер кнопок не увеличили — [9to5Google, 2025-10-26](https://9to5google.com/2025/10/26/chrome-141-android-m3-expressive-redesign/)
- Невыбранная группа теперь показывает выбранный цвет только рамкой, раньше вся карточка была залита цветом — [Yahoo Tech / Android Authority](https://tech.yahoo.com/apps/articles/chrome-android-getting-fresh-material-124910824.html)
- В Canary (июнь 2025) замечены «пружинящая» анимация кнопки переключателя вкладок при открытии ссылки в новой вкладке и цветовые чипы-«пилюли» в выборе цвета группы — [Android Authority](https://www.androidauthority.com/chrome-android-material-3-expressive-redesign-3564959/)
- Выкатка серверная: началась в конце августа 2025, к концу октября завершена с Chrome 141. Ранние сообщения связывали её с Chrome 139 на Android 16 QPR2 Beta — [9to5Google, 2025-10-26](https://9to5google.com/2025/10/26/chrome-141-android-m3-expressive-redesign/); [9to5Google, 2025-08-31](https://9to5google.com/2025/08/31/chrome-android-material-3-expressive/); [Android Authority, stable](https://www.androidauthority.com/chrome-stable-material-3-expressive-redesign-3593166/)
- Цвета групп: 9 вариантов, как на десктопе. Chrome назначает цвет автоматически, его можно сменить нажатием или через меню. Появилось серверным обновлением около Chrome 127 (2024, старое поведение) — [9to5Google](https://9to5google.com/?p=637091)
- Паритет групп с десктопом: в полосе вкладок (планшет) показываются цвета и названия, выбранная группа обведена, есть нижний индикатор. Флаги «Tab Group Parity Android» и «Tab Strip Indicators Android» — [Windows Report](https://windowsreport.com/chrome-tab-group-parity-android/)
- Флаг `#tab-strip-group-reorder-android`: долгое нажатие на индикатор группы → режим перестановки, группу можно перетащить (дата не указана) — [TechRadar](https://www.techradar.com/phones/android/this-new-feature-for-chrome-for-android-is-going-to-make-tidying-up-your-tabs-easier)
- В Chrome 138 (июль 2025) переставили элементы управления группами — [9to5Google, 2025-07-09](https://9to5google.com/2025/07/09/chrome-tab-groups-tweaks/)
- Chrome 139 (30.07.2025): открытые вкладки можно перетащить в раздел «Неактивные вкладки» (Inactive tabs), чтобы архивировать — [сводка Android Central/Yahoo](https://tech.yahoo.com/general/articles/chrome-rolls-search-tools-mobile-160230888.html)
- Поиск: кнопка переключателя вкладок → поле «Search your tabs» ищет по названию вкладки или группы. Чтобы в результатах были группы, нужны вход и синхронизация — [Google Chrome Help: Manage tabs (Android)](https://support.google.com/chrome/answer/2391819?co=GENIE.Platform%3DAndroid&hl=en)
- «Закрыть все»: ⋮ → «Close all tabs», подтверждение «Close all tabs and groups». Для инкогнито: вкладка «Инкогнито» в переключателе → ⋮ → «Close all Incognito tabs» — [Google Chrome Help](https://support.google.com/chrome/answer/2391819?co=GENIE.Platform%3DAndroid&hl=en); [Google Chrome Help: Incognito](https://support.google.com/chrome/answer/95464?hl=en&co=GENIE.Platform%3DAndroid)
- Изменения групп сохраняются и синхронизируются между устройствами с тем же аккаунтом. «Поделиться» группой означает выбрать вкладки → ⋮ → «Share tabs»: получатель получает ссылки, а не живую копию группы — [Google Chrome Help](https://support.google.com/chrome/answer/2391819?hl=en_ZA&co=GENIE.Platform%3DAndroid); [techidea.net, 2026](https://www.techidea.net/chrome-tab-groups/)
- Сворачивание групп в полосе вкладок официально описано только для десктопа — [Google Chrome Help (Computer)](https://support.google.com/chrome/answer/2391819/use-tabs-in-chrome-android)
- Пользователи жалуются на навязанные группы и сетку, есть гайды по их отключению (старые, 2021) — [Android Police, 2021](https://www.androidpolice.com/2021/02/24/how-to-disable-chrome-tab-group-grid-switcher/); [TNW](https://thenextweb.com/news/ditch-chromes-dumbass-tab-groups-on-android-with-this-tweak)

### Inferences
- Пользователю Chrome привычны: сегментированный переключатель «Обычные / Приватные / Группы» вверху обзора, крупная «+», поиск по вкладкам и группам, «Закрыть все» с подтверждением, 9 именованных цветов групп. В модели профилей Vola это можно отразить так: сегмент «Вкладки / Приватные / Группы» плюс цвет профиля.
- Рамка в цвете группы вместо заливки — удачный M3E-приём: цвет различим, а превью страницы не перекрыто.
- «Неактивные вкладки» (автоархив) — функциональный паттерн Chrome, которого нет в Safari. Его стоит рассмотреть.

### Gaps
- Не найдены точные числа: радиус углов карточек, их соотношение сторон, отступы сетки, количество столбцов на планшете.
- Не подтверждено, есть ли на телефоне сворачивание групп и «полоса группы» внизу вкладки (group strip) в 2025–2026: в справке это не описано.

## 3. Подсказки омнибокса

### Takeaway
Надёжных источников 2025–2026 по порядку и категориям подсказок Chrome для Android найти не удалось. Есть только отрывочные данные: метка «Switch to tab» для открытых вкладок и инвертированный список при нижнем тулбаре в старых экспериментах. При нижней строке Chrome в 2025 году переносил омнибокс с подсказками наверх, то есть фактически уходил от проблемы порядка.

### Cited Findings
- Подсказка «Switch to tab» показывается рядом с совпадающей открытой вкладкой. Исторически она была за флагом `chrome://flags/#omnibox-tab-switch-suggestions` (старые источники) — [ctrlshiftcopy, 2026 guide](https://www.ctrlshiftcopy.com/blog/how-to-use-chrome-omnibox); [Vivaldi forum](https://forum.vivaldi.net/topic/42224/switch-to-tab-function)
- При нажатии на нижнюю строку она уезжает наверх «вместе с подсказками»: подсказки показываются сверху вниз, как в верхнем режиме — [Android Authority hands-on](https://www.androidauthority.com/chrome-android-bottom-url-bar-hands-on-3571842/)
- В Chromium 108 с флагом `omnibox-modernize-visual-update` и нижним тулбаром список результатов показывался инвертированным (Bromite) — [GitHub bromite#2514](https://github.com/bromite/bromite/issues/2514)
- В 2025 году адресная строка стала давать «больше подсказок, чем раньше» (подробностей во фрагменте нет) — [сводка Android Central/Yahoo](https://tech.yahoo.com/general/articles/chrome-rolls-search-tools-mobile-160230888.html)

### Inferences
- Для Vola с нижней строкой есть два варианта: (1) подсказки над клавиатурой, ближайшая к пальцу — лучшая (обратный порядок, как в Safari iOS), или (2) поведение Chrome с переносом наверх. Критика Chrome говорит в пользу первого.
- «Переключиться на вкладку» в подсказках — must-have для привычности пользователю Chrome.

### Gaps
- Порядок и категории (на фокусе: буфер обмена «Ссылка, которую вы скопировали», недавние запросы, «Что вы ищете?», быстрые действия Pedals/Actions in suggest), а также дизайн подсказок M3E на Android не подтверждены источниками в этой сессии.
- Нет данных о поведении подсказок в макете Chrome 150 с двумя панелями.

## 4. Перевод, менеджер паролей, Password Checkup, ключи доступа (passkeys)

### Takeaway
Перевод в Chrome для Android запускается из ⋮ → «Translate» и показывается панелью у края экрана. Официальная справка устарела, а данных о переводе на устройстве для страниц нет. Google Password Manager на Android — системная поверхность Play services: нижняя навигация «Passwords / Checkup / Settings», Checkup проверяет утёкшие, повторяющиеся и слабые пароли. С мая 2025 года пароли могут автоматически превращаться в ключи доступа.

### Cited Findings
- Перевод: ⋮ → Translate вызывает панель перевода внизу экрана. Некоторые гайды описывают подсказку вверху. Язык по умолчанию — Settings → Languages → «Translate into this language». Автоперевод и «More languages» — в меню панели — [Google Chrome Help (Android)](https://support.google.com/chrome/answer/173424?hl=en&co=GENIE.Platform%3DAndroid); [browserhow](https://browserhow.com/how-to-translate-webpage-in-chrome-android-language-settings/)
- Перевод в браузере на устройстве существует как Translator API в Chrome Canary за флагами (февраль 2025, неофициальный источник). Его использование для перевода страниц на Android не подтверждено — [dev.to, 2025-02](https://dev.to/brunorelima/ai-powered-local-translations-february-2025-1b0c)
- Password Checkup показывает пароли, которые могли утечь, слабые и используемые в нескольких аккаунтах. На Android: Настройки → «Password Manager» → Checkup — [Google Account Help](https://support.google.com/accounts/answer/9457609); [Google Security Blog, 2019](https://security.googleblog.com/2019/08/new-research-lessons-from-password.html)
- Редизайн Password Manager на Android: нижняя панель с вкладками Passwords / Checkup / Settings, серверно через Play services 24.16.16, май 2024 — [9to5Google, 2024-05-17](https://9to5google.com/2024/05/17/google-password-manager-material-you/); [Android Central](https://www.androidcentral.com/apps-software/google-password-manager-redesign-preview)
- Каждый заход во вкладку Checkup заново запускает проверку на утёкшие, повторяющиеся и слабые пароли — [сводка 9to5Google/Android Police](https://www.androidpolice.com/google-password-manager-material-you-redesign/)
- Автоматическое обновление пароля до ключа доступа: при входе по паролю на поддерживаемый сервис GPM создаёт passkey и уведомляет пользователя. Есть настройка «Automatically create a passkey to sign in faster», её можно отключить. Май 2025 — [Forbes, 2025-05-10](https://www.forbes.com/sites/daveywinder/2025/05/10/googles-android-update---passwords-automatically-become-passkeys/); [Android Police](https://www.androidpolice.com/google-may-auto-convert-passwords-to-passkeys-on-android/)
- M3 Expressive в Password Manager — с Play services 25.31 (август 2025). Dark Web Report, по одному стороннему источнику, закрыт в феврале 2026 (не подтверждено Google) — [tech-insider.org, 2026](https://tech-insider.org/ca/how-to-set-up-google-password-manager-2026/)
- Старое (2022): на Android Checkup помечает не только утёкшие, но и слабые/повторяющиеся пароли, есть автоматическая смена пароля — [BetaNews, 2022](https://betanews.com/2022/06/30/google-revamps-password-manager)

### Inferences
- Для Vola (без Play Services) системный GPM недоступен, пароли — свой модуль поверх движка. Структура «Пароли / Проверка / Настройки» и три категории риска (утёкшие, повторяющиеся, слабые) — привычная пользователю Chrome модель. Проверку утечек без Google можно делать через k-anonymity (идея, требует отдельного решения по приватности).
- Перевод: пользователю Chrome привычно найти «Перевести» в меню ⋮ и видеть компактную панель у нижнего края с выбором языка и «Всегда переводить». Для приватности Vola лучше перевод на устройстве (Firefox Translations в Gecko — тема отдельного исследования).

### Gaps
- Нет актуального (2025–2026) описания UI перевода: это панель-«message», нижний лист или пузырь в омнибоксе.
- Не найдены точки входа ключей доступа в самом Chrome для Android (autofill-лист, Credential Manager) и вид Checkup в 2026 году.

## 5. Material 3 Expressive в Chrome и Android 16

### Takeaway
В Chrome для Android M3 Expressive сдержанный. Он добавил круглые контейнеры для действий в верхнем ряду меню ⋮ (вперёд, закладка, загрузка, сведения о сайте, обновить), квадрат со скруглением для активного состояния «в закладках», сегментированный индикатор загрузки со скруглёнными концами в омнибоксе, а также контейнеры и цвета в обзоре вкладок. Размеры кнопок и списочные экраны вроде настроек не менялись. Официальная система движения M3E строится на пружинных токенах (spatial/effects × fast/default/slow), которые переключаются схемой «expressive» или «standard».

### Cited Findings
- Меню ⋮: «Вперёд», «Закладка», «Загрузка», «Сведения о сайте», «Обновить» — в круглых контейнерах. У сохранённой в закладки страницы фон звезды становится скруглённым квадратом (морфинг формы как индикатор состояния) — [9to5Google, 2025-10-26](https://9to5google.com/2025/10/26/chrome-141-android-m3-expressive-redesign/); [Android Police](https://www.androidpolice.com/google-chrome-android-material-3-expressive-refresh/)
- Индикатор загрузки в омнибоксе: прямая полоса заменена сегментированной (split) со скруглёнными концами. По 9to5Google, выкачен не всем — [9to5Google, 2025-10-26](https://9to5google.com/2025/10/26/chrome-141-android-m3-expressive-redesign/); [Android Central](https://www.androidcentral.com/apps-software/chrome-for-android-just-got-its-long-awaited-dose-of-googles-latest-redesign)
- Размеры кнопок не увеличены, экраны-списки (Настройки) не тронуты, функции не менялись — [9to5Google, 2025-10-26](https://9to5google.com/2025/10/26/chrome-141-android-m3-expressive-redesign/); [gHacks, 2025-10-28](https://www.ghacks.net/2025/10/28/chrome-for-android-gets-material-3-expressive-redesign/)
- Пружинная анимация кнопки счётчика вкладок при открытии ссылки в новой вкладке (Canary, июнь 2025) — [Android Authority](https://www.androidauthority.com/chrome-android-material-3-expressive-redesign-3564959/)
- Общий список выкаток M3E по приложениям Google — [9to5Google, 2025-11-17](https://9to5google.com/2025/11/17/google-material-3-expressive-redesign/)
- Система движения M3E: пружинные токены двух типов — spatial (положение, размер, форма) и effects (цвет, прозрачность), каждый в вариантах fast / default / slow. Схема движения (expressive или standard) задаётся на уровне продукта, токен вида `md.sys.motion.spring.fast.spatial` от схемы не зависит. Доступно в Jetpack Compose и MDC-Android — [m3.material.io: Motion](https://m3.material.io/styles/motion/overview/how-it-works); [M3 blog: Motion physics with Compose](https://m3.material.io/blog/m3-expressive-motion-theming)

### Inferences
- Для Vola приём «форма меняется при смене состояния» (круг → скруглённый квадрат для активной закладки) дешёвый и выразительный, в духе пружинных «фирменных моментов» из plan-v5. Сегментированный индикатор загрузки привычен пользователю Android 16, но для Safari-first можно оставить тонкую полосу в цвете акцента.
- Сдержанность Chrome (без увеличения кнопок и без перерисовки списков) — показатель, что M3E в браузере применяют точечно, к «рамке» вокруг страницы, а не ко всему.

### Gaps
- Официальные числа (жёсткость и демпфирование пружин, радиусы углов, токены формы, спецификация loading indicator) в этой сессии не подтверждены: страницы m3.material.io не загрузились. По памяти модели (не проверено в сессии, сверить с исходниками `androidx.compose.material3` `MotionScheme`): expressive spatial — default ≈ stiffness 380 / damping 0.8, fast ≈ 800 / 0.6, slow ≈ 200 / 0.8; effects — damping 1.0 при stiffness ≈ 1600 / 3800 / 800; standard spatial — damping 0.9. Шкала форм M3: 0 / 4 / 8 / 12 / 16 / 28 / полное скругление, в Expressive добавлены 20 / 32 / 48 dp. Использовать только после проверки.
- Типографика M3E (emphasized-стили, Google Sans Flex) применительно к Chrome не найдена.
- В сводках нет данных о том, что Chrome использует новый M3E LoadingIndicator (морфинг полигонов) где-либо, кроме сегментированного прогресса.

## 6. Загрузки и режим чтения

### Takeaway
Режим чтения в Chrome для Android полностью переделан в Chrome 143–145 (декабрь 2025 — март 2026). Он доступен всегда через меню ⋮, сохраняет омнибокс и настраивается в нижнем листе M3E: шрифты Sans/Serif/Mono/Lexend, фон «Светлый / Сепия / Тёмный», размер текста до 250%, настройки сохраняются между страницами. Свежих источников по UI загрузок на Android не найдено.

### Cited Findings
- Новый режим чтения: настройки в нижнем листе со стилем M3 Expressive, вид сохраняет омнибокс (раньше занимал весь экран) — [9to5Google, 2026-02-28](https://9to5google.com/2026/02/28/chrome-android-reading-mode-redesign/); [Chrome Unboxed](https://chromeunboxed.com/chrome-for-android-is-rolling-out-a-sweet-looking-reading-mode-redesign/)
- Шрифты Sans Serif, Serif, Mono и Lexend. Фон Light / Sepia / Dark. Размер текста до 250% — [Android Headlines, 2026-03](https://www.androidheadlines.com/2026/03/google-chrome-android-redesigned-reading-mode.html); [Chrome Unboxed](https://chromeunboxed.com/chrome-for-android-is-rolling-out-a-sweet-looking-reading-mode-redesign/)
- Вход: пункт «Reading mode» в меню ⋮ сразу под «Listen to this page», «всегда доступен». Раньше режим предлагался только на поддерживаемых статьях — [9to5Google, 2026-02-28](https://9to5google.com/2026/02/28/chrome-android-reading-mode-redesign/); [Android Authority](https://www.androidauthority.com/chrome-for-android-is-reading-mode-redesign-3628818/)
- Настройки сохраняются между страницами — [Android Headlines, 2025-12](https://www.androidheadlines.com/2025/12/google-chrome-android-reading-mode-redesign.html)
- Ранние сообщения связывали редизайн с Chrome 143, широкая выкатка — с Chrome 145 (есть серверная часть). Флаг `chrome://flags/#reader-mode-improvements` — [Chrome Unboxed](https://chromeunboxed.com/chrome-for-android-is-rolling-out-a-sweet-looking-reading-mode-redesign/); [9to5Google](https://9to5google.com/2026/02/28/chrome-android-reading-mode-redesign/)
- Справка Google по режиму чтения на Android — [Google Chrome Help 14218344](https://support.google.com/chrome/answer/14218344?hl=en&co=GENIE.Platform%3DAndroid)
- Загрузки (старое): в Canary 2021 текущие загрузки показывались тостом вверху, флаг «show download progress message» — [XDA](https://www.xda-developers.com/google-chrome-android-tests-new-ui-downloads-bookmarks/); [Android Police](https://www.androidpolice.com/chrome-upcoming-download-ui-tweaks/)
- Прогресс загрузки в шторке уведомлений на Android 13+ требует разрешения на уведомления (сторонний гайд) — [techbii](https://techbii.com/download-progress-not-showing-in-android-notification-bar/)
- Для контраста, десктоп (2023): нижнюю полосу загрузок заменили «лотком» у адресной строки с кольцом прогресса — [9to5Google, 2023-08-02](https://9to5google.com/2023/08/02/chrome-download-tray-bar/)

### Inferences
- Режим чтения Vola стоит сравнить именно с этим набором (4 шрифта, включая Lexend для доступности; 3 фона; до 250%; сохранение настроек; омнибокс остаётся; вход из меню всегда). Safari-ориентир — кнопка в адресной строке, Chrome-привычка — пункт в меню. Стоит дать оба входа.
- «Listen to this page» (чтение вслух) рядом с режимом чтения — функциональный паттерн Chrome, который пользователи ожидают.
- Для загрузок у Vola есть место для улучшения: кольцо прогресса в панели (идея с десктопного Chrome и Safari) плюс системное уведомление.

### Gaps
- Не найдены источники 2025–2026 о том, как Chrome для Android сейчас показывает начало и прогресс загрузки (message-панель, снекбар «Downloading…», уведомление).

## 7. Доступность: масштаб страницы и размер текста

### Takeaway
В Chrome для Android масштаб задаётся в «Настройки → Специальные возможности → Масштаб по умолчанию» (слайдер с превью текста). Отдельно есть «Принудительное масштабирование» для сайтов, которые его запрещают, и масштаб для каждого сайта. Page Zoom заменил старое «масштабирование текста». Описания 2025 года (меняется только текст или вся страница) расходятся.

### Cited Findings
- Settings → Accessibility → «Default zoom»: слайдер, двигать до удобного чтения превью-текста. «Force enable zoom» — масштабирование на страницах, которые его блокируют — [Google Chrome Help (Android, 96810)](https://support.google.com/chrome/answer/96810?hl=en&co=GENIE.Platform%3DAndroid)
- Page Zoom — современная замена классического Text Scaling — [itechguides](https://www.itechguides.com/chrome-for-android-page-zoom-now-just-boosts-text-but-not-on-every-page/); [PhoneArena](https://www.phonearena.com/news/chrome-android-getting-new-page-zoom-feature_id135642)
- Масштаб запоминается отдельно для каждого сайта — [itechguides](https://www.itechguides.com/chrome-for-android-page-zoom-now-just-boosts-text-but-not-on-every-page/)
- Противоречие: по одному источнику, после анонса доступности Google 15 мая 2025 года Page Zoom увеличивает текст, не меняя макет. Официальная справка по-прежнему говорит о масштабе текста, изображений и видео — [itechguides](https://www.itechguides.com/chrome-for-android-page-zoom-now-just-boosts-text-but-not-on-every-page/) vs [Google Chrome Help](https://support.google.com/chrome/answer/96810?hl=en&co=GENIE.Platform%3DAndroid)
- Google улучшал учёт системного размера шрифта в Chrome для Android — [Winaero](https://winaero.com/google-is-finally-improving-text-scaling-in-chrome-for-android/)
- Ссылки из Gmail/WhatsApp часто открываются во встроенном браузере (Custom Tabs/WebView), который игнорирует масштаб Chrome — [itechguides](https://www.itechguides.com/chrome-for-android-page-zoom-now-just-boosts-text-but-not-on-every-page/)

### Inferences
- Для Vola на GeckoView: «Масштаб по умолчанию» со слайдером и живым превью, «Принудительное масштабирование» и масштаб для каждого сайта (быстрый доступ из меню страницы, как в Safari «Аа») закрывают и привычки Chrome, и Safari. Масштаб по умолчанию стоит связывать с системным fontScale.

### Gaps
- Не подтверждено, что именно изменилось 15 мая 2025 года (текст или вся страница) и есть ли быстрый пункт «Масштаб» в меню ⋮ на Android в 2026 году.
