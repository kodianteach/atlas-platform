package co.com.atlas.jwt.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    
    private String secret;
    private long accessTokenExpiration = 3600000; // 1 hora en milisegundos
    private long refreshTokenExpiration = 86400000; // 24 horas en milisegundos
    private String issuer = "atlas-platform";
}
