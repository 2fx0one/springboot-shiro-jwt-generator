package com.tfx0one.utils;

import com.tfx0one.controller.GenQuery;
import com.tfx0one.entity.ColumnEntity;
import com.tfx0one.entity.TableEntity;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 代码生成器   工具类
 *
 * @author chenshun
 * @email sunlightcs@gmail.com
 * @date 2016年12月19日 下午11:40:24
 */
public class GenUtils {

    private static final String ENTITY_JAVA = "Entity.java.vm";
    private static final String DAO_JAVA = "Dao.java.vm";
    private static final String DAO_XML = "Dao.xml.vm";
    private static final String SERVICE_JAVA = "Service.java.vm";
    private static final String SERVICE_IMPL_JAVA = "ServiceImpl.java.vm";
    private static final String CONTROLLER_JAVA = "Controller.java.vm";
    private static final String MENU_SQL = "menu.sql.vm";
    private static final String INDEX_VUE = "index.vue.vm";
    private static final String API_JS_VM = "api.js.vm";
    private static final String ADD_OR_UPDATE_VUE = "add-or-update.vue.vm";

    private static final List<String> vmFileList = Arrays.asList(
            ENTITY_JAVA, DAO_JAVA, DAO_XML, SERVICE_JAVA, SERVICE_IMPL_JAVA, CONTROLLER_JAVA, MENU_SQL, INDEX_VUE, API_JS_VM, ADD_OR_UPDATE_VUE
    );

//    private static final String TEMPLATE_DIR = "template";

//    public static List<String> getTemplates() {
//
//        return vmFileList.stream().map(s -> TEMPLATE_DIR + "/" + s).collect(Collectors.toList());
//    }

