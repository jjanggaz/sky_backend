package com.wai.admin.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(servers());
    }

    private Info apiInfo() {
        return new Info()
                .title("WAI Admin Backend API")
                .description("WAI 관리자 백엔드 API 문서\n" +
                "1. JWT 토큰 발급 및 검증이 있으므로 Main API의 로그인 처리 후 사용\n" +
                "2. 로그인 예시\n" +
                "{\"username\": \"아이디\",\"password\": \"비번\"}\n")
                .version("1.0.0")
                .contact(new Contact()
                        .name("WAI Development Team")
                        .email("dev@wai.com")
                        .url("https://wai.com"))
                .license(new License()
                        .name("Apache 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0"));
    }

    private List<Server> servers() {
        return List.of(
                new Server()
                        .url("http://localhost:8080")
                        .description("로컬 개발 서버")
                // ,new Server()
                //         .url("https://api.wai.com")
                //         .description("프로덕션 서버")
        );
    }
}
