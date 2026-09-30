# Подпись сборок Vola

Все сборки собирает GitHub Actions (`.github/workflows/build.yml`). Ключи хранятся только в
секретах репозитория и никогда не попадают в git.

Секреты добавляются в GitHub: **Settings → Secrets and variables → Actions → New repository secret**.

## Ключ для предварительных сборок (Vola Preview из PR)

CI собирает на каждый PR оптимизированную сборку **Vola Preview** (отдельное приложение рядом с
релизным Vola). Без этого ключа каждая новая сборка подписывается случайным ключом, и Android не даст
поставить её поверх предыдущей: придётся удалять приложение вместе с вкладками, паролями и историей.

Всё делается с телефона в браузере, минут за 5. Termux не нужен.

### Шаг 1. Получить ключ

Попросите Claude в сессии: «сгенерируй ключ Preview». Он создаст ключ командой ниже в своей
временной папке и пришлёт два файла:

- `VOLA_PREVIEW_KEYSTORE_BASE64.txt` — значение секрета, одна длинная строка;
- `vola-preview.jks` — сам ключ, резервная копия.

```bash
keytool -genkeypair -keystore vola-preview.jks -storetype PKCS12 \
  -alias androiddebugkey -storepass android -keypass android \
  -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Vola Preview"
base64 -w0 vola-preview.jks > VOLA_PREVIEW_KEYSTORE_BASE64.txt
```

Пароль `android` и алиас `androiddebugkey` стандартные для отладочных ключей Android, так и
задумано: сборка находит ключ автоматически. Этот ключ не годится для релизов. Ключ не попадает в
git; после добавления секрета его копия остаётся только у вас.

### Шаг 2. Секрет в GitHub

Приложение GitHub секреты не показывает, нужен браузер.

1. Откройте в браузере `github.com/MikeSPL187/Browser/settings/secrets/actions`
   (или *Settings → Secrets and variables → Actions*). Если страница урезана, в меню браузера
   включите «Версия для ПК».
2. **New repository secret**.
   - **Name:** `VOLA_PREVIEW_KEYSTORE_BASE64`
   - **Secret:** всё содержимое `VOLA_PREVIEW_KEYSTORE_BASE64.txt` (открыть файл → выделить всё →
     копировать → вставить). Без пробелов и переносов в начале и в конце.
3. **Add secret**.
4. Проверка: в любом новом запуске *Actions → Build → Preview APKs* не должно быть уведомления
   «VOLA_PREVIEW_KEYSTORE_BASE64 is not set». Если строка вставилась с ошибкой, шаг
   «Restore preview signing key» упадёт с понятной ошибкой `keytool`.

### Шаг 3. Резервная копия ключа

Сохраните `vola-preview.jks` в менеджер паролей или в облако. Если ключ потеряется, придётся
сделать новый и один раз переустановить Preview с потерей данных.

### Шаг 4. Один последний переход

Уже установленная Vola Preview подписана случайным ключом. Первую сборку с новым ключом придётся
поставить, удалив старую (перед этим сохраните данные: *Настройки → Приватность и защита → Данные приложения → Экспорт всех данных*, а после установки — «Импорт данных»).
Дальше все сборки из PR ставятся поверх, данные сохраняются.

Ограничение GitHub: сборки из PR от Dependabot секреты не получают и подписываются случайным ключом.
Их ставить поверх не нужно.

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
