# UI (Jetpack Compose)

Подгружается, когда работаешь с файлами `ui/`. Общие правила дизайна — в корневом `CLAUDE.md`.

- **Значения — только токены** из `ui/theme` (`VolaTokens.kt` и тематические `Vola*Tokens.kt`): цвета,
  размеры, формы, motion. Гейт `quality_gates.py tokens` проверяет добавленные строки.
- **Семантика:** не добавляй и не убирай `clearAndSetSemantics` по условию (`.then(if …)`, `if/else`
  в модификаторе) — узел пересоздаётся, и после анимации обзора адресная строка пропадала из TalkBack.
  Скрывать по флагу — `Modifier.clearSemanticsWhen(hidden)` (`ModalSemantics.kt`).
  Известные старые нарушения: `WideAddressTabStrip.kt` (строка с `.then(if (interactionEnabled)`),
  `hiddenUnderModal` — исправляются отдельным PR вместе с проверкой в `quality_gates.py`.
- **`testTag` ставь до `clearAndSetSemantics`**: всё после очистки, включая тег, отбрасывается, и тесты
  молча проходят впустую.
- Зона касания интерактивного элемента — не меньше 48 dp (`IconButton` даёт её сам; свой размер — через токен).
- Каждая новая строка — сразу в `values/` и `values-ru/` (плейсхолдеры как в оригинале, в русском
  множественном — one/few/many/other); проверка — `python3 scripts/test_translations.py`.
- UI-изменение — с Compose Preview (светлая и тёмная тема; тёмная — чистый чёрный).
- Крупные файлы (`BrowserScreen.kt`, `TabOverview.kt`, `MainActivity.kt`) не должны расти — гейт
  `size` и `scripts/ci/size-baseline.json`. Новый код — в отдельный файл функции (пример —
  `LaunchPrompts.kt` рядом с `MainActivity.kt`).
- Типы GeckoView/WebView здесь запрещены (гейт `engine`): UI видит только нейтральные порты.
