package micrometer.boot

import io.micrometer.core.instrument.MeterRegistry
import org.reactivestreams.Publisher
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


	class MyIterator<T>(private val it: Iterator<T>,
	                    private val start: () -> Unit,
	                    private val stop: () -> Unit) : Iterator<T> {

		private var started = false
		private var finished = false

		override fun next(): T = it.next()

		override fun hasNext(): Boolean {
			val hasNext = this.it.hasNext()
			if (hasNext) {
				val startEvent = !started
				started = true
				if (startEvent) {
					start()
				}
			}
			if (!hasNext) {
				finished = true
				if (finished) {
					stop()
				}
			}
			return hasNext
		}
	}



	@Bean
	fun routes(mr: MeterRegistry) = router {

		GET("/counter") {
			mr.counter("hello").increment()
			val iterable = Flux.just("a")

			ServerResponse.ok().body(Flux.just("hello"), String::class.java)

		}
	}
}

fun main(args: Array<String>) {
	SpringApplication.run(BootApplication::class.java, *args)
}