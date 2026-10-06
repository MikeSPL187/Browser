# Инструментальные тесты (androidTest) в CI

## Как устроено сейчас (S5, PR #121)

- **Workflow** `.github/workflows/instrumented.yml`: эмулятор API 35 x86_64 (как в `screenshots.yml`),
  **8 шардов**, swap 8 ГБ. Запуск: метка `instrumented` на PR, каждую ночь на `main`, вручную с
  фильтром (пакет `dev.sk2andy.materialbrowser.ui` или класс, можно `Класс#метод`, через запятую).
- **AndroidX Test Orchestrator** (`testOptions` в `app/build.gradle.kts`): каждый тест в своём процессе,
  данные приложения очищаются, тайм-аут 2 мин на тест. Падение одного теста не роняет остальные.
- **Задача Report** собирает JUnit XML всех шардов и пишет сводку на страницу запуска: сколько тестов,
  прошло, упало (новые/известные), пропущено, время; у каждого упавшего — сообщение, стек и ошибки из
  его logcat. Скачивать артефакты не нужно.
- **Храповик** `scripts/ci/instrumented-baseline.txt`: прогон краснеет на любом упавшем тесте, которого
  нет в списке известных падений. Тест из списка, который прошёл, сводка называет «Fixed — remove
  from the baseline». Новые строки в список не добавляем, чтобы позеленить PR, — только чиним.
- **Пропуски** (`assumeTrue`: нужен планшет, внешний менеджер загрузок, отладочная функция) считаются
  пропущенными, а не упавшими.
- В логе шарда видно, какой тест идёт сейчас (`TestRunner`), и раз в 5 минут — память, swap, диск и
  самые тяжёлые процессы (`[monitor]`). Память раннера растёт за прогон (~7 ГБ на 250 тестов), поэтому
  шардов 8: с четырьмя раннер самого тяжёлого шарда убивало дважды.

**Первый полный прогон (2026-10-06):** 1034 теста, 942 прошли, 68 упали, 24 пропущено; шард
7–15 мин, вместе со сборкой ~25 мин на прогон. Починка устаревших тестов и список известных падений — в PR #121.

**Подводный камень Compose:** `Modifier.testTag()` после `clearAndSetSemantics {}` отбрасывается — тег
ставить до очистки. Так же «молча» проходили проверки исчезновения `DragOverlay` в редакторах действий.

# Исходное предложение

## Что есть сейчас

- В `app/src/androidTest` уже **241 файл** тестов Candy: вкладки, Gecko, WebView, загрузки,
  картинка в картинке, профили. Многие помечены `@SdkSuppress(minSdkVersion = 34)`.
- В CI они **не запускаются**. Есть только `screenshots.yml`: эмулятор API 35 (x86_64, KVM),
  скрипт обходит ключевые экраны и публикует PNG в ветку `screenshots`.
- Скриншоты показывают, как выглядит экран, но не проверяют поведение: закрытие вкладки,
  восстановление сессии, разрешения, загрузки.

## Предложение: три уровня

| Уровень | Что проверяет | Когда | Время |
| --- | --- | --- | --- |
| 1. Скриншоты Compose Preview на JVM | Внешний вид компонентов в светлой, тёмной, OLED-теме, на телефоне и планшете | Каждый PR | 3–5 мин |
| 2. androidTest на эмуляторе, 2–4 шарда | Поведение: вкладки, пространства, Gecko, загрузки, пароли | PR с меткой `instrumented`, каждую ночь на `main`, вручную | 15–25 мин |
| 3. Дымовой прогон на Android 12 (API 31) | Что приложение запускается и основные сценарии работают на минимальной версии | Каждую ночь | ~10 мин |

### 1. Скриншоты Compose Preview (без эмулятора)

Официальный плагин Google `com.android.compose.screenshot` рендерит `@Preview` через layoutlib
на JVM и сравнивает с эталонными PNG в репозитории. `CLAUDE.md` и так требует Preview для
каждого UI-изменения, значит тесты появляются почти бесплатно.

