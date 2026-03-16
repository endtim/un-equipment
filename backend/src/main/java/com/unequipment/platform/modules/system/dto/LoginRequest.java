package com.unequipment.platform.modules.system.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过50个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(max = 50, message = "密码长度不能超过50个字符")
    private String password;
}
