# Промпт для следующей сессии

Скопируйте текст ниже целиком в новую сессию Claude Code (репозиторий MikeSPL187/Browser).

---

Продолжаем Vola. PR 1 переноса дизайна v4 (токены темы) — PR #25. Холст:
https://claude.ai/artifact/CEgKgiZHg4zuju2u7ALZ8C.

**Сначала прочитай:**
- `CLAUDE.md`;
- `docs/vola/STATUS.md`;
- `docs/vola/tech-plan.md`, раздел 4;
- `app/src/main/java/dev/sk2andy/materialbrowser/ui/theme/` — новые токены v4: `VolaColors.kt`,
  `VolaSchemes.kt` (генерируется, руками не править), `VolaType.kt`, `VolaTokens.kt`.

Перед работой порекомендуй мне уровень Effort (правило 10) и дай план из 3–7 шагов. Если план
совпадает с заданием — сразу начинай.

**Шаг 0. PR #25.** Если он ещё открыт: проверь CI на последнем коммите и доведи его до зелёного.
Сливать без моего слова не надо.

**Задача: lint `LocalContextGetResourceValueCall` (Effort: High).** Отдельный PR от свежего `main`.
1. Dependabot #21 (Kotlin 2.4.20, Compose Multiplatform 1.12) и #22 (Compose BOM 2026.09) красные:
   новая проверка lint запрещает `context.getString(...)` / `context.resources` внутри Compose.
   Список ошибок — в шаге «Show lint errors» их CI (18 штук, первая `ui/BrowserScreen.kt:423`).
2. Замени такие вызовы на `stringResource` / `pluralStringResource` или `LocalResources.current`
   там, где строка нужна в лямбде. Поведение не менять, строки не добавлять.
3. После слияния перезапусти #21 и #22 (комментарий `@dependabot rebase`) и убедись, что они
   зелёные.

**Правила (из `CLAUDE.md`, обязательны):**
- Код, комментарии и коммиты — на английском (Conventional Commits); описание PR — на русском с
  чек-листом ручной проверки.
- Никаких захардкоженных цветов и размеров вне `ui/theme`; новые значения — через токены v4.
- Имена, унаследованные от Candy, не переименовывай.
- Android SDK в облаке нет — сборку, тесты и lint проверяет CI, он должен быть зелёным. Части кода
  без Android можно проверить в JVM-проекте на Compose Desktop (см. «Заметки о среде» в
  `STATUS.md`).
- После открытия PR подпишись на его события и доведи CI до зелёного.

**Не делай без моего отдельного слова:** следующие PR плана (2 — значки Material Symbols, 3 — рама
и адресная панель, …).

После PR предложи, что брать следующим, с рекомендацией Effort. В конце работы обнови
`docs/vola/STATUS.md` и этот файл.

---

## Что должен сделать владелец (вне сессии)

- Добавить секрет `VOLA_PREVIEW_KEYSTORE_BASE64`:
  Settings → Secrets and variables → Actions → New repository secret.
  Значение — из файла, который Claude прислал в сессии 2026-09-30.
- Удалить ветки слитых PR на странице *Branches*.
- Включить *Settings → General → Automatically delete head branches*.
- Проверить PR #25 по чек-листу в его описании и слить.
