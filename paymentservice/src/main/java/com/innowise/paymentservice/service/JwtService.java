package com.innowise.paymentservice.service;

public interface JwtService {

    /**
     * Returns the id of authenticated user;
     * @param token JWT token;
     * @return returns user id.
     */
    Long getUserId(String token);

    /**
     * Returns the role of authenticated user;
     * @param token JWT token;
     * @return returns user role.
     */
    String getRole(String token);
}
