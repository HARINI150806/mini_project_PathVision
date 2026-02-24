package com.pathvision;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PathVisionApplication {

	public static void main(String[] args) {
		// Load .env file
		try {
			io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure()
				.directory("backend")
				.ignoreIfMalformed()
				.ignoreIfMissing()
				.load();
			dotenv.entries().forEach(entry -> {
				if (System.getenv(entry.getKey()) == null && System.getProperty(entry.getKey()) == null) {
					System.setProperty(entry.getKey(), entry.getValue());
				}
			});
		} catch (Exception e) {
			System.err.println("Warning: Could not load .env file: " + e.getMessage());
		}
		SpringApplication.run(PathVisionApplication.class, args);
	}

}
