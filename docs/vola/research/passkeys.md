# Passkeys в Vola без Google Play services

_Исследование v5 · V25 · 2026-10-11 · `main` после #171, GeckoView 157. Только доклад: код — по решению
владельца (раздел «Варианты»)._

**Коротко.** GeckoView умеет passkeys через системный Credential Manager Android 14+, а запасной путь
ведёт в Google Play services FIDO, которого в Vola нет. Из-за этого запасного пути Gecko падал на
accounts.google.com (#127). Включить passkeys можно, если закрыть три обращения GeckoView к Play services
своей «заглушкой». Тогда passkeys заработают на Android 14+ через установленный менеджер паролей:
Bitwarden, KeePassDX, Proton Pass и другие. На Android 12–13 passkeys не будет. Google Password Manager
и часть менеджеров пустят Vola только после внесения её в их списки доверенных браузеров.

## 1. Что сейчас в Vola

- WebAuthn выключен настройкой Gecko `security.webauth.webauthn = false`
  (`GeckoRuntimeSettingsFactory.disableWebAuthn()` через `CandyGeckoPrefsBridge`, коммит `6efb34c`, #127).
  Сайты не видят `navigator.credentials.create/get` для ключей и показывают вход по паролю.
- Остальная обвязка осталась: разрешения `CREDENTIAL_MANAGER_SET_ORIGIN` и
  `CREDENTIAL_MANAGER_QUERY_CANDIDATE_CREDENTIALS` в манифесте, `GeckoWebAuthnActivityDelegate`
  (запуск окна провайдера через `MainActivity` и доставка результата только той же вкладке),
  `GeckoWebAuthnDeliveryRules` с юнит-тестами, инструментальные тесты в `GeckoCredentialsInstrumentedTest`.
- `play-services-fido` исключён из зависимостей GeckoView (`app/build.gradle.kts`), сборку сторожит
  `verifyNoPlayServicesDependencies`. `minSdk` = 31 (Android 12).

## 2. Как GeckoView 157 обрабатывает WebAuthn

Код — `WebAuthnTokenManager.java` и `WebAuthnCredentialManager.java` в
[mozilla-firefox/firefox](https://github.com/mozilla-firefox/firefox/tree/main/mobile/android/geckoview/src/main/java/org/mozilla/geckoview)
(сборка Vola — не `MOZILLA_OFFICIAL`, поэтому идёт «непривилегированная» ветка).

| Вызов страницы | Первый шаг | Когда уходит в Play services FIDO |
|---|---|---|
| `PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable()` | **сразу Play services FIDO** (`Fido2ApiClient`) | всегда |
| `navigator.credentials.create()` — создать passkey | Credential Manager (API 34+, есть `android.software.credentials`) с `setOrigin(origin)` | Android < 14; нет системной функции; `SecurityException`; провайдер ответил «нечем создать» (`TYPE_NO_CREATE_OPTIONS`) |
| `navigator.credentials.get()` — войти | `prepareGetCredential` в Credential Manager с `setOrigin(origin)` | Android < 14; у провайдеров нет подходящего ключа (`TYPE_NO_CREDENTIAL`); любое исключение при вызове |

Что из этого следует без Play services в APK:

1. **`isUVPAA` — падение.** Метод вызывается из Gecko через JNI без `try/catch`. Класса
   `com.google.android.gms.fido.Fido` нет, `NoClassDefFoundError` уходит в нативный код, Gecko падает
   (SIGSEGV в libxul). Это и есть H1 из #127: accounts.google.com спрашивает об этом при загрузке.
2. **Запасной путь `create`/`get` — тоже падение.** Код ловит `java.lang.Exception`, а
   `NoClassDefFoundError` — это `Error`. Комментарий в коде обещает обратное, но не срабатывает.
3. **Даже с классами на месте `get` может зависнуть.** В ветке `getAssertion` у задачи Play services
   нет слушателя ошибки. Если задача завершится ошибкой, обещание на странице не завершится никогда:
   кнопка «Войти с ключом» молчит. Об этом же пишут в
   [room-browser-geckoview#12](https://github.com/SecretArrow/room-browser-geckoview/pull/12).
4. Обход только `isUVPAA` (как в [browsem#703](https://github.com/matsudamper/browsem/pull/703)) не
   спасает: пп. 2–3 остаются. Поэтому в #127 выключен весь API.

Mozilla официально этот случай не поддерживает: [bug 1678045](https://bugzilla.mozilla.org/show_bug.cgi?id=1678045)
«Support WebAuthn on Android without Google services» открыт с 2020 года. Ждать исправления в
GeckoView — значит ждать неопределённо долго.

## 3. Credential Manager и провайдеры

**Как браузер получает passkey.** На Android 14+ браузер передаёт системе запрос с адресом сайта
(`origin`). Система показывает выбор из установленных провайдеров: Google Password Manager (если есть
Play services), Bitwarden, KeePassDX, Proton Pass, 1Password и т. д. Провайдер подписывает ответ для
этого `origin`. Подробно — [Android: privileged apps](https://developer.android.com/identity/sign-in/privileged-apps).

**Главный барьер — списки доверенных браузеров.** Провайдер обязан проверить, что `origin` ставит
настоящий браузер, а не фишинговое приложение. Поэтому у каждого провайдера свой список: имя пакета +
SHA-256 сертификата подписи.

| Провайдер | Без Play services на телефоне | Как попасть в список |
|---|---|---|
| Google Password Manager | не работает (часть Play services) | заявка Google через форму на странице privileged apps, ручная проверка |
| Bitwarden | работает на Android 14+ | три списка: Google, сообщества (`fido2_privileged_community.json`) и «доверено вами». Вне списков Bitwarden пишет «browser (…) is not recognized» и предлагает **«Trust»**. Для всех — PR в [bitwarden/android](https://github.com/bitwarden/android/pull/4005) с пакетом и отпечатком релизного ключа; там уже IronFox, Fennec F-Droid, Cromite и WebLibre (тоже GeckoView) |
| KeePassDX | работает (passkeys с 4.2, ключи хранятся в KDBX локально) | в 4.4.3 появилось «Add browser in the list of privileged apps» ([F-Droid](https://f-droid.org/packages/com.kunzisoft.keepass.libre/)); как именно — проверить на устройстве |
| Proton Pass, 1Password и др. | обычно работают на Android 14+ | у каждого свой список; проверить на устройстве и писать им |
| Аппаратные ключи (YubiKey и т. п.) | только через провайдер-приложение: HW Fido2 Provider, Authnkey/FIDO Bridge, YubiKey Passkey Enabler | ключ подключается как обычный провайдер Credential Manager |

Важно для Vola:

- Список проверяет **релизный** сертификат и пакет `io.github.mikespl187.vola`. Отладочные и preview-сборки
  (другой ключ) провайдеры не узнают — в них passkeys будут работать только через «Trust» в Bitwarden.
- Сервер сайта сверяет `origin` из подписанного ответа. Gecko берёт его из настоящего адреса страницы,
  а не из JavaScript: подменить его страница не может.
- Android 12–13 (часть пользователей при `minSdk` 31): Credential Manager для чужих `origin` не работает,
  passkeys там не будет ни в одном варианте, кроме microG.
- Passkeys хранит провайдер, а не Vola: приватные вкладки ничего не пишут на диск, профили v5 видят одни
  и те же ключи (как пароли в профилях Safari).
- Вход «с другого устройства» по QR (гибридный транспорт) делает провайдер. Без Play services его
  умеют не все; у microG он работает не всегда ([microG#3290](https://github.com/microg/GmsCore/issues/3290)).

## 4. Как это решили другие браузеры на GeckoView

- **Firefox** — подписан ключом Mozilla, есть во всех списках, вызывает Play services FIDO как
  привилегированный клиент. Нам недоступно.
- **IronFox, Fennec F-Droid** — заменяют `play-services-fido` Google библиотекой
  [microG](https://github.com/microg/GmsCore/tree/master/play-services-fido) (Apache-2.0, артефакт
  `org.microg.gms:play-services-fido`). С установленным microG работают passkeys и ключи по USB/NFC даже
  на Android 12–13; без microG — через Credential Manager. Минусы: GrapheneOS помечает такой браузер как
  «зависит от Play services»; библиотека связывается с пакетом `com.google.android.gms`, то есть на
  обычном телефоне — **с настоящими Play services Google**.
- **WebLibre** (GeckoView) — есть в списке сообщества Bitwarden, значит путь через Credential Manager у
  стороннего браузера на GeckoView рабочий.

## 5. Риски

| Риск | Чем грозит | Как закрыть |
|---|---|---|
| Вернуть падение H1 | Gecko падает на accounts.google.com и любом сайте с `isUVPAA` | включать API только вместе с заглушкой; инструментальный тест: страница вызывает `isUVPAA`, `create`, `get` и получает ответ без падения |
| Зависание `get` | кнопка «Войти с ключом» молчит | заглушка отвечает **синхронной** ошибкой (её ловит `catch`), а не неудачной задачей |
| Обновление GeckoView | новый вызов Play services, которого нет в заглушке, — снова `NoClassDefFoundError` | тот же тест на каждом обновлении GeckoView (#149 и далее); заглушка повторяет только то, что вызывает `WebAuthnTokenManager` |
| Нет в списках провайдеров | «passkey не найден» или отказ провайдера | заявки в Google, PR в Bitwarden, письма KeePassDX/Proton; до тех пор — «Trust» в Bitwarden |
| Android 12–13 | нет passkeys | на этих версиях API остаётся выключенным, сайты показывают вход по паролю |
| Приватность | — | наружу ничего не уходит: обмен только между Vola, системой и провайдером; без телеметрии |
| Гейт `verifyNoPlayServicesDependencies` | — | своя заглушка не зависит от `com.google.android.gms`; microG — группа `org.microg.gms`, гейт её пропустит, но по духу правила это спорно |

## 6. Варианты для владельца

**Решение владельца 2026-10-11: вариант A** (`decisions.md`). Работает на Android 14+; на Android 12–13
WebAuthn остаётся выключенным.

**A. Своя заглушка FIDO + Credential Manager (рекомендую).**
Маленький модуль в Vola с теми же именами классов, что вызывает GeckoView (`Fido`, `Fido2ApiClient`,
`Task` и около 30 типов запроса — только оболочки без логики). `isUVPAA` отвечает «нет», создание и вход через Play services — сразу ошибкой
`NotSupported`, без связи с Google. WebAuthn включается только на Android 14+ с
`android.software.credentials`, иначе остаётся выключенным. Плюс заявки в списки: Google, Bitwarden
(PR с отпечатком релизного ключа), KeePassDX, Proton Pass.
- Что получим: passkeys на Android 14+ через любой установленный менеджер; Bitwarden — сразу через
  «Trust», для всех — после попадания в списки. Ни одного обращения к Google.
- Цена: один PR кода (Effort Extra) + тесты на эмуляторе CI; проверка заглушки на каждом обновлении
  GeckoView.
- Не получим: Android 12–13; аппаратные ключи без провайдер-приложения; `isUVPAA` = «нет», поэтому часть
  сайтов не предложит создать passkey сама — его можно добавить в настройках аккаунта на сайте.

**B. Библиотека microG FIDO, как IronFox.**
Вместо своей заглушки — `org.microg.gms:play-services-fido`.
- Плюсы: у пользователей microG — passkeys и ключи USB/NFC даже на Android 12–13; меньше своего кода.
- Минусы: на обычном телефоне библиотека обращается к настоящим Play services Google (противоречит
  «без Google Play services» по смыслу, хоть гейт и пропустит); GrapheneOS покажет «зависит от Play
  services»; зависание `get` при ошибке задачи нужно отдельно проверить; больше чужого кода в APK.

**C. Оставить выключенным и ждать Mozilla.**
- Плюсы: ноль риска и работы; сайты показывают вход по паролю, пароли Vola работают.
- Минусы: passkeys нет неопределённо долго (bug 1678045 открыт с 2020 года); всё больше сайтов
  предлагают вход только по ключу.

Если выбран A, план кода (одним PR, после ядра интерфейса или раньше — как решит владелец):

1. Модуль-заглушка и правило R8, чтобы классы не вырезались.
2. `security.webauth.webauthn` = `true` только на API 34+ с `FEATURE_CREDENTIALS`; на остальных — как сейчас.
3. Инструментальный тест на эмуляторе CI (API 35): фикстура вызывает `isUVPAA`, `create`, `get` — ответ
   приходит, Gecko живёт. Тот же тест для System WebView — без изменений (там своя поддержка).
4. Чек-лист на телефоне: webauthn.io → создать и войти через Bitwarden; accounts.google.com открывается
   без падения; Android 13 (если есть) — вход по паролю.
5. Заявки в списки провайдеров — после релиза с этим PR (нужен отпечаток релизного ключа; он публичный,
   секретом не является).

## Источники

- Исходники GeckoView: [`WebAuthnTokenManager.java`](https://github.com/mozilla-firefox/firefox/blob/main/mobile/android/geckoview/src/main/java/org/mozilla/geckoview/WebAuthnTokenManager.java),
  [`WebAuthnCredentialManager.java`](https://github.com/mozilla-firefox/firefox/blob/main/mobile/android/geckoview/src/main/java/org/mozilla/gecko/WebAuthnCredentialManager.java).
- [Bug 1678045 — Support WebAuthn on Android without Google services](https://bugzilla.mozilla.org/show_bug.cgi?id=1678045).
- [Android: privileged apps и `setOrigin`](https://developer.android.com/identity/sign-in/privileged-apps),
  [passkeys.dev: Android](https://passkeys.dev/docs/reference/android/).
- Bitwarden: [списки сообщества (#4005)](https://github.com/bitwarden/android/pull/4005), строки
  «Privileged apps» и «Trust» в `ui/src/main/res/values/strings.xml`, [справка о passkeys](https://bitwarden.com/help/storing-passkeys/).
- [KeePassDX на F-Droid](https://f-droid.org/packages/com.kunzisoft.keepass.libre/).
- microG: [play-services-fido](https://github.com/microg/GmsCore/tree/master/play-services-fido),
  [#2034](https://github.com/microg/GmsCore/issues/2034), [#3290](https://github.com/microg/GmsCore/issues/3290).
- Сторонние наблюдения: [room-browser-geckoview#12](https://github.com/SecretArrow/room-browser-geckoview/pull/12),
  [browsem#703](https://github.com/matsudamper/browsem/pull/703).
- Vola: #127 (H1), коммиты `6efb34c`, `900f570`; `docs/browsing/platform-engines.md`,
  `docs/browsing/runtime-and-navigation.md`.
