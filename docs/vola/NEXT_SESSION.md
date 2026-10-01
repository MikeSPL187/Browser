# Промпт для следующей сессии

Скопируйте текст ниже целиком в новую сессию Claude Code (репозиторий MikeSPL187/Browser).

---

Продолжаем Vola — Android-браузер на GeckoView (форк Candy, MPL-2.0) в эстетике Zen Browser.
Я работаю только с телефона; источник истины для сборки — GitHub Actions. Отвечай по-русски,
коротко; экономь контекст: логи CI — только через grep, картинки — только нужные.

**Где мы.** План работ — `docs/vola/ROADMAP.md`. Слиты: Q1 (#31, зависимости), Q2 (#32,
GeckoView 157 и ежедневный `geckoview-update.yml`), Q3a (#33, проверки качества в CI:
`scripts/ci/quality_gates.py`), Q3b (#34, доступность в туре, шрифт 200 %, холодный старт,
`@VolaPreviews`). Фаза A (фундамент) закрыта — начинаем экраны.

**Сначала прочитай:** `CLAUDE.md`; `docs/vola/STATUS.md`; `docs/vola/ROADMAP.md` (разделы 2–5,
карточка Q4, предложение П1); доски `docs/vola/design/canvas/W-Editing.dc.html`,
`V4A-Address.dc.html`, `V4B-Address.dc.html`, `W-Components.dc.html`; токены
`app/.../ui/theme/Vola*.kt`; `ui/ExpandedAddressBar.kt`, `ui/AddressSuggestions.kt`,
`browser/AddressBarController.kt`.

**Шаг 0.** Если #34 не слит — доведи CI до зелёного. Проверь, что Dependabot закрыл #21 и #22.

**Задача 1 — Q4: ввод адреса и подсказки** (Effort High). Строка ввода у большого пальца на
месте острова, морф капсула → строка пружиной `VolaMotion`, чип «ссылка из буфера» и «Перейти» у
ввода, группы подсказок на токенах v4; состояние ввода — в `AddressBarController`. Буфер обмена:
до нажатия читать только `ClipDescription` (иначе системное уведомление Android 12+). Предложение
П1 («Перейти на вкладку» с самоцветом пространства) — спроси меня до кода. Порядок PR — ROADMAP,
раздел 3: доска → порт (если нужен) → UI на токенах → строки EN/RU → `@VolaPreviews` → тесты →
шаг в туре → CI и снимки → таблица «Vola vs лидер» и чек-лист в описании.

**Задача 2 (следующим PR) — счётчик поиска «0/0»** (Effort High, небольшой). В тёмном проходе тура
на GeckoView 156 дважды подряд счётчик показывал «0/0» при подсвеченных совпадениях (#31,
`bbeb28f`, `6b8d08d`). Разобрать результаты `GeckoSession.finder` (`GeckoViewRuntimeHandle`,
`FindInPageRules`, `BrowserController.updateFindInPageQuery`) и показывать «ищу…», пока Gecko не
вернул окончательный итог; тест на правило; шаг `find-*` в туре.

**Дальше по очереди (ROADMAP, раздел 2):** Q5 Essentials и новая вкладка → Q6 приватная вкладка →
Q7 обзор вкладок (Extra) → Q8 пространства: свайп, создание, настройки (Extra) → Q9 меню плитками →
Q10 сведения о сайте → веха «первый подписанный релиз» → фаза C (Compact, чтение, Glance, Split
View). Перед Q5 и Q8 нужны мои решения (раздел 6: Essentials своя/общая, раскладка жестов).

**Правила (из `CLAUDE.md` и ROADMAP, обязательны):** одна фича — один PR, ветка от свежего `main`;
код и коммиты на английском (Conventional Commits), описание PR на русском; только токены
`ui/theme` (проверка `tokens` в CI; исключение — `// token-exempt: причина`); строки сразу в
`values/` и `values-ru/`; CI зелёный, снимки эмулятора просмотрены, новых находок доступности нет;
после открытия PR — подписка на события. Перед каждой задачей — рекомендация Effort.

**Не делай без моего отдельного слова:** не сливай PR и не начинай следующий пункт очереди.

В конце работы обнови `docs/vola/STATUS.md`, `docs/vola/ROADMAP.md` и этот файл.

---

## Что должен сделать владелец (вне сессии)

- *Settings → Actions → General → Allow GitHub Actions to create and approve pull requests* — чтобы
  воркфлоу обновления GeckoView мог открывать PR.
- Ответить на вопросы ROADMAP, раздел 6 (до Q5 и Q8).
- По желанию: секрет `VOLA_PREVIEW_KEYSTORE_BASE64` (*Settings → Secrets and variables → Actions*).
