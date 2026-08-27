package com.ruoyi.research;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.ruoyi.common.security.annotation.EnableCustomConfig;
import com.ruoyi.common.security.annotation.EnableRyFeignClients;
import com.ruoyi.common.swagger.annotation.EnableCustomSwagger2;

@EnableCustomConfig
@EnableCustomSwagger2
@EnableRyFeignClients
@SpringBootApplication
public class RuoYiResearchApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(RuoYiResearchApplication.class, args);
        System.out.println("Research management service started: ruoyi-research");
    }
}
