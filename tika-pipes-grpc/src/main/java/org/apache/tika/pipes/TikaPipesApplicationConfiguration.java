package org.apache.tika.pipes;

import javax.annotation.PostConstruct;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {"org.apache.tika.pipes"})
@Slf4j
public class TikaPipesApplicationConfiguration {
	@PostConstruct
	public void init() {
		log.info("Initializing Tika Pipes GRPC Services...");
	}
	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}
}
