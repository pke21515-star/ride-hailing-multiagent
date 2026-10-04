package io.github.pke21515.ride.api;

import io.github.pke21515.ride.entity.UserEntity;
import io.github.pke21515.ride.repository.UserRepo;
import io.github.pke21515.ride.security.TokenService;
import io.github.pke21515.ride.security.TokenService.Claims;
import io.github.pke21515.ride.security.AuthInterceptor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 认证：注册 / 登录 / 当前用户。 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepo userRepo;
    private final TokenService tokens;

    public AuthController(UserRepo userRepo, TokenService tokens) {
        this.userRepo = userRepo; this.tokens = tokens;
    }

    public record RegisterReq(String phone, String password, String name) {}
    public record LoginReq(String phone, String password) {}

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody RegisterReq req) {
        if (req.phone() == null || req.phone().isBlank() || req.password() == null || req.password().length() < 6)
            return Map.of("error", "手机号不能为空，密码至少 6 位");
        if (userRepo.findByPhone(req.phone()).isPresent())
            return Map.of("error", "该手机号已注册");
        UserEntity u = new UserEntity();
        u.phone = req.phone().trim();
        u.passwordHash = TokenService.hash(req.password());
        u.name = req.name() == null || req.name().isBlank() ? "乘客" + req.phone().substring(Math.max(0, req.phone().length() - 4)) : req.name();
        u.role = "PASSENGER";
        userRepo.save(u);
        return loginResponse(u);
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginReq req) {
        UserEntity u = userRepo.findByPhone(req.phone()).orElse(null);
        if (u == null || !u.passwordHash.equals(TokenService.hash(req.password())))
            return Map.of("error", "手机号或密码错误");
        return loginResponse(u);
    }

    @GetMapping("/me")
    public Map<String, Object> me(@RequestAttribute("claims") Claims c) {
        return Map.of("userId", c.uid(), "role", c.role(), "name", c.name(), "driverId", c.driverId() == null ? "" : c.driverId());
    }

    private Map<String, Object> loginResponse(UserEntity u) {
        String token = tokens.issue(u.id, u.role, u.name, u.driverId);
        return Map.of("token", token, "role", u.role, "name", u.name,
                "userId", u.id, "driverId", u.driverId == null ? "" : u.driverId);
    }
}
