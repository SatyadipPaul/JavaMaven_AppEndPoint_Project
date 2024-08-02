package com.example.vaadin;

import com.vaadin.flow.component.combobox.ComboBox;
import lombok.Builder;
import lombok.Setter;
import org.springframework.stereotype.Component;
import java.util.Collection;

@Component
public class ComboBoxFactory {

    public enum ComboBoxType {
        DEFAULT, CUSTOM
    }

    @Setter
    @Builder
    public static class ComboBoxConfig<T> {
        private String label;
        @Builder.Default private ComboBoxType type = ComboBoxType.DEFAULT;
        @Builder.Default private boolean required = false;
        @Builder.Default private boolean allowCustomValue = false;
        @Builder.Default private String placeholder = "Choose...";
        @Builder.Default private Collection<T> items = null;
    }

    public <T> ComboBox<T> create(ComboBoxConfig<T> config) {
        ComboBox<T> comboBox = new ComboBox<>();
        comboBox.setLabel(config.getLabel());
        comboBox.setRequired(config.isRequired());
        comboBox.setAllowCustomValue(config.isAllowCustomValue());
        comboBox.setPlaceholder(config.getPlaceholder());

        if (config.getItems() != null) {
            comboBox.setItems(config.getItems());
        }

        switch (config.getType()) {
            case CUSTOM -> {
                // Apply custom logic if needed
            }
            case DEFAULT -> {
                // Apply default logic if needed
            }
        }

        return comboBox;
    }

    public <T> ComboBox<T> createDefault(String label, Collection<T> items) {
        return create(ComboBoxConfig.<T>builder()
                .label(label)
                .items(items)
                .build());
    }

    public <T> ComboBox<T> createCustom(String label, Collection<T> items, boolean required, String placeholder) {
        return create(ComboBoxConfig.<T>builder()
                .label(label)
                .items(items)
                .required(required)
                .allowCustomValue(true)
                .placeholder(placeholder)
                .type(ComboBoxType.CUSTOM)
                .build());
    }
}
