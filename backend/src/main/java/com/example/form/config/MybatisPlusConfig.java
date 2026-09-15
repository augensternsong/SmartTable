package com.example.form.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置.
 *
 * <p>自定义 ID 生成器(NanoId)已在 {@code NanoIdGenerator} 上以 @Component 注册,
 * MyBatis-Plus 自动装配会自动选用 String 类型的 id 生成路径.
 *
 * <p>这里仅注册字段自动填充处理器, 用于 created_at / updated_at, 以及分页拦截器.
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 分页拦截器(否则 selectPage 不会真正分页).
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }

    /**
     * 自动填充 created_at / updated_at / createdAt / updatedAt.
     */
    @Component
    public static class AutoFillMetaObjectHandler implements MetaObjectHandler {
        @Override
        public void insertFill(MetaObject metaObject) {
            LocalDateTime now = LocalDateTime.now();
            fillIfExists(metaObject, "createdAt", now);
            fillIfExists(metaObject, "created_at", now);
            fillIfExists(metaObject, "updatedAt", now);
            fillIfExists(metaObject, "updated_at", now);
        }

        @Override
        public void updateFill(MetaObject metaObject) {
            LocalDateTime now = LocalDateTime.now();
            fillIfExists(metaObject, "updatedAt", now);
            fillIfExists(metaObject, "updated_at", now);
        }

        private void fillIfExists(MetaObject metaObject, String fieldName, Object value) {
            if (metaObject.hasSetter(fieldName)) {
                Object existing = metaObject.getValue(fieldName);
                if (existing == null) {
                    metaObject.setValue(fieldName, value);
                }
            }
        }
    }
}
