package io.github.pke21515.ride.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

/** 角色鉴权拦截器：/api/admin/** → ADMIN；/api/driver/** → DRIVER；/api/passenger/** → PASSENGER/ADMIN。 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenService tokens;
    private final ObjectMapper om = new ObjectMapper();

    public AuthInterceptor(TokenService tokens) { this.tokens = tokens; }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
        String auth = req.getHeader("Authorization");
        String token = auth != null && auth.startsWith("Bearer ") ? auth.substring(7) : null;
        try {
            TokenService.Claims c = tokens.verify(token);
            String path = req.getRequestURI();
            String need = path.startsWith("/api/admin/") ? "ADMIN"
                    : path.startsWith("/api/driver/") ? "DRIVER" : "PASSENGER";
            boolean ok = switch (need) {
                case "ADMIN" -> "ADMIN".equals(c.role());
                case "DRIVER" -> "DRIVER".equals(c.role());
                default -> "PASSENGER".equals(c.role()) || "ADMIN".equals(c.role());
            };
            if (!ok) return reject(resp, 403, "权限不足（需要 " + need + " 角色）");
            req.setAttribute("claims", c);
            return true;
        } catch (Exception e) {
            return reject(resp, 401, "未登录或令牌无效：" + e.getMessage());
        }
    }

    private boolean reject(HttpServletResponse resp, int code, String msg) throws Exception {
        resp.setStatus(code);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(om.writeValueAsString(Map.of("error", msg)));
        return false;
    }
}
