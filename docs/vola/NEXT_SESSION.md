# Промпт для следующей сессии

Скопируйте текст ниже целиком в новую сессию Claude Code (репозиторий MikeSPL187/Browser).

---

Продолжаем Vola — Android-браузер на GeckoView (форк Candy, MPL-2.0) в эстетике Zen Browser.
Я работаю только с телефона; источник истины для сборки — GitHub Actions. Отвечай по-русски,
коротко; экономь контекст: логи CI — только через grep и короткий хвост, картинки — только нужные.

**Где мы.** План — `docs/vola/ROADMAP.md`, состояние — `docs/vola/STATUS.md`. Открыты и ждут моего
слияния: #34 (Q3b), #35 (Q4), #36 (счётчик поиска), #37 (Q5a: Essentials, поверх #34), #38 (Q5b:
карточка защиты и недельный отчёт, база — ветка #37). Порядок: #34 → #35 → #36 → #37 → #38. После
каждого слияния влей `main` в оставшиеся ветки; конфликты ожидаются в `scripts/ci/size-baseline.json`
(решать `python3 scripts/ci/quality_gates.py size --update`), в `scripts/ci/capture_screenshots.py`
(оставить шаги обеих сторон) и в `docs/vola/*.md` (брать версию из самой свежей ветки). После
слияния #37 переключи базу #38 на `main`.

**Сначала прочитай:** `CLAUDE.md`; `docs/vola/STATUS.md`; `docs/vola/ROADMAP.md` (разделы 2–6,
карточка Q6, предложение П9); доску `W-PrivateTab`; `ui/NewTabPage.kt` (герой Candy для приватной
вкладки), `ui/theme/MaterialBrowserTheme.kt` (`privateMode`), `MainActivity.kt` (`CandyTheme`),
`browser/PrivateTabsNotifier.kt`, `browser/systemwebview/WebViewProfileRules.kt`.

**Шаг 0.** Проверь #34–#38: слиты ли, CI на последнем коммите, конфликты; что не слито — довести до
зелёного и спросить меня про слияние. Подпишись на события открытых PR.

**Задача — Q6: приватная вкладка** (Effort High). По доске W-PrivateTab:
1. Фиолетовая схема: `privateMode = selectedTab.isIncognito` в `CandyTheme` (`MainActivity`),
   перекраска пружиной без вспышки (анимировать роли цвета).
2. Новая вкладка в приватном режиме: значок-маска, «Приватная вкладка», фраза, 4 факта; вместо
   героя Candy (удалить `BlankTabModeMorph`, если больше не нужен). Факты — правдивые для движка:
   в WebView без мультипрофилей приватная вкладка делит профиль с обычными → флаг порта
   (`supportsEphemeralPrivateStorage` или подобный) и другой текст.
3. «Закрыть все приватные» (есть `closeAllPrivateTabs`), «Поиск без следов» в поле адреса.
4. Миниатюры приватных вкладок не пишутся на диск (`TabPreview`) — тест.
5. «Замок при выходе» — только если я принял П9; иначе строку с доски убрать.
6. Строки EN/RU, `@VolaPreviews`, шаг тура `private-*`, тесты.

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
- Слить #34, затем #35 (Q4), #36 (счётчик поиска), #37 (Q5a) и #38 (Q5b).
- Одобрить или поправить доску `W-ProtectionReport` (#38); решить П9 (замок приватных вкладок).
- Ответить на вопросы ROADMAP, раздел 6 (до Q5 и Q8).
- По желанию: секрет `VOLA_PREVIEW_KEYSTORE_BASE64` (*Settings → Secrets and variables → Actions*).
