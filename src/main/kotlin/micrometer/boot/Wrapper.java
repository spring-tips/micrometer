package micrometer.boot;

import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
public class Wrapper {

	public static void main(String args[]) throws InterruptedException {

		Flux<String> xx = Flux.just("A", "B", "C", "D").delayElements(Duration.ofSeconds(2));
		SimpleMeterRegistry simpleMeterRegistry = new SimpleMeterRegistry();
		Publisher<String> measuredX = measure(simpleMeterRegistry, "test", xx);
		Flux.from(measuredX).subscribe(System.out::println);
		Thread.sleep(Duration.ofSeconds(10).toMillis());
	}

	private static <T> Publisher<T> wrap(
			Publisher<T> p,
			Runnable start,
			Runnable stop) {
		Flux<T> delegate = Flux.from(p);
		return delegateSubscriber -> {
			Subscriber<T> subscriber = new Subscriber<T>() {

				@Override
				public void onSubscribe(Subscription s) {
					delegateSubscriber.onSubscribe(s);
					start.run();
				}

				@Override
				public void onNext(T t) {
					delegateSubscriber.onNext(t);
				}

				@Override
				public void onError(Throwable t) {
					delegateSubscriber.onError(t);
					stop.run();
				}

				@Override
				public void onComplete() {
					delegateSubscriber.onComplete();
					stop.run();
				}
			};
			delegate.subscribe(subscriber);
		};
	}

	private static <T> Publisher<T> measure(
			MeterRegistry mr,
			String measureKey,
			Publisher<T> publisher) {
		AtomicReference<LongTaskTimer.Sample> sampleAtomicReference = new AtomicReference<>();
		LongTaskTimer longTaskTimer = LongTaskTimer
				.builder(measureKey)
				.register(mr);
		Log log = LogFactory.getLog(Wrapper.class.getName());
		return wrap(publisher,
				() -> {
					sampleAtomicReference.set(longTaskTimer.start());
					log.info("starting..");
				},
				() -> {
					long time = sampleAtomicReference.get().stop();
					Duration ofNanos = Duration.ofNanos(time);
					log.info("time=" + ofNanos.toMillis());
				});
	}
}
