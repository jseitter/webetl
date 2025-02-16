package io.webetl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
public class Application {
    @Value("${data.dir:#{systemProperties['user.home']+'/.webetl'}}")
    private String dataDir;

    @Bean
    public Path dataDirectory() {
        return Paths.get(dataDir);
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
} 