package com.aton.proj.oneGasMeteor.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * Meta-annotation che attiva un bean solo quando database.type = influxdb.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(InfluxDatabaseCondition.class)
public @interface ConditionalOnInfluxDatabase {
}
