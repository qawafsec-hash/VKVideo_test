package org.example.vkvideo;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.appium.SelenideAppium;
import io.appium.java_client.android.AndroidDriver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Map;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.sleep;
import static com.codeborne.selenide.appium.SelenideAppium.$;
import static com.codeborne.selenide.appium.SelenideAppium.$$;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VkVideoPlaybackTestPositive {

    private static AndroidDriver driver;

    private static final String APPIUM_SERVER_URL = "http://127.0.0.1:4723/";
    private static final String PLATFORM_NAME = "Android";
    private static final String AUTOMATION_NAME = "UiAutomator2";
    private static final String DEVICE_NAME = "emulator-5554";
    private static final String PLATFORM_VERSION = "14";
    private static final String APP_PACKAGE = "com.vk.vkvideo";
    private static final String APP_ACTIVITY = "com.vk.video.screens.main.MainActivity";

    private static final By FIRST_VIDEO_CARD = By.id("com.vk.vkvideo:id/content");
    private static final By VIDEO_AD_SKIP = By.id("com.vk.vkvideo:id/video_ad_skip");
    private static final By VIDEO_AD_PROGRESS = By.id("com.vk.vkvideo:id/video_ad_progress_bar");
    private static final By PLAYER_CONTROL = By.id("com.vk.vkvideo:id/player_control");
    private static final By VIDEO_DISPLAY = By.id("com.vk.vkvideo:id/video_display");
    private static final By CURRENT_PROGRESS = By.id("com.vk.vkvideo:id/current_progress");
    private static final By PLAYER_DURATION = By.id("com.vk.vkvideo:id/duration");

    @BeforeAll
    static void setUp() throws MalformedURLException {
        Configuration.browserSize = null;
        Configuration.timeout = Duration.ofSeconds(15).toMillis();
        MutableCapabilities caps = new DesiredCapabilities();
        caps.setCapability("platformName", PLATFORM_NAME);
        caps.setCapability("appium:automationName", AUTOMATION_NAME);
        caps.setCapability("appium:deviceName", DEVICE_NAME);
        caps.setCapability("appium:platformVersion", PLATFORM_VERSION);
        caps.setCapability("appium:appPackage", APP_PACKAGE);
        caps.setCapability("appium:appActivity", APP_ACTIVITY);
        caps.setCapability("appium:noReset", true);
        caps.setCapability("appium:newCommandTimeout", 180);
        driver = new AndroidDriver(new URL(APPIUM_SERVER_URL), caps);
        WebDriverRunner.setWebDriver(driver);
        SelenideAppium.launchApp();
        openVkVideoIfNeeded();
    }

    @AfterAll
    static void tearDown() {
        if (driver != null) driver.quit();
    }

    @AfterEach
    void closeApp() {
        if (driver != null) {
            try {
                driver.terminateApp(APP_PACKAGE);
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    @DisplayName("VK Видео: видео воспроизводится — время увеличивается")
    void videoPlaysAndTimeIncreases() {
        $(By.id("com.vk.vkvideo:id/vk_video_root_view")).shouldBe(visible);
        $$(FIRST_VIDEO_CARD).first().shouldBe(visible).click();
        sleep(1500);
        if ($(VIDEO_AD_PROGRESS).exists() && $(VIDEO_AD_SKIP).exists()) {
            $(VIDEO_AD_SKIP).click();
            sleep(2000);
        }
        if ($(PLAYER_CONTROL).exists()) $(PLAYER_CONTROL).shouldBe(visible);
        else if ($(VIDEO_DISPLAY).exists()) $(VIDEO_DISPLAY).shouldBe(visible);

        int time1 = getCurrentTimeSeconds();
        assertTrue(time1 >= 0, "Не удалось прочитать текущее время воспроизведения");
        sleep(2000);
        int time2 = getCurrentTimeSeconds();
        assertTrue(time2 > time1, "Ожидалось увеличение времени: было " + time1 + ", стало " + time2);
    }

    private static void openVkVideoIfNeeded() {
        try {
            $(By.id("com.vk.vkvideo:id/vk_video_root_view")).shouldBe(visible);
            return;
        } catch (Throwable ignore) {}
        $(By.xpath("//*[@text='VK Video' or @content-desc='VK Video' or @content-desc='Predicted app: VK Video']")).shouldBe(visible).click();
        $(By.id("com.vk.vkvideo:id/vk_video_root_view")).shouldBe(visible);
    }

    private void tapToShowPlayerControls() {
        WebElement area = null;
        if ($(PLAYER_CONTROL).exists()) area = $(PLAYER_CONTROL).getWrappedElement();
        else if ($(VIDEO_DISPLAY).exists()) area = $(VIDEO_DISPLAY).getWrappedElement();
        if (area == null) { sleep(300); return; }
        Point loc = area.getLocation();
        Dimension size = area.getSize();
        try {
            ((JavascriptExecutor) driver).executeScript("mobile: clickGesture", Map.of("x", loc.getX() + size.getWidth() / 2, "y", loc.getY() + size.getHeight() / 2));
        } catch (Exception e) {
            area.click();
        }
        sleep(300);
    }

    private static int parseTimeToSeconds(String raw) {
        if (raw == null || raw.isEmpty()) return -1;
        String part = raw.split("\\s*/\\s*|\\s+")[0].trim();
        String[] mmss = part.split(":");
        if (mmss.length < 2) return -1;
        try {
            return Integer.parseInt(mmss[0].trim()) * 60 + Integer.parseInt(mmss[1].trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private int getCurrentTimeSeconds() {
        tapToShowPlayerControls();
        try {
            if ($(CURRENT_PROGRESS).exists()) {
                String text = $(CURRENT_PROGRESS).getText();
                if (text == null) text = $(CURRENT_PROGRESS).getAttribute("text");
                if (text != null && !text.isEmpty()) {
                    int sec = parseTimeToSeconds(text);
                    if (sec >= 0) return sec;
                }
            }
            if ($(PLAYER_CONTROL).exists()) {
                SelenideElement d = $(PLAYER_CONTROL).$(By.id("com.vk.vkvideo:id/duration"));
                if (d.exists()) {
                    String text = d.getText();
                    if (text == null) text = d.getAttribute("text");
                    if (text != null && !text.isEmpty()) return parseTimeToSeconds(text);
                }
            }
            if ($(PLAYER_DURATION).exists()) {
                String text = $(PLAYER_DURATION).getText();
                if (text == null) text = $(PLAYER_DURATION).getAttribute("text");
                if (text != null && !text.isEmpty()) return parseTimeToSeconds(text);
            }
        } catch (Exception ignore) {}
        return -1;
    }
}
