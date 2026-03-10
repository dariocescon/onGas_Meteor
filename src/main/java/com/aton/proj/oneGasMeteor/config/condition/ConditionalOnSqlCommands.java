package com.aton.proj.oneGasMeteor.config.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * Meta-annotation che attiva un bean quando il database SQL per i comandi
 * è necessario: database.type = sqlserver, timescaledb, o influxdb.
 * <p>
 * In modalità influxdb, i dati time-series vanno su InfluxDB ma i comandi
 * (device_commands) restano su SQL.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(SqlCommandsCondition.class)
public @interface ConditionalOnSqlCommands {
}
