package com.unequipment.platform.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.unequipment.platform.modules")
public class MybatisConfig {
}
