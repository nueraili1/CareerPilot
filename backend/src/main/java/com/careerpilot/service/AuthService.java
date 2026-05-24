package com.careerpilot.service;

import com.careerpilot.dto.AuthResponse;
import com.careerpilot.dto.LoginRequest;
import com.careerpilot.dto.RegisterRequest;
import com.careerpilot.dto.ResetPasswordRequest;
import com.careerpilot.dto.SendCodeRequest;
import com.careerpilot.dto.SendCodeResponse;
import com.careerpilot.dto.UserProfile;
import com.careerpilot.model.AppUser;
import com.careerpilot.repository.AppUserRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordService passwordService;
    private final TokenService tokenService;
    private final AuthContext authContext;
    private final VerificationCodeService verificationCodeService;

    public AuthService(
            AppUserRepository userRepository,
            PasswordService passwordService,
            TokenService tokenService,
            AuthContext authContext,
            VerificationCodeService verificationCodeService) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.tokenService = tokenService;
        this.authContext = authContext;
        this.verificationCodeService = verificationCodeService;
    }

    public SendCodeResponse sendCode(SendCodeRequest request) {
        String phone = normalizePhone(request.getPhone());
        String purpose = normalizePurpose(request.getPurpose());
        if ("REGISTER".equals(purpose) && userRepository.existsByPhone(phone)) {
            throw new IllegalArgumentException("手机号已注册");
        }
        if ("RESET_PASSWORD".equals(purpose) && userRepository.findByPhone(phone).isEmpty()) {
            throw new IllegalArgumentException("该手机号尚未注册");
        }
        return verificationCodeService.send(phone, purpose);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = normalizeUsername(request.getUsername());
        String phone = normalizePhone(request.getPhone());
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new IllegalArgumentException("手机号已注册");
        }
        verificationCodeService.verify(phone, "REGISTER", request.getCode());

        String salt = passwordService.newSalt();
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPhone(phone);
        user.setSalt(salt);
        user.setPasswordHash(passwordService.hash(request.getPassword(), salt));
        userRepository.save(user);
        return toResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        String identifier = request.getIdentifier() == null ? "" : request.getIdentifier().trim();
        AppUser user = findByIdentifier(identifier)
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));
        if (!passwordService.matches(request.getPassword(), user.getSalt(), user.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        return toResponse(user);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String phone = normalizePhone(request.getPhone());
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }
        verificationCodeService.verify(phone, "RESET_PASSWORD", request.getCode());
        AppUser user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new IllegalArgumentException("该手机号尚未注册"));
        String salt = passwordService.newSalt();
        user.setSalt(salt);
        user.setPasswordHash(passwordService.hash(request.getPassword(), salt));
        userRepository.save(user);
    }

    public UserProfile me() {
        TokenPayload tokenPayload = authContext.requireUser();
        AppUser user = userRepository.findById(tokenPayload.userId())
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return new UserProfile(user.getId(), user.getUsername(), user.getPhone());
    }

    private AuthResponse toResponse(AppUser user) {
        return new AuthResponse(
                tokenService.createToken(user.getId(), user.getUsername()),
                user.getId(),
                user.getUsername(),
                user.getPhone());
    }

    private Optional<AppUser> findByIdentifier(String identifier) {
        if (identifier.matches("^1\\d{10}$")) {
            return userRepository.findByPhone(identifier);
        }
        return userRepository.findByUsername(normalizeUsername(identifier));
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    private String normalizePhone(String phone) {
        return phone == null ? "" : phone.trim();
    }

    private String normalizePurpose(String purpose) {
        return purpose == null ? "" : purpose.trim().toUpperCase();
    }
}
