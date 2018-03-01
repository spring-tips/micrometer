package com.example.bootiful2

import com.netflix.spectator.atlas.AtlasConfig
import com.sun.net.httpserver.HttpServer
import io.micrometer.atlas.AtlasMeterRegistry
import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.apache.commons.logging.LogFactory
import java.io.IOException
import java.net.InetSocketAddress
import java.time.Duration


// https://docs.spring.io/spring-boot/docs/current-SNAPSHOT/reference/htmlsingle/#production-ready-metrics-export-prometheus
// http://micrometer.io/docs
fun main(args: Array<String>) {

	val log = LogFactory.getLog("RawAtlasApplication")

	fun exercise(registry: MeterRegistry) {
		for (i in 1..10) {
			registry.counter("orders.submitted").increment()
		}
	}

	fun atlas() {
		// http://micrometer.io/docs/registry/atlas
		val atlasConfig: AtlasConfig = object : AtlasConfig {
			override fun step(): Duration = Duration.ofSeconds(10)
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

	prometheus()
}

