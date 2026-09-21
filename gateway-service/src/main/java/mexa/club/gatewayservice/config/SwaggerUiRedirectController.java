package mexa.club.gatewayservice.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.net.URI;

@RestController
public class SwaggerUiRedirectController {

    private static final String TARGET_TEMPLATE = "/swagger-ui.html?urls.primaryName=%s";

    @GetMapping({
            "/swagger/auth/swagger-ui/index.html",
            "/swagger/auth/swagger-ui.html"
    })
    public Mono<Void> authSwagger(ServerHttpResponse response) {
        return redirect(response, "auth");
    }

    @GetMapping({
            "/swagger/product/swagger-ui/index.html",
            "/swagger/product/swagger-ui.html"
    })
    public Mono<Void> productSwagger(ServerHttpResponse response) {
        return redirect(response, "product");
    }

    @GetMapping({
            "/swagger/shop/swagger-ui/index.html",
            "/swagger/shop/swagger-ui.html"
    })
    public Mono<Void> shopSwagger(ServerHttpResponse response) {
        return redirect(response, "shop");
    }

    @GetMapping({
            "/swagger/warehouse/swagger-ui/index.html",
            "/swagger/warehouse/swagger-ui.html"
    })
    public Mono<Void> warehouseSwagger(ServerHttpResponse response) {
        return redirect(response, "warehouse");
    }

    @GetMapping({
            "/swagger/order/swagger-ui/index.html",
            "/swagger/order/swagger-ui.html"
    })
    public Mono<Void> orderSwagger(ServerHttpResponse response) {
        return redirect(response, "order");
    }

    @GetMapping({
            "/swagger/notification/swagger-ui/index.html",
            "/swagger/notification/swagger-ui.html"
    })
    public Mono<Void> notificationSwagger(ServerHttpResponse response) {
        return redirect(response, "notification");
    }

    @GetMapping({
            "/swagger/delivery/swagger-ui/index.html",
            "/swagger/delivery/swagger-ui.html"
    })
    public Mono<Void> deliverySwagger(ServerHttpResponse response) {
        return redirect(response, "delivery");
    }

    @GetMapping({
            "/swagger/report/swagger-ui/index.html",
            "/swagger/report/swagger-ui.html"
    })
    public Mono<Void> reportSwagger(ServerHttpResponse response) {
        return redirect(response, "report");
    }

    @GetMapping({
            "/swagger/search/swagger-ui/index.html",
            "/swagger/search/swagger-ui.html"
    })
    public Mono<Void> searchSwagger(ServerHttpResponse response) {
        return redirect(response, "search");
    }

    @GetMapping({
            "/swagger/catalog/swagger-ui/index.html",
            "/swagger/catalog/swagger-ui.html"
    })
    public Mono<Void> catalogSwagger(ServerHttpResponse response) {
        return redirect(response, "catalog");
    }

    private Mono<Void> redirect(ServerHttpResponse response, String serviceName) {
        response.setStatusCode(HttpStatus.FOUND);
        response.getHeaders().setLocation(URI.create(TARGET_TEMPLATE.formatted(serviceName)));
        return response.setComplete();
    }
}
