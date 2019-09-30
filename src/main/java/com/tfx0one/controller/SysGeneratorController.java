/**
 * Copyright (c) 2018 人人开源 All rights reserved.
 * <p>
 * https://www.renren.io
 * <p>
 * 版权所有，侵权必究！
 */

package com.tfx0one.controller;

import com.tfx0one.service.SysGeneratorService;
import com.tfx0one.utils.R;
import com.tfx0one.utils.PageUtils;
import com.tfx0one.utils.Query;

import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * 代码生成器
 *
 * @author Mark sunlightcs@gmail.com
 */
@Controller
@RequestMapping("/sys/generator")
public class SysGeneratorController {

    @Autowired
    private SysGeneratorService sysGeneratorService;

    /**
     * 列表
     */
    @ResponseBody
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils pageUtil = sysGeneratorService.queryList(new Query(params));

        return R.ok().put("page", pageUtil);
    }

    /**
     * 生成代码
     */
    @RequestMapping("/code")
    public void code(GenQuery query,
                     HttpServletResponse response) throws IOException {
//        byte[] data = sysGeneratorService.generatorCode(tables.split(","), tablePrefix, moduleName);
        byte[] data = sysGeneratorService.generatorCode(query);

        String zipFileName = "genCode" + DateFormatUtils.format(new Date(), "yyyyMMdd_HHmmss") + ".zip";
        response.reset();
        response.setHeader("Content-Disposition", "attachment; filename=\"" + zipFileName + "\"");
        response.addHeader("Content-Length", "" + data.length);
        response.setContentType("application/octet-stream; charset=UTF-8");

        IOUtils.write(data, response.getOutputStream());
    }
}
