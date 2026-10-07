package mexa.club.reportservice.config;

import feign.RequestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class FeignForwardAuthInterceptor implements feign.RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return;
        }
        String incoming = servletAttributes.getRequest().getHeader("Authorization");
        if (incoming == null || incoming.isBlank()) {
            return;
        }
        boolean alreadySet = template.headers().keySet().stream()
                .anyMatch(key -> key != null && key.equalsIgnoreCase("Authorization"));
        if (!alreadySet) {
            template.header("Authorization", incoming);
        }
    }
}
