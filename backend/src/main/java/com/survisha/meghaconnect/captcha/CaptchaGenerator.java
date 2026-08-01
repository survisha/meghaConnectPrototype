package com.survisha.meghaconnect.captcha;

import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import com.survisha.meghaconnect.exception.MeghaConnectException;

@Component
public class CaptchaGenerator {
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final int width;
    private final int height;
    private final int fontSize;
    private final int length;
    private final int noiseLines;

    public CaptchaGenerator(@Value("${captcha.width:180}") int width,
                            @Value("${captcha.height:60}") int height,
                            @Value("${captcha.font-size:34}") int fontSize,
                            @Value("${captcha.length:6}") int length,
                            @Value("${captcha.noise-lines:8}") int noiseLines) {
        this.width = width;
        this.height = height;
        this.fontSize = fontSize;
        this.length = length;
        this.noiseLines = noiseLines;
    }

    public String generateText() {
        StringBuilder value = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            value.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return value.toString();
    }

    public String generateImage(String text) {
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(246, 250, 251));
            graphics.fillRect(0, 0, width, height);
            graphics.setColor(new Color(209, 226, 232));
            for (int i = 0; i < noiseLines; i++) {
                int y = RANDOM.nextInt(height);
                graphics.drawLine(0, y, width, RANDOM.nextInt(height));
            }
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize));
            int spacing = Math.max(fontSize - 6, (width - 24) / Math.max(length, 1));
            for (int i = 0; i < text.length(); i++) {
                graphics.setColor(i % 2 == 0 ? new Color(18, 107, 88) : new Color(22, 119, 184));
                graphics.drawString(String.valueOf(text.charAt(i)), 12 + (i * spacing),
                        Math.min(height - 8, fontSize + RANDOM.nextInt(Math.max(1, height - fontSize - 4))));
            }
            graphics.dispose();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception ex) {
            throw new MeghaConnectException("CAPTCHA_GENERATION_FAILED", "Unable to generate captcha", 500, ex);
        }
    }
}
