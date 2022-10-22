package net.mehvahdjukaar.moonlight2.api.platform.configs.fabric.values;

import com.google.gson.JsonObject;
import net.mehvahdjukaar.labels.LabelsMod;

public class IntConfigValue extends ConfigValue<Integer> {

    private final Integer min;
    private final Integer max;

    public IntConfigValue(String name, Integer defaultValue, Integer min, Integer max) {
        super(name, defaultValue);
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean isValid(Integer value) {
        return value >= min && value <= max;
    }

    @Override
    public void loadFromJson(JsonObject element) {
        if (element.has(this.name)) {
            try {
                this.value = element.get(this.name).getAsInt();
                if (this.isValid(value)) return;
                //if not valid it defaults
                this.value = defaultValue;
            } catch (Exception ignored) {
            }
            LabelsMod.LOGGER.warn("Config file had incorrect entry {}, correcting", this.name);
        } else {
            LabelsMod.LOGGER.warn("Config file had missing entry {}", this.name);
        }
    }

    @Override
    public void saveToJson(JsonObject object) {
        if (this.value == null) this.value = defaultValue;
        object.addProperty(this.name, this.value);
    }

    public Integer getMax() {
        return max;
    }

    public Integer getMin() {
        return min;
    }
}
