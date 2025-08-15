package com.igorsudijovski.integrationtests.util;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.SneakyThrows;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.util.*;

public final class JwtGenerator {

    private static final int EXPIRATION_TIME_IN_MINUTES = 1200;
    private static final String ISSUER = "%s/idp/realms/%s";
    private static final String LOCATION_KEYS = "src/main/resources/keys/";


    public static String generateJwt(String issuer) {
        Map<String, Object> claims = new HashMap<>();

        claims.put("typ", "Bearer");
        claims.put("scope", "openid");

        return generateJwt(issuer, claims);
    }

    @SneakyThrows
    private static String generateJwt(String issuer, Map<String, Object> claims) {
        String keyId = "auth";
        String realm = "user";
        JWSHeader jweHeader = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(JOSEObjectType.JWT)
                .keyID(keyId)
                .build();

        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .issuer(String.format(ISSUER, issuer, realm))
                .audience("all")
                .expirationTime(Date.from(Instant.now().plusSeconds(60 * EXPIRATION_TIME_IN_MINUTES)))
                .notBeforeTime(Date.from(Instant.now()))
                .issueTime(Date.from(Instant.now()))
                .jwtID(UUID.randomUUID().toString())
                .subject(UUID.randomUUID().toString());

        claims.forEach(builder::claim);
        JWTClaimsSet claimsSet = builder.build();


        JWSSigner signer = new RSASSASigner(getPrivateKey(LOCATION_KEYS));
        SignedJWT signedJWT = new SignedJWT(jweHeader, claimsSet);
        signedJWT.sign(signer);

        final NimbusJwtDecoder build = NimbusJwtDecoder.withPublicKey((RSAPublicKey) getPublicKey(LOCATION_KEYS)).build();
        final Jwt decode = build.decode(signedJWT.serialize());
        if (decode == null) {
            throw new RuntimeException("Jwt key not valid");
        }
        return signedJWT.serialize();
    }

    @SneakyThrows
    private static PrivateKey getPrivateKey(String location) {
        byte[] keyBytes = Files.readAllBytes(Paths.get(location + "private.key"));
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);

    }

    @SneakyThrows
    private static PublicKey getPublicKey(String location) {
        String n = Files.readString(Paths.get(location + "n.key"), Charset.defaultCharset());
        String e = Files.readString(Paths.get(location + "e.key"), Charset.defaultCharset());

        BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(n));
        BigInteger publicExponent = new BigInteger(1, Base64.getUrlDecoder().decode(e));
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(new RSAPublicKeySpec(modulus, publicExponent));
    }

}