- `./gradlew validateFullDebugScreenshotTest` в `build.yml`, отчёт о различиях — артефакт и сводка.
- Эталоны обновляет `updateFullDebugScreenshotTest` в отдельном коммите PR, дифф PNG виден в GitHub.
- Альтернатива: Roborazzi (Robolectric). Тяжелее, зато умеет взаимодействия. Начать лучше с плагина Google.

### 2. androidTest на эмуляторе через Gradle Managed Devices

Gradle сам скачивает образ, создаёт и запускает эмулятор, поэтому воркфлоу короче, чем
ручной `avdmanager` в `screenshots.yml`. Образы ATD (Automated Test Device) облегчённые и
загружаются быстрее обычных.

```kotlin
// app/build.gradle.kts
android {
    testOptions {
        animationsDisabled = true
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        managedDevices {
            localDevices {
                create("pixel6api35") {
                    device = "Pixel 6"
                    apiLevel = 35
                    systemImageSource = "aosp-atd"
                }
                create("pixel6api31") {
                    device = "Pixel 6"
                    apiLevel = 31
                    systemImageSource = "aosp-atd"
                }
            }
        }
    }
    defaultConfig {
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
    }
}
```

```yaml
# .github/workflows/instrumented.yml (набросок)
on:
  pull_request:
    types: [labeled, synchronize]
  schedule:
    - cron: "17 2 * * *"
  workflow_dispatch:
    inputs:
      filter:
        description: Класс или пакет, например dev.sk2andy.materialbrowser.browser
        required: false

jobs:
  androidTest:
    if: >-
      github.event_name != 'pull_request' ||
      contains(github.event.pull_request.labels.*.name, 'instrumented')
    runs-on: ubuntu-latest
    timeout-minutes: 60
    strategy:
      fail-fast: false
      matrix:
        shard: [0, 1, 2, 3]
    steps:
      # checkout, JDK 17, Android SDK — как в build.yml (actions закреплены по SHA)
      - name: Enable KVM
        run: sudo chmod 666 /dev/kvm
      - name: Cache system images
        uses: actions/cache@<sha>
        with:
          path: ~/.android/gradle/avd
          key: gmd-${{ runner.os }}-pixel6api35
      - name: Instrumented tests
        run: >-
          ./gradlew --no-daemon pixel6api35FullDebugAndroidTest
          -Pandroid.testoptions.manageddevices.emulator.gpu=swiftshader_indirect
          -Pandroid.testInstrumentationRunnerArguments.numShards=4
          -Pandroid.testInstrumentationRunnerArguments.shardIndex=${{ matrix.shard }}
      - name: Failed tests summary
        if: failure()
        run: python3 scripts/ci/junit_summary.py app/build/outputs/androidTest-results >> "$GITHUB_STEP_SUMMARY"
```

Главное для работы с телефона: **сводка упавших тестов в `$GITHUB_STEP_SUMMARY`** (имя теста,
сообщение, первые строки стека). Её видно прямо на странице запуска, скачивать отчёт не нужно.
Небольшой скрипт `scripts/ci/junit_summary.py` разбирает JUnit XML.

### 3. Android 12

После PR #20 минимальная версия — Android 12. Тесты с `@SdkSuppress(minSdkVersion = 34)` на
API 31 пропустятся сами. Для API 31 стоит завести аннотацию `@SmokeTest` на 10–15 ключевых
сценариев (запуск, открытие сайта, новая вкладка, уведомление о загрузке, приватная вкладка) и
гонять их ночью на `pixel6api31`.

## Порядок внедрения

1. **PR A** (Effort: High): плагин скриншотов Compose Preview + эталоны для 5–10 существующих
   Preview. Запуск в `build.yml` на каждый PR.
2. **PR B** (Effort: Extra): Gradle Managed Devices, `instrumented.yml`, шарды, сводка в
   `$GITHUB_STEP_SUMMARY`. Сначала прогнать весь набор вручную и починить или пометить
   нестабильные тесты: тесты не отключаем, а чиним.
3. **PR C** (Effort: High): `@SmokeTest` и ночной прогон на Android 12.

## Стоимость

Для публичного репозитория раннеры GitHub бесплатны, KVM на `ubuntu-latest` есть. Четыре шарда
по ~15 минут — это около часа машинного времени за прогон, поэтому для PR — только по метке.
