# VK Video — автотесты воспроизведения

Проект содержит UI-автотесты для приложения **VK Video** (Android): проверка воспроизведения видео и работы паузы. Тесты написаны на **Java 21**, **Appium 2**, **Selenide**, **JUnit 5** и запускаются через **Maven**.

## Что внутри

- **Приложение** — Android-проект (Kotlin, Gradle) в каталогах `app/`, `build.gradle.kts`.
- **Автотесты** — Maven-модуль в `src/test/java/org/example/vkvideo/`:
  - `VkVideoPlaybackTestPositive` — проверка, что время воспроизведения увеличивается.
  - `VkVideoPlaybackTestNegative` — проверка, что после паузы время не меняется в течение 4 секунд.

## Требования

- **JDK 21**
- **Maven 3.6+**
- **Android SDK** (переменная `ANDROID_HOME` или путь в настройках)
- **Appium 2** (сервер на порту 4723)
- **Устройство или эмулятор Android** с установленным приложением VK Video (`com.vk.vkvideo`)

В тестах по умолчанию указаны:
- URL сервера Appium: `http://127.0.0.1:4723/`
- устройство: `emulator-5554`
- версия платформы: `14`

При другом эмуляторе или устройстве измените константы `DEVICE_NAME` и при необходимости `PLATFORM_VERSION` в классах тестов.

## Установка Appium 2

```bash
npm install -g appium@next
appium driver install uiautomator2
```

Проверка:

```bash
appium --version
```

## Запуск тестов

1. Запустите эмулятор или подключите устройство с включённой отладкой по USB.
2. Убедитесь, что на устройстве установлено приложение **VK Video**.
3. В отдельном терминале запустите сервер Appium:

   ```bash
   appium
   ```

4. В корне проекта выполните:

   ```bash
   mvn test
   ```

Будут запущены все классы, имя которых заканчивается на `Test` (в т.ч. `VkVideoPlaybackTestPositive` и `VkVideoPlaybackTestNegative`).

Запуск только позитивного или только негативного теста:

```bash
mvn test -Dtest=VkVideoPlaybackTestPositive
mvn test -Dtest=VkVideoPlaybackTestNegative
```

## Сборка Android-приложения (опционально)

Если нужно собрать APK самого приложения (не для тестов):

```bash
./gradlew assembleDebug
```

APK будет в `app/build/outputs/apk/debug/`.

## Лицензия

Учебный/тестовый проект. Приложение VK Video — продукт VK.
