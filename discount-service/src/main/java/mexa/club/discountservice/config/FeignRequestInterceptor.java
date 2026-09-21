package mexa.club.discountservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collection;

@Component
public class FeignRequestInterceptor implements RequestInterceptor {

    private static final String AUTHORIZATION = "Authorization";

    @Override
    public void apply(RequestTemplate template) {
        String token = resolveJwtToken();
        if (StringUtils.hasText(token) && template.headers().get(AUTHORIZATION) == null) {
            template.header(AUTHORIZATION, "Bearer " + token);
        }
    }

    private String resolveJwtToken() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            String header = servletAttrs.getRequest().getHeader(AUTHORIZATION);
            String token = extractBearerToken(header);
            if (StringUtils.hasText(token)) {
                return token;
            }
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            String fromDetails = extractBearerToken(toStringSafe(authentication.getDetails()));
            if (StringUtils.hasText(fromDetails)) {
                return fromDetails;
            }
            String fromCred = extractBearerToken(toStringSafe(authentication.getCredentials()));
            if (StringUtils.hasText(fromCred)) {
                return fromCred;
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
