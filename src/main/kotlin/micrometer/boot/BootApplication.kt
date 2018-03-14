package micrometer.boot

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Flux
import java.time.Duration

/**
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
@SpringBootApplication
class BootApplication {


	@Bean
	fun routes(mr: MeterRegistry) = router {

		GET("/counter") {
			mr.counter("hello").increment()

			val delayedNames = Flux
					.just("A", "B", "C", "D")
					.delayElements(Duration.ofSeconds(2))

			val measured = MeasuredPublishers
					.from(mr, "hello-publisher", delayedNames)

			ServerResponse.ok().body(measured, String::class.java)
		}
	}
}

fun main(args: Array<String>) {
	SpringApplication.run(BootApplication::class.java, *args)
}