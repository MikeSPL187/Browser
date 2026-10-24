# Промт: перенос макета всех экранов в код

Готовый промт для новой сессии Claude (вставить целиком). Макет — `docs/vola/design/canvas-screens/`
(карта — `README.md`), холст — https://claude.ai/artifact/JhnpiTiy1Z1oAaWArr7bzG. Решения владельца
(23.10.2026): макет нравится, переносить в код.

---

Ты работаешь в репозитории Vola (форк Candy Browser). Задача — привести интерфейс v5 на телефоне в
точное соответствие утверждённому макету. **Большая часть экранов v5 уже есть в коде** (`app/src/main/java/dev/sk2andy/materialbrowser/ui/v5/*`,
токены — `ui/theme/v5/*`). Поэтому работа — не «написать с нуля», а **найти расхождения между макетом и
кодом и закрыть их**.

## 0. Перед началом (обязательно)
1. Прочитай `CLAUDE.md`, `ui/CLAUDE.md`, `docs/vola/STATUS.md`, `docs/vola/design/system-v5.md`,
   `docs/vola/design/canvas-screens/README.md`.
2. Рекомендуй владельцу уровень Effort и жди ответа, если он не задан (правило 10): **Extra** для групп
   «движение» и «новые экраны», **High** для остальных.
3. Прочитай доску в описании #80 и открытые PR (правило 11). Своя строка на доске **до первого коммита**
   (навык `/claim`), по одной на каждый PR. Чужие файлы не трогай.
4. Время — по Москве (`TZ=Europe/Moscow date`).

## 1. Как читать макет
- Каждый файл `docs/vola/design/canvas-screens/*.dc.html` — один экран. Это HTML-исходник холста: стили
  в `style="…"` и переменные `--…` в `<helmet>`. Значения (цвета, размеры, радиусы, шрифты) берём оттуда
  **только как справку** и сверяем с токенами `system-v5.md`. В коде — **только токены** `ui/theme/v5`
  (`V5Colors`, `V5Type`, `V5Shapes`, `V5Spacing`, `V5Motion`, `V5Haptics`); захардкоженных значений нет,
  гейт `quality_gates.py tokens` это проверяет. Нужного токена нет — сначала добавь его в тему.
- `canvas.json` — расположение и названия артбордов. `Bar.dc.html` — эталон нижней панели.
- Каждый экран макета имеет `dark` (тёмная тема) и `accent` (шесть акцентов профиля) — оба должны
  работать в коде и иметь Compose Preview.
- Тексты макета — на русском; каждую строку добавь в `values/` **и** `values-ru/` (английский перевод
  пиши сам; множественное число русского — one/few/many/other). Проверка: `python3 scripts/test_translations.py`.
- Сайты и люди в макете вымышлены — в коде их быть не должно.

## 2. Соответствие «макет → код» (начни отсюда)
| Макет | Вероятный файл v5 |
|---|---|
| `Main`, `EmptyStart` | `V5NewTab.kt`, `V5NewTabRules.kt` |
| `Page`, `Bar`, `Panel` | `V5PageBar.kt`, `V5BottomBar.kt`, `theme/v5/V5SiteTint.kt` |
| `Address` | `V5AddressEditor.kt`, `V5SuggestionLayout.kt` |
| `Overview`, `OverviewV2`, `GroupEditor` | `V5TabOverview.kt`, `V5TabGroups.kt`, `V5TabGroupSheets.kt` |
| `Menu`, `MenuV2` | `V5PageMenu.kt`, `V5PageMenuRules.kt` |
| `Shield`, `HttpsOnly` | `V5SiteProtection.kt` |
| `Report` | `V5PrivacyReport.kt` |
| `Private`, `PrivateLock` | `V5PrivateNewTab.kt` и экран замка |
| `Danger`, `CertError`, `Error` | `V5StatePage.kt` |
| `Reader`, `ReaderSettings` | режим чтения (Gecko) + `V5ListenRow.kt` |
| `Glance` | `V5LinkGlance.kt` |
| `Translate` | `V5Translate.kt` |
| `Downloads` + кольцо | `V5Library.kt`, `V5DownloadRing.kt`, `V5DownloadNotice.kt` |
| `Profiles`, `NewProfile` | `V5ProfileSheet.kt`, `V5ProfileEditSheet.kt` |
| `Settings`, `Passwords`, `History` | `V5SettingsSkin.kt`, `V5PasswordHealth.kt`, `V5Library.kt`, `V5List.kt` |
| `Sync` | `V5SyncTransfer.kt`, `V5SyncSettings.kt` |
| `Onboarding` | `V5FirstRun.kt` |
| `FindInPage`, `HideMode`, `Permission` | `V5FindInPage.kt`, `V5DistractionsPanel.kt`, `V5PromptSheet.kt` |
| `Tablet*` | **не трогать** — планшет v5 отложен владельцем (13.10, 17.10) |

