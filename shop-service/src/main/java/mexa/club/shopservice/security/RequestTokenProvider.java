package mexa.club.shopservice.security;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class RequestTokenProvider {

    public String resolveAuthorizationHeader() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes servletAttributes)) {
            return null;
        }
        String authHeader = servletAttributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
        return StringUtils.hasText(authHeader) ? authHeader.trim() : null;
    }
}
