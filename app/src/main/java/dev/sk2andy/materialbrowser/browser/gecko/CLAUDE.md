# Адаптер GeckoView

Подгружается, когда работаешь с файлами `browser/gecko/`. Типы GeckoView живут только здесь и в
`app/src/main/java/org/mozilla/geckoview/` (мосты к закрытым API); UI и ViewModel видят нейтральные порты.

- **Новая возможность движка:** сначала порт (interface) в общем коде, затем реализация для Gecko и
  System WebView; если в WebView невозможно — фича явно скрыта в UI для flavor `systemwebview`.
- **Настройки Gecko без публичного API** — через `CandyGeckoPrefsBridge` (пакет `org.mozilla.geckoview`).
- **WebAuthn (passkeys, вариант A):** GeckoView зовёт Google Play services FIDO, которых в Vola нет; без
  классов Gecko падал (SIGSEGV в `libxul`, #127). Теперь вызовы GeckoView принимают отказывающие заглушки
  `src/full/java/com/google/**`, и WebAuthn включён только на Android 14+ с Credential Manager
  (`PasskeySupport`). Обновление GeckoView с новым вызовом Play services ловит `GeckoWebAuthnFallbackTest`:
  добавь заглушку, которая отказывает исключением (`Exception`, не `Error`), а не правь список в тесте.
- **Debug и release ведут себя по-разному:** вылет H1 был только в release (и без R8 тоже — дело не в
  минификации). Изменения, способные уронить движок, проверяй на release-сборке (`localRelease` или
  `benchmark`) на эмуляторе CI, а не только тестами на debug.
- **Нативный сбой** (tombstone `SIGSEGV … libxul.so`): символизируется сервером Mozilla
  `symbolication.services.mozilla.com/symbolicate/v5` (из облачной сессии закрыт — запускать в CI).
  Debug ID модуля = первые 16 байт BuildId с переставленными по GUID первыми тремя полями + `0`.
- `BrowserEngineRestartActivity` завершает процесс намеренно — это не сбой.
- GeckoView держим на последней stable (`geckoview-update.yml`); `maven.mozilla.org` закрыт в облаке,
  обновление проверяет CI.