    /**
     * 生成代码
     */
    public static void generatorCode(GenQuery query, Map<String, String> table,
                                     List<Map<String, String>> columns, ZipOutputStream zip) {

        String tablePrefix = query.getTablePrefix();
        String moduleName = query.getModuleName();
        String packageName = query.getPackageName();  //config.getString("package");

        //配置信息
        Configuration config = getConfig();
        boolean hasBigDecimal = false;
        //表信息
        TableEntity tableEntity = new TableEntity();
        tableEntity.setTableName(table.get("tableName"));
        tableEntity.setComments(table.get("tableComment"));

        //controller controllerUri
        tablePrefix = tablePrefix + "_";
        String controllerUri = tableToURI(tableEntity.getTableName(), tablePrefix);
        tableEntity.setControllerUri(controllerUri);

        //表名转换成Java类名
        String className = tableToJava(tableEntity.getTableName(), tablePrefix);
        tableEntity.setUpperCaseClassName(className);
        tableEntity.setLowerCaseClassName(StringUtils.uncapitalize(className));

//         表名转成 VueFilename 使用 classname 即可
        String vueFilename = tableToVueFilename(tableEntity.getTableName(), tablePrefix);
        tableEntity.setVueFilename(vueFilename);

        //列信息
        List<ColumnEntity> columsList = new ArrayList<>();
        for (Map<String, String> column : columns) {
            ColumnEntity columnEntity = new ColumnEntity();
            columnEntity.setColumnName(column.get("columnName"));
            columnEntity.setDataType(column.get("dataType"));
            columnEntity.setComments(column.get("columnComment"));
            columnEntity.setExtra(column.get("extra"));

            //列名转换成Java属性名
            String attrName = columnToJava(columnEntity.getColumnName());
            columnEntity.setAttrName(attrName);
            columnEntity.setAttrname(StringUtils.uncapitalize(attrName));

            //列的数据类型，转换成Java类型
            String attrType = config.getString(columnEntity.getDataType(), "unknowType");
            columnEntity.setAttrType(attrType);
            if (!hasBigDecimal && "BigDecimal".equals(attrType)) {
                hasBigDecimal = true;
            }
            //是否主键
            if ("PRI".equalsIgnoreCase(column.get("columnKey")) && tableEntity.getPk() == null) {
                tableEntity.setPk(columnEntity);
            }

            columsList.add(columnEntity);
        }
        tableEntity.setColumns(columsList);

        //没主键，则第一个字段为主键
        if (tableEntity.getPk() == null) {
            tableEntity.setPk(tableEntity.getColumns().get(0));
        }

        //设置velocity资源加载器
        Properties prop = new Properties();
        prop.put("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init(prop);
        String mainPath = config.getString("mainPath");
        mainPath = StringUtils.isBlank(mainPath) ? "com.tfx0one" : mainPath;
        //封装模板数据
        Map<String, Object> contextMap = new HashMap<>();
        contextMap.put("tableName", tableEntity.getTableName());
        contextMap.put("comments", tableEntity.getComments());
        contextMap.put("pk", tableEntity.getPk());
        //类名(第一个字母大写)
        contextMap.put("className", tableEntity.getUpperCaseClassName());
        //类名(第一个字母小写)
        contextMap.put("classname", tableEntity.getLowerCaseClassName());
        contextMap.put("controllerUri", tableEntity.getControllerUri());
        contextMap.put("vueFilename", tableEntity.getVueFilename());
        contextMap.put("pathName", tableEntity.getLowerCaseClassName());
        contextMap.put("columns", tableEntity.getColumns());
        contextMap.put("hasBigDecimal", hasBigDecimal);
        contextMap.put("mainPath", mainPath);
        contextMap.put("package", packageName);
//        String moduleName = config.getString("moduleName"); //不使用配置的
        contextMap.put("moduleName", moduleName);
        contextMap.put("author", config.getString("author"));
        contextMap.put("email", config.getString("email"));
        contextMap.put("datetime", DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        VelocityContext context = new VelocityContext(contextMap);


        //获取模板列表
        vmFileList.forEach(templateFilename -> {
            //渲染模板文件
            Template tpl = Velocity.getTemplate("template/" + templateFilename, "UTF-8");
            StringWriter writer = new StringWriter();
            tpl.merge(context, writer);

            try {
                //添加到zip
                zip.putNextEntry(
                        new ZipEntry(
                                getFileName(templateFilename, tableEntity, packageName, moduleName)
                        )
                );
                IOUtils.write(writer.toString(), zip, "UTF-8");
                IOUtils.closeQuietly(writer);
                zip.closeEntry();
            } catch (IOException e) {
                throw new RRException("渲染模板失败，表名：" + tableEntity.getTableName(), e);
            }
        });
    }


    //列名转换成Java属性名
    public static String columnToJava(String columnName) {
        return WordUtils.capitalizeFully(columnName, new char[]{'_'}).replace("_", "");
    }


    //表名转换成Java类名
    public static String tableToJava(String tableName, String tablePrefix) {
        return columnToJava(replacePrefix(tableName, tablePrefix));
    }

    // 替换前缀
    public static String replacePrefix(String tableName, String tablePrefix) {
        return StringUtils.isNotBlank(tablePrefix) ? tableName.replaceFirst(tablePrefix, "") : tableName;
    }

    // 表名转换成uri
    public static String tableToURI(String tableName, String tablePrefix) {
        return replacePrefix(tableName, tablePrefix).replace("_", "/");
    }

    // 表名转换成vue名称 sys_user_abc ==> user-abc
    public static String tableToVueFilename(String tableName, String tablePrefix) {
        return replacePrefix(tableName, tablePrefix).replace("_", "-");
    }


    /**
     * 获取配置信息
     */
    public static Configuration getConfig() {
        try {
            return new PropertiesConfiguration("generator.properties");
        } catch (ConfigurationException e) {
            throw new RRException("获取配置文件失败，", e);
        }
    }

    /**
     * 获取文件名
     */
    private static String getFileName(String template, TableEntity tableEntity, String packageName, String moduleName) {

//        Function<String, String> captureName = name -> name.substring(0, 1).toUpperCase() + name.substring(1);
//        String moduleNameCapture = captureName.apply(moduleName); //首字母大写模块名

        String javaBasePath = "java" + File.separator + "src" + File.separator + "main" + File.separator;
        String javaPackagePath = javaBasePath + "java" + File.separator;
        String javaResourcesPath = javaBasePath + "resources" + File.separator;

        String vuePath = "vue" + File.separator;

        if (StringUtils.isNotBlank(packageName)) {
            javaPackagePath += packageName.replace(".", File.separator) + File.separator + moduleName + File.separator;
        }

        String className = tableEntity.getUpperCaseClassName();
        String vueFilename = tableEntity.getVueFilename();

        Function<String, String> replaceVMSuffix = name -> name.replace(".vm", "");
        if (template.contains(ENTITY_JAVA)) {
            return javaPackagePath + "entity" + File.separator + className + replaceVMSuffix.apply(ENTITY_JAVA);
        }

        if (template.contains(DAO_JAVA)) {
            return javaPackagePath + "dao" + File.separator + className + replaceVMSuffix.apply(DAO_JAVA);
        }

        if (template.contains(SERVICE_JAVA)) {
            return javaPackagePath + "service" + File.separator + className + replaceVMSuffix.apply(SERVICE_JAVA);
        }

        if (template.contains(SERVICE_IMPL_JAVA)) {
            return javaPackagePath + "service" + File.separator + "impl" + File.separator + className + replaceVMSuffix.apply(SERVICE_IMPL_JAVA);
        }

        if (template.contains(CONTROLLER_JAVA)) {
            return javaPackagePath + "controller" + File.separator + className + replaceVMSuffix.apply(CONTROLLER_JAVA);
        }

        if (template.contains(DAO_XML)) {
            return javaResourcesPath + File.separator + "mapper" + File.separator + moduleName + File.separator + className + replaceVMSuffix.apply(DAO_XML);
        }

        if (template.contains(MENU_SQL)) {
            return className + "_" + replaceVMSuffix.apply(MENU_SQL);
        }

        if (template.contains(INDEX_VUE)) {
            return vuePath + "src" + File.separator + "views" + File.separator + "modules" +
                    File.separator + moduleName + File.separator + vueFilename + ".vue";
        }


        if (template.contains(API_JS_VM)) {
            return vuePath + "src" + File.separator + "api" + File.separator + moduleName + File.separator + vueFilename + ".js";
        }


        if (template.contains(ADD_OR_UPDATE_VUE)) {
            return vuePath + "src" + File.separator + "views" + File.separator + "modules" +
                    File.separator + moduleName + File.separator + vueFilename + "-add-or-update.vue";
        }

        return null;
    }
}
