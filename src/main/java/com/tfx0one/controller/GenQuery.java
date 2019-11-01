package com.tfx0one.controller;

import lombok.Data;

/**
 * 描述
 *
 * @author 2fx0one
 * @version 1.0
 * @createDate 2019-09-30 14:42
 * @projectName springboot-shiro-jwt-generator
 */
@Data
public class GenQuery {
    String tables;
    String moduleName;
    String tablePrefix;
    String packageName;
}
