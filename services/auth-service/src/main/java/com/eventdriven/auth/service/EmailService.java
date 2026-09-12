package com.eventdriven.auth.service;


public interface EmailService {

    void sendVerificationEmail(String email,String token);

}
