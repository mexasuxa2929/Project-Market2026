package mexa.club.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * After successful mutating requests, publishes a small JSON envelope to Redis so
 * clients can subscribe (e.g. gateway SSE) without changing each REST handler.
 */
public class RealtimeMutationPublishFilter extends OncePerRequestFilter {

    private static final Set<String> MUTATIONS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final RealtimeProperties props;
    private final String applicationName;

    public RealtimeMutationPublishFilter(
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            RealtimeProperties props,
            String applicationName) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.props = props;
        this.applicationName = applicationName;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        if (!MUTATIONS.contains(request.getMethod())) {
            return true;
        }
        String p = request.getRequestURI();
        return p.contains("/actuator/")
                || p.startsWith("/actuator")
                || p.contains("/swagger")
                || p.contains("/v3/api-docs")
                || p.contains("/webjars/");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(request, response);
        int status = response.getStatus();
        if (status < 200 || status >= 300) {
            return;
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("service", applicationName);
            payload.put("method", request.getMethod());
            payload.put("path", request.getRequestURI());
            payload.put("query", request.getQueryString());
            payload.put("status", status);
            payload.put("timestamp", Instant.now().toString());
            redis.convertAndSend(props.getChannel(), objectMapper.writeValueAsString(payload));
        } catch (Exception ignored) {
            // never fail the HTTP exchange because of realtime
        }
    }
}
