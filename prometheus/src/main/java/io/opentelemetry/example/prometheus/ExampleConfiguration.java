/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.example.prometheus;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.prometheus.PrometheusHttpServer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.internal.SdkMeterProviderUtil;
import io.opentelemetry.sdk.metrics.internal.exemplar.ExemplarFilter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;

public final class ExampleConfiguration {

  /**
   * Initializes the Meter SDK and configures the prometheus collector with all default settings.
   *
   * @param prometheusPort the port to open up for scraping.
   * @return A MeterProvider for use in instrumentation.
   */
  static MeterProvider initializeOpenTelemetry(int prometheusPort) {
    MetricReader prometheusReader = PrometheusHttpServer.builder().setPort(prometheusPort).build();

    SdkMeterProviderBuilder builder = SdkMeterProvider.builder();
    SdkMeterProviderUtil.setExemplarFilter(builder, ExemplarFilter.sampleWithTraces());

    return builder.registerMetricReader(prometheusReader).build();
  }
  static OpenTelemetry initOpenTelemetry() {
    SdkTracerProvider sdkTracerProvider =
            SdkTracerProvider.builder()
                    .addSpanProcessor(SimpleSpanProcessor.create(new LoggingSpanExporter()))
                    .build();

    OpenTelemetrySdk sdk =
            OpenTelemetrySdk.builder()
                    .setTracerProvider(sdkTracerProvider)
                    .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                    .build();

    Runtime.getRuntime().addShutdownHook(new Thread(sdkTracerProvider::close));
    return sdk;
  }
}
