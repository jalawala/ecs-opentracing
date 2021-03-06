package com.mikelduke.vhs.catalog;

import io.jaegertracing.LogData;
import io.jaegertracing.metrics.InMemoryMetricsFactory;
import io.jaegertracing.metrics.Metrics;
import io.jaegertracing.reporters.InMemoryReporter;
import io.jaegertracing.reporters.RemoteReporter;
import io.jaegertracing.reporters.Reporter;
import io.jaegertracing.samplers.ConstSampler;
import io.jaegertracing.samplers.Sampler;
import io.jaegertracing.senders.Sender;
import io.jaegertracing.senders.UdpSender;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

public class TracingConfiguration {

    private static boolean enableConsoleReporter = Boolean.parseBoolean(EnvUtil.getEnv("jaeger.system.out", "false"));

    private static String jaegerHost = EnvUtil.getEnv("jaeger.host", "localhost");
    private static int jaegerPort = Integer.parseInt(EnvUtil.getEnv("jaeger.port", "6831"));
    
    public static void configureGlobalTracer() {

        Tracer tracer;

        if (enableConsoleReporter) {
            tracer = getConsoleTracer();
        } else {
            tracer = getTracer();
        }

        GlobalTracer.register(tracer);
    }
    
    private static Tracer getConsoleTracer() {
		Reporter reporter = new InMemoryReporter() {
            @Override
            public void report(io.jaegertracing.Span span) {
                super.report(span);
                String logs = "";
                for (LogData l : span.getLogs()) {
                    logs += "\n\t" + l.getTime() + ": " + l.getMessage();
                };

                System.out.println("Span Reported: " + span 
                        + " Tags: " + span.getTags() + " \nLogs: " + logs);
            }
        };
        
        Sampler sampler = new ConstSampler(true);
        io.opentracing.Tracer tracer = new io.jaegertracing.Tracer.Builder("vhs-catalog")
                .withReporter(reporter)
                .withSampler(sampler)
                .build();

        return tracer;
    }

    private static Tracer getTracer() {
        Sender sender = new UdpSender(jaegerHost, jaegerPort, 0);

        Reporter reporter = new RemoteReporter.Builder()
                .withSender(sender)
                .withMetrics(new Metrics(new InMemoryMetricsFactory()))
                .build();

        Sampler sampler = new ConstSampler(true);
        io.opentracing.Tracer tracer = new io.jaegertracing.Tracer.Builder("vhs-catalog")
                .withReporter(reporter)
                .withSampler(sampler)
                .build();

        return tracer;
    }
}