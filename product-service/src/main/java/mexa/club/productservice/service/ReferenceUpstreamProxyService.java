package mexa.club.productservice.service;

import mexa.club.productservice.exception.UpstreamReferenceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Warehouse (reference) ilovasiga HTTP orqali yo‘naltirish — kategoriyalar va boshqa ma'lumotnomalar.
 */
@Service
public class ReferenceUpstreamProxyService {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    private final String baseUrl;

    public ReferenceUpstreamProxyService(@Value("${app.reference-data.url}") String baseUrl) {
        String trimmed = baseUrl != null ? baseUrl.trim() : "";
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        this.baseUrl = trimmed;
    }

    public ResponseEntity<byte[]> forward(
            String method,
            String path,
            String rawQuery,
            byte[] body,
            String contentType,
            String authorization
    ) {
        if (baseUrl.isEmpty()) {
            throw new UpstreamReferenceException("app.reference-data.url is not configured", null);
        }
        String p = path.startsWith("/") ? path : "/" + path;
        StringBuilder url = new StringBuilder(baseUrl).append(p);
        if (rawQuery != null && !rawQuery.isBlank()) {
            url.append("?").append(rawQuery);
        }

        try {
            HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create(url.toString()))
                    .timeout(Duration.ofSeconds(15));

            if (authorization != null && !authorization.isBlank()) {
                rb.header(HttpHeaders.AUTHORIZATION, authorization);
            }

            String m = method != null ? method.toUpperCase() : "GET";
            boolean hasBody = body != null && body.length > 0;
            if (hasBody) {
                String ct = contentType != null && !contentType.isBlank()
                        ? contentType
                        : MediaType.APPLICATION_JSON_VALUE;
                rb.header(HttpHeaders.CONTENT_TYPE, ct);
                rb.method(m, HttpRequest.BodyPublishers.ofByteArray(body));
            } else {
                rb.method(m, HttpRequest.BodyPublishers.noBody());
            }

            HttpResponse<byte[]> response = httpClient.send(rb.build(), HttpResponse.BodyHandlers.ofByteArray());

            HttpHeaders out = new HttpHeaders();
            List<String> upstreamCt = response.headers().allValues(HttpHeaders.CONTENT_TYPE);
            if (!upstreamCt.isEmpty()) {
                out.put(HttpHeaders.CONTENT_TYPE, upstreamCt);
            }
            return ResponseEntity.status(response.statusCode()).headers(out).body(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UpstreamReferenceException("Reference catalog request interrupted", e);
        } catch (IOException e) {
            throw new UpstreamReferenceException(
                    "Reference catalog service is not reachable (" + e.getClass().getSimpleName() + ")", e);
        }
    }

    public boolean isAvailable() {
        if (baseUrl.isEmpty()) {
            return false;
        }
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/actuator/health"))
                    .GET()
                    .timeout(Duration.ofSeconds(2))
                    .build();
            HttpResponse<Void> response = httpClient.send(req, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (ConnectException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (IOException e) {
            return false;
        }
    }
}
