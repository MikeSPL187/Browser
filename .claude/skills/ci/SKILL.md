---
name: ci
description: "Find out why CI is red on a Vola pull request or branch: list the failed jobs, have the cheap ci-log-reader agent boil each log down, then fix the cause. Use when a CI failure event arrives, when the user asks why a build failed, and when the user types /ci."
---

# Разбор красного CI

Логи CI — сотни килобайт. Их читает агент `ci-log-reader` (`.claude/agents/`, дешёвая модель),
а сессия получает выжимку и чинит причину.

## Порядок
1. **Какие задачи упали.** `pull_request_read` (`get_check_runs`) для PR или
   `gh api repos/MikeSPL187/Browser/commits/<sha>/check-runs`; взять `failure` с последнего коммита.
   `gh run view --log` в облаке отвечает 403 — логи только через `mcp__github__get_job_logs`.
2. **Выжимка.** Для каждой упавшей задачи — агент `ci-log-reader` с id задачи (несколько — параллельно).
3. **Причина.** По выжимке найти место в коде. Частые случаи:
   - гейты `quality_gates.py` (`tokens`, `size`, `semantics`) — см. навык `/preflight`;
   - `MissingTranslation` / `test_translations.py` — строка не во всех `values*/strings.xml`;
   - ошибка компиляции — локально `./gradlew compileFullDebugKotlin` (SDK ставит хук старта сессии);
   - инструментальные тесты (#121) — раздел «New failures»; известные — в `scripts/ci/instrumented-baseline.txt`.
4. **Чинить и проверять** перед пушем (`scripts/ci/preflight.sh`). Не перезапускать задачу, чтобы
   «проскочило»: перезапуск — только если она упала до тестов (checkout, установка, потеря раннера).
5. Владельцу — одна строка: что упало и что исправлено.
