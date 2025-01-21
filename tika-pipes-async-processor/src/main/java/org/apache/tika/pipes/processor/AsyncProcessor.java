package org.apache.tika.pipes.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;

@Slf4j
public class AsyncProcessor implements CommandLineRunner {
    public static void main(String[] args) {
        SpringApplication.run(AsyncProcessor.class, args);
    }

    @Override
    public void run(String... args) {
        // Your processing logic here
        log.info("Running AsyncProcessor...");

        // Fetcher ID, Emitter ID, and Pipe Iterator ID can be passed as arguments
        if (args.length < 3) {
            log.error("Please provide fetcherId, emitterId, and pipeIteratorId as arguments.");
            return;
        }

        String fetcherId = args[0];
        String emitterId = args[1];
        String pipeIteratorId = args[2];

        log.info("Fetcher ID: " + fetcherId);
        log.info("Emitter ID: " + emitterId);
        log.info("Pipe Iterator ID: " + pipeIteratorId);
    }
}
