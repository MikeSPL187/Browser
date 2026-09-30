# Промпт для следующей сессии

Скопируйте текст ниже целиком в новую сессию Claude Code (репозиторий MikeSPL187/Browser).

---

Продолжаем Vola. В `main` слиты PR 1–3 плана (токены, значки, «Рама», «Остров», поиск по
странице) и первая часть PR 5 (новая вкладка на ореоле, «Продолжить»). Холст:
https://claude.ai/artifact/CEgKgiZHg4zuju2u7ALZ8C.

**Сначала прочитай:**
- `CLAUDE.md`;
- `docs/vola/STATUS.md`;
- `docs/vola/tech-plan.md`, раздел 4;
- `docs/vola/design/review-v4.md`;
- доски `docs/vola/design/canvas/W-Main.dc.html`, `W-Scrolled.dc.html`, `W-Editing.dc.html`,
  `W-Find.dc.html`, `W-Reader.dc.html`, `W-Compact.dc.html`, `V4B-Page.dc.html` («Воздух»);
- токены `app/.../ui/theme/Vola*.kt`, рамку `ui/BrowserContentFrame*.kt`, значки
  `shared/.../ui/icons/VolaIcons.kt`.

Перед работой порекомендуй мне уровень Effort (правило 10) и дай план из 3–7 шагов. Если план
совпадает с заданием — сразу начинай.

**Шаг 0.** Доведи Dependabot #21 и #22 до зелёного; после слияния #22 замени `currentResources()`
на `LocalResources.current`. Проверь снимки `find-*` в туре скриншотов.

**Задача (по одной за сессию, спроси, с какой начать):**
1. Остаток PR 5 (High): сетка Essentials по доске (плитки 68 dp, «Изменить»), карточка защиты,
   приватная вкладка на своём фиолетовом.
2. Остаток PR 3 (Extra): режим чтения (доска Reader) и Compact Mode (доска Compact).
3. PR 6 (High): главное меню плитками, сведения о сайте. PR 7 (High): экраны состояний и защиты.
4. PR 4 (Extra): обзор вкладок с переключателем пространств.

**Правила (из `CLAUDE.md`, обязательны):** код и коммиты на английском, описание PR на русском с
чек-листом; никаких захардкоженных цветов и размеров вне `ui/theme`; строки сразу в `values/` и
`values-ru/`; CI должен быть зелёным; после открытия PR подпишись на события.

**Не делай без моего отдельного слова:** следующие PR плана (4 — обзор вкладок, …).

В конце работы обнови `docs/vola/STATUS.md` и этот файл.

---

## Что должен сделать владелец (вне сессии)

- Добавить секрет `VOLA_PREVIEW_KEYSTORE_BASE64`:
  Settings → Secrets and variables → Actions → New repository secret.
  Значение — из файла, который Claude прислал в сессии 2026-09-30.
- Удалить ветки слитых PR на странице *Branches*.
- Включить *Settings → General → Automatically delete head branches*.
- Проверить PR #25, #26, #27, #28 по чек-листам в описаниях и слить.
