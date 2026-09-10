package com.bankops.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class PasswordService {
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordValue create(String rawPassword) {
        validatePolicy(rawPassword);
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        return new PasswordValue(hash(rawPassword, salt), Base64.getEncoder().encodeToString(salt));
    }

    public boolean matches(String rawPassword, String saltText, String expectedHash) {
        try {
            byte[] salt = Base64.getDecoder().decode(saltText);
            String actualHash = hash(rawPassword, salt);
            return constantTimeEquals(actualHash, expectedHash);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public void validatePolicy(String password) {
        if (password == null || password.length() < 8 || password.length() > 64
                || !password.matches(".*[A-Za-z].*")
                || !password.matches(".*\\d.*")
                || !password.matches(".*[^A-Za-z0-9].*")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "密码需为8-64位，且同时包含字母、数字和特殊字符");
        }
    }

    private String hash(String rawPassword, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        try {
            byte[] encoded = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(encoded);
        } catch (Exception ex) {
            throw new IllegalStateException("密码加密失败", ex);
        } finally {
            spec.clearPassword();
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null || left.length() != right.length()) return false;
        int result = 0;
        for (int i = 0; i < left.length(); i++) result |= left.charAt(i) ^ right.charAt(i);
        return result == 0;
    }

    public record PasswordValue(String hash, String salt) {}
}
