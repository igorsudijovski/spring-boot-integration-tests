package com.igorsudijovski.integrationtests.util;

import jakarta.annotation.Nonnull;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Base64;
import java.util.List;

public class KeysGeneratorUtilTest {

    private static final String KEYS_LOCATION = "src/main/resources/keys/";

    @Disabled
    @Test
    @SneakyThrows
    public void generateKeys() {
        String kid = "auth";
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        RSAKeyGenParameterSpec kpgSpec = new RSAKeyGenParameterSpec(2048, BigInteger.valueOf(65537));
        generator.initialize(kpgSpec);
        KeyPair pair = generator.generateKeyPair();
        PrivateKey privateKey = pair.getPrivate();
        RSAPublicKey publicKey = (RSAPublicKey) pair.getPublic();

        String n = base64Encode(publicKey.getModulus());
        String e = base64Encode(publicKey.getPublicExponent());
        try (FileOutputStream fos = new FileOutputStream(KEYS_LOCATION + "n.key")) {
            fos.write(encoded(n));
        }
        try (FileOutputStream fos = new FileOutputStream(KEYS_LOCATION + "e.key")) {
            fos.write(encoded(e));
        }

        try (FileOutputStream fos = new FileOutputStream(KEYS_LOCATION + "private.key")) {
            fos.write(privateKey.getEncoded());
        }
        overwritePublicKey(kid, n, e);
    }

    @SneakyThrows
    private static void overwritePublicKey(String kid, String n, String e) {
        String replaceRegex = "\"[^\",]+\",";
        String valueFormat = "\"%s\",";
        String valueName = "\"%s\":";
        Path filePath = Paths.get("src/main/resources/wiremock/__files/keycloak.json");

        boolean inCorrectBlock = false;
        try {
            List<String> lines = Files.readAllLines(filePath);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.contains("kid")) {
                    inCorrectBlock = line.endsWith(String.format(valueFormat, kid));
                    continue;
                }
                if (line.contains(String.format(valueName, "n")) && inCorrectBlock) {
                    lines.set(i, line.replaceAll(replaceRegex, String.format(valueFormat, n)));
                    continue;
                }
                if (line.contains(String.format(valueName, "e")) && inCorrectBlock) {
                    lines.set(i, line.replaceAll(replaceRegex, String.format(valueFormat, e)));
                }
            }
            Files.write(filePath, lines);
        } catch (IOException ex) {
            System.out.println("An error occurred: " + ex.getMessage());
        }
    }


    private static byte[] encoded(@Nonnull String str) {
        return str.getBytes(Charset.defaultCharset());
    }

    private static String base64Encode(@Nonnull BigInteger value) {
        return Base64.getUrlEncoder().encodeToString(value.toByteArray());
    }

}
