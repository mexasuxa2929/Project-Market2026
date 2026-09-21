package mexa.club.realtime;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.realtime")
public class RealtimeProperties {

    /**
     * When false, no Redis publish filter is registered.
     */
    private boolean enabled = true;

    /**
     * Redis PUB/SUB channel (must match gateway SSE subscription).
     */
    private String channel = "mexa:realtime:events";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }
}
