package mexa.club.notificationservice.service;

import mexa.club.notificationservice.entity.NotificationTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TemplateRenderService {
    public String render(String template, Map<String, Object> variables) {
        String result = template == null ? "" : template;
        if (variables == null) {
            return result;
        }
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            result = result.replace("#{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return result;
    }

    public String renderSubject(NotificationTemplate template, Map<String, Object> vars) {
        return render(template.getSubject(), vars);
    }

    public String renderBody(NotificationTemplate template, Map<String, Object> vars) {
        return render(template.getBodyTemplate(), vars);
    }
}
