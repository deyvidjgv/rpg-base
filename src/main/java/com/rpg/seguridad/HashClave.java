package com.rpg.seguridad;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Convierte una clave en su hash SHA-256 para no guardarla en texto plano. */
public final class HashClave {

    private HashClave() {
    }

    /** @return el hash en hexadecimal en minusculas (64 caracteres), igual que SHA2(texto, 256) en MySQL */
    public static String sha256(String clave) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(clave.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // Toda JVM debe incluir SHA-256; si falta, no se puede autenticar a nadie.
            throw new IllegalStateException("La JVM no tiene el algoritmo SHA-256", e);
        }
    }
}
