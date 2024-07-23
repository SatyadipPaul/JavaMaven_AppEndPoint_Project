import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import lombok.Builder;
import lombok.Setter;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.logging.Logger;

@Component
public class NotificationFactory {

    private static final Logger logger = Logger.getLogger(NotificationFactory.class.getName());

    public enum NotificationType {
        SUCCESS, ERROR, NORMAL
    }

    @Setter
    @Builder
    public static class NotificationConfig {
        private String text;
        @Builder.Default private NotificationType type = NotificationType.NORMAL;
        @Builder.Default private Notification.Position position = Notification.Position.BOTTOM_START;
        @Builder.Default private int duration = 5000; // milliseconds
        @Builder.Default private boolean closeButtonVisible = true;
    }

    public Notification create(NotificationConfig config) {
        if (config.text == null || config.text.isEmpty()) {
            throw new IllegalArgumentException("Notification text cannot be null or empty");
        }

        Notification notification = new Notification();
        notification.setText(config.text);
        notification.setPosition(config.position);
        notification.setDuration(config.duration);

        switch (config.type) {
            case SUCCESS -> notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            case ERROR -> notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            case NORMAL -> {} // No additional variants for NORMAL type
        }

        if (config.closeButtonVisible) {
            notification.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
        } else {
            notification.setCloseButtonVisible(false);
        }

        logger.info("Created notification: " + config.text);

        return notification;
    }

    public Notification createSuccess(String text) {
        return create(NotificationConfig.builder()
                .text(text)
                .type(NotificationType.SUCCESS)
                .build());
    }

    public Notification createError(String text) {
        return create(NotificationConfig.builder()
                .text(text)
                .type(NotificationType.ERROR)
                .build());
    }

    public Notification createNormal(String text) {
        return create(NotificationConfig.builder()
                .text(text)
                .type(NotificationType.NORMAL)
                .build());
    }
}
