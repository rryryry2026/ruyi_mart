package com.ruyi.ruyi_mart;

import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(RocketMQAutoConfiguration.class)
public class RuyiMartApplication {

    public static void main(String[] args) {
        SpringApplication.run(RuyiMartApplication.class, args);
    }

}
