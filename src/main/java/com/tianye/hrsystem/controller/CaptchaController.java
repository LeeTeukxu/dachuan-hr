package com.tianye.hrsystem.controller;

import lombok.extern.slf4j.Slf4j;

import com.tianye.hrsystem.common.Redis;
import com.tianye.hrsystem.model.successResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;

/**
 * 图形验证码（后端生成，Redis 存储，一次性使用）
 * SaaS 安全改造 P0-1
 */
@RestController
@Slf4j
public class CaptchaController {

    private static final String CAPTCHA_KEY_PREFIX = "hr:captcha:";
    private static final int CAPTCHA_EXPIRE_SECONDS = 300;
    private static final String CAPTCHA_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    @Autowired
    Redis redis;

    /**
     * 生成图形验证码
     * 返回 data: { captchaId, imageBase64 }  imageBase64 可直接用于 <img :src="'data:image/png;base64,'+imageBase64">
     */
    @GetMapping("/captcha/generate")
    public successResult generate() {
        successResult result = new successResult();
        try {
            String code = randomCode();
            String captchaId = UUID.randomUUID().toString().replace("-", "");
            redis.setex(CAPTCHA_KEY_PREFIX + captchaId, CAPTCHA_EXPIRE_SECONDS, code);
            String imageBase64 = drawImage(code);
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("captchaId", captchaId);
            data.put("expireSeconds", CAPTCHA_EXPIRE_SECONDS);
            data.put("imageBase64", imageBase64);
            result.setData(data);
        } catch (Exception ax) {
            result.raiseException(ax);
        }
        return result;
    }

    /**
     * 校验验证码（供 LoginController 调用）。校验通过后立即失效（一次性）。
     * @return null 表示校验通过；否则返回错误信息
     */
    public static String validate(Redis redis, String captchaId, String captchaCode) {
        if (captchaId == null || captchaId.trim().isEmpty()) {
            return "验证码已失效，请刷新后重试";
        }
        if (captchaCode == null || captchaCode.trim().isEmpty()) {
            return "请输入验证码";
        }
        String key = CAPTCHA_KEY_PREFIX + captchaId.trim();
        String saved = redis.get(key);
        // 无论成功与否都删除，保证一次性使用
        redis.del(key);
        if (saved == null) {
            return "验证码已过期，请刷新后重试";
        }
        if (!saved.equalsIgnoreCase(captchaCode.trim())) {
            return "验证码不正确";
        }
        return null;
    }

    private String randomCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            sb.append(CAPTCHA_CHARS.charAt(random.nextInt(CAPTCHA_CHARS.length())));
        }
        return sb.toString();
    }

    private String drawImage(String code) throws Exception {
        int width = 120, height = 40;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            Random random = new Random();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(245, 247, 250));
            g.fillRect(0, 0, width, height);

            for (int i = 0; i < 6; i++) {
                g.setColor(randomColor(random, 130, 220));
                g.setStroke(new BasicStroke(1f));
                g.drawLine(random.nextInt(width), random.nextInt(height),
                        random.nextInt(width), random.nextInt(height));
            }

            int charWidth = width / code.length();
            for (int i = 0; i < code.length(); i++) {
                g.setColor(randomColor(random, 30, 120));
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
                double theta = (random.nextDouble() - 0.5) * 0.5;
                int x = charWidth * i + charWidth / 2;
                int y = height / 2;
                g.rotate(theta, x, y);
                g.drawString(String.valueOf(code.charAt(i)), x - 8, y + 9);
                g.rotate(-theta, x, y);
            }

            for (int i = 0; i < 24; i++) {
                g.setColor(randomColor(random, 120, 230));
                g.fillOval(random.nextInt(width), random.nextInt(height), 2, 2);
            }
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    private Color randomColor(Random random, int min, int max) {
        return new Color(
                min + random.nextInt(max - min + 1),
                min + random.nextInt(max - min + 1),
                min + random.nextInt(max - min + 1));
    }
}
