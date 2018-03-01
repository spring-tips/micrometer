package micrometer.boot

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Flux

/**
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
@SpringBootApplication
class BootApplication {

	@Bean
	fun routes(mr:MeterRegistry) = router {

		GET("/counter") {
			mr.counter("/GET-counter").increment()
			ServerResponse.ok().body(Flux.just("hello"), String::class.java)
		}
	}
}

fun main(args: Array<String>) {
	SpringApplication.run(BootApplication::class.java, *args)
}