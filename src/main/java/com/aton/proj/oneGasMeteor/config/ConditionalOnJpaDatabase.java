package com.aton.proj.oneGasMeteor.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * Meta-annotation che attiva un bean solo quando database.type
 * è un database JPA-based (sqlserver o timescaledb).
 * <p>
 * Sostituisce {@code @ConditionalOnProperty(name = "database.type",
 * havingValue = "sqlserver", matchIfMissing = true)} per supportare
 * entrambi i database relazionali.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(JpaDatabaseCondition.class)
public @interface ConditionalOnJpaDatabase {
}
