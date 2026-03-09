package com.aton.proj.oneGasMeteor.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition che matcha quando database.type = influxdb.
 */
public class InfluxDatabaseCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String dbType = context.getEnvironment().getProperty("database.type", "sqlserver");
        return "influxdb".equalsIgnoreCase(dbType);
    }
}
