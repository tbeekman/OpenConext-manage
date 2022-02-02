package manage.hook;

import manage.conf.MetaDataAutoConfiguration;
import manage.model.EntityType;
import manage.model.MetaData;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

public class RequiredAttributesHook extends MetaDataHookAdapter {

    private final MetaDataAutoConfiguration metaDataAutoConfiguration;

    public RequiredAttributesHook(MetaDataAutoConfiguration metaDataAutoConfiguration) {
        this.metaDataAutoConfiguration = metaDataAutoConfiguration;
    }

    @Override
    public MetaData prePut(MetaData previous, MetaData newMetaData) {
        validate(newMetaData);
        return super.prePut(previous, newMetaData);
    }

    @Override
    public MetaData prePost(MetaData metaData) {
        validate(metaData);
        return super.prePost(metaData);
    }

    @SuppressWarnings("unchecked")
    private void validate(MetaData newMetaData) {
        Map<String, Object> metaDataFields = newMetaData.metaDataFields();
        Map<String, Object> schemaRepresentation = this.metaDataAutoConfiguration.schemaRepresentation(EntityType.fromType(newMetaData.getType()));
        Schema schema = metaDataAutoConfiguration.schema(newMetaData.getType());
        Map<String, Object> schemaMetaDataFields = Map.class.cast(Map.class.cast(schemaRepresentation.get("properties")).get("metaDataFields"));
        Map<String, Object> properties = (Map<String, Object>) schemaMetaDataFields.get("properties");
        Map<String, Object> patternProperties = (Map<String, Object>) schemaMetaDataFields.get("patternProperties");

        properties.forEach((key, value) -> {
            if (value instanceof Map && ((Map) value).containsKey("requiredAttributes")) {
                List<String> requiredAttributes = (List<String>) ((Map<?, ?>) value).get("requiredAttributes");
                this.ensureMetaDataFieldIsPresent(requiredAttributes, metaDataFields, schema, key);
            }
        });

        patternProperties.forEach((key, value) -> {
            if (value instanceof Map && ((Map) value).containsKey("requiredAttributes")) {
                Map<String, List<String>> requiredAttributesMap = (Map<String, List<String>>) ((Map<?, ?>) value).get("requiredAttributes");
                requiredAttributesMap.forEach((attr, requiredAttributes) -> this.ensureMetaDataFieldIsPresent(requiredAttributes, metaDataFields, schema, attr));
            }
        });
    }

    private void ensureMetaDataFieldIsPresent(List<String> names, Map<String, Object> metaDataFields, Schema schema, String parentKey) {
        if (metaDataFields.containsKey(parentKey)) {
            names.forEach(name -> {
                Object value = metaDataFields.get(name);
                if (value == null || (value instanceof String && !StringUtils.hasText((String) value))) {
                    throw new ValidationException(schema,
                            String.format("Missing required attribute %s defined as required by %s", name, parentKey),
                            name);
                }
            });
        }
    }

}