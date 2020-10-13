package com.atguigu.gmall.gateway.filter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsFilterConfig {

    @Bean
    public CorsWebFilter corsWebFilter(){

        CorsConfiguration corsConfiguration = new CorsConfiguration();
        // 允许跨域访问的域名，为了方便将来携带cookie，这里不用使用*号，*号代表允许所有域名跨域访问
        corsConfiguration.addAllowedOrigin("http://manager.gmall.com");
        corsConfiguration.addAllowedOrigin("http://www.gmall.com");
        corsConfiguration.addAllowedOrigin("http://gmall.com");
        corsConfiguration.addAllowedOrigin("http://index.gmall.com");
        // 允许携带cookie信息
        corsConfiguration.setAllowCredentials(true);
        // 允许所有请求方式跨域访问
        corsConfiguration.addAllowedMethod("*");
        // 允许携带所有头信息跨域访问
        corsConfiguration.addAllowedHeader("*");

        UrlBasedCorsConfigurationSource configurationSource = new UrlBasedCorsConfigurationSource();
        configurationSource.registerCorsConfiguration("/**", corsConfiguration);

        return new CorsWebFilter(configurationSource);
    }
}
