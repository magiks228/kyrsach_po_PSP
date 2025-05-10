package org.strah.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

public final class PasswordUtil {

    private static final SecureRandom RNG = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();

    /** генерирует 16‑байтовую соль и возвращает hex‑строку */
    public static String newSalt() {
        byte[] salt = new byte[16];
        RNG.nextBytes(salt);
        return HEX.formatHex(salt);
    }

    /** sha256( salt + password ) → hex */
    public static String hash(String saltHex, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(HEX.parseHex(saltHex));
            md.update(password.getBytes());
            return HEX.formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA‑256 not found", e);
        }
    }

    /** проверка */
    public static boolean matches(String saltHex, String expectedHash, String rawPwd){
        return expectedHash.equalsIgnoreCase(hash(saltHex, rawPwd));
    }
}
