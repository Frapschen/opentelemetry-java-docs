package io.opentelemetry.example.prometheus;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

import java.util.concurrent.ThreadLocalRandom;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

/**
 * Example of using the PrometheusHttpServer to convert OTel metrics to Prometheus format and expose
 * these to a Prometheus instance via a HttpServer exporter.
 *
 * <p>A Gauge is used to periodically measure how many incoming messages are awaiting processing.
 * The Gauge callback gets executed every collection interval.
 */
public final class PrometheusExample {
  private long incomingMessageCount;

  private LongHistogram latencyHistogram;

  private LongCounter  longCounter;

  private static final OpenTelemetry openTelemetry = ExampleConfiguration.initOpenTelemetry();
  private static final Tracer tracer =
          openTelemetry.getTracer("io.opentelemetry.example.http.HttpServer");

  public PrometheusExample(MeterProvider meterProvider) {
    Meter meter = meterProvider.get("PrometheusExample");
//    meter
//        .gaugeBuilder("incoming.messages")
//        .setDescription("No of incoming messages awaiting processing")
//        .setUnit("message")
//        .buildWithCallback(result -> result.record(incomingMessageCount, Attributes.empty()));

    this.latencyHistogram =  meter.histogramBuilder("request.latency")
            .setDescription("request latency")
            .setUnit("ms")
            .ofLongs()
            .build();

    this.longCounter = meter.counterBuilder("request.count")
            .setDescription("request count")
            .build();
  }

  void simulate() {
    for (int i = 500; i > 0; i--) {
      try {
        System.out.println(
            i + " Iterations to go, current incomingMessageCount is:  " + incomingMessageCount);
        incomingMessageCount = ThreadLocalRandom.current().nextLong(100);
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        // ignored here
      }
    }
  }

  // work for Exemplars demo
  void simulate2() {
    for (int i = 500; i > 0; i--) {
      try {
        Context context = Context.current();
        Span span =
                tracer.spanBuilder("GET /").setSpanKind(SpanKind.SERVER).startSpan();
        context = span.storeInContext(context);
        long la = ThreadLocalRandom.current().nextLong(10000);
        Attributes eventAttributes = Attributes.of(stringKey("feature"), "Exemplar");
        if(la>8000){
          System.out.println(
                  i + " Iterations to go, current latency is:  " + la+" with trace id "+span.getSpanContext().getTraceId()+" span id "+span.getSpanContext().getSpanId());
          latencyHistogram.record(la,eventAttributes,context);
        }else {
//          System.out.println(
//                  i + " Iterations to go, current latency is:  " + la);
          latencyHistogram.record(la,eventAttributes);

        }
//        if(i<5){
//          longCounter.add(1,eventAttributes);
//        }else {
//          System.out.println(i + " Iterations to go with trace id "+span.getSpanContext().getTraceId()+" span id "+span.getSpanContext().getSpanId());
//          longCounter.add(1,eventAttributes, context);
//        }
        Thread.sleep(1000);
        span.end();
      } catch (InterruptedException e) {
        // ignored here
      }
    }
  }

  /*
  command:
  docker run -it --rm\
    -p 9090:9090 \
    -v /Users/fraps/Desktop/DC-dev/prometheus:/etc/prometheus \
    -e ENABLE-FEATURE=remote-write-receiver \
    prom/prometheus:v2.35.0 \
    --config.file=/etc/prometheus/prometheus-test.yml \
    --storage.tsdb.path=/prometheus \
    --web.console.libraries=/usr/share/prometheus/console_libraries \
    --web.console.templates=/usr/share/prometheus/consoles \
    --enable-feature=remote-write-receiver \
    --enable-feature=exemplar-storage
  */

  /*
    file: /Users/fraps/Desktop/DC-dev/prometheus/prometheus-test.yml
    global:
      scrape_interval:     15s
      external_labels:
        monitor: 'codelab-monitor'
    scrape_configs:
      - job_name: java_metrics
        scrape_interval: 5s
        static_configs:
          - targets:
              - '10.70.4.138:9999'
  */
  public static void main(String[] args) {
    int prometheusPort = 0;
    try {
      prometheusPort = Integer.parseInt(args[0]);
    } catch (Exception e) {
      System.out.println("Port not set, or is invalid. Exiting");
      System.exit(1);
    }

    // it is important to initialize the OpenTelemetry SDK as early as possible in your process.
    MeterProvider meterProvider = ExampleConfiguration.initializeOpenTelemetry(prometheusPort);

    PrometheusExample prometheusExample = new PrometheusExample(meterProvider);

//    prometheusExample.simulate();
    prometheusExample.simulate2();

    System.out.println("Exiting");
  }
}
