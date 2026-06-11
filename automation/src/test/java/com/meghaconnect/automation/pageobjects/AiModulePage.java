package com.meghaconnect.automation.pageobjects;

import com.meghaconnect.automation.utils.WebElementUtil;
import org.openqa.selenium.By;

public class AiModulePage extends MeghaConnectModulePage {
    public static final By CHAT_TRIGGER = By.cssSelector("[data-testid='ai-chatbot-trigger'], .chatbot-trigger");
    public static final By CHAT_INPUT = By.cssSelector("[data-testid='ai-chatbot-input'], .chat-textarea");
    public static final By SEND_BUTTON = By.cssSelector("[data-testid='ai-chatbot-send-btn'], .send-btn");

    public void openChat() {
        WebElementUtil.clickWithHighlight(CHAT_TRIGGER);
    }

    public void ask(String question) {
        WebElementUtil.typeWithHighlight(CHAT_INPUT, question);
        WebElementUtil.clickWithHighlight(SEND_BUTTON);
    }
}
