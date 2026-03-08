package org.example.vkvideo;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.appium.SelenideAppium;
import io.appium.java_client.android.AndroidDriver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.net.MalformedURLException;
import java.util.Map;
import java.net.URL;
import java.time.Duration;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.sleep;
import static com.codeborne.selenide.appium.SelenideAppium.$;
import static com.codeborne.selenide.appium.SelenideAppium.$$;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Автотест воспроизведения видео в приложении VK Видео (com.vk.vkvideo)
 *
 * Перед запуском:
 *  - Установите приложение VK Видео на устройство / эмулятор
 *  - Запустите Appium server (например, http://127.0.0.1:4723/wd/hub)
 *  - Уточните appActivity и локаторы элементов с помощью Appium Inspector и поправьте константы ниже
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class VkVideoPlaybackTest {

    private static AndroidDriver driver;

    // Настройки устройства / Appium
    // Для Appium 2.x базовый путь по умолчанию "/" (а не "/wd/hub")
    private static final String APPIUM_SERVER_URL = "http://127.0.0.1:4723/";
    private static final String PLATFORM_NAME = "Android";
    private static final String AUTOMATION_NAME = "UiAutomator2";
    private static final String DEVICE_NAME = "emulator-5554"; // замените на имя вашего девайса при необходимости
    // Фактическая версия эмулятора из лога Appium: emulator-5554 (14)
    // При необходимости подставьте сюда реальную версию из:
    //   adb -s emulator-5554 shell getprop ro.build.version.release
    private static final String PLATFORM_VERSION = "14";

    // Пакет и главное Activity VK Видео
    private static final String APP_PACKAGE = "com.vk.vkvideo";
    // Найдено через: adb shell dumpsys window | grep mCurrentFocus
    // mCurrentFocus=... com.vk.vkvideo/com.vk.video.screens.main.MainActivity
    private static final String APP_ACTIVITY = "com.vk.video.screens.main.MainActivity";

    // Локаторы, подобранные по реальному дереву элементов VK Видео
    // Карточка видео в гриде (первая видимая карточка)
    private static final By FIRST_VIDEO_CARD = By.id("com.vk.vkvideo:id/content");
    // Прогресс рекламного ролика (ad)
    private static final By VIDEO_AD_PROGRESS = By.id("com.vk.vkvideo:id/video_ad_progress_bar");
    private static final By VIDEO_AD_SKIP = By.id("com.vk.vkvideo:id/video_ad_skip");
    // Прогресс мини‑плеера основного видео
    private static final By MINI_PLAYER_PROGRESS = By.id("com.vk.vkvideo:id/miniPlayerProgressView");
    // Кнопка воспроизведения/паузы (content-desc "Pause" когда играет, "Play" когда на паузе)
    private static final By PLAYER_PLAY_PAUSE_BUTTON = By.id("com.vk.vkvideo:id/playView");
    // Время воспроизведения (текущее или "текущее / общее"), видно после тапа по экрану
    private static final By PLAYER_DURATION = By.id("com.vk.vkvideo:id/duration");
    // Область плеера для тапа (показ контролов и времени)
    private static final By PLAYER_CONTROL = By.id("com.vk.vkvideo:id/player_control");
    private static final By VIDEO_DISPLAY = By.id("com.vk.vkvideo:id/video_display");

    @BeforeAll
    static void setUp() throws MalformedURLException {
        // Базовая конфигурация Selenide под мобильные тесты
        Configuration.browserSize = null;
        Configuration.timeout = Duration.ofSeconds(15).toMillis();

        MutableCapabilities caps = new DesiredCapabilities();
        // W3C-совместимые capability: стандартные ключи + vendor-префикс "appium:"
        caps.setCapability("platformName", PLATFORM_NAME);
        caps.setCapability("appium:automationName", AUTOMATION_NAME);
        caps.setCapability("appium:deviceName", DEVICE_NAME);
        caps.setCapability("appium:platformVersion", PLATFORM_VERSION);
        caps.setCapability("appium:appPackage", APP_PACKAGE);
        caps.setCapability("appium:appActivity", APP_ACTIVITY);
        caps.setCapability("appium:noReset", true);
        caps.setCapability("appium:newCommandTimeout", 180);

        driver = new AndroidDriver(new URL(APPIUM_SERVER_URL), caps);

        // "вкручиваем" драйвер в Selenide и запускаем приложение
        WebDriverRunner.setWebDriver(driver);
        SelenideAppium.launchApp();

        // Гарантируем, что открыто именно приложение VK Видео, а не лаунчер
        openVkVideoIfNeeded();
    }

    @AfterAll
    static void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    /**
     * Позитивный сценарий — проверка, что видео воспроизводится:
     * 1. Включить видео (открыть приложение, главный экран, тап по первому видео).
     * 2. Тапнуть на экран, проверить, что время увеличивается.
     * 3. Если время увеличивается — тест пройден.
     */
    @Test
    @Order(1)
    @DisplayName("VK Видео: позитивный сценарий — видео воспроизводится (время растёт)")
    void videoShouldStartPlaying() {
        // 1. Включить видео: главный экран с лентой → тап по первому видео
        $(By.id("com.vk.vkvideo:id/vk_video_root_view")).shouldBe(visible);
        SelenideElement firstCard = $$(FIRST_VIDEO_CARD).first().shouldBe(visible);
        firstCard.click();
        sleep(2000);

        if ($(VIDEO_AD_PROGRESS).exists()) {
            if ($(VIDEO_AD_SKIP).exists()) {
                $(VIDEO_AD_SKIP).click();
            }
            sleep(3000);
        }

        if ($(PLAYER_CONTROL).exists()) {
            $(PLAYER_CONTROL).shouldBe(visible);
        } else if ($(VIDEO_DISPLAY).exists()) {
            $(VIDEO_DISPLAY).shouldBe(visible);
        }

        // 2. Тапнуть на экран, проверить, что время увеличивается
        tapToShowPlayerControls();
        sleep(1000);
        assertTrue(isVideoPlayingByTime(),
                "Ожидалось, что видео воспроизводится: время на таймере увеличивается.");
    }

    /**
     * Негативный сценарий — проверка после паузы (продолжение в открытом приложении):
     * 1. Нажать паузу.
     * 2. Таймаут 5 секунд.
     * 3. Если в течение 5 секунд таймер не увеличивался — тест пройден.
     */
    @Test
    @Order(2)
    @DisplayName("VK Видео: негативный сценарий — после паузы время не увеличивается")
    void videoShouldNotBePlayingWhenPaused() {
        if ($(PLAYER_CONTROL).exists()) {
            $(PLAYER_CONTROL).shouldBe(visible);
        } else {
            $(VIDEO_DISPLAY).shouldBe(visible);
        }

        // 1. Нажать паузу
        clickPauseButton();

        // 2. Проверить, что время не увеличивается
        assertTrue(isVideoTimeUnchangedAfterPause(),
                "Ожидалось, что в течение 5 секунд после паузы время не увеличивается.");
    }

    /**
     * Нажимает паузу: тап по экрану плеера → ждём контролы → клик по кнопке «Пауза».
     */
    private void clickPauseButton() {
        By pauseByDesc = By.xpath("//*[@content-desc='Pause' or @content-desc='Пауза' or contains(@content-desc,'ause')]");
        for (int attempt = 0; attempt < 4; attempt++) {
            tapToShowPlayerControls();
            sleep(1000);
            try {
                if ($(PLAYER_PLAY_PAUSE_BUTTON).exists()) {
                    SelenideElement btn = $(PLAYER_PLAY_PAUSE_BUTTON);
                    String state = btn.getAttribute("content-desc");
                    if (state != null && (state.equalsIgnoreCase("Pause") || state.contains("ауза") || state.contains("Pause"))) {
                        btn.click();
                        return;
                    }
                }
                SelenideElement pauseBtn = $(pauseByDesc);
                if (pauseBtn.exists() && pauseBtn.isDisplayed()) {
                    pauseBtn.click();
                    return;
                }
            } catch (StaleElementReferenceException e) {
                sleep(400);
            }
        }
    }

    /** Видна ли кнопка «Пауза» (приложение в состоянии «воспроизведение»). */
    private boolean isPauseButtonVisible() {
        try {
            if ($(PLAYER_PLAY_PAUSE_BUTTON).exists()) {
                String state = $(PLAYER_PLAY_PAUSE_BUTTON).getAttribute("content-desc");
                if (state != null && (state.equalsIgnoreCase("Pause") || state.contains("ауза"))) {
                    return true;
                }
            }
            By pauseByDesc = By.xpath("//*[@content-desc='Pause' or @content-desc='Пауза' or contains(@content-desc,'ause')]");
            return $(pauseByDesc).exists() && $(pauseByDesc).isDisplayed();
        } catch (StaleElementReferenceException e) {
            return false;
        }
    }

    /** Видна ли кнопка «Play» (приложение в состоянии «пауза»). */
    private boolean isPlayButtonVisible() {
        try {
            if ($(PLAYER_PLAY_PAUSE_BUTTON).exists()) {
                String state = $(PLAYER_PLAY_PAUSE_BUTTON).getAttribute("content-desc");
                if (state != null && (state.equalsIgnoreCase("Play") || state.contains("lay") || state.contains("оспрои"))) {
                    return true;
                }
            }
            By playByDesc = By.xpath("//*[@content-desc='Play' or @content-desc='Воспроизвести' or contains(@content-desc,'lay')]");
            return $(playByDesc).exists() && $(playByDesc).isDisplayed();
        } catch (StaleElementReferenceException e) {
            return false;
        }
    }

    /**
     * Если по каким‑то причинам после старта сессии мы попали не в VK Видео, а на рабочий стол лаунчера,
     * пытаемся найти и открыть иконку приложения "VK Video", затем ждём появления корневого view приложения.
     */
    private static void openVkVideoIfNeeded() {
        try {
            // Проверяем, что уже в приложении VK Видео
            $(By.id("com.vk.vkvideo:id/vk_video_root_view")).shouldBe(visible);
            return;
        } catch (Throwable ignore) {
            // не нашли — пробуем открыть приложение с домашнего экрана
        }

        By vkIcon = By.xpath(
                "//*[@text='VK Video' or @content-desc='VK Video' or @content-desc='Predicted app: VK Video']");

        $(vkIcon).shouldBe(visible).click();
        $(By.id("com.vk.vkvideo:id/vk_video_root_view")).shouldBe(visible);
    }

    /**
     * Тап по центру области плеера, чтобы показать контролы (и время воспроизведения).
     * Обычный .click() по элементу в эмуляторе может попасть в дочернюю зону и вызвать другое действие,
     * поэтому тапаем по координатам центра — так приложение с большей вероятностью покажет контролы.
     */
    private void tapToShowPlayerControls() {
        WebElement area = null;
        if ($(PLAYER_CONTROL).exists()) {
            area = $(PLAYER_CONTROL).getWrappedElement();
        } else if ($(VIDEO_DISPLAY).exists()) {
            area = $(VIDEO_DISPLAY).getWrappedElement();
        }
        if (area == null) {
            sleep(500);
            return;
        }
        Point loc = area.getLocation();
        Dimension size = area.getSize();
        int centerX = loc.getX() + size.getWidth() / 2;
        int centerY = loc.getY() + size.getHeight() / 2;
        try {
            ((JavascriptExecutor) driver).executeScript("mobile: clickGesture",
                    Map.of("x", centerX, "y", centerY));
        } catch (Exception e) {
            // fallback: обычный клик по элементу
            area.click();
        }
        sleep(500);
    }

    /**
     * Парсит строку времени в секунды. Поддерживает "0:15", "1:23", "0:15 / 5:00" (берётся первая часть).
     */
    private static int parseTimeToSeconds(String raw) {
        if (raw == null || raw.isEmpty()) {
            return -1;
        }
        String part = raw.split("\\s*/\\s*|\\s+")[0].trim();
        String[] mmss = part.split(":");
        if (mmss.length < 2) {
            return -1;
        }
        try {
            int min = Integer.parseInt(mmss[0].trim());
            int sec = Integer.parseInt(mmss[1].trim());
            return min * 60 + sec;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Читает текущее время воспроизведения с экрана (после тапа по плееру).
     * Ищем duration внутри области плеера, чтобы не взять пустой элемент с карточки в ленте.
     */
    private int getCurrentTimeSeconds() {
        tapToShowPlayerControls();
        try {
            // Сначала ищем время внутри player_control (полноэкранный плеер)
            SelenideElement durationInPlayer = null;
            if ($(PLAYER_CONTROL).exists()) {
                durationInPlayer = $(PLAYER_CONTROL).$(By.id("com.vk.vkvideo:id/duration"));
            }
            if (durationInPlayer == null || !durationInPlayer.exists()) {
                if ($(VIDEO_DISPLAY).exists()) {
                    durationInPlayer = $(VIDEO_DISPLAY).$(By.xpath(".//*[@resource-id='com.vk.vkvideo:id/duration']"));
                }
            }
            if (durationInPlayer == null || !durationInPlayer.exists()) {
                durationInPlayer = $(PLAYER_DURATION);
            }
            if (!durationInPlayer.exists()) {
                return -1;
            }
            String text = durationInPlayer.getText();
            if (text == null || text.isEmpty()) {
                text = durationInPlayer.getAttribute("text");
            }
            if (text == null || text.isEmpty()) {
                SelenideElement child = durationInPlayer.$(By.xpath(".//*"));
                if (child.exists()) {
                    text = child.getText();
                }
            }
            // Если время не в duration, ищем любой элемент с текстом вида "M:SS" внутри плеера
            if ((text == null || text.isEmpty()) && $(PLAYER_CONTROL).exists()) {
                for (SelenideElement el : $(PLAYER_CONTROL).$$(By.xpath(".//*[contains(@text, ':') and string-length(normalize-space(@text)) <= 8]")).snapshot()) {
                    String t = el.getAttribute("text");
                    if (t != null && !t.isEmpty() && parseTimeToSeconds(t) >= 0) {
                        return parseTimeToSeconds(t);
                    }
                }
            }
            return parseTimeToSeconds(text != null ? text : "");
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Проверка воспроизведения по времени: тап → время T1 → пауза 3 с → тап → время T2. Играет, если T2 > T1.
     */
    private boolean isVideoPlayingByTime() {
        int t1 = getCurrentTimeSeconds();
        if (t1 < 0) {
            return false;
        }
        sleep(3000);
        int t2 = getCurrentTimeSeconds();
        return t2 > t1;
    }

    /**
     * После паузы время не должно измениться в течение 5 секунд.
     */
    private boolean isVideoTimeUnchangedAfterPause() {
        int t1 = getCurrentTimeSeconds();
        if (t1 < 0) {
            return false;
        }
        sleep(5000);
        int t2 = getCurrentTimeSeconds();
        return t2 == t1;
    }

    /**
     * Универсальная проверка: определяем, воспроизводится ли видео, по изменению числового значения прогресса.
     */
    private boolean isProgressGrowing(By progressLocator) {
        double before = readProgressSafely(progressLocator);
        sleep(3000);
        double after = readProgressSafely(progressLocator);
        return after > before;
    }

    /**
     * Безопасное чтение числового значения прогресса ProgressBar по атрибуту text.
     * В случае отсутствия элемента возвращаем 0.0.
     */
    private double readProgressSafely(By progressLocator) {
        try {
            SelenideElement progress = $(progressLocator);
            if (!progress.exists()) {
                return 0.0;
            }
            String text = progress.getText();
            if (text == null || text.isEmpty()) {
                text = progress.getAttribute("text");
            }
            if (text == null || text.isEmpty()) {
                return 0.0;
            }
            return Double.parseDouble(text);
        } catch (Exception e) {
            return 0.0;
        }
    }
}

