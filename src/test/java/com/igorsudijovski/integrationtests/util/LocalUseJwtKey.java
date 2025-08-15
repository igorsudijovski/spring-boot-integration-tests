package com.igorsudijovski.integrationtests.util;

public class LocalUseJwtKey {

    public static void main(String[] args) {
        System.out.println(JwtGenerator.generateJwt("http://localhost:8089"));
    }

}
