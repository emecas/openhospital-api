package org.isf.admtype.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Class representing an admission type")
public class AdmissionTypeDTO {

    @ApiModelProperty(notes = "Code", example = "", position = 1)
    private String code;

    @ApiModelProperty(notes = "Description", example = "", position = 2)
    private String description;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
