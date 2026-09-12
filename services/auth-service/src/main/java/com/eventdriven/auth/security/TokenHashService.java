package com.eventdriven.auth.security;

import org.springframework.stereotype.Service;

import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class TokenHashService {

    public String hash(String token){

        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        }catch (NoSuchAlgorithmException ex){
            throw new IllegalStateException(
                    "SHA-256 algorithm not available",
                    ex
            );
        }
    }

}
