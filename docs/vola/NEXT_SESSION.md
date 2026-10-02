# Промпт для следующей сессии

Скопируйте текст ниже целиком в новую сессию Claude Code (репозиторий MikeSPL187/Browser).

---

Продолжаем Vola — Android-браузер на GeckoView (форк Candy, MPL-2.0) в эстетике Zen Browser.
Я работаю только с телефона; источник истины для сборки — GitHub Actions. Отвечай по-русски,
коротко; экономь контекст: логи CI — только через grep и короткий хвост, картинки — только нужные.

**Где мы.** План — `docs/vola/ROADMAP.md`, состояние — `docs/vola/STATUS.md`. Открыты и ждут моего
слияния: #34 (Q3b), #35 (Q4), #36 (счётчик поиска), #37 (Q5a: Essentials, поверх #34), #38 (Q5b:
карточка защиты, база — ветка #37), #39 (Q6: приватная вкладка, база — ветка #38). Порядок:
#34 → #35 → #36 → #37 → #38 → #39. После каждого слияния влей `main` в оставшиеся ветки; конфликты
ожидаются в `scripts/ci/size-baseline.json` (решать `python3 scripts/ci/quality_gates.py size --update`),
в `scripts/ci/capture_screenshots.py` (оставить шаги обеих сторон) и в `docs/vola/*.md` (брать версию
из самой свежей ветки). После слияния базовой ветки переключай базу следующего PR на `main`.

**Сначала прочитай:** `CLAUDE.md`; `docs/vola/STATUS.md`; `docs/vola/ROADMAP.md` (разделы 2–6,
карточка Q7, предложения П2, П3); доски `W-Tabs`, `W-TabActions`, `W-TabsDark`, `V4A-Tabs`,
`V4B-Tabs`; `ui/TabOverview.kt` (структура), `ui/TabOverviewModels.kt`, `ui/ProfileSwitcher.kt`.

**Шаг 0.** Проверь #34–#39: слиты ли, CI на последнем коммите, конфликты; что не слито — довести до
зелёного и спросить меня про слияние. Подпишись на события открытых PR. Если я ответил по П9 —
замок приватных вкладок отдельным PR на механизме `ProfileProtection`.

**Задача — Q7a: обзор вкладок v4, раскладка** (Effort **Extra**). Q7 делится на 7a и 7b:
- 7a: миниатюры без двойной рамки на ореоле, ряд Essentials (`controller.essentials`), переключатель
  пространств внизу у пальца; подпись карточки вкладки для TalkBack (долг из Q3b — удалить строку
  `tab-overview` из `scripts/ci/a11y-baseline.txt`); `TabOverview.kt` разбить на файлы.
- 7b: действия с вкладкой листом, смахивание с «Отменить», морф «вкладка ↔ обзор» (П2),
  `browser/TabsController.kt`.
До кода — план из 3–7 шагов и мои решения по П2 (морф) и П3 (автоархив).

**Правила (из `CLAUDE.md` и ROADMAP, обязательны):** одна фича — один PR; код и коммиты на английском
(Conventional Commits), описание PR на русском с таблицей «Vola vs лидер» и чек-листом; только токены
`ui/theme` (исключение — `// token-exempt: причина`); строки сразу в `values/` и `values-ru/`; перед
пушем `python3 scripts/ci/quality_gates.py size|tokens --base <база>|engine|brand`,
`python3 scripts/test_translations.py` и JVM-проект из STATUS («Заметки о среде») для чистых частей;
CI зелёный, снимки эмулятора просмотрены глазами (контактный лист); после открытия PR — подписка.

**Не делай без моего отдельного слова:** не сливай PR.

В конце работы обнови `docs/vola/STATUS.md`, `docs/vola/ROADMAP.md` и этот файл.

---

## Что должен сделать владелец (вне сессии)

- *Settings → Actions → General → Allow GitHub Actions to create and approve pull requests* — чтобы
  воркфлоу обновления GeckoView мог открывать PR.
- Слить #34, затем #35 (Q4), #36 (счётчик поиска), #37 (Q5a), #38 (Q5b) и #39 (Q6).
- Одобрить или поправить доску `W-ProtectionReport` (#38); решить П9 (замок приватных вкладок).
- Ответить на вопросы ROADMAP, раздел 6 (до Q5 и Q8).
- По желанию: секрет `VOLA_PREVIEW_KEYSTORE_BASE64` (*Settings → Secrets and variables → Actions*).
