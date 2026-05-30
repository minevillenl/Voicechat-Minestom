package dev.lu15.voicechat.network.voice.encryption;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

public final class AES {

    private AES() {}

    private static final @NotNull Random RANDOM = new SecureRandom();
    // simple-voice-chat (compatibility version 20) uses AES/GCM/NoPadding with a
    // 12-byte IV and a 128-bit authentication tag; the IV is prepended to the payload.
    private static final @NotNull String CIPHER = "AES/GCM/NoPadding";
    private static final int UUID_LENGTH = 16;
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    public static byte @NotNull[] getBytesFromUuid(@NotNull UUID uuid) {
        NetworkBuffer buffer = NetworkBuffer.staticBuffer(UUID_LENGTH);
        buffer.write(NetworkBuffer.UUID, uuid);
        byte[] bytes = new byte[UUID_LENGTH];
        buffer.copyTo(0, bytes, 0, UUID_LENGTH);
        return bytes;
    }

    private static byte @NotNull[] generateIv() {
        byte[] iv = new byte[IV_LENGTH];
        RANDOM.nextBytes(iv);
        return iv;
    }

    private static @NotNull SecretKeySpec createKey(@NotNull UUID secret) {
        return new SecretKeySpec(getBytesFromUuid(secret), "AES");
    }

    public static byte @NotNull[] encrypt(@NotNull UUID secret, byte @NotNull[] data) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        byte[] iv = generateIv();
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
        Cipher cipher = Cipher.getInstance(CIPHER);
        cipher.init(Cipher.ENCRYPT_MODE, createKey(secret), spec);

        byte[] encrypted = cipher.doFinal(data);
        byte[] result = new byte[iv.length + encrypted.length];

        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
        return result;
    }

    public static byte @NotNull[] decrypt(@NotNull UUID secret, byte @NotNull[] result) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        byte[] iv = Arrays.copyOfRange(result, 0, IV_LENGTH);
        byte[] data = Arrays.copyOfRange(result, IV_LENGTH, result.length);

        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
        Cipher cipher = Cipher.getInstance(CIPHER);
        cipher.init(Cipher.DECRYPT_MODE, createKey(secret), spec);

        return cipher.doFinal(data);
    }

}
