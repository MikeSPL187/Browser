# Адаптер GeckoView

Подгружается, когда работаешь с файлами `browser/gecko/`. Типы GeckoView живут только здесь и в
`app/src/main/java/org/mozilla/geckoview/` (мосты к закрытым API); UI и ViewModel видят нейтральные порты.

- **Новая возможность движка:** сначала порт (interface) в общем коде, затем реализация для Gecko и
  System WebView; если в WebView невозможно — фича явно скрыта в UI для flavor `systemwebview`.
- **Порт движка — в `browser/engine/`, не здесь** (#500): `AndroidBrowserEngineSessionPort`,
  `BrowserEngineViewPort`, `BrowserEngineFindPort` — в `BrowserEnginePorts.kt`. Код здесь уедет в `src/full`,
  поэтому тип, который нужен общему коду или адаптеру WebView, не объявляй в файле с импортом GeckoView.
- **Настройки Gecko без публичного API** — через `CandyGeckoPrefsBridge` (пакет `org.mozilla.geckoview`).
  Мост задаёт только **значение по умолчанию** (`RuntimeSettings` → `setDefaultPrefs`): пользовательское
  значение, которое Gecko записал сам, его перекрывает. Снять пользовательское —
  `GeckoPreferenceController.clearGeckoUserPref` (экспериментальный API: `@OptIn(markerClass = …)` из
  androidx, иначе lint `UnsafeOptInUsageError`).
- **Частные вкладки и настройки профиля.** Код Gecko сам пишет пользовательские настройки, и частные
  вкладки он не исключает. Пример: перевод запоминает язык в `browser.translations.mostRecentTargetLanguages`.
  `prefs.js` сохраняется через 500 мс после первого изменения (`Preferences::HandleDirty`); очистка,
  отправленная сразу, успевает раньше (`GeckoTranslationPrivacy`, V23). Новую функцию Gecko проверяй на
  такие записи по исходнику (`TranslationsParent`, `Services.prefs.set*`).
- **WebAuthn (passkeys, вариант A):** GeckoView зовёт Google Play services FIDO, которых в Vola нет; без
  классов Gecko падал (SIGSEGV в `libxul`, #127). Теперь вызовы GeckoView принимают отказывающие заглушки
  `src/full/java/com/google/**`, и WebAuthn включён только на Android 14+ с Credential Manager
  (`PasskeySupport`). Обновление GeckoView с новым вызовом Play services ловит `GeckoWebAuthnFallbackTest`:
  добавь заглушку, которая отказывает исключением (`Exception`, не `Error`), а не правь список в тесте.
  `GeckoPlayServicesReferenceTest` проверяет все ссылки GeckoView на Play services, по тесту на ссылку
  (имя ссылки видно в логе CI). Ссылку без заглушки вноси в `testFull/resources/gecko_play_services_unreachable.txt`
  только после того, как прочитал в исходнике GeckoView, что она выполняется лишь после отказа заглушки.
  Исходник — `git clone --filter=blob:none` репозитория `mozilla-firefox/firefox` на теге релиза
  (maven.mozilla.org из облака закрыт).
- **Debug и release ведут себя по-разному:** вылет H1 был только в release (и без R8 тоже — дело не в
  минификации). Изменения, способные уронить движок, проверяй на release-сборке (`localRelease` или
  `benchmark`) на эмуляторе CI, а не только тестами на debug.
- **Нативный сбой** (tombstone `SIGSEGV … libxul.so`): символизируется сервером Mozilla
  `symbolication.services.mozilla.com/symbolicate/v5` (из облачной сессии закрыт — запускать в CI).
  Debug ID модуля = первые 16 байт BuildId с переставленными по GUID первыми тремя полями + `0`.
- `BrowserEngineRestartActivity` завершает процесс намеренно — это не сбой.
- GeckoView держим на последней stable (`geckoview-update.yml`); `maven.mozilla.org` закрыт в облаке,
  обновление проверяет CI.
