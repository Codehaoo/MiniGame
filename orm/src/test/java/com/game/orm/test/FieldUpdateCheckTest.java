package com.game.orm.test;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * JSON库性能测试 - 对比 Jackson、Gson、FastJSON 在 checkUpdateFields 场景下的性能
 */
public class FieldUpdateCheckTest {

    // ==================== 测试配置 ====================
    private static final int WARMUP_ITERATIONS = 2000;   // 预热次数
    private static final int TEST_ITERATIONS = 10_0000;    // 测试次数
    private static final int LIST_SIZE = 100;             // List大小
    private static final int MAP_SIZE = 100;              // Map大小

    // ==================== 测试模型 ====================

    static class SimpleObject {
        private Long id;
        private String name;
        private Integer level;
        private Double score;
        private Boolean active;

        public SimpleObject() {
        }

        public SimpleObject(Long id, String name, Integer level, Double score, Boolean active) {
            this.id = id;
            this.name = name;
            this.level = level;
            this.score = score;
            this.active = active;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getLevel() {
            return level;
        }

        public void setLevel(Integer level) {
            this.level = level;
        }

        public Double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }

        public Boolean getActive() {
            return active;
        }

        public void setActive(Boolean active) {
            this.active = active;
        }
    }

    static class TestEntity {
        private Long id;
        private SimpleObject simpleField;
        private List<SimpleObject> listField;
        private Map<Long, SimpleObject> mapField;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public SimpleObject getSimpleField() {
            return simpleField;
        }

        public void setSimpleField(SimpleObject simpleField) {
            this.simpleField = simpleField;
        }

        public List<SimpleObject> getListField() {
            return listField;
        }

        public void setListField(List<SimpleObject> listField) {
            this.listField = listField;
        }

        public Map<Long, SimpleObject> getMapField() {
            return mapField;
        }

        public void setMapField(Map<Long, SimpleObject> mapField) {
            this.mapField = mapField;
        }
    }

    // ==================== Jackson 实现 ====================

    static class JacksonChecker {
        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
        private final Map<String, Integer> fieldNameHash = new HashMap<>();
        private final Map<String, JsonNode> fieldNameJsonNode = new HashMap<>();

