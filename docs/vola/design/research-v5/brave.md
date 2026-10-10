# Brave (Android и iOS): как защита показана пользователю, 2025–2026

> Метод и ограничения. Сайты brave.com, support.brave.app, privacyguides.org и cyberinsider.com из этой среды не открывались: прокси отказал в соединении, DNS не разрешил имена. Поэтому основной первичный источник — **исходники Brave на GitHub** (строки интерфейса Android и iOS в `brave-core`, каталог фильтр-листов в `adblock-resources`, вики `brave-browser`). Это фактический текст интерфейса в ветке master на октябрь 2026 года. Сведения с brave.com и support взяты из выдачи поиска, и это отмечено у каждого факта. Наличие строки в ресурсах не доказывает, что она сейчас видна на экране: строки бывают устаревшими. Где это важно, сказано отдельно. Код не копировался, взяты только идеи и формулировки как факты о продукте.

Ключевые первичные источники (дальше по тексту — короткие ссылки):
- [Android strings, brave-core](https://github.com/brave/brave-core/blob/master/browser/ui/android/strings/android_brave_strings.grd) — «Android-строки».
- [iOS BraveStrings.swift, brave-core](https://github.com/brave/brave-core/blob/master/ios/brave-ios/Sources/BraveStrings/BraveStrings.swift) — «iOS-строки».
- [Каталог фильтр-листов, brave/adblock-resources](https://github.com/brave/adblock-resources/blob/master/filter_lists/list_catalog.json) — «каталог листов».
- [Вики: Features controlled by Shields](https://github.com/brave/brave-browser/wiki/Features-controlled-by-Shields), [Fingerprinting Protections](https://github.com/brave/brave-browser/wiki/Fingerprinting-Protections), [Shields Debugging Guide](https://github.com/brave/brave-browser/wiki/Shields-Debugging-Guide), [Query String Filter](https://github.com/brave/brave-browser/wiki/Query-String-Filter), [Debouncing](https://github.com/brave/brave-browser/wiki/Debouncing), [Cosmetic Filtering](https://github.com/brave/brave-browser/wiki/Cosmetic-Filtering), [Block all cookies](https://github.com/brave/brave-browser/wiki/Block-all-cookies-global-Shields-setting), [Update Adblock Lists](https://github.com/brave/brave-browser/wiki/Update-Adblock-Lists).
- [Privacy Guides, mobile-browsers.md (исходник страницы)](https://github.com/privacyguides/privacyguides.org/blob/main/docs/mobile-browsers.md).

## 1. Панель Shields на мобильных: открытие, раскладка, счётчики, расширенный вид, поломки

### Takeaway
Панель открывается иконкой Shields (лев) в адресной строке. Вверху — один главный переключатель «Shields up/down for this site» и одно крупное число «Trackers, ads, and more blocked». Ниже — подсказка про поломку и кнопка «Report». Все тонкие настройки спрятаны под «Advanced controls»: уровень блокировки (Aggressive/Standard/Allow), HTTPS, скрипты, отпечатки, cookie, блок элементов, Shred, сброс сайта к умолчаниям. Простой вид по замыслу только показывает состояние, в нём меняется лишь главный переключатель.

### Cited Findings
**Открытие и простой вид**
- Панель открывается касанием иконки льва справа в адресной строке. «Простой вид» только показывает настройки сайта (меняется лишь Shields up/down). Отдельные настройки сайта меняются в «Advanced controls» внизу панели — [поиск по support.brave.app, «How do I configure global and site-specific Shields settings?»](https://support.brave.app/hc/en-us/articles/360023646212) (страница не открылась, данные из выдачи).
- Если сайт не менялся, его настройки совпадают с глобальными — там же.
- Текст простого вида на Android: статус «Shields up for this site» / «Shields down for this site», счётчик «Trackers, ads, and more blocked.», подсказка «If this site appears broken, try Shields down.», приглашение «Tell us if the site wasn't working properly with Shields up.» с кнопкой «Report», кнопка «Advanced controls» — [Android-строки](https://github.com/brave/brave-core/blob/master/browser/ui/android/strings/android_brave_strings.grd).
- Подпись иконки для доступности на Android: «Enable/Disable Brave Shields» — [Android-строки](https://github.com/brave/brave-core/blob/master/browser/ui/android/strings/android_brave_strings.grd).
- Подсказка при первом знакомстве: «See all the bad stuff Brave blocked on every page with Shields. Tap to view». Счётчик в подсказке: «%1$s ads & trackers blocked». Заголовок: «Brave Shields» — [Android-строки](https://github.com/brave/brave-core/blob/master/browser/ui/android/strings/android_brave_strings.grd).
- На iOS при первом показе: «Brave Shields just protected your online privacy.» и окно «Поделиться» с текстом «%ld trackers & ads blocked! / Congratulations. You're pretty special.». Кнопка «Open Brave Shields» — [iOS-строки](https://github.com/brave/brave-core/blob/master/ios/brave-ios/Sources/BraveStrings/BraveStrings.swift).
- Счётчик в панели — число заблокированного на сайте за этот визит. Он обнуляется при уходе с сайта и при перезагрузке страницы — [пересказ support-статьи «How do I use Shields while browsing?» в выдаче поиска](https://support.brave.app/hc/tr/articles/360022806212-How-do-I-use-Shields-while-browsing) (статья написана для десктопа, на Android раскладка может отличаться).

**Расширенный вид (Advanced controls), Android: пункты и формулировки** — все из [Android-строк](https://github.com/brave/brave-core/blob/master/browser/ui/android/strings/android_brave_strings.grd):
- Трекеры и реклама: «Block trackers & ads (Aggressive)» / «Block trackers & ads (Standard)» / «Allow all trackers & ads». Описание: «Choose how to handle trackers and ads».
- HTTPS: «Require all connections to use HTTPS (strict)» / «Upgrade to HTTPS whenever possible (default)» / «Don't upgrade connections to HTTPS (disabled)». Описание: «Ensure the connections to websites are secure.»
- Скрипты: «Block scripts — Blocks JavaScript (may break sites)».
- Отпечатки: «Block fingerprinting — Prevents websites from recognizing your device». В ресурсах ещё лежат три варианта: «Fingerprinting blocked (strict, may break sites)» / «(standard)» / «Allow all fingerprinting». Но строгий режим убран в 2024 году (раздел 5), так что это, скорее всего, остаток старой версии.
- Cookie: «Block cookies — Prevents websites from storing information about your previous visits». Варианты: «Block all cookies» / «Block third-party cookies» / «Allow all cookies». Описание: «Choose how to handle cookies». Глобальный «Block all cookies» объявлен устаревшим (строка «The "Block all Cookies" option has been deprecated…»).
- Блок элементов: «Block element» и «Clear all blocked elements».
- Удаление данных сайта: «Shred site's data» / «Shred site data now», «Auto shred»: «When site tabs are closed» / «When app exits». Подтверждение: «Shredding will delete site data and close all '%1$s' tabs. This cannot be undone.»
- Сброс: «Reset this site to Shields defaults» → тост «Site reset to Shields defaults».
- Ссылка «Shields global settings».
- Предупреждение при ослаблении защиты: «Note: this may reduce Brave's privacy protections.»
- Отпечатки по отдельным API показаны на десктопе в «Shields > Advanced > стрелка у Block fingerprinting» (список заблокированных методов) — [вики Fingerprinting Protections](https://github.com/brave/brave-browser/wiki/Fingerprinting-Protections). На мобильных такой детализации не нашёл.

**Отчёт о сломанном сайте (Android)** — [Android-строки](https://github.com/brave/brave-core/blob/master/browser/ui/android/strings/android_brave_strings.grd):
- Заголовок «Report a broken site». Текст: «Thank you for helping make Brave better for all. This report will only contain information necessary for us to fix this site.»
- Поля: «What is not working on this site?» / «Additional details (optional)», «Contact me at (optional)» с подсказкой «Email, X username, and more». Флажок «Include screenshot of the current page», кнопка «View screenshot». Ответ после отправки: «Report submitted. Thank you!»
- В настройках есть «Store contact information for future broken site reports».
- Причина поломки «Website doesn't work» есть и в других местах интерфейса.
- Порядок диагностики у Brave: сначала проверить другой браузер и приватное окно, затем шаг за шагом снижать защиту: Aggressive → Standard → Disabled, разрешить сторонние cookie, отключить фильтр-листы, и только в конце Shields down для сайта — [вики Shields Debugging Guide](https://github.com/brave/brave-browser/wiki/Shields-Debugging-Guide).

**Блок элементов на Android (1.78, 2025)**
- Путь: иконка Shields → Advanced controls → «Block element» → выбрать элемент → подтвердить. Это правила косметической фильтрации для одного сайта. Посмотреть их можно в `brave://settings/shields/filters`, отменить — кнопкой «Clear all blocked elements» в панели — [выдача поиска по brave.com/privacy-updates/34 и CyberInsider](https://brave.com/privacy-updates/34-block-page-elements-android/) (страница не открылась).
- Жалобы: нижняя всплывающая панель выбора закрывает элементы внизу страницы (март 2025); на части сайтов выбор не срабатывает — [Brave Community, выдача поиска](https://community.brave.app/t/bug-element-picker-block-element-is-unresponsive-on-android/648946).
- Есть настройка «Allow element blocking in private windows» с оговоркой, что элементы, скрытые в приватном окне, скрываются и в обычном — [Android-строки](https://github.com/brave/brave-core/blob/master/browser/ui/android/strings/android_brave_strings.grd).

**Shred (Auto Shred) вместо «Forget me when I close this site»**
- На десктопе «Forget me when I close this site» удаляет всё хранилище сайта, когда закрыта его последняя вкладка. Переключается и для сайта, и глобально — [вики Features controlled by Shields](https://github.com/brave/brave-browser/wiki/Features-controlled-by-Shields).
- На Android это заменил Auto Shred в Brave 1.89 (весна 2026). Режимы: удалять данные при закрытии всех вкладок сайта или при перезапуске приложения. Перед удалением есть 30-секундная пауза, чтобы успеть вернуть случайно закрытую вкладку. Старые настройки переносятся автоматически. Для сайта: Shields → Advanced Controls → Shred site's data. Глобально: Settings → Brave Shields & privacy → Auto Shred — [выдача поиска: brave.com/privacy-updates/37, Chipp.in 28.04.2026, CyberInsider](https://brave.com/privacy-updates/37-shred-button-android/) (страницы не открылись).
- По сообщениям пользователей, после обновления Auto Shred не включился даже у тех, кто включал «forget me», и на Android работал ненадёжно (апрель–май 2026; отдельные отзывы) — [Privacy Guides forum](https://discuss.privacyguides.net/t/brave-mobile-settings-page-doesnt-reflect-new-android-update/37433).
- Глобальные строки Auto Shred на Android: «Never — Browsing data is never shredded automatically», «Site tab closed — …when all tabs open to a site are closed», «App close — …when the Brave app is closed / restarted» — [Android-строки](https://github.com/brave/brave-core/blob/master/browser/ui/android/strings/android_brave_strings.grd).

**Блокировка соцсетей (глобально, Android):** «Social Media Blocking» с переключателями «Allow Google login buttons on third party sites», «Allow Facebook logins and embedded posts», «Allow Twitter embedded tweets», «Allow LinkedIn embedded posts» — [Android-строки](https://github.com/brave/brave-core/blob/master/browser/ui/android/strings/android_brave_strings.grd). За ними стоят скрытые листы «Allow Facebook/X/LinkedIn Embeds» — [каталог листов](https://github.com/brave/adblock-resources/blob/master/filter_lists/list_catalog.json).

### Inferences
- Главная мысль Brave: **один переключатель и одно число**, а всё тонкое — во втором слое. Для Vola это значит: в простом виде панели — статус сайта, крупный счётчик, переключатель «Защита для сайта», строка «Сайт работает не так?» и вход в «Подробнее».
- Формулировки на «сайт» («Shields down for this site») и подсказка «если сайт сломан — попробуй Shields down» прямо связывают переключатель со сценарием поломки. Хороший приём, но у Brave это выглядит как совет выключить защиту целиком. Vola может сначала предлагать мягкий шаг (ослабить уровень), а уже потом «выключить для сайта». Это совпадает с порядком в Debugging Guide.
- Отчёт о поломке у Brave лёгкий: текст, контакт по желанию, скриншот по желанию, явное «только необходимое». Если Vola когда-нибудь сделает отчёты, это потребует сетевого канала, а он противоречит правилу «никакой телеметрии» без решения владельца. Безопасная альтернатива — готовый текст отчёта для отправки на GitHub вручную.
- «Reset this site to defaults» и хорошо видимый признак того, что сайт отличается от глобальных настроек, — дешёвая и полезная функция.

### Gaps
- Не удалось подтвердить, показывает ли иконка Shields на Android/iOS в 2026 году бейдж с числом. В сообществе есть просьба «Show blocked count on Brave Shields icon» (февраль 2026), что косвенно говорит против, но это не подтверждено — [выдача поиска, Brave Community](https://community.brave.app/t/how-do-i-not-show-trackers-blocked-count-ads-blocked-count-https-upgrades-time-save-time/73336).
- Точная визуальная раскладка (нижний лист или полноэкранная панель, расположение элементов) на свежих скриншотах не проверена: brave.com и support недоступны.
- Строки панели Shields на iOS лежат в отдельном модуле, который я не нашёл. Для iOS известны только тексты Privacy Hub и онбординга.
- Отдельного переключателя «Block cookie consent notices» в панели сайта на Android в строках не нашёл (раздел 2).

## 2. Уровни по умолчанию, глобальные и пер-сайтовые настройки, функции вне Shields

### Takeaway
По умолчанию: трекеры и реклама — Standard (блокируются сторонние), HTTPS — «по возможности», сторонние cookie заблокированы, отпечатки — standard (рандомизация), скрипты разрешены. Баннеры cookie, de-AMP, дебаунсинг и очистка параметров URL включены по умолчанию, но это **глобальные** функции. В панель сайта они не входят, а очистку параметров вообще нельзя отключить.

### Cited Findings
- Глобальный экран на Android: «Brave shields global defaults» с пояснением «These are the default Shields settings for new sites. Changing these won't affect your existing per-site settings.» — [Android-строки](https://github.com/brave/brave-core/blob/master/browser/ui/android/strings/android_brave_strings.grd). Путь: ⋮ → Settings → Brave Shields & privacy — [Privacy Guides](https://github.com/privacyguides/privacyguides.org/blob/main/docs/mobile-browsers.md).
- Standard — режим по умолчанию: «лучшее для приватности большинства пользователей». Aggressive включает все средства, в том числе с повышенным риском поломки. Aggressive дополнительно блокирует **собственную** (first-party) рекламу и трекеры сайта — [выдача поиска по brave.com/shields](https://brave.com/shields/); [вики Features controlled by Shields](https://github.com/brave/brave-browser/wiki/Features-controlled-by-Shields).
- HTTPS по умолчанию: «Upgrade to HTTPS whenever possible (default)». Strict показывает промежуточную страницу, если перейти на HTTPS не удалось — [Android-строки](https://github.com/brave/brave-core/blob/master/browser/ui/android/strings/android_brave_strings.grd); [вики](https://github.com/brave/brave-browser/wiki/Features-controlled-by-Shields).
- Таблица Brave для десктопа: для сайта **и** глобально переключаются Ad & tracker blocking, Upgrade to HTTPS, Block scripts, Block fingerprinting (на десктопе ещё и по отдельным API), Block cookies, Forget me. Обрезка Referer отдельно не переключается: она идёт вместе с «Block cookies». Очистка параметров URL не переключается вовсе — [вики Features controlled by Shields](https://github.com/brave/brave-browser/wiki/Features-controlled-by-Shields).
- Вне Shields, только глобально: GPC; De-AMP; «Auto-redirect tracking URLs» (debouncing); «Reduce language fingerprinting»; Client hints; window.name. Brave сам признаёт, что часть из них «должна бы» быть в Shields — [там же](https://github.com/brave/brave-browser/wiki/Features-controlled-by-Shields).
- Тексты на Android: «Auto-redirect AMP pages — Always visit original (non-AMP) page URLs, instead of Google's Accelerated Mobile Page versions»; «Auto-redirect tracking URLs — Enable support for bypassing top-level redirect tracking URLs.»; «Prevent fingerprinting via language settings — Reduces how much websites can learn about you based on your browser's language settings.»; «Forget me when I close this site — Clears cookies and other site data when you close a site»; в контекстном меню — «Copy clean link» — [Android-строки](https://github.com/brave/brave-core/blob/master/browser/ui/android/strings/android_brave_strings.grd).
- Дебаунсинг превращает межсайтовые серверные редиректы во внутренние, чтобы промежуточный трекер не мог поставить cookie. Редиректоры, которые проверяют ссылки на безопасность (Facebook, Google), из правил исключены после отчёта на HackerOne. De-AMP сделан правилом дебаунсинга с привязкой к настройке — [вики Debouncing](https://github.com/brave/brave-browser/wiki/Debouncing).
- Очистка параметров удаляет параметры, привязанные к пользователю, email или отдельному клику, но не трогает агрегированные параметры кампаний. Срабатывает на межсайтовых GET-запросах: при навигации, загрузке ресурсов и редиректах. Исключение — ссылки отписки (например, `mkt_tok` остаётся, если в URL есть «unsubscribe»). Типы правил: simple, conditional, scoped. Это отдельная функция от «Copy clean link» — [вики Query String Filter](https://github.com/brave/brave-browser/wiki/Query-String-Filter).
- Баннеры cookie Brave блокирует **по умолчанию**. В 2023 году это сделано через фильтр-лист, отключается он через «EasyList Cookie» в Content Filtering. В 2025 году появился Cookiecrumbler: инструмент на открытых LLM, который находит баннеры и предлагает правила. Баннеры при этом скрываются, а не «отклоняются» кликом — [выдача поиска: brave.com/privacy-updates/21 и /33, Digitec 2025, BetaNews 27.04.2025](https://brave.com/privacy-updates/33-cookiecrumbler/).
- В каталоге листов «Cookie notice blocker» включён по умолчанию: источники Fanboy Cookiemonster, uBO annoyances-cookies и Brave cookie-specific. «Mobile app promo blocker» (Fanboy mobile notifications) тоже включён по умолчанию — [каталог листов](https://github.com/brave/adblock-resources/blob/master/filter_lists/list_catalog.json).
- Глобальный «Block all cookies» убирают: он даёт «ложное чувство безопасности» и ломает больше всего сайтов. Brave и так разделяет всё сетевое состояние по сайтам. Блокировать все cookie для отдельного сайта по-прежнему можно — [вики Block-all-cookies](https://github.com/brave/brave-browser/wiki/Block-all-cookies-global-Shields-setting).
- На iOS есть отдельный экран «Site and Shields Settings» и предупреждение «Shields already blocks 3rd-party cookies. This setting to block ALL cookies will remove existing cookies and site data, and could break websites.» — [iOS-строки](https://github.com/brave/brave-core/blob/master/ios/brave-ios/Sources/BraveStrings/BraveStrings.swift). На Android такого экрана со списком сайтов, где настройки изменены, нет. Пользователи просят его, ссылаясь на десктоп (запрос в сообществе, не официальное подтверждение) — [Brave Community](https://community.brave.app/t/mobile-missing-site-shields-settings-view-available-on-desktop/648970).
- Privacy Guides (для опытных) советует на Android глобально: Aggressive, оба auto-redirect, HTTPS strict, сторонние cookie заблокированы, отпечатки, язык, Auto Shred «Site Tabs Closed»; скрипты — по желанию. Дополнительные фильтр-листы **не включать**: они выделяют пользователя среди других и расширяют поверхность атаки — [Privacy Guides](https://github.com/privacyguides/privacyguides.org/blob/main/docs/mobile-browsers.md).

### Inferences
- Для Vola с Gecko: ETP Standard/Strict примерно соответствует Standard/Aggressive у Brave. Наш блокировщик — это слой «Trackers & ads». HTTPS-only по умолчанию у нас строже, чем у Brave (у Brave по умолчанию «whenever possible»). Это стоит подать в панели как преимущество, а для сайта давать исключение.
- Brave сам считает изъяном, что GPC, de-AMP, дебаунсинг и защита языка живут вне Shields. Vola может с самого начала собрать всё в одну модель «защита сайта» и в одно место настроек.
- Отказ Brave от глобального «Block all cookies» стоит перенять: глобально не давать опций, которые в основном ломают сайты; оставить их только для отдельного сайта.
- Нужен экран «Сайты с особыми настройками». На мобильных у Brave его нет, а пользователи просят.

### Gaps
- Включены ли на мобильных в 2026 году по умолчанию GPC и «Reduce language fingerprinting», первоисточником не подтверждено.
- Нет ли в 2026 году переключателя баннеров cookie для отдельного сайта на мобильных — не подтверждено.

## 3. Статистика на новой вкладке (трекеры и реклама, данные, время): вид и тексты

### Takeaway
Виджет на новой вкладке — три крупных числа с подписями «Trackers & Ads Blocked», «Est. Data Saved», «Est. Time Saved». Слово «Est.» прямо признаёт, что это оценка. Виджет можно скрыть, данные можно обнулить, есть кнопка «Поделиться».

### Cited Findings
- Подписи на Android: «Trackers & Ads\nBlocked», «Est. Data\nSaved», «Est. Time\nSaved» (на две строки, под крупным числом). Формат числа «%1$s%2$s», то есть число и единица отдельно. Переключатель «Show Brave Stats». Текст «Поделиться»: «Every day I save data by browsing the Web with Brave.» — [Android-строки](https://github.com/brave/brave-core/blob/master/browser/ui/android/strings/android_brave_strings.grd).
- Маркетинговые строки рядом: «Websites load faster — Loading less data means websites in Brave load faster - up to 8x faster than other browsers.» На iOS: «By blocking trackers & ads, websites use less data and load way faster.» — [Android-строки](https://github.com/brave/brave-core/blob/master/browser/ui/android/strings/android_brave_strings.grd); [iOS-строки](https://github.com/brave/brave-core/blob/master/ios/brave-ios/Sources/BraveStrings/BraveStrings.swift).
- На iOS виджет называется «Privacy Stats», действия у него: «Hide Privacy Stats» и «Open Privacy Hub», возвращается он в настройках New Tab Page — [iOS-строки](https://github.com/brave/brave-core/blob/master/ios/brave-ios/Sources/BraveStrings/BraveStrings.swift).
- «Время» Brave раньше считал очень грубо: число заблокированного × 50 мс. Сам Brave назвал этот способ «very conservative and somewhat naive» и предложил модель точнее (статья старая, вероятно 2020 года; какая формула сейчас — неизвестно) — [выдача поиска: brave.com/accurately-predicting-ad-blocker-savings](https://brave.com/accurately-predicting-ad-blocker-savings/).
- Обнулить счётчики на новой вкладке через интерфейс нельзя: пользователи правили файл Preferences, поля `ads_blocked` и `bandwidth_saved_bytes` (сообщения на форуме) — [Brave Community](https://community.brave.com/t/how-to-reset-the-time-saved-with-brave/436036). На Android есть «Clear all privacy reports data — Resets your privacy reports to zero», но относится ли это к виджету новой вкладки, неясно — [Android-строки](https://github.com/brave/brave-core/blob/master/browser/ui/android/strings/android_brave_strings.grd).
- На той же новой вкладке у Brave показываются Sponsored Images: на каждой 4-й новой вкладке, вперемешку с обычными фонами. Отключаются в Customize — [выдача поиска: brave.com/blog/introducing-sponsored-images](https://brave.com/blog/introducing-sponsored-images-in-brave/).

### Inferences
- Для Vola подходят три числа максимум. Для «данных» и «времени» нужна честная пометка «≈» или «оценка» и пояснение по нажатию, как это считается. Ещё нужны «скрыть виджет», «обнулить» и запрет на подсчёт в приватных вкладках (так сделано в iOS Privacy Hub, раздел 4).
- Метрики «загрузка до 8× быстрее» и счётчик времени при 50 мс за запрос воспринимаются как маркетинг. В Vola лучше показывать проверяемые числа (заблокировано запросов, сайтов с трекерами), а оценки помечать как оценки.

### Gaps
- Точная текущая формула «Data saved» и «Time saved» на мобильных не найдена: исходник для этого не читался, brave.com недоступен.

## 4. Privacy Hub (iOS) и Privacy Report (Android): что показывает, недельный отчёт

### Takeaway
На iOS Privacy Hub (с версии 1.38, 2022) — локальная сводка: за прошлую неделю — самый частый трекер и сайт с наибольшим числом трекеров; за всё время — списки трекеров и сайтов; при подписке на VPN — его оповещения. Раз в неделю приходит уведомление. На Android есть аналог «Privacy Report» с периодами Week / Month / 3 Months и тоже с недельным уведомлением. Обе версии подчёркивают, что данные хранятся только на устройстве.

### Cited Findings
**iOS (Privacy Hub)** — тексты из [iOS-строк](https://github.com/brave/brave-core/blob/master/ios/brave-ios/Sources/BraveStrings/BraveStrings.swift):
- Заголовок «Privacy Hub». Блок «Last week»:
  - «Most Frequent Tracker & Ad — **%@** was blocked by Brave Shields on **%lld** sites»;
  - «Site with the most trackers — **%@** had an average of **%lld** trackers & ads blocked per visit».
- Блок «All time»: строки «Tracker & Ad — %lld sites» и «Website — %lld trackers & ads». Кнопка «All time lists» ведёт к вкладкам «Trackers & ads» / «Websites» с заголовками «Most frequent trackers & ads on sites you Visit» и «Websites with the most trackers & ads». Колонка «Blocked by»: Shields или Firewall + VPN.
- Блок «Brave Firewall + VPN Alerts» с типами «Trackers & Ads», «Location Pings», «Email Trackers» и «Total count». Показывается только при подписке на VPN.
- Пустые состояния: «Visit some websites to see data here.», «No data to show yet.», «Enable '%@' to see data here».
- Уведомление: заголовок «Weekly Privacy Report», текст «A recap of how Brave protected you online this week.». Приглашение включить: «Get weekly privacy updates on tracker & ad blocking.» / «Turn on notifications».
- Настройки: «Show Shields Data» с пояснением «Privacy Hub shows a count of what Shields blocked. Setting will not affect Shields counter on new tab page. Shields data is not counted in private windows.»; «Clear Shields Data — Resets the count of everything Shields has blocked.»; подтверждение «Clear all Shields data?».
- Заявление о приватности: «Privacy Hub data is stored locally and never sent anywhere.»
- Privacy Hub появился в Brave 1.38 для iOS. Он показывает число заблокированных трекеров по сайту или за период (например, прошлую неделю) и связывает трекеры с компаниями, которым они принадлежат — [выдача поиска: brave.com/blog/1.38-release-ios](https://brave.com/blog/1.38-release-ios/) (2022, старая версия; есть ли сейчас группировка по компаниям — не проверено).
- Пользователи жалуются, что недельное уведомление навязчиво. Сотрудник Brave советует отключить «Show Shields Data» — [Brave Community](https://community.brave.app/t/disable-the-ios-weekly-privacy-report/413813).

**Android (Privacy Report)** — [Android-строки](https://github.com/brave/brave-core/blob/master/browser/ui/android/strings/android_brave_strings.grd):
- Заголовок «Privacy Report» («Privacy Stats»). Периоды: «Week» / «Month» / «3 Months». Метрики: «Trackers & Ads Blocked», «Data Saved (%1$s)», «Time Saved». Разделы: «Trackers», «Websites», «Top Visited Site». Пустое состояние: «No data to show yet».
- Приглашение: «Get weekly updates — Be informed about which websites you visit are the most privacy-invasive. Only you can see these privacy statistics. No personal data or browsing history ever leaves the Brave browser on your device.»
- Уведомление: «You blocked %1$s trackers & ads with Brave browser last week.» Переключатели «Sends a weekly privacy report» и «Turn on privacy reports». Сброс: «Clear all privacy reports data — Resets your privacy reports to zero».
- Запрос на уведомления смешан с наградами: «Turn notifications back on if you'd like to keep earning BAT and receiving weekly privacy reports.» / «Enable device notifications to earn Brave Rewards and see Privacy Reports.»

### Inferences
- Для «Отчёта о приватности» в Vola лучше всего подходит структура iOS: короткий «итог недели» (самый частый трекер и сайт с наибольшим числом трекеров, с понятными фразами), затем «за всё время» со списками, переключатель «учитывать данные», «стереть», пустые состояния и строка «хранится только на устройстве».
- Ошибку Brave повторять не надо: просьба включить уведомления смешана с наградами и криптовалютой (BAT). Недельное уведомление в Vola — только по явному согласию и выключено по умолчанию: пользователи называют его навязчивым.
- Приватные вкладки не учитывать (как на iOS). Это совпадает с правилом Vola «приватные вкладки ничего не пишут на диск».
- Нужен справочник «трекер → компания». У Gecko есть списки Disconnect с категориями и владельцами, но проверять это нужно отдельно, не в этом исследовании.

### Gaps
- Нет свежих скриншотов: графики или только числа, как выглядят списки.
- Есть ли на Android сводка «самый частый трекер / самый рискованный сайт», как на iOS, — по строкам неясно.

## 5. Фильтр-листы и косметическая фильтрация: что важно для продукта

### Takeaway
Brave блокирует движком adblock-rust с синтаксисом ABP и uBO (сеть, косметика, подмена ресурсов). По умолчанию включены скрытые от пользователя базовые наборы: фильтры uBO, EasyList, EasyPrivacy и uBO privacy, собственные листы Brave для first-party. Видимые и включённые по умолчанию — баннеры cookie и промо мобильных приложений. Остальное — «Content Filtering» с региональными листами, «Annoyances», пользовательскими URL и своими правилами.

### Cited Findings
- adblock-rust — движок встроенного блокировщика Brave, доступный как библиотека. Умеет блокировать запросы, косметическую фильтрацию, подмену ресурсов и расширения синтаксиса uBO. Различия с uBO Brave считает багами — [выдача поиска: lib.rs/docs.rs adblock](https://docs.rs/adblock/latest/adblock/).
- Скрытые листы, включённые по умолчанию: «Brave Default Adblock Filters» (uBO `filters*.txt`), «Brave Default Privacy Filters» (EasyPrivacy + uBO `privacy.txt`), «Brave First Party Adblock Filters», «Brave iOS-Specific Filters» (только iOS). Видимые и включённые по умолчанию: «Cookie notice blocker», «Mobile app promo blocker». Не включены по умолчанию: «Annoying distractions blocker» и региональные листы. Всего в каталоге 62 листа — [каталог листов](https://github.com/brave/adblock-resources/blob/master/filter_lists/list_catalog.json).
- Тексты Content Filtering на Android: «Enable custom filters that block regional and language-specific trackers and Annoyances»; «Filter lists — Additional popular community lists. Note that enabling too many filters will degrade browsing speeds.»; «Add custom filter list» с предупреждением «Only subscribe to lists from entities you trust…»; «Create custom filters» с отсылкой к «Adblock filter syntax»; у списка статус «Last updated %1$s» / «Update failed»; ручное «UPDATE» — [Android-строки](https://github.com/brave/brave-core/blob/master/browser/ui/android/strings/android_brave_strings.grd); [вики Update Adblock Lists](https://github.com/brave/brave-browser/wiki/Update-Adblock-Lists).
- Встроенные переключатели YouTube (2025–2026) на Android: «Block YouTube distracting elements», блок Shorts, «up next and end card», auto-dubbed, thumbnails, Playables, members-only — [Android-строки](https://github.com/brave/brave-core/blob/master/browser/ui/android/strings/android_brave_strings.grd).
- Как косметика защищает от ложных срабатываний: не прятать first-party элементы. Элемент с известным рекламным ID — прятать. Есть first-party ресурс — не прятать. Есть сторонний ресурс — прятать. Больше 5 слов текста — не прятать. CSS вставляется с приоритетом пользовательского стиля — [вики Cosmetic Filtering](https://github.com/brave/brave-browser/wiki/Cosmetic-Filtering) (страница старая; применяются ли эти правила к листам 2026 года, не проверено).
- Производительность косметики по замерам Brave: uBO около +17% процессорного времени, «blanket injection» около +4%, MutationObserver около +22%; память +4–5 МБ (на вкладку или на фрейм — не выяснено) — [там же](https://github.com/brave/brave-browser/wiki/Cosmetic-Filtering) (старые данные).
- Privacy Guides советует не добавлять листы сверх стандартных: это выделяет пользователя и расширяет поверхность атаки — [Privacy Guides](https://github.com/privacyguides/privacyguides.org/blob/main/docs/mobile-browsers.md).

### Inferences
- Решение для Vola: базовый набор не показывать пользователю как список. Видимыми оставить несколько понятных категорий («Баннеры cookie», «Промо приложений», «Отвлекающее», региональные), а «свои листы и правила» убрать в раздел для опытных с предупреждением о доверии и скорости.
- Gecko уже блокирует трекеры по спискам Disconnect, но не трогает рекламу и косметику. Собственный блокировщик Vola закрывает слой «реклама + косметика + баннеры cookie». В счётчике панели нужно не считать одни и те же блокировки дважды.
- Встроенные переключатели для отдельных сайтов (YouTube) пользователи ценят, но это постоянная ручная работа по поддержке. Это кандидат на позже.

### Gaps
- Какие региональные листы Brave включает автоматически по языку системы на Android, не проверено.
- Текущие замеры производительности косметики на мобильных не найдены.

## 6. Защита от отпечатков («farbling») и как она видна в интерфейсе

### Takeaway
Brave сочетает два подхода. Первый — нормализация: убрать или выровнять API. Второй — «farbling»: незаметный шум в значениях, с ключом, который зависит от сессии, сайта (eTLD+1) и хранилища. В 2024 году (1.64) Brave убрал строгий режим и оставил один стандартный. В интерфейсе это один переключатель «Block fingerprinting». Список затронутых API виден только на десктопе.

### Cited Findings
- Два вида защиты: (1) блокировать, убирать или менять API, чтобы экземпляры Brave выглядели одинаково, и возвращать пустые значения «правильной формы»; (2) рандомизировать значения, чтобы связать сессии и сайты было нельзя. Ключ шума — на сессию, на сайт и на хранилище. Сторонние фреймы получают ключ сайта верхнего уровня. Шум в одном значении «портит» весь хэш отпечатка — [вики Fingerprinting Protections](https://github.com/brave/brave-browser/wiki/Fingerprinting-Protections).
- FingerprintJS (открытая библиотека) по признанию самих её авторов не работает против Brave. Fingerprint Pro добирает идентификацию на сервере по IP и времени. Brave блокирует домены fingerprint.com и советует проверять защиту на EFF Cover Your Tracks и privacytests.org, а не на коммерческих демо — [там же](https://github.com/brave/brave-browser/wiki/Fingerprinting-Protections).
- Проверить работу защиты: browserleaks.com/canvas даёт разный отпечаток в приватном окне, после перезапуска, в другом профиле и после очистки данных сайта — [там же](https://github.com/brave/brave-browser/wiki/Fingerprinting-Protections).
- Где видно в интерфейсе: Shields → Advanced → стрелка у «Block fingerprinting» открывает список методов (скриншоты для десктопа) — [там же](https://github.com/brave/brave-browser/wiki/Fingerprinting-Protections). На Android это пункт «Block fingerprinting — Prevents websites from recognizing your device», а глобально ещё «Prevent fingerprinting via language settings» — [Android-строки](https://github.com/brave/brave-core/blob/master/browser/ui/android/strings/android_brave_strings.grd).
- Строгий режим убран в 1.64 (2024) на десктопе и Android. Причины: им пользовались меньше 0,5% (по телеметрии с возможностью отказа, так что цифра может быть занижена), он ломал сайты и отнимал силы инженеров. Стандартный режим Brave называет самым сильным среди крупных браузеров — [Privacy Guides forum](https://discuss.privacyguides.net/t/brave-removes-strict-fingerprinting-protection/16302); [gHacks](https://www.ghacks.net/?p=204966); [TechRadar](https://www.techradar.com/pro/security/braves-strict-fingerprint-protection-isnt-playing-nice-with-websites-so-its-getting-the-axe); [AlternativeTo](https://alternativeto.net/news/2024/1/brave-to-sunset-strict-fingerprinting-protection-mode-as-it-breaks-too-many-websites).
- Brave 1.38 для iOS (2022) усилил защиту от отпечатков на iOS до уровня десктопа и Android — [выдача поиска: brave.com/blog/1.38-release-ios](https://brave.com/blog/1.38-release-ios/) (старое).

### Inferences
- Для Vola на Gecko: у Gecko есть свои средства — `privacy.fingerprintingProtection` (FPP, рандомизация canvas и др., в ETP Strict) и `resistFingerprinting` (нормализация, много поломок). Опыт Brave подсказывает: один понятный переключатель «Защита от отпечатков» включён по умолчанию, без строгого режима в основном интерфейсе. Если строгий режим всё-таки нужен, место ему — в экспертном разделе. Детали Gecko проверять отдельно, не в этом исследовании.
- Пояснение для пользователя лучше строить на смысле («сайт не узнает ваше устройство при следующем визите»), а не на перечне API. Перечень API — в «Подробнее».
- Нужно заранее готовить ответ на «тест показывает, что меня видно»: Brave держит для этого отдельный раздел FAQ.

### Gaps
- Какие API рандомизирует Brave на мобильных в 2026 году, по мобильному интерфейсу не проверено.

## 7. Что критикуют в мобильном интерфейсе Brave: чего избегать

### Takeaway
Главная претензия — перегруженность монетизацией и побочными продуктами: Rewards/BAT, кошелёк, Leo AI, VPN, News, рекламные фоны на новой вкладке. В 2026 году Brave сам признал это, выпустив платную «минималистичную» Brave Origin, которая эти функции отключает. Отдельные претензии к Shields: на мобильных нет списка исключений по сайтам, нельзя скрыть счётчики и иконку, недельные уведомления навязчивы, блок элементов глючит.

### Cited Findings
- Brave Origin (апрель–июнь 2026) — платная версия «без функций, на которых Brave зарабатывает»: отключены Rewards, Leo, VPN, News, Speedreader, Tor, Talk, Wayback Machine и др., Shields остаются. Около $60 на Windows и Mac, бесплатно на Linux. Обычный Brave не меняется. Реакция неоднозначная: критики замечают, что всё это можно было просто выключить — [выдача поиска: Privacy Guides News 21.04.2026 и 07.06.2026; Thurrott; brave.com/blog/brave-origin](https://www.privacyguides.org/news/2026/04/21/brave-launches-paid-bloat-free-brave-origin/). Заголовок TweakTown: «Brave launches Origin, a $60 browser that removes the features it added without asking you first» — [TweakTown](https://www.tweaktown.com/news/112064/brave-launches-origin-a-60-browser-that-removes-the-features-it-added-without-asking-you-first/index.html) (заголовок из выдачи поиска).
- В Android-приложении уже есть экран Origin: «Minimalist browser UI centered on Brave Shields», «Maintain core adblock, privacy, & speed», «One-time purchase…», переключатели News, Email aliases, Privacy preserving analytics, Statistics reporting, Web Discovery Project, а смена функций требует перезапуска — [Android-строки](https://github.com/brave/brave-core/blob/master/browser/ui/android/strings/android_brave_strings.grd).
- Рекламные фоны на новой вкладке (каждая 4-я вкладка) вызывают жалобы, в 2025 году — на рекламу «сомнительных товаров» — [Brave Community](https://community.brave.app/t/ads-for-dubious-products-on-brave-home-screen/596437); [выдача поиска: brave.com/blog/introducing-sponsored-images](https://brave.com/blog/introducing-sponsored-images-in-brave/).
- В марте 2024 года Brave перестал сам устанавливать службы VPN и позволил отключать функции Leo — ответ на жалобы о навязанных функциях — [AlternativeTo news](https://alternativeto.net/software/brave/news/?p=3) (из выдачи поиска, старое).
- Privacy Guides для мобильных советует снять галочки с P3A, диагностических отчётов, ежедневного пинга и автодополнения Leo в адресной строке. Это значит, что по умолчанию всё это включено. На Android Safe Browsing не идёт через прокси Brave, Google видит IP — [Privacy Guides](https://github.com/privacyguides/privacyguides.org/blob/main/docs/mobile-browsers.md). На Android по умолчанию есть «Automatically send daily usage ping to Brave — This private ping lets Brave estimate active users.» — [Android-строки](https://github.com/brave/brave-core/blob/master/browser/ui/android/strings/android_brave_strings.grd).
- Счётчик на иконке Shields нельзя отключить, иконку Shields нельзя скрыть (запросы в сообществе остаются без решения) — [Brave Community](https://community.brave.com/t/how-to-disable-number-of-blocked-ads-and-trackers-on-brave-shields-icon/97717); [Brave Community](https://community.brave.app/t/please-allow-us-to-hide-the-shields-icon-without-disabling-it-and-provide-more-color-customization-options/433388).
- На мобильных нет обзора «Site & Shields Settings» с исключениями по сайтам (запрос в сообществе) — [Brave Community](https://community.brave.app/t/mobile-missing-site-shields-settings-view-available-on-desktop/648970).
- В строках запроса уведомлений недельный отчёт идёт в связке с заработком BAT (раздел 4) — [Android-строки](https://github.com/brave/brave-core/blob/master/browser/ui/android/strings/android_brave_strings.grd).
- Тон онбординга на iOS: «Congratulations. You're pretty special.» и «See all the bad stuff Brave blocked…» — [iOS-строки](https://github.com/brave/brave-core/blob/master/ios/brave-ios/Sources/BraveStrings/BraveStrings.swift); [Android-строки](https://github.com/brave/brave-core/blob/master/browser/ui/android/strings/android_brave_strings.grd).

### Inferences
- Чего Vola избегать: (1) смешивать приватность с монетизацией или промо в одном экране или запросе; (2) рекламы и промо на новой вкладке; (3) телеметрии по умолчанию (у Vola она запрещена правилом 5); (4) навязанных уведомлений; (5) «геймификации» счётчиков в стиле «You're pretty special». Тон Safari спокойнее и больше подходит для v5.
- Дать то, чего у Brave нет на мобильных: список сайтов с особыми настройками, возможность скрыть счётчик на иконке и сам виджет статистики, спокойный режим без уведомлений.
- Brave Origin — рыночное подтверждение, что спрос на «браузер, сведённый к Shields», есть. Это совпадает с курсом Vola v5 «Zen-минимализм плюс защита Brave».

### Gaps
- Свежих (2025–2026) обзоров мобильного Brave в Android Police, XDA и PCMag с конкретной критикой интерфейса я не получил: сайты недоступны, а поиск не дал профильных материалов. Критика выше взята из сообщества, Privacy Guides и новостей про Origin.
- Дата выхода Origin на Android (и выйдет ли он на iOS) не подтверждена. Есть только строки в Android-ресурсах и сообщения о выпуске для десктопа.
