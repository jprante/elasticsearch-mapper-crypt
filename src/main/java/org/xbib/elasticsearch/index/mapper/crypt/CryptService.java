package org.xbib.elasticsearch.index.mapper.crypt;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.Settings;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Field;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;

public class CryptService extends AbstractLifecycleComponent<CryptService> {

    private final static ESLogger logger = ESLoggerFactory.getLogger(CryptService.class.getSimpleName());

    private SecureRandom secureRandom;

    private KeyStore keystore;

    private char[] keystorePassword;

    private SecretKeyFactory secretKeyFactory;

    private Cipher cipher;

    private int passwordLength;

    private int iterationCount;

    private int saltLength;

    private byte[] salt;

    private PBEParameterSpec pbeParamSpec;

    private AlgorithmParameters algorithmParameters;

    @Inject
    public CryptService(Settings settings) {
        super(settings);
        this.keystorePassword = settings.get("index.mapper.crypt.keystore_password", "elasticsearch").toCharArray();
        this.passwordLength = settings.getAsInt("index.mapper.crypt.password_length", 16);
        saltLength = settings.getAsInt("index.mapper.crypt.salt_length", 8);
        iterationCount = settings.getAsInt("index.mapper.crypt.iteration_count", 1);
        secureRandom = new SecureRandom();
        salt = secureRandom.generateSeed(saltLength);
    }

    @Override
    protected void doStart() throws ElasticsearchException {
        try {
            removeCryptoRestrictions();
            keystore = KeyStore.getInstance("jks");
            secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            if (secretKeyFactory == null) {
                throw new ElasticsearchException("no secret key factory");
            }
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            algorithmParameters = cipher.getParameters();
            pbeParamSpec = new PBEParameterSpec(salt, iterationCount);
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(256);
            logger.info("successfully started cryptography service");
        } catch (IllegalAccessException | ClassNotFoundException | NoSuchPaddingException | NoSuchFieldException | KeyStoreException | NoSuchAlgorithmException e) {
           throw new ElasticsearchException(e.getMessage(), e);
        }
    }

    @Override
    protected void doStop() throws ElasticsearchException {
    }

    @Override
    protected void doClose() throws ElasticsearchException {
    }

    public KeyStore getKeystore() {
        return keystore;
    }

    public char[] getKeystorePassword() {
        return keystorePassword;
    }

    public int getIterationCount() {
        return iterationCount;
    }

    public byte[] getSalt() {
        return salt;
    }

    public AlgorithmParameters getAlgorithmParameters() {
        return algorithmParameters;
    }

    public SecretKey generateKey() throws ElasticsearchException {
        try {
            salt = secureRandom.generateSeed(saltLength);
            pbeParamSpec = new PBEParameterSpec(salt, iterationCount);
            char[] randomPassword = new char[passwordLength];
            for(int i = 0; i < passwordLength; i++) {
                randomPassword[i] = (char)(secureRandom.nextInt('~' - '!' + 1) + '!');
            }
            PBEKeySpec keySpec = new PBEKeySpec(randomPassword, salt, iterationCount, 256);
            SecretKey key = secretKeyFactory.generateSecret(keySpec);
            return new SecretKeySpec(key.getEncoded(), "AES");
        } catch (InvalidKeySpecException e) {
            throw new ElasticsearchException(e.getMessage(), e);
        }
    }

    public void setKey(String keyName, SecretKey key) throws ElasticsearchException {
        try {
            synchronized (keystore) {
                keystore.setEntry(keyName, new KeyStore.SecretKeyEntry(key),
                        new KeyStore.PasswordProtection(keystorePassword));
            }
        } catch (KeyStoreException e) {
            throw new ElasticsearchException(e.getMessage(), e);
        }
    }

    public SecretKey getKey(String keyName) throws ElasticsearchException {
        try {
            synchronized (keystore) {
                return (SecretKey) keystore.getKey(keyName, keystorePassword);
            }
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new ElasticsearchException(e.getMessage(), e);
        }
    }

    public synchronized byte[] encrypt(String keyName, byte[] clearText)
            throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        SecretKey secretKey = getKey(keyName);
        if (secretKey == null) {
            throw new InvalidKeyException("no key found for name " + keyName);
        }
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, pbeParamSpec);
        byte[] encryptedBytes = cipher.doFinal(clearText);
        byte[] bytesToEncode = new byte[saltLength + encryptedBytes.length];
        System.arraycopy(salt, 0, bytesToEncode, 0, saltLength);
        System.arraycopy(encryptedBytes, 0, bytesToEncode, saltLength, encryptedBytes.length);
        return bytesToEncode;
    }

    public synchronized byte[] decrypt(String name, byte[] cipherText)
            throws InvalidKeySpecException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        SecretKey secretKey = getKey(name);
        if (secretKey == null) {
            throw new InvalidKeyException("no key found for " + name);
        }
        cipher.init(Cipher.DECRYPT_MODE, secretKey, pbeParamSpec);
        int decryptedBytesLength = cipherText.length - saltLength;
        byte[] decryptedBytes = new byte[decryptedBytesLength];
        System.arraycopy(cipherText, saltLength, decryptedBytes, 0, decryptedBytesLength);
        return cipher.doFinal(decryptedBytes);
    }

    private void removeCryptoRestrictions() throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        if ("Java(TM) SE Runtime Environment".equals(System.getProperty("java.runtime.name"))) {
            final Class<?> jceSecurity = Class.forName("javax.crypto.JceSecurity");
            final Class<?> cryptoPermissions = Class.forName("javax.crypto.CryptoPermissions");
            final Class<?> cryptoAllPermission = Class.forName("javax.crypto.CryptoAllPermission");
            final Field isRestrictedField = jceSecurity.getDeclaredField("isRestricted");
            isRestrictedField.setAccessible(true);
            isRestrictedField.set(null, false);
            final Field defaultPolicyField = jceSecurity.getDeclaredField("defaultPolicy");
            defaultPolicyField.setAccessible(true);
            final PermissionCollection defaultPolicy = (PermissionCollection) defaultPolicyField.get(null);
            final Field perms = cryptoPermissions.getDeclaredField("perms");
            perms.setAccessible(true);
            ((Map<?, ?>) perms.get(defaultPolicy)).clear();
            final Field instance = cryptoAllPermission.getDeclaredField("INSTANCE");
            instance.setAccessible(true);
            defaultPolicy.add((Permission) instance.get(null));
        }
    }

}
