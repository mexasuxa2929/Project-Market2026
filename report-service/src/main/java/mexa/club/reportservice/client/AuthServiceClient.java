package mexa.club.reportservice.client;

import mexa.club.reportservice.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "reportAuthClient", url = "${services.auth.url}")
public interface AuthServiceClient {
    @GetMapping("/api/admin/users")
    ApiResponse<Map<String, Object>> users(@RequestParam("page") int page, @RequestParam("size") int size);
}
