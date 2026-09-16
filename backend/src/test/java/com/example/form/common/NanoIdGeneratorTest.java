package com.example.form.common;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * NanoId 生成器单元测试(无外部依赖).
 */
class NanoIdGeneratorTest {

    @Test
    void nextId_应是8位且仅含字母表字符() {
        String id = NanoIdGenerator.nextId();
        assertEquals(8, id.length());
        String alphabet = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz";
        for (char c : id.toCharArray()) {
            assertTrue(alphabet.indexOf(c) >= 0, "非法字符: " + c);
        }
    }

    @Test
    void 不应包含易混字符() {
        for (int i = 0; i < 10_000; i++) {
            String id = NanoIdGenerator.nextId();
            assertFalse(id.matches(".*[01OlI].*"), "出现易混字符: " + id);
        }
    }

    @Test
    void 大量生成应基本唯一() {
        int n = 100_000;
        Set<String> set = new HashSet<>(n);
        for (int i = 0; i < n; i++) {
            set.add(NanoIdGenerator.nextId());
        }
        assertEquals(n, set.size(), "8位NanoId在10万次内不应碰撞");
    }

    @Test
    void 指定长度应生效() {
        assertEquals(1, NanoIdGenerator.randomNanoId(1).length());
        assertEquals(16, NanoIdGenerator.randomNanoId(16).length());
    }

    @Test
    void 非法长度应抛异常() {
        assertThrows(IllegalArgumentException.class, () -> NanoIdGenerator.randomNanoId(0));
        assertThrows(IllegalArgumentException.class, () -> NanoIdGenerator.randomNanoId(-1));
    }

    @RepeatedTest(5)
    void 重复生成结果应不同() {
        assertFalse(NanoIdGenerator.nextId().equals(NanoIdGenerator.nextId()));
    }
}
