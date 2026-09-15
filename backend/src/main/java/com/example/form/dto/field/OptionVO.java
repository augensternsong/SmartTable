package com.example.form.dto.field;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 栏位选项视图.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionVO {
    private String id;
    private String fieldId;
    private String optionValue;
    private String optionLabel;
    private Integer sortOrder;
    private Integer status;
}
