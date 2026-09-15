package com.example.form.dto.userform;

import com.example.form.dto.field.FieldVO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 用户视角下的栏位(继承栏位结构 + 当前值/填写时间/超期标记).
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserFieldVO extends FieldVO {
    /** 当前填写的值(多选为 JSON 数组字符串) */
    private String currentValue;
    /** 上次填写时间 */
    private LocalDateTime filledAt;
    /** 填写人 */
    private String filledBy;
    /** 是否超期需更新(fill_cycle_days 已过) */
    private Boolean expired;
    /** 是否从未填写 */
    private Boolean pending;
    /** 距上次填写天数(null 表示从未填写) */
    private Integer daysSinceFilled;
}
