# uBlock Origin и уровни защиты сайта

_Исследование v5 · защита по сайту, часть 2 · 2026-10-11 · `main` после #172, uBO 1.75.0, GeckoView 157.
Только доклад: выбор — за владельцем (раздел «Варианты»)._

**Коротко.** На Gecko рекламу и трекеры по сети блокирует только встроенный uBlock Origin. Наше
расширение `candy_privacy` режет лишь несколько хостов консент-платформ. Управлять uBO по сайтам из Vola
нельзя: у него нет API для других расширений, а настройки администратора он читает из
`storage.managed`, которого нет в Firefox для Android. Поэтому уровни «Выкл» и «Строгий» сейчас не
действуют на uBO. Сайт на паузе у Vola блокируется uBO как прежде. В плане v5 владелец уже решил:
«uBlock Origin не встраиваем, свой блокировщик со списками EasyList/EasyPrivacy/uBO». Только так уровни
станут полными.

## 1. Как сейчас устроена блокировка на Gecko

- `assets/gecko_default_extensions/catalog.json`: uBO 1.75.0 и «I still don't care about cookies» 1.1.9
  ставятся при первом запуске как обычные подписанные расширения (`delivery: bundled`). Пользователь
  может их выключить или удалить.
- `candy_privacy/background.js` (`webRequest.onBeforeRequest`): учитывает паузу сайта и блокирует только
  `cmp.inmobi.com`. Косметика и правила Vola — в `rules.js`, по флагу `blockAds`.
- Наш сетевой блокировщик со списками (`RequestBlocker`, EasyList, hagezi, uAssets) работает только в
  System WebView.
- ETP Gecko (`GeckoRuntimeSettingsFactory`) — по-прежнему строгий. Пауза сайта снимает его через
  разрешение `PERMISSION_TRACKING`.

## 2. Можно ли управлять uBO извне

| Путь | Что выяснилось | Итог |
|---|---|---|
| Сообщения от другого расширения | В исходнике uBO нет `runtime.onMessageExternal` и `onConnectExternal` (`messaging.js`, `vapi-background.js`) | нет |
| Настройки администратора (`toOverwrite.trustedSiteDirectives`, белый список) | Только через `storage.managed` (`vapi-background.js`, `vAPI.adminStorage`). В Firefox для Android есть хранилища local, session и sync, managed — нет (Extension Workshop). uBO читает их один раз, при старте | нет |
| GeckoView: выключить расширение для одного сайта или вкладки | В `WebExtensionController` есть только «включено/выключено» целиком и приватный режим | нет |
| Своя сборка uBO с API для Vola | Неподписанное встроенное расширение (`installBuiltIn`) примет патч. Но это форк GPL-3.0, который нужно обновлять с каждым выпуском uBO и публиковать его исходники. Противоречит решению «uBO не встраиваем» | возможно, дорого |
| Открыть окно uBO из панели сайта | Vola уже нажимает кнопки расширений (`clickExtensionAction`). Пользователь сам выключит uBO для сайта большой кнопкой | возможно, вручную |

## 3. Варианты

**Решение владельца 2026-10-11: вариант A** (`decisions.md`).

**A. Свой блокировщик на Gecko вместо uBO (рекомендую, это уже решение плана v5).**
Сетевую блокировку переносим в `candy_privacy`: те же списки, что у WebView, скомпилированные в
компактный формат, с проверкой в `onBeforeRequest`. Тогда уровни управляют всем: «Выкл» снимает
блокировку, «Строгий» отключает списки совместимости. uBO перестаёт ставиться при новых установках. У
тех, у кого он уже стоит, остаётся по их выбору; Vola покажет подсказку, что он больше не нужен.
- Цена: несколько PR (Extra): формат правил и компилятор, проверка в расширении, перенос косметики,
  замеры скорости и памяти на гейтах V2–V3, размер APK.
- Риск: качество блокировки ниже uBO на сложных сайтах (скриплеты, `$redirect`) до тех пор, пока не
  перенесены нужные возможности. Список отличий — в первом PR.

**B. Оставить uBO, уровни без него.**
Уровни управляют cookie, Referer, правилами Vola, ETP и отпечатками. uBO блокирует одинаково на всех
уровнях. В панели честно сказано, что «Выкл» не выключает uBO, и есть кнопка «Открыть uBlock Origin».
- Цена: только часть 2, как задумано. Минус: «Выкл» не чинит сайт, который ломает uBO.

**C. Свой форк uBO с API для Vola.**
Полное управление при качестве uBO. Минусы: форк GPL-3.0, обновлять его с каждым выпуском uBO,
противоречит решению плана v5.

Если выбран A, порядок такой: часть 2 защиты по сайту без uBO (как B) → passkeys (решение владельца) →
свой блокировщик на Gecko отдельной серией PR. После его появления «Выкл» и «Строгий» сразу начнут
действовать на рекламу.

## Источники

- uBO 1.75.0 (`21f0e686`): [`platform/common/vapi-background.js`](https://github.com/gorhill/uBlock/blob/21f0e686506bb21b514b451c8c6cb9bf8c82d232/platform/common/vapi-background.js) (`vAPI.adminStorage`),
  [`src/js/storage.js`](https://github.com/gorhill/uBlock/blob/21f0e686506bb21b514b451c8c6cb9bf8c82d232/src/js/storage.js) (`toOverwrite`),
  [`src/js/start.js`](https://github.com/gorhill/uBlock/blob/21f0e686506bb21b514b451c8c6cb9bf8c82d232/src/js/start.js).
- [Deploying uBlock Origin: configuration](https://github-wiki-see.page/m/uBlockOrigin/uBlock-issues/wiki/Deploying-uBlock-Origin:-configuration).
- [Differences between desktop and Android extensions](https://extensionworkshop.com/documentation/develop/differences-between-desktop-and-android-extensions/) — хранилища на Android.
- Vola: `plan-v5.md` раздел 1 («uBlock Origin не встраиваем»), `docs/privacy-protection/blocking-and-rules.md`,
  `candy_privacy/background.js`, `gecko_default_extensions/catalog.json`.
