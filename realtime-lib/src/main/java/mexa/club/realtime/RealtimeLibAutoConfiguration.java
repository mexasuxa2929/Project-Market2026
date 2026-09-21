package mexa.club.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;

@AutoConfiguration(after = RedisAutoConfiguration.class)
@EnableConfigurationProperties(RealtimeProperties.class)
@ConditionalOnProperty(prefix = "app.realtime", name = "enabled", havingValue = "true")
public class RealtimeLibAutoConfiguration {

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    public FilterRegistrationBean<RealtimeMutationPublishFilter> realtimeMutationPublishFilter(
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            RealtimeProperties props,
            @Value("${spring.application.name:unknown}") String appName) {
        FilterRegistrationBean<RealtimeMutationPublishFilter> reg = new FilterRegistrationBean<>(
                new RealtimeMutationPublishFilter(redis, objectMapper, props, appName));
        reg.setOrder(Ordered.LOWEST_PRECEDENCE);
        return reg;
    }
}