Таблица — отправная точка, а не истина: проверь `grep`-ом.

## 3. Порядок работы
**Шаг А — аудит расхождений (без кода).** Для каждого экрана из таблицы сравни макет с кодом и
превью-снимками (обход экранов в CI, артефакты ночных прогонов). Запиши результат документом
`docs/vola/design/canvas-screens/GAP-AUDIT.md`: экран · что совпадает · что расходится (размер, цвет, текст,
состояние, движение) · оценка (S/M/L) · нужен ли новый порт движка. **Покажи владельцу и жди «да»** — это
один небольшой PR, только документ.

**Шаг Б — реализация по группам.** Один PR — одна группа, строго в таком порядке (стабильность раньше функций):
1. **Исправления «было → стало»** (`FixRing`, `MenuV2`, `OverviewV2`). Кольцо загрузки цвета текста панели
   на цветной ленте — **меняет правило `system-v5.md`, раздел 10**: сначала PR с правкой документа и
   запрос «да» владельца.
2. **Недостающие состояния** (`EmptyHistory`, `EmptyStart`, `CertError`, `HttpsOnly`, `FindInPage`,
   `ReaderSettings`, `HideMode`, `Permission`, `NewProfile`, `GroupEditor`): пустое, загрузка, ошибка —
   у каждой ошибки слово и действие (S-08 из `ui/CLAUDE.md`).
3. **Расхождения основных экранов** по `GAP-AUDIT.md`, по ряду макета (главное → защита → чтение → профили).
4. **Доступность** (`Font200`, `Landscape`, `English`, `TalkBack`): шрифт 200 % без обрезки, альбомная
   раскладка, порядок фокуса и подписи для TalkBack. Добавь экраны в обход экранов (`AppScreenWalk`),
   не уменьшай проверки.
5. **Движение** (`Motion*`) — последним и по одному моменту на PR: морфинг адреса, свайп вкладок, вход в
   обзор, предпросмотр ссылки, тянущееся обновление. В макете это CSS-кривые, **в коде — пружины
   `V5Motion`** (значения в `system-v5.md`, раздел 7) и отдача `V5Haptics`. Режим без анимаций
   (`V5Motion.animationsEnabled`) — только смена прозрачности 150 мс (`reducedFade`). Нужны новые порты
   движка (снимок вкладки, растяжение страницы) — сначала порт (`interface` в `browser/engine`), потом
   Gecko-адаптер в `src/full`; WebView — скрыть функцию в UI.

## 4. Правила каждого PR
- Один PR — одна функция/группа, маленький. Заголовок — Conventional Commits на английском, описание на русском,
  с чек-листом ручной проверки на устройстве (что нажать, что должно произойти).
- Compose Preview: светлая и тёмная тема (v5 тёмная — тёмно-серая `#1C1C1E`), акцент профиля, шрифт 200 %.
- Не ломай разделение «порты → адаптеры»: типы GeckoView/WebView только в адаптерах (гейт `engine`).
- Крупные файлы (`BrowserScreen.kt`, `TabOverview.kt`, `MainActivity.kt`) не растут — новый код в новый файл
  (гейт `size`, `size-baseline.json`).
- Семантика и `testTag` — по правилам `ui/CLAUDE.md` (тег до `clearAndSetSemantics`, без условной очистки).
- Цели касания ≥ 48 dp. Без телеметрии и сторонних SDK. Без бренда «Candy».
- Перед пушем: `/preflight` (или `python3 scripts/ci/quality_gates.py size|tokens|engine|brand` и
  `python3 scripts/test_translations.py`); с SDK — `./gradlew testFullDebugUnitTest lintFullDebug`. Без SDK —
  скажи об этом и положись на CI.
- Каждый PR зелёный в CI (в том числе «Cold start and frames» и видео сценариев). Ухудшение бюджетов
  (`plan-v5.md`, раздел 7) краснит PR — не обходи меткой без слова владельца.
- Сливать — по слову владельца (правило 12); выпуск релиза — тоже. Есть рекомендованный вариант — бери его и
  пиши в PR, что выбрал и почему (правило 13).
- В конце каждой сессии обнови строку на доске #80 и итог в описании PR; `STATUS.md` ведёт ведущая сессия.

## 5. Чего не делать
- Не переписывать то, что уже совпадает с макетом.
- Не добавлять функций, которых нет в макете и в `plan-v5.md` (принцип «сказать нет»).
- Не трогать планшет, код v4 и платформу сборки WebView сверх «скрыть, где функции нет».
- Не копировать код других браузеров.
- Не выдумывать значения: если макет и `system-v5.md` расходятся, верна `system-v5.md` — поставь вопрос
  владельцу в PR.

## 6. Критерий готовности
Для каждой группы: экраны в коде визуально совпадают с макетом в обеих темах и во всех шести акцентах;
превью и обход экранов зелёные; видео сценария из CI приложено; владелец проверил чек-лист на телефоне
и сказал «да».
