package com.mealplanner.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Name;

/**
 * Propriétés applicatives (préfixe {@code app.*} dans application.yml).
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Cors cors, Ciqual ciqual, @Name("import") Import importing) {

    public AppProperties {
        if (cors == null) {
            cors = new Cors(List.of("http://localhost:4200"));
        }
        if (ciqual == null) {
            ciqual = new Ciqual(null);
        }
        if (importing == null) {
            importing = new Import(0.45);
        }
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record Ciqual(String autoImportPath) {
    }

    /** Mappé sur {@code app.import.*} via {@link #importing}. */
    public record Import(double matchThreshold) {
    }
}
