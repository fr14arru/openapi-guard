package com.openapiguard.adapter.cli;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import com.openapiguard.application.CompareContracts;
import com.openapiguard.adapter.openapi.SwaggerContractAnalyzer;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/** Ensamblado exterior de una CLI no web; el motor no necesita este arranque. */
@SpringBootApplication
public class OpenApiGuardApplication {
    /** Configuración exterior instanciada por Spring para ensamblar la CLI. */
    public OpenApiGuardApplication() {}

    /** Ejecuta una comparación, cierra el contexto y conserva el código de salida de la CLI.
     * @param args argumentos recibidos por el proceso
     */
    public static void main(String[] args) {
        int exitCode;
        try (ConfigurableApplicationContext context = start(args)) {
            PrintStream output = new PrintStream(System.out, true, StandardCharsets.UTF_8);
            PrintStream errors = new PrintStream(System.err, true, StandardCharsets.UTF_8);
            exitCode = context.getBean(ComparisonCommand.class).execute(args, output, errors);
        }
        System.exit(exitCode);
    }

    /** Mantiene el perfil predeterminado histórico de parámetros. */
    @Bean
    CompareContracts comparison() {
        return new CompareContracts(new SwaggerContractAnalyzer());
    }

    /** Conecta el caso de uso Java puro con el adaptador de terminal. */
    @Bean
    ComparisonCommand command(CompareContracts comparison) {
        return new ComparisonCommand(comparison);
    }

    /** Arranca sin servidor ni banner; el llamante debe cerrar el contexto devuelto. */
    static ConfigurableApplicationContext start(String... args) {
        SpringApplication application = new SpringApplication(OpenApiGuardApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setBannerMode(Banner.Mode.OFF);
        return application.run(args);
    }
}