        public boolean checkUpdateFields(Object entity, List<Field> fields) {
            boolean hasUpdate = false;
            for (Field field : fields) {
                field.setAccessible(true);
                try {
                    String fieldName = field.getName();
                    if ("id".equals(fieldName)) continue;

                    Object value = field.get(entity);
                    if (value == null) {
                        if (fieldNameHash.containsKey(fieldName) || fieldNameJsonNode.containsKey(fieldName)) {
                            fieldNameHash.remove(fieldName);
                            fieldNameJsonNode.remove(fieldName);
                            hasUpdate = true;
                        }
                    } else {
                        int hashCode = value.hashCode();
                        if (!fieldNameHash.containsKey(fieldName) || fieldNameHash.get(fieldName) != hashCode) {
                            fieldNameHash.put(fieldName, hashCode);
                            JsonNode jsonNode = OBJECT_MAPPER.valueToTree(value);
                            fieldNameJsonNode.put(fieldName, jsonNode);

                            // 深拷贝
                            Object clone = OBJECT_MAPPER.treeToValue(jsonNode, value.getClass());
                            if (clone != null) {
                                hasUpdate = true;
                            }
                        } else {
                            // hashcode一致，再用JsonNode检测
                            JsonNode jsonNode = OBJECT_MAPPER.valueToTree(value);
                            if (!fieldNameJsonNode.containsKey(fieldName) ||
                                    !fieldNameJsonNode.get(fieldName).equals(jsonNode)) {
                                fieldNameJsonNode.put(fieldName, jsonNode);
                                Object clone = OBJECT_MAPPER.treeToValue(jsonNode, value.getClass());
                                if (clone != null) {
                                    hasUpdate = true;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return hasUpdate;
        }

        public void reset() {
            fieldNameHash.clear();
            fieldNameJsonNode.clear();
        }
    }

    // ==================== Gson 实现 ====================

    static class GsonChecker {
        private static final Gson GSON = new Gson();
        private final Map<String, Integer> fieldNameHash = new HashMap<>();
        private final Map<String, JsonElement> fieldNameJsonNode = new HashMap<>();

        public boolean checkUpdateFields(Object entity, List<Field> fields) {
            boolean hasUpdate = false;
            for (Field field : fields) {
                field.setAccessible(true);
                try {
                    String fieldName = field.getName();
                    if ("id".equals(fieldName)) continue;

                    Object value = field.get(entity);
                    if (value == null) {
                        if (fieldNameHash.containsKey(fieldName) || fieldNameJsonNode.containsKey(fieldName)) {
                            fieldNameHash.remove(fieldName);
                            fieldNameJsonNode.remove(fieldName);
                            hasUpdate = true;
                        }
                    } else {
                        int hashCode = value.hashCode();
                        if (!fieldNameHash.containsKey(fieldName) || fieldNameHash.get(fieldName) != hashCode) {
                            fieldNameHash.put(fieldName, hashCode);
                            JsonElement jsonElement = GSON.toJsonTree(value);
                            fieldNameJsonNode.put(fieldName, jsonElement);

                            // 深拷贝
                            Object clone = GSON.fromJson(jsonElement, value.getClass());
                            if (clone != null) {
                                hasUpdate = true;
                            }
                        } else {
                            // hashcode一致，再用JsonElement检测
                            JsonElement jsonElement = GSON.toJsonTree(value);
                            if (!fieldNameJsonNode.containsKey(fieldName) ||
                                    !fieldNameJsonNode.get(fieldName).equals(jsonElement)) {
                                fieldNameJsonNode.put(fieldName, jsonElement);
                                Object clone = GSON.fromJson(jsonElement, value.getClass());
                                if (clone != null) {
                                    hasUpdate = true;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return hasUpdate;
        }

        public void reset() {
            fieldNameHash.clear();
            fieldNameJsonNode.clear();
        }
    }

    // ==================== FastJSON 实现 ====================

    static class FastJsonChecker {
        private final Map<String, Integer> fieldNameHash = new HashMap<>();
        private final Map<String, String> fieldNameJsonNode = new HashMap<>();

        public boolean checkUpdateFields(Object entity, List<Field> fields) {
            boolean hasUpdate = false;
            for (Field field : fields) {
                field.setAccessible(true);
                try {
                    String fieldName = field.getName();
                    if ("id".equals(fieldName)) continue;

                    Object value = field.get(entity);
                    if (value == null) {
                        if (fieldNameHash.containsKey(fieldName) || fieldNameJsonNode.containsKey(fieldName)) {
                            fieldNameHash.remove(fieldName);
                            fieldNameJsonNode.remove(fieldName);
                            hasUpdate = true;
                        }
                    } else {
                        int hashCode = value.hashCode();
                        if (!fieldNameHash.containsKey(fieldName) || fieldNameHash.get(fieldName) != hashCode) {
                            fieldNameHash.put(fieldName, hashCode);
                            String jsonStr = JSON.toJSONString(value);
                            fieldNameJsonNode.put(fieldName, jsonStr);

                            // 深拷贝
                            Object clone = JSON.parseObject(jsonStr, value.getClass());
                            if (clone != null) {
                                hasUpdate = true;
                            }
                        } else {
                            // hashcode一致，再用JSON字符串检测
                            String jsonStr = JSON.toJSONString(value);
                            if (!fieldNameJsonNode.containsKey(fieldName) ||
                                    !fieldNameJsonNode.get(fieldName).equals(jsonStr)) {
                                fieldNameJsonNode.put(fieldName, jsonStr);
                                Object clone = JSON.parseObject(jsonStr, value.getClass());
                                if (clone != null) {
                                    hasUpdate = true;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return hasUpdate;
        }

        public void reset() {
            fieldNameHash.clear();
            fieldNameJsonNode.clear();
        }
    }

    // ==================== 测试执行 ====================

    public static void main(String[] args) {
        printHeader();

        System.out.println("\n场景1: 简单对象 (SimpleObject)");
        System.out.println("=".repeat(100));
        testSimpleObject();

        System.out.println("\n\n场景2: List<SimpleObject> (大小: " + LIST_SIZE + ")");
        System.out.println("=".repeat(100));
        testListObject();

        System.out.println("\n\n场景3: Map<Long, SimpleObject> (大小: " + MAP_SIZE + ")");
        System.out.println("=".repeat(100));
        testMapObject();

        System.out.println("\n\n" + "=".repeat(100));
        System.out.println("测试完成!");
        System.out.println("=".repeat(100));
    }

    private static void printHeader() {
        System.out.println("=".repeat(100));
        System.out.println("JSON 库性能对比测试 - checkUpdateFields 方法");
        System.out.println("=".repeat(100));
        System.out.println("配置:");
        System.out.println("  预热次数: " + WARMUP_ITERATIONS);
        System.out.println("  测试次数: " + TEST_ITERATIONS);
        System.out.println("  List大小: " + LIST_SIZE);
        System.out.println("  Map大小:  " + MAP_SIZE);
        System.out.println("=".repeat(100));
    }

    private static void testSimpleObject() {
        TestEntity entity = new TestEntity();
        entity.setId(1L);
        entity.setSimpleField(createSimpleObject(1L));

        List<Field> fields = getEntityFields(entity.getClass());

        Map<String, TestResult> results = new LinkedHashMap<>();
        results.put("Jackson", performTest(new JacksonChecker(), entity, fields));
        results.put("Gson", performTest(new GsonChecker(), entity, fields));
        results.put("FastJSON", performTest(new FastJsonChecker(), entity, fields));

        printResults(results);
    }

    private static void testListObject() {
        TestEntity entity = new TestEntity();
        entity.setId(1L);

        List<SimpleObject> list = new ArrayList<>();
        for (int i = 0; i < LIST_SIZE; i++) {
            list.add(createSimpleObject((long) i));
        }
        entity.setListField(list);

        List<Field> fields = getEntityFields(entity.getClass());

        Map<String, TestResult> results = new LinkedHashMap<>();
        results.put("Jackson", performTest(new JacksonChecker(), entity, fields));
        results.put("Gson", performTest(new GsonChecker(), entity, fields));
        results.put("FastJSON", performTest(new FastJsonChecker(), entity, fields));

        printResults(results);
    }

    private static void testMapObject() {
        TestEntity entity = new TestEntity();
        entity.setId(1L);

        Map<Long, SimpleObject> map = new HashMap<>();
        for (int i = 0; i < MAP_SIZE; i++) {
            map.put((long) i, createSimpleObject((long) i));
        }
        entity.setMapField(map);

        List<Field> fields = getEntityFields(entity.getClass());

        Map<String, TestResult> results = new LinkedHashMap<>();
        results.put("Jackson", performTest(new JacksonChecker(), entity, fields));
        results.put("Gson", performTest(new GsonChecker(), entity, fields));
        results.put("FastJSON", performTest(new FastJsonChecker(), entity, fields));

        printResults(results);
    }

    private static TestResult performTest(Object checker, TestEntity entity, List<Field> fields) {
        String libName = checker.getClass().getSimpleName().replace("Checker", "");

        // 预热
        System.out.print(libName + " 预热中... ");
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            checkFields(checker, entity, fields);
        }
        System.out.println("完成");

        // 重置状态
        resetChecker(checker);

        // 强制GC
        System.gc();
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 正式测试 - 多轮取平均
        List<Long> times = new ArrayList<>();
        int rounds = 5;

        System.out.print(libName + " 测试中... ");
        for (int round = 0; round < rounds; round++) {
            resetChecker(checker);

            long startTime = System.nanoTime();
            for (int i = 0; i < TEST_ITERATIONS; i++) {
                checkFields(checker, entity, fields);
            }
            long endTime = System.nanoTime();

            times.add(endTime - startTime);
        }
        System.out.println("完成");

        // 计算统计数据
        Collections.sort(times);
        TestResult result = new TestResult(libName);
        result.minTimeMs = times.get(0) / 1_000_000;
        result.maxTimeMs = times.get(times.size() - 1) / 1_000_000;
        result.avgTimeMs = times.stream().mapToLong(Long::longValue).sum() / times.size() / 1_000_000;
        result.medianTimeMs = times.get(times.size() / 2) / 1_000_000;

        return result;
    }

    private static void checkFields(Object checker, TestEntity entity, List<Field> fields) {
        if (checker instanceof JacksonChecker) {
            ((JacksonChecker) checker).checkUpdateFields(entity, fields);
        } else if (checker instanceof GsonChecker) {
            ((GsonChecker) checker).checkUpdateFields(entity, fields);
        } else if (checker instanceof FastJsonChecker) {
            ((FastJsonChecker) checker).checkUpdateFields(entity, fields);
        }
    }

    private static void resetChecker(Object checker) {
        if (checker instanceof JacksonChecker) {
            ((JacksonChecker) checker).reset();
        } else if (checker instanceof GsonChecker) {
            ((GsonChecker) checker).reset();
        } else if (checker instanceof FastJsonChecker) {
            ((FastJsonChecker) checker).reset();
        }
    }

    private static void printResults(Map<String, TestResult> results) {
        System.out.println("\n性能数据:");
        System.out.println("-".repeat(100));

        results.forEach((lib, result) -> {
            double avgPerOp = (double) result.avgTimeMs * 1000 / TEST_ITERATIONS; // 微秒
            System.out.printf("%-10s: 最小=%4d ms, 平均=%4d ms, 中位=%4d ms, 最大=%4d ms (平均每次: %.2f μs)%n",
                    lib, result.minTimeMs, result.avgTimeMs, result.medianTimeMs, result.maxTimeMs, avgPerOp);
        });

        System.out.println("\n性能对比 (基于平均时间):");
        System.out.println("-".repeat(100));

        long fastest = results.values().stream()
                .mapToLong(r -> r.avgTimeMs)
                .min()
                .orElse(1);

        results.forEach((lib, result) -> {
            double ratio = (double) result.avgTimeMs / fastest;
            System.out.printf("%-10s: %.2fx%s%n", lib, ratio, ratio == 1.0 ? " ★ 最快" : "");
        });
    }

    private static SimpleObject createSimpleObject(Long id) {
        return new SimpleObject(
                id,
                "Player_" + id,
                50 + id.intValue(),
                1000.0 + id * 10.5,
                id % 2 == 0
        );
    }

    private static List<Field> getEntityFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        int modifiers = Modifier.TRANSIENT | Modifier.STATIC;

        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if ((field.getModifiers() & modifiers) == 0) {
                    fields.add(field);
                }
            }
            clazz = clazz.getSuperclass();
        }

        return fields;
    }

    static class TestResult {
        String libraryName;
        long minTimeMs;
        long maxTimeMs;
        long avgTimeMs;
        long medianTimeMs;

        TestResult(String libraryName) {
            this.libraryName = libraryName;
        }
    }
}