package eci.dosw.alpha.BienestarService.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bienestarServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BienestarService API")
                        .description("Centro de Bienestar Universitario: recursos, eventos de bienestar y contactos de emergencia.")
                        .version("1.0.0"));
    }
}
