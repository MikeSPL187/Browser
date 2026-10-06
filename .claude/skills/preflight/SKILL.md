---
name: preflight
description: "Run the checks CI runs first (quality gates, script tests, and with the Android SDK the Kotlin compile and unit tests) before a commit or push, and read the result. Use before every push of a Vola branch, when a gate or test fails, and when the user types /preflight."
---

# Проверка перед пушем

Одна команда делает то, что CI проверяет первым (`docs/vola/claude-tooling.md`):

```bash
scripts/ci/preflight.sh          # гейты, тесты скриптов, node-тесты, компиляция Kotlin и юнит-тесты
scripts/ci/preflight.sh --fast   # без Gradle: секунды, а не минуты
```

`.githooks/pre-push` запускает её сам перед каждым `git push` (хук старта сессии включает
`core.hooksPath .githooks`). `git push --no-verify` — только когда причина названа владельцу.

## Порядок
1. Перед пушем — `scripts/ci/preflight.sh` (или дать отработать хуку `pre-push`).
2. Если SDK ещё ставится (сообщение «no Android SDK yet»), Gradle пропускается; лог установки —
   `$ANDROID_HOME/.vola-sdk-install.log`. Повторить позже или пушить с проверкой без Gradle.
3. Провал печатает хвост вывода упавшего шага. Чинить причину, а не обходить:
   - `gate tokens` — литералы `dp/sp`/цветов в добавленных строках → токены из `ui/theme`;
   - `gate size` — файл вырос → вынести код; уменьшился → `quality_gates.py size --update`;
   - `gate semantics` — `Modifier.clearSemanticsWhen(...)`, `testTag` перед `clearAndSetSemantics`;
   - `scripts/test_translations.py` — строка есть в `values/`, но нет в `values-ru/` (или наоборот);
   - Gradle — читать первую ошибку компилятора или упавший тест.
4. Maven Central иногда отвечает 429 (общий исходящий IP облака): Gradle повторяет запросы сам,
   скрипт задаёт больше повторов; при повторном провале — подождать и запустить снова.
5. Доложить владельцу одной строкой: что прошло, что нет.

## Нельзя
- Пушить с красной проверкой, не сказав владельцу.
- Отключать или пропускать тесты, чтобы проверка прошла.
