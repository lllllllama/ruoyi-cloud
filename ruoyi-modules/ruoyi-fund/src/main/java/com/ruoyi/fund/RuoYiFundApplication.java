package com.ruoyi.fund;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.ruoyi.common.security.annotation.EnableCustomConfig;
import com.ruoyi.common.security.annotation.EnableRyFeignClients;
import com.ruoyi.common.swagger.annotation.EnableCustomSwagger2;

@EnableCustomConfig
@EnableCustomSwagger2
@EnableRyFeignClients
@SpringBootApplication
public class RuoYiFundApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(RuoYiFundApplication.class, args);
        System.out.println("资金管理模块启动成功: ruoyi-fund");
    }
}
