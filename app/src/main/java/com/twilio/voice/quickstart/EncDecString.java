package com.twilio.voice.quickstart;

import android.util.Base64;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncDecString {

    //                                          1234567890123456
    private final String encryptionKeyString = "16-character-Str";

    //                                 1234567890123456 - 16 bytes IV
    private final String initVector = "RandomInitVector";
    private final byte[] ivBytes = initVector.getBytes();

    private byte[] encrypt(final byte[] keyBytes, final byte[] ivBytes, final byte[] messageBytes) throws InvalidKeyException, InvalidAlgorithmParameterException {
        return transform(Cipher.ENCRYPT_MODE, keyBytes, ivBytes, messageBytes);
    }

    private byte[] decrypt(final byte[] keyBytes, final byte[] ivBytes, final byte[] messageBytes) throws InvalidKeyException, InvalidAlgorithmParameterException {
        return transform(Cipher.DECRYPT_MODE, keyBytes, ivBytes, messageBytes);
    }

    private byte[] transform(final int mode, final byte[] keyBytes, final byte[] ivBytes, final byte[] messageBytes) throws InvalidKeyException, InvalidAlgorithmParameterException {
        final SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        final IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
        byte[] transformedBytes = null;
        try {
            final Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
            cipher.init(mode, keySpec, ivSpec);
            transformedBytes = cipher.doFinal(messageBytes);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
        return transformedBytes;
    }

    public String encryptBase64String(String msgString) throws InvalidKeyException, InvalidAlgorithmParameterException {
        byte[] keyBytes = encryptionKeyString.getBytes();
        byte[] msgBytes = msgString.getBytes();
        byte[] msgEncryptedBytes = encrypt(keyBytes, ivBytes, msgBytes);
        return Base64.encodeToString(msgEncryptedBytes, Base64.DEFAULT);
    }
    public String decryptBase64String(String msgBase64encodedEncryptedString) throws InvalidKeyException, InvalidAlgorithmParameterException {
        byte[] keyBytes = encryptionKeyString.getBytes();
        byte[] msgDecryptedBase64bytes = decrypt(keyBytes, ivBytes, Base64.decode(msgBase64encodedEncryptedString, Base64.DEFAULT));
        return new String(msgDecryptedBase64bytes);
    }

}
