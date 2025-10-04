package com.igorsudijovski.integrationtests.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MultiIssuerJwtDecoder implements JwtDecoder {

    private final IssuersProperties issuersProperties;

    @Override
    public Jwt decode(String token) throws JwtException {
        if (issuersProperties.getIssuers() != null && !issuersProperties.getIssuers().isEmpty()) {
            return handleIssuers(issuersProperties.getIssuers(), token);
        }
        if (issuersProperties.getJwkSetUris() != null && !issuersProperties.getJwkSetUris().isEmpty()) {
            return handleJwkSet(issuersProperties.getJwkSetUris(), token);
        }
        return null;
    }

    private Jwt handleIssuers(List<String> issuers, String token) {
        String issuer = extractIssuer(token);
        if (issuers.contains(issuer)) {
            NimbusJwtDecoder nimbusJwtDecoder = NimbusJwtDecoder.withIssuerLocation(issuer).build();
            nimbusJwtDecoder.setClaimSetConverter(MappedJwtClaimSetConverter.withDefaults(defaultAddedClaims()));
            return nimbusJwtDecoder.decode(token);
        }
        return null;
    }

    private Jwt handleJwkSet(List<String> jwkSets, String token) {
        String issuer = extractIssuer(token);
        String jwkSet = jwkSets.stream().filter(jwk -> jwk.startsWith(issuer)).findFirst().orElse(null);
        if (jwkSet != null) {
            NimbusJwtDecoder nimbusJwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSet).build();
            nimbusJwtDecoder.setClaimSetConverter(MappedJwtClaimSetConverter.withDefaults(defaultAddedClaims()));
            return nimbusJwtDecoder.decode(token);
        }
        return null;
    }


    private Map<String, Converter<Object, ?>> defaultAddedClaims() {
        return Map.of("tenant", source -> "tenant");
    }

    @SneakyThrows
    public String extractIssuer(String token) {
        String[] claims = token.split("\\.");
        if (claims.length != 3) {
            throw new RuntimeException();
        }
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(decodeBase64(claims[1]));
        return jsonNode.get("iss").asText();
    }

    public String decodeBase64(String str) {
        byte[] decoded = Base64.getDecoder().decode(str);
        return new String(decoded, StandardCharsets.UTF_8);
    }

}
