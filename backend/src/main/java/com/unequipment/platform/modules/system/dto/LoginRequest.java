package com.unequipment.platform.modules.system.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "username is required")
    @Size(max = 50, message = "username length must be <= 50")
    private String username;

    @NotBlank(message = "password is required")
    @Size(max = 50, message = "password length must be <= 50")
    private String password;
}
