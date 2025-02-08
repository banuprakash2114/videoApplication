package com.VideoCall.application;

import com.VideoCall.application.User.User;
import com.VideoCall.application.User.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner(UserService service) {
		return args -> {
			// Use the manual builder for creating User instances
			service.register(new User.Builder()
					.username("banu")
					.email("banu@gmail.com")
					.password("banu@123")
					.status("online")
					.build());

			service.register(new User.Builder()
					.username("Goutham")
					.email("goutham@gmail.com")
					.password("goutham@123")
					.status("online")
					.build());

			service.register(new User.Builder()
					.username("Pavan")
					.email("pavan@gmail.com")
					.password("pavan@123")
					.status("online")
					.build());
		};
	}
}