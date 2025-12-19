package net.minecraft.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public final class CryptManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CryptManager.class);

    private CryptManager() {
    }

    public static SecretKey createNewSharedKey() {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(128);

            return generator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Required algorithm 'AES' not supported by JRE.", e);
            throw new RuntimeException("AES algorithm not supported.", e);
        }
    }

    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(1024);

            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Required algorithm 'RSA' not supported by JRE.", e);
            return null;
        }
    }

    public static byte[] getServerIdHash(String serverId, PublicKey publicKey, SecretKey secretKey) {
        try {
            MessageDigest message = MessageDigest.getInstance("SHA-1");

            message.update(serverId.getBytes(StandardCharsets.ISO_8859_1));
            message.update(secretKey.getEncoded());
            message.update(publicKey.getEncoded());

            return message.digest();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Required algorithm 'SHA-1' not supported by JRE.", e);
            return null;
        }
    }

    public static PublicKey decodePublicKey(byte[] encodedKey) {
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            EncodedKeySpec encodedKeySpec = new X509EncodedKeySpec(encodedKey);

            return factory.generatePublic(encodedKeySpec);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Algorithm 'RSA' not found while decoding public key.", e);
        } catch (InvalidKeySpecException e) {
            LOGGER.error("Invalid key specification while decoding public key.", e);
        }

        LOGGER.error("Public key reconstitution failed!");
        return null;
    }

    public static SecretKey decryptSharedKey(PrivateKey key, byte[] secretKeyEncrypted) {
        byte[] decryptedBytes = decryptData(key, secretKeyEncrypted);
        return new SecretKeySpec(decryptedBytes, "AES");
    }

    public static byte[] encryptData(Key key, byte[] data) {
        return cipherOperation(Cipher.ENCRYPT_MODE, key, data);
    }

    public static byte[] decryptData(Key key, byte[] data) {
        return cipherOperation(Cipher.DECRYPT_MODE, key, data);
    }

    private static byte[] cipherOperation(int opMode, Key key, byte[] data) {
        try {
            Cipher cipher = createTheCipherInstance(opMode, key.getAlgorithm(), key);

            if (cipher == null) {
                return null;
            }

            return cipher.doFinal(data);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            LOGGER.error("Cipher operation failed for mode {} and algorithm {}", opMode, key.getAlgorithm(), e);
        }

        LOGGER.error("Cipher data failed!");
        return null;
    }

    private static Cipher createTheCipherInstance(int opMode, String transformation, Key key) {
        try {
            Cipher cipher = Cipher.getInstance(transformation);
            cipher.init(opMode, key);

            return cipher;
        } catch (InvalidKeyException e) {
            LOGGER.error("Invalid key provided for cipher initialization: {}", key.getAlgorithm(), e);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            LOGGER.error("Required cipher algorithm/padding not found: {}", transformation, e);
        }

        return null;
    }

    public static Cipher createNetCipherInstance(int opMode, Key key) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CFB8/NoPadding");
            cipher.init(opMode, key, new IvParameterSpec(key.getEncoded()));

            return cipher;
        } catch (GeneralSecurityException e) {
            LOGGER.error("Failed to create network cipher instance ('AES/CFB8/NoPadding').", e);
            throw new RuntimeException("Failed to create network cipher instance.", e);
        }
    }
}
