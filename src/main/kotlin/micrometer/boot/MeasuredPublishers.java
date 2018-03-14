package micrometer.boot;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

public class MeasuredPublishers {

	private static Log log = LogFactory.getLog(MeasuredPublishers.class);

	public static <T> Publisher<T> from(
			MeterRegistry mr,
			String measureKey,
			Publisher<T> publisher) {
		
		return delegateSubscriber -> publisher.subscribe(new Subscriber<T>() {

			private final AtomicReference<Timer.Sample> sampleAtomicReference = new AtomicReference<>();

			@Override
			public void onNext(T t) {
				delegateSubscriber.onNext(t);
			}

			@Override
			public void onError(Throwable t) {
				delegateSubscriber.onError(t);
			}

			@Override
			public void onSubscribe(Subscription s) {
				delegateSubscriber.onSubscribe(s);
				log.info("onSubscribe()");
				sampleAtomicReference.set(Timer.start(mr));
			}

			@Override
			public void onComplete() {
				delegateSubscriber.onComplete();
				long stopNs = sampleAtomicReference.get().stop(mr.timer(measureKey));
				log.info("onComplete(): " + Duration.ofNanos(stopNs).toMillis() + "ms");
			}
		});
	}

}
