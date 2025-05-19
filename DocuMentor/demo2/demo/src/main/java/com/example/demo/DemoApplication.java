package com.example.demo;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

import java.io.File;

@SpringBootApplication
@EnableMongoAuditing
public class DemoApplication {

	private static final Logger logger = LoggerFactory.getLogger(DemoApplication.class);

	public static void main(String[] args) {
		logger.info("Starting the DemoApplication...");

		try {
			System.out.println("Current Directory: " + new File(".").getAbsolutePath());
			Dotenv dotenv = Dotenv.configure()
					.directory("demo2/demo") // Root directory
					.load();
			System.setProperty("MONGODB_CLUSTER", dotenv.get("MONGODB_CLUSTER"));
			System.setProperty("MONGODB_DB", dotenv.get("MONGODB_DB"));
			System.setProperty("GOOGLE_CLIENT_ID", dotenv.get("GOOGLE_CLIENT_ID"));


			System.setProperty("GOOGLE_CLIENT_SECRET", dotenv.get("GOOGLE_CLIENT_SECRET"));
			System.setProperty("EMBEDDING_DIMENSIONS", dotenv.get("EMBEDDING_DIMENSIONS"));
			System.setProperty("SPRING_SESSION_TIMEOUT", dotenv.get("SPRING_SESSION_TIMEOUT"));
			System.setProperty("COHERE_TOKEN", dotenv.get("COHERE_TOKEN"));
			System.setProperty("HUGGINGFACE_TOKEN", dotenv.get("HUGGINGFACE_TOKEN"));


			System.out.println(System.getProperty("COHERE_TOKEN"));
			SpringApplication.run(DemoApplication.class, args);


			logger.info("DemoApplication started successfully.");
		} catch (Exception e) {
			logger.error("Application failed to start: {}", e.getMessage(),e);
			throw e;
		}
	}
}
