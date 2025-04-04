// UserLoginResult.java
package com.example.auth.dto;

import com.example.auth.domain.User;

public record UserLoginResult(User user, boolean isNew) {}
