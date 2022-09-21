package io.opentelemetry.example.prometheus;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.*;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.internal.SdkMeterProviderUtil;
import io.opentelemetry.sdk.metrics.internal.exemplar.ExemplarFilter;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

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
