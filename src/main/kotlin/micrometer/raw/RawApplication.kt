package micrometer.raw

import com.netflix.spectator.atlas.AtlasConfig
import com.sun.net.httpserver.HttpServer
import io.micrometer.atlas.AtlasMeterRegistry
import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.influx.InfluxConfig
import io.micrometer.influx.InfluxMeterRegistry
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.apache.commons.logging.LogFactory
import org.springframework.core.task.SimpleAsyncTaskExecutor
import java.io.IOException
import java.net.InetSocketAddress
import java.time.Duration
import java.util.*


// https://docs.spring.io/spring-boot/docs/current-SNAPSHOT/reference/htmlsingle/#production-ready-metrics-export-prometheus
// http://micrometer.io/docs
fun main(args: Array<String>) {

	val log = LogFactory.getLog("RawAtlasApplication")

	fun exercise(registry: MeterRegistry) {
		val r = Random()
		val te = SimpleAsyncTaskExecutor()
		fun iterate(count: Int = 20, x: () -> Unit) {
			te.submit {
				(0..count).forEach {
					val delay = Math.max(r.nextInt(5) * 1000L, 1000L)
					log.info("sleeping $delay")
					Thread.sleep(delay)
					x()
				}
			}
		}

		iterate {
			log.info("counter")
			registry.counter("orders.submitted").increment()
		}
		iterate {
			log.info("gauge")
			registry.gauge("orders.in-cart", Math.max(Math.max(1, r.nextInt(20)), r.nextInt(10)))
		}
		iterate {
			log.info("timer")
			registry.timer("orders.checkout").record {
				Thread.sleep(Math.max(5, r.nextInt(10)) * 1000L)
			}
		}
	}

	fun atlas() {
		// http://micrometer.io/docs/registry/atlas
		val atlasConfig: AtlasConfig = object : AtlasConfig {
			override fun step(): Duration = Duration.ofSeconds(1)
			override fun get(k: String): String? = null // accept the rest of the defaults
		}
		val registry = AtlasMeterRegistry(atlasConfig, Clock.SYSTEM)
		exercise(registry)
	}

	@Throws(IOException::class)
	fun prometheus() {
		// http://micrometer.io/docs/registry/prometheus
		val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
		val server = HttpServer.create(InetSocketAddress(8080), 0)
		server.createContext("/prometheus") { httpExchange ->
			log.info("new request on /prometheus")
			val response = registry.scrape()
			httpExchange.sendResponseHeaders(200, response.length.toLong())
			httpExchange.responseBody.use {
				it.write(response.toByteArray())
			}
		}
		Thread(server::start).start()
		exercise(registry)
	}

	fun influx() {
		// https://www.influxdata.com/time-series-platform/
		// https://github.com/influxdata/sandbox
		val config = object : InfluxConfig {
			override fun step(): Duration = Duration.ofSeconds(10)
			override fun db(): String = "micrometer-st"
			override fun get(k: String): String? = null // accept the rest of the defaults
		}
		val registry = InfluxMeterRegistry(config, Clock.SYSTEM)
		exercise(registry)
	}

	influx()
//	atlas()
//	prometheus()
}

