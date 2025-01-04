import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.Sampler;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class OpenTelemetryDemoApplication {

    public static void main(String[] args) {
        setupOpenTelemetry();
        SpringApplication.run(OpenTelemetryDemoApplication.class, args);
    }

    private static void setupOpenTelemetry() {
        OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint("http://localhost:4317")
                .build();
        BatchSpanProcessor spanProcessor = BatchSpanProcessor.builder(spanExporter).build();
        OpenTelemetrySdk.builder()
                .setTracerProvider(
                        io.opentelemetry.sdk.trace.TracerProvider.builder()
                                .setSampler(Sampler.alwaysOn())
                                .addSpanProcessor(spanProcessor)
                                .build())
                .buildAndRegisterGlobal();
    }

    @RestController
    public static class DemoController {

        private static final Tracer tracer = GlobalOpenTelemetry.getTracer("io.opentelemetry.demo");

        @GetMapping("/hello")
        public String hello() {
            Span span = tracer.spanBuilder("hello-span")
                    .setNoParent()  // No parent span is inherited
                    .startSpan();
            try (Scope scope = span.makeCurrent()) {
                String spanId = span.getSpanContext().getSpanId();
                System.out.println("New Span Created with Span ID: " + spanId);  // Print the span ID for each request
                Thread.sleep(1000);
                return "Hello, OpenTelemetry! Span ID: " + spanId;
            } catch (InterruptedException e) {
                return "Error in processing";
            } finally {
                span.end();
            }
        }
    }
}
