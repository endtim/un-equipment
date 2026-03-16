package com.unequipment.platform.modules.system.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 50, message = "用户名长度必须在4到50个字符之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度必须在6到50个字符之间")
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    @Size(min = 2, max = 50, message = "真实姓名长度必须在2到50个字符之间")
    private String realName;
}
