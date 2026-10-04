package io.github.pke21515.ride.security;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 轻量 JWT 风格令牌：HMAC-SHA256 签名，无第三方依赖。
 * 生产可替换为 jjwt / Spring Security OAuth2，接口不变。
 */
@Component
public class TokenService {

    private static final String SECRET = "ride-multiagent-portfolio-secret-2026";
    private static final long EXPIRE_MS = 1000L * 60 * 60 * 24 * 7;   // 7 天

    public record Claims(Long uid, String role, String name, String driverId, long exp) {}

    public String issue(Long uid, String role, String name, String driverId) {
        long exp = System.currentTimeMillis() + EXPIRE_MS;
        String payload = uid + "|" + role + "|" + name + "|" + (driverId == null ? "-" : driverId) + "|" + exp;
        String body = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return body + "." + hmac(body);
    }

    /** 校验签名与有效期，失败抛出 IllegalStateException */
    public Claims verify(String token) {
        if (token == null || !token.contains(".")) throw new IllegalStateException("令牌格式错误");
        String[] parts = token.split("\\.", 2);
        if (!hmac(parts[0]).equals(parts[1])) throw new IllegalStateException("令牌签名不合法");
        String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        String[] f = payload.split("\\|", -1);
        if (f.length < 5) throw new IllegalStateException("令牌载荷不完整");
        if (Long.parseLong(f[4]) < System.currentTimeMillis()) throw new IllegalStateException("令牌已过期");
        return new Claims(Long.valueOf(f[0]), f[1], f[2], "-".equals(f[3]) ? null : f[3], Long.parseLong(f[4]));
    }

    private String hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** 口令摘要：SHA-256(盐 + 口令)。生产替换 BCrypt。 */
    public static String hash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(("ride-salt$" + password).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
