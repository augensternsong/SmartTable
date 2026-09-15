package com.example.form.common;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 8 位 NanoId 风格 ID 生成器, 注册到 MyBatis-Plus 作为全局 ID 生成策略.
 *
 * <p>实现自包含(无外部依赖), 字符表去除易混字符(0/O/1/I/l)以降低抄录错误;
 * 8 位长度理论空间 ≈ 31^8 ≈ 8.5e11, 满足本系统并发量需求且便于 URL/导出展示.
 */
@Component
public class NanoIdGenerator implements IdentifierGenerator {

    /** 不含易混字符的字母表, 共 31 个字符 */
    private static final char[] ALPHABET =
            "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz".toCharArray();

    /** 默认长度 */
    private static final int DEFAULT_SIZE = 8;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public String nextUUID(Object entity) {
        return nextId();
    }

    /**
     * 本系统主键统一为 8 位 String NanoId, 不使用 Long 类型主键, 此方法不会被调用.
     */
    @Override
    public Number nextId(Object entity) {
        throw new UnsupportedOperationException("本系统使用 String NanoId, 请勿调用 nextId");
    }

    /**
     * 生成一个 8 位 NanoId 风格 ID.
     */
    public static String nextId() {
        return randomNanoId(DEFAULT_SIZE);
    }

    /**
     * 生成指定长度的 NanoId 风格字符串.
     */
    public static String randomNanoId(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size 必须为正数");
        }
        char[] buf = new char[size];
        int mask = ALPHABET.length;
        for (int i = 0; i < size; i++) {
            buf[i] = ALPHABET[RANDOM.nextInt(mask)];
        }
        return new String(buf);
    }
}
