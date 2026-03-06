package com.aton.proj.oneGasMeteor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OneGasMeteorApplication {

	public static void main(String[] args) {
		SpringApplication.run(OneGasMeteorApplication.class, args);
	}

}
