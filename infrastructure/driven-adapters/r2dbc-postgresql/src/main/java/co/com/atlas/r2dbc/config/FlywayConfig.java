package co.com.atlas.r2dbc.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuración de Flyway para ejecutar migraciones de base de datos.
 * 
 * Flyway no soporta R2DBC directamente, por lo que se configura con JDBC
 * para ejecutar las migraciones antes de que la aplicación inicie.
 * 
 * Las migraciones deben ubicarse en: src/main/resources/db/migration
 * Convención de nombres: V{version}__{description}.sql
 * Ejemplo: V1__create_initial_tables.sql
 * 
 * Se activa/desactiva con spring.flyway.enabled (default: true).
 */
@Configuration
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true", matchIfMissing = false)
public class FlywayConfig {

    @Value("${spring.flyway.url}")
    private String url;

    @Value("${spring.flyway.user}")
    private String user;

    @Value("${spring.flyway.password}")
    private String password;

    @Value("${spring.flyway.locations:classpath:db/migration}")
    private List<String> locations;

    @Value("${spring.flyway.baseline-on-migrate:true}")
    private boolean baselineOnMigrate;

    /**
     * Configura y ejecuta Flyway para las migraciones de base de datos.
     * Se ejecuta automáticamente al iniciar la aplicación.
     *
     * @return instancia de Flyway configurada
     */
    @Bean(initMethod = "migrate")
    public Flyway flyway() {
        return Flyway.configure()
                .dataSource(url, user, password)
                .locations(locations.toArray(new String[0]))
                .baselineOnMigrate(baselineOnMigrate)
                .load();
    }
}
