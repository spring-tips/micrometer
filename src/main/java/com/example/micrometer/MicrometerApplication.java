package com.example.micrometer;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class MicrometerApplication {

	public static void main(String[] args) {
		SpringApplication.run(MicrometerApplication.class, args);
	}

	private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

	/*

	@Bean
	MeterFilter meterFilter() {
		return MeterFilter.accept(); //.denyNameStartsWith("jvm");
	}
*/

	@Bean
	MeterRegistryCustomizer<MeterRegistry> custom(@Value("${my-app.region:us-west}") String region) {
		return new MeterRegistryCustomizer<MeterRegistry>() {
			@Override
			public void customize(MeterRegistry registry) {
				registry.config().commonTags("us-region", region);
			}
		};
	}

	@Bean
	ApplicationRunner run(MeterRegistry mr) {
		return args ->
				this.executorService.scheduleAtFixedRate(() -> {
					String region = Math.random() > .5 ? "us-east" : "us-west";

					long min = (long) Math.min(Math.random() * 60, 60);
					Duration ofSeconds = Duration.ofSeconds(min);
					Timer.builder("st-task")
							.tag("region", region)
							.publishPercentiles(.5, .95)
							.sla(Duration.ofMillis(1), Duration.ofSeconds(10))
							.register(mr)
							.record(ofSeconds);


				}, 500, 500, TimeUnit.MILLISECONDS);
	}
}

@Timed(extraTags = {"region", "us-east"})
@RestController
class GreetingsRestController {

	@GetMapping("/hi/{name}")
	String hi(@PathVariable String name) {
		return "Hello, " + name + "!";
	}
}