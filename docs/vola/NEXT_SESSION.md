# Промпт для следующей сессии

Скопируйте текст ниже целиком в новую сессию Claude Code (репозиторий MikeSPL187/Browser).

---

Продолжаем Vola. Открыты PR #25 (токены темы v4), #26 (lint для обновления Compose) и #27
(значки Material Symbols Rounded). Холст: https://claude.ai/artifact/CEgKgiZHg4zuju2u7ALZ8C.

**Сначала прочитай:**
- `CLAUDE.md`;
- `docs/vola/STATUS.md`;
- `docs/vola/tech-plan.md`, раздел 4;
- `docs/vola/design/review-v4.md`;
- доски `docs/vola/design/canvas/W-Main.dc.html`, `W-Scrolled.dc.html`, `W-Editing.dc.html`,
  `W-Find.dc.html`, `W-Reader.dc.html`, `W-Compact.dc.html`;
- токены `app/.../ui/theme/Vola*.kt` и значки `shared/.../ui/icons/VolaIcons.kt`.

Перед работой порекомендуй мне уровень Effort (правило 10) и дай план из 3–7 шагов. Если план
совпадает с заданием — сразу начинай.

**Шаг 0.** Проверь #25, #26, #27: если какой-то не слит и CI красный — почини. Сливать без моего
слова не надо. Если #26 слит — команда Dependabot `rebase` в #21 и #22 и доведи их до зелёного.

**Задача: PR 3 плана — рама пространства и адресная панель-остров (Effort: Extra).** Только после
слияния #25 и #27. Отдельный PR от свежего `main`:
1. «Рама»: оболочка на ореоле пространства (`VolaTheme.extendedColors.aura`), страница —
   карточка со скруглением `VolaShapes.cardRadius`; «Воздух»: страница на весь экран, ореол в
   ободке острова. Выбор — `AppearanceSettings.chromeStyle`.
2. Адресная панель-остров по доскам Main/Scrolled/Editing: сжатие в капсулу при прокрутке,
   пружины `VolaMotion`, значки `VolaIcons`.
3. Вынести логику адресной панели из `BrowserController` в `AddressBarController` без смены
   поведения; движки не трогать напрямую (только порты).
4. Превью: светлая/тёмная, «Рама»/«Воздух», крупный шрифт 200 %.

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
- Проверить PR #25, #26, #27 по чек-листам в описаниях и слить.
