package org.apache.tika.pipes.parser;

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
		log.info("""
        
        ___________  __     __   ___       __             _______   __       _______    _______   ________ \s
        ("     _   ")|" \\   |/"| /  ")     /""\\           |   __ "\\ |" \\     |   __ "\\  /"     "| /"       )\s
        )__/  \\\\__/ ||  |  (: |/   /     /    \\          (. |__) :)||  |    (. |__) :)(: ______)(:   \\___/ \s
            \\\\_ /    |:  |  |    __/     /' /\\  \\         |:  ____/ |:  |    |:  ____/  \\/    |   \\___  \\   \s
            |.  |    |.  |  (// _  \\    //  __'  \\        (|  /     |.  |    (|  /      // ___)_   __/  \\\\  \s
            \\:  |    /\\  |\\ |: | \\  \\  /   /  \\\\  \\      /|__/ \\    /\\  |\\  /|__/ \\    (:      "| /" \\   :) \s
             \\__|   (__\\_|_)(__|  \\__)(___/    \\___)    (_______)  (__\\_|_)(_______)    \\_______)(_______/  \s
        """);
	}
	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}
}
