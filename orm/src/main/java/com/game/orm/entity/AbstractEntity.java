package com.game.orm.entity;

import com.alibaba.fastjson2.JSON;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.query.Update;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
public abstract class AbstractEntity<PK extends Comparable<PK>> implements IEntity<PK> {

    @Id
    @Setter
    private PK id;

    @Override
    public PK id() {
        return id;
    }

    private static final Map<Class<?>, List<Field>> ENTITY_FIELDS_MAP = new ConcurrentHashMap<>();

    @Transient
    private final Map<String, Integer> fieldNameHash = new HashMap<>();
    @Transient
    private final Map<String, String> fieldNameJson = new HashMap<>();
    @Transient
    @Getter
    private final Queue<Update> updateQueue = new ConcurrentLinkedQueue<>();

    public boolean checkUpdateFields() {
        Update update = new Update();
        boolean hasUpdate = false;
        List<Field> fields = getAllPersistFields();
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                String fieldName = field.getName();
                if ("id".equals(fieldName)) {
                    continue;
                }
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
                        String json = null;
                        if (needCheckJsonNode(value)) {
                            json = JSON.toJSONString(value);
                            fieldNameJson.put(fieldName, json);
                        }
                        if (isImmutableType(value)) {
                            update.set(fieldName, value);
                            hasUpdate = true;
                        } else {
                            if (json == null) {
                                json = JSON.toJSONString(value);
                            }
                            Object clone = JSON.parseObject(json, value.getClass());
                            if (clone != null) {
                                update.set(fieldName, clone);
                                hasUpdate = true;
                            }
                        }
                    } else {
                        // hashcode一致再转jsonNode检测是否一致
                        if (needCheckJsonNode(value)) {
                            String json = JSON.toJSONString(value);
                            if (!fieldNameJson.containsKey(fieldName) || !fieldNameJson.get(fieldName).equals(json)) {
                                fieldNameJson.put(fieldName, json);
                                Object clone = JSON.parseObject(json, value.getClass());
                                if (clone != null) {
                                    update.set(fieldName, clone);
                                    hasUpdate = true;
                                }
                            }
                        }
                    }
                }
            } catch (IllegalAccessException e) {
                log.error("{} {} field value get error, id={}", this.getClass().getName(), field.getName(), id, e);
            }
        }

        if (hasUpdate) {
            updateQueue.offer(update);
        }

        return !updateQueue.isEmpty();
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

    private boolean needCheckJsonNode(Object obj) {
        // 一定不发生哈希碰撞
        if (obj instanceof Boolean || obj instanceof Byte || obj instanceof Short || obj instanceof Character || obj instanceof Integer) {
            return false;
        }
        return true;
    }

    private boolean isImmutableType(Object obj) {
        if (obj instanceof Boolean || obj instanceof Byte || obj instanceof Short || obj instanceof Character ||
                obj instanceof Integer || obj instanceof Long || obj instanceof Float || obj instanceof Double) {
            return true;
        }

        if (obj instanceof String || obj instanceof Enum) {
            return true;
        }

        if (obj instanceof java.math.BigInteger || obj instanceof java.math.BigDecimal) {
            return true;
        }

        if (obj instanceof java.time.LocalDate ||
                obj instanceof java.time.LocalDateTime ||
                obj instanceof java.time.LocalTime ||
                obj instanceof java.time.Instant ||
                obj instanceof java.time.ZonedDateTime) {
            return true;
        }

        if (obj instanceof java.util.UUID) {
            return true;
        }

        return false;
    }
}
