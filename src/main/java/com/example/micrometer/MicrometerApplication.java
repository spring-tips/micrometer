package com.example.micrometer;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

@SpringBootApplication
public class MicrometerApplication {

	private final AtomicLong number = new AtomicLong(0);

	/*
	@Bean
	MeterFilter filters() {
		return MeterFilter
				.accept(id -> id.getTags().stream().anyMatch(t -> t.getKey().equalsIgnoreCase("method")));
	}*/

	@Bean
	MeterBinder customBinder() {
		return registry -> registry.gauge("pull-number", number, (ToDoubleFunction<AtomicLong>) atomicLong -> {
			long pull = atomicLong.get();
			System.out.println("pull-number: " + pull);
			return pull;
		});
	}

	@Bean
	ApplicationRunner runner(MeterRegistry mr) {
		return args ->
				Executors.newSingleThreadScheduledExecutor()
						.scheduleWithFixedDelay(() -> {
							number.set((long) (Math.random() * 1000L));
							mr.gauge("push-number", this.number.get());
							System.out.println("push-number: " + this.number.get());
						}, 1, 1, TimeUnit.SECONDS);
	}

	public static void main(String[] args) {
		SpringApplication.run(MicrometerApplication.class, args);
	}
}

@Timed(percentiles = {.95})
@RestController
class GreetingsRestController {

	private final MeterRegistry meterRegistry;

	GreetingsRestController(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	@GetMapping("/slow")
	ResponseEntity<?> ok() throws Exception {

		this.meterRegistry.timer("slow-request").record(() -> {
			try {
				Thread.sleep((long) (Math.random() * 10 * 1000));
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		});

		return ResponseEntity.ok().build();
	}

	@Timed(histogram = true)
	@PostMapping("/post")
	void post() {
	}

	@GetMapping("/hi/{name}")
	String hi(@PathVariable String name) {
		return "hello, " + name + "!";
	}
}

