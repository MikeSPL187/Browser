# Адаптер GeckoView

Подгружается, когда работаешь с файлами `browser/gecko/`. Типы GeckoView живут только здесь и в
`app/src/main/java/org/mozilla/geckoview/` (мосты к закрытым API); UI и ViewModel видят нейтральные порты.

- **Новая возможность движка:** сначала порт (interface) в общем коде, затем реализация для Gecko и
  System WebView; если в WebView невозможно — фича явно скрыта в UI для flavor `systemwebview`.
- **Настройки Gecko без публичного API** — через `CandyGeckoPrefsBridge` (пакет `org.mozilla.geckoview`).
- **WebAuthn выключен (#127):** в GeckoView он работает только через FIDO Google Play services, которых в
  Vola нет; Google спрашивал платформенный аутентификатор — Gecko падал (SIGSEGV в `libxul`). Не включать
  обратно без провайдера без Google Play.
- **Debug и release ведут себя по-разному:** вылет H1 был только в release (и без R8 тоже — дело не в
  минификации). Изменения, способные уронить движок, проверяй на release-сборке (`localRelease` или
  `benchmark`) на эмуляторе CI, а не только тестами на debug.
- **Нативный сбой** (tombstone `SIGSEGV … libxul.so`): символизируется сервером Mozilla
  `symbolication.services.mozilla.com/symbolicate/v5` (из облачной сессии закрыт — запускать в CI).
  Debug ID модуля = первые 16 байт BuildId с переставленными по GUID первыми тремя полями + `0`.
- `BrowserEngineRestartActivity` завершает процесс намеренно — это не сбой.
- GeckoView держим на последней stable (`geckoview-update.yml`); `maven.mozilla.org` закрыт в облаке,
  обновление проверяет CI.
