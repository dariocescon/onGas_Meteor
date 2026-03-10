package com.aton.proj.oneGasMeteor.config.condition;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition che matcha quando è necessario il database SQL per i comandi.
 * Matcha per tutti i database.type supportati: sqlserver, timescaledb, influxdb.
 * In modalità influxdb, SQL è usato solo per device_commands.
 */
public class SqlCommandsCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String dbType = context.getEnvironment().getProperty("database.type", "sqlserver");
        return "sqlserver".equalsIgnoreCase(dbType)
                || "timescaledb".equalsIgnoreCase(dbType)
                || "influxdb".equalsIgnoreCase(dbType);
    }
}
