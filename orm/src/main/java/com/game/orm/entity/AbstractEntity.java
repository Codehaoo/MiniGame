package com.game.orm.entity;

import com.game.orm.util.ObjectCloner;
import com.google.gson.Gson;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.query.Update;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractEntity<PK extends Comparable<PK>> implements IEntity<PK>, Serializable {

    @Id
    @Setter
    private PK id;

    @Override
    public PK id() {
        return id;
    }

    private static final Map<Class<?>, List<Field>> ENTITY_FIELDS_MAP = new ConcurrentHashMap<>();
    private static final Gson GSON = new Gson();

    @Transient
    private final Map<String, Integer> fieldNameHash = new HashMap<>();
    @Transient
    private final Map<String, String> fieldNameJson = new HashMap<>();

    public Optional<Update> checkUpdateFields() {
        Update update = new Update();
        boolean hasUpdate = false;
        List<Field> fields = getAllPersistFields();
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                String fieldName = field.getName();
                Object value = field.get(this);
                if (value == null) {
                    if (fieldNameHash.containsKey(fieldName) || fieldNameJson.containsKey(fieldName)) {
                        fieldNameHash.remove(fieldName);
                        fieldNameJson.remove(fieldName);
                        update.set(fieldName, null);
                        hasUpdate = true;
                    }
                } else {
                    int hashCode = value.hashCode();
                    if (!fieldNameHash.containsKey(fieldName) || fieldNameHash.get(fieldName) != hashCode) {
                        fieldNameHash.put(fieldName, hashCode);
                        Serializable clone = ObjectCloner.clone(value);
                        if (clone != null) {
                            update.set(fieldName, clone);
                            hasUpdate = true;
                        }
                        if (needCheckJson(value)) {
                            String json = GSON.toJson(value);
                            fieldNameJson.put(fieldName, json);
                        }
                    } else {
                        // hashcode一致再转json检测是否一致
                        if (needCheckJson(value)) {
                            String json = GSON.toJson(value);
                            if (!fieldNameJson.containsKey(fieldName) || !fieldNameJson.get(fieldName).equals(json)) {
                                fieldNameJson.put(fieldName, json);
                                Serializable clone = ObjectCloner.clone(value);
                                if (clone != null) {
                                    update.set(fieldName, clone);
                                    hasUpdate = true;
                                }
                            }
                        }
                    }
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        return hasUpdate ? Optional.of(update) : Optional.empty();
    }

    private List<Field> getAllPersistFields() {
        return ENTITY_FIELDS_MAP.computeIfAbsent(this.getClass(), clazz -> {
            List<Field> fields = new ArrayList<>();
            int modifiers = Modifier.TRANSIENT | Modifier.STATIC;
            while (clazz != null) {
                for (Field field : clazz.getDeclaredFields()) {
                    // skip transient and static field
                    if ((field.getModifiers() & modifiers) != 0) {
                        continue;
                    }
                    if (field.isAnnotationPresent(org.springframework.data.annotation.Transient.class)) {
                        continue;
                    }
                    fields.add(field);
                }
                clazz = clazz.getSuperclass();
            }
            return fields;
        });
    }

    private boolean needCheckJson(Object obj) {
        if (obj instanceof Boolean || obj instanceof Byte || obj instanceof Short || obj instanceof Character || obj instanceof Integer) {
            return false;
        }
        return true;
    }
}
