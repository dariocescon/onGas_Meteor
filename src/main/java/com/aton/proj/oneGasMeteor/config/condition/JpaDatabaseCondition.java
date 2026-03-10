package com.aton.proj.oneGasMeteor.config.condition;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition che matcha quando database.type è un database JPA-based (sqlserver
 * o timescaledb). Matcha anche se la property è assente (default = sqlserver).
 */
public class JpaDatabaseCondition implements Condition {

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		String dbType = context.getEnvironment().getProperty("database.type", "sqlserver");
		return "sqlserver".equalsIgnoreCase(dbType) || "postgresql".equalsIgnoreCase(dbType)
				|| "h2mem".equalsIgnoreCase(dbType) || "timescaledb".equalsIgnoreCase(dbType);
	}
}
