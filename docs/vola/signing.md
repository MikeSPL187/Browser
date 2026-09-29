# Подпись сборок Vola

Все сборки собирает GitHub Actions (`.github/workflows/build.yml`). Ключи хранятся только в
секретах репозитория и никогда не попадают в git.

Секреты добавляются в GitHub: **Settings → Secrets and variables → Actions → New repository secret**.

## Ключ для предварительных сборок (debug APK из PR)

Без него каждая новая debug-сборка подписывается случайным ключом, и Android не даст поставить её
поверх предыдущей — придётся удалять приложение вместе с данными.

В Termux:

```bash
pkg install openjdk-17
keytool -genkeypair -keystore vola-preview.jks -storetype PKCS12 \
  -alias androiddebugkey -storepass android -keypass android \
  -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Vola Preview"
base64 -w0 vola-preview.jks > vola-preview.b64
```

Содержимое `vola-preview.b64` → секрет `VOLA_PREVIEW_KEYSTORE_BASE64`.

Пароль `android` и алиас `androiddebugkey` — стандартные для отладочных ключей Android, так и
задумано: сборка находит ключ автоматически. Этот ключ не годится для релизов.

## Релизный ключ

```bash
keytool -genkeypair -keystore vola-release.jks -storetype PKCS12 \
  -alias vola -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=Vola"
base64 -w0 vola-release.jks > vola-release.b64
```

`keytool` спросит пароль. У PKCS12 пароль хранилища и пароль ключа совпадают.

| Секрет | Значение |
| --- | --- |
| `VOLA_RELEASE_KEYSTORE_BASE64` | содержимое `vola-release.b64` |
| `VOLA_RELEASE_STORE_PASSWORD` | пароль |
| `VOLA_RELEASE_KEY_ALIAS` | `vola` |
| `VOLA_RELEASE_KEY_PASSWORD` | тот же пароль |

Рекомендуется закрепить отпечаток сертификата: **Settings → Secrets and variables → Actions →
Variables → New repository variable** `VOLA_RELEASE_CERTIFICATE_SHA256`. Точное значение первый
релизный запуск напишет в предупреждении. Если ключ когда-нибудь подменят, сборка остановится.

**Сделай резервную копию `vola-release.jks` и пароля** (например, в менеджере паролей). Если их
потерять, обновить установленный Vola поверх станет невозможно.

## Как выпустить релиз

1. В `gradle.properties` поднять версию, добавить `release-notes/<версия>.md` (или попросить Claude).
2. Смёржить PR в `main`.
3. GitHub → **Actions → Build → Run workflow**, ветка `main`, включить **release** → Run.
4. CI прогонит тесты, соберёт подписанные APK, проверит подпись, создаст тег `v<версия>` и
   опубликует GitHub Release. Obtainium подхватит обновление сам.

Релиз не создавай вручную в интерфейсе GitHub: его публикует только CI.
