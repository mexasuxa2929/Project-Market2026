package mexa.club.orderservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collection;

@Component
public class FeignClientInterceptor implements RequestInterceptor {

    private static final String AUTHORIZATION = "Authorization";
    private static final String X_INTERNAL_SECRET = "X-Internal-Secret";
    private static final String X_INTERNAL_API_KEY = "X-Internal-Api-Key";

    private final String internalSecret;
    private final String shopInternalApiKey;

    public FeignClientInterceptor(
            @Value("${app.internal-secret}") String internalSecret,
            @Value("${app.shop.internal-api-key:change-me-internal}") String shopInternalApiKey
    ) {
        this.internalSecret = internalSecret;
        this.shopInternalApiKey = shopInternalApiKey;
    }

    @Override
    public void apply(RequestTemplate template) {
        String token = resolveJwtToken();
        if (StringUtils.hasText(token) && template.headers().get(AUTHORIZATION) == null) {
            template.header(AUTHORIZATION, "Bearer " + token);
        }

        String path = template.path();
        if (path == null) {
            return;
        }
        if (path.startsWith("/internal/") && StringUtils.hasText(internalSecret)) {
            template.header(X_INTERNAL_SECRET, internalSecret);
        }
        if (path.startsWith("/api/shops/internal/") && StringUtils.hasText(shopInternalApiKey)) {
            template.header(X_INTERNAL_API_KEY, shopInternalApiKey);
        }
    }

    private String resolveJwtToken() {
        // 1) Current HTTP request header is the most reliable source.
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            String header = servletAttrs.getRequest().getHeader(AUTHORIZATION);
            String token = extractBearerToken(header);
            if (StringUtils.hasText(token)) {
                return token;
            }
        }

        // 2) Fallback to SecurityContext details/credentials (if present).
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            String tokenFromDetails = extractBearerToken(toStringSafe(authentication.getDetails()));
            if (StringUtils.hasText(tokenFromDetails)) {
                return tokenFromDetails;
            }
            String tokenFromCredentials = extractBearerToken(toStringSafe(authentication.getCredentials()));
            if (StringUtils.hasText(tokenFromCredentials)) {
                return tokenFromCredentials;
            }
        }
        return null;
    }

    private String extractBearerToken(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }
        String value = rawValue.trim();
        if (value.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = value.substring(7).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }

    private String toStringSafe(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Collection<?> collection && collection.isEmpty()) {
            return null;
        }
        return String.valueOf(value);
    }
}

