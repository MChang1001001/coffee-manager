package com.example.coffeebean.config;

import org.mybatis.spring.annotation.MapperScan;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = "com.example.coffeebean", annotationClass = Mapper.class)
public class MyBatisPlusConfig {
}
