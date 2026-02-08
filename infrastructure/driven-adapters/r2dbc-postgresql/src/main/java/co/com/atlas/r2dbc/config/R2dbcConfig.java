package co.com.atlas.r2dbc.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * Configuración principal para R2DBC y MySQL.
 * Habilita las propiedades de conexión y los repositorios reactivos.
 */
@Configuration
@EnableConfigurationProperties(MysqlConnectionProperties.class)
@EnableR2dbcRepositories(basePackages = "co.com.atlas.r2dbc")
public class R2dbcConfig {
}
