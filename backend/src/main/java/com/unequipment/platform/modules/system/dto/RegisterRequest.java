package com.unequipment.platform.modules.system.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "username is required")
    @Size(min = 4, max = 50, message = "username length must be between 4 and 50")
    private String username;

    @NotBlank(message = "password is required")
    @Size(min = 6, max = 50, message = "password length must be between 6 and 50")
    private String password;

    @NotBlank(message = "realName is required")
    @Size(min = 2, max = 50, message = "realName length must be between 2 and 50")
    private String realName;
}
