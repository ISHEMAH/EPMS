package rw.gov.epms.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Bean
    public OpenAPI epmsOpenAPI() {
        Server localServer = new Server()
                .url("http://localhost:8080" + contextPath)
                .description("Local Development Server");

        Contact contact = new Contact()
                .name("Rwanda Government")
                .email("support@gov.rw")
                .url("https://www.gov.rw");

        License license = new License()
                .name("Government License")
                .url("https://www.gov.rw/license");

        Info info = new Info()
                .title("Enterprise Payroll Management System API")
                .description("API documentation for the Enterprise Payroll Management System (EPMS) for the Government of Rwanda")
                .version("1.0.0")
                .contact(contact)
                .license(license);

        // Define the security scheme
        Components components = new Components()
                .addSecuritySchemes("bearerAuth", 
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter JWT token with Bearer prefix, e.g. 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...'"));

        // Add global security requirement
        SecurityRequirement securityRequirement = new SecurityRequirement().addList("bearerAuth");

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer))
                .components(components)
                .addSecurityItem(securityRequirement);
    }
}
