package com.example.form.service;

import com.example.form.common.exception.BusinessException;
import com.example.form.dto.field.FieldVO;
import com.example.form.dto.field.OptionVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * 覆盖用户填写值校验的核心分支(必填/长度/正则/数值范围/日期/选项).
 *
 * <p>UserFormService 依赖多个 Mapper, 但 validateValue 是纯函数, 不触碰任何依赖;
 * 因此用无参构造(依赖保持 null) + 反射调用私有方法即可, 无需 Spring/数据库.
 */
class UserFormValueValidationTest {

    private static Method validate;

    @BeforeAll
    static void setup() throws Exception {
        validate = UserFormService.class.getDeclaredMethod("validateValue", FieldVO.class, String.class);
        validate.setAccessible(true);
    }

    private UserFormService newService() {
        return new UserFormService(null, null, null, null, null, null, null, new ObjectMapper());
    }

    /** 合法值: 不抛 BusinessException */
    private void check(FieldVO f, String value) throws Exception {
        try {
            validate.invoke(newService(), f, value);
        } catch (InvocationTargetException e) {
            throw new AssertionError("合法值被拒绝: " + value + " -> " + e.getTargetException().getMessage());
        }
    }

    /** 非法值: 必须抛 BusinessException */
    private void checkInvalid(FieldVO f, String value) throws Exception {
        try {
            validate.invoke(newService(), f, value);
            fail("非法值未被拒绝: " + value);
        } catch (InvocationTargetException e) {
            assertTrue(e.getTargetException() instanceof BusinessException,
                    "期望 BusinessException, 实际: " + e.getTargetException());
        }
    }

    private FieldVO field(String type) {
        return FieldVO.builder().id("f1").fieldName("测试栏位").fieldType(type).build();
    }

    @Test
    void 必填项为空应拒绝() throws Exception {
        FieldVO f = field("TEXT");
        f.setRequired(1);
        checkInvalid(f, null);
        checkInvalid(f, "   ");
    }

    @Test
    void 非必填为空应通过() throws Exception {
        FieldVO f = field("TEXT");
        f.setRequired(0);
        check(f, null);
        check(f, "");
    }

    @Test
    void 文本超长应拒绝() throws Exception {
        FieldVO f = field("TEXT");
        f.setMaxLength(3);
        check(f, "abc");
        checkInvalid(f, "abcd");
    }

    @Test
    void 正则规则应生效() throws Exception {
        FieldVO f = field("TEXT");
        f.setRegexPattern("^[A-Z]+$");
        check(f, "ABC");
        checkInvalid(f, "abc");
    }

    @Test
    void 数字范围与格式应校验() throws Exception {
        FieldVO f = field("NUMBER");
        f.setMinValue(new java.math.BigDecimal("0"));
        f.setMaxValue(new java.math.BigDecimal("100"));
        check(f, "50");
        checkInvalid(f, "abc");
        checkInvalid(f, "-1");
        checkInvalid(f, "101");
    }

    @Test
    void 日期格式应校验() throws Exception {
        FieldVO f1 = field("DATE");
        check(f1, "2026-09-16");
        checkInvalid(f1, "2026/09/16");

        FieldVO f2 = field("DATETIME");
        check(f2, "2026-09-16 10:20:30");
        checkInvalid(f2, "2026-09-16");
    }

    @Test
    void 单选必须命中选项() throws Exception {
        FieldVO f = field("SELECT_SINGLE");
        f.setOptions(List.of(option("a"), option("b")));
        check(f, "a");
        checkInvalid(f, "x");
    }

    @Test
    void 多选必须全部命中选项且为合法数组() throws Exception {
        FieldVO f = field("SELECT_MULTI");
        f.setOptions(List.of(option("a"), option("b")));
        check(f, "[\"a\",\"b\"]");
        checkInvalid(f, "[\"a\",\"x\"]");
        checkInvalid(f, "not-json");
    }

    private OptionVO option(String value) {
        return OptionVO.builder().optionValue(value).optionLabel(value).build();
    }
}
