package com.yjl.distributed.mq.config.common.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.reflections.ReflectionUtils;
import com.google.common.collect.Maps;
import com.yjl.distributed.mq.config.common.annotation.ExportFiledComment;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;

/**
 * 反射工具
 *
 * @see "https://github.com/ronmamo/reflections"
 * @author zhaoyc
 * @version 创建时间：2017年11月27日 上午11:08:28
 */
public final class ReflectionPlusUtils extends ReflectionUtils {

    private final static String GETTERS_PREFIX = "get";


    /**
     * 反射调用对象字段的Getters方法,结果定制输出成字符串
     * <p>
     * 只适用于个别业务场景
     *
     * @param object
     * @param fieldName
     * @return 结果定制化
     */
    public static String invokeFieldGettersMethodToCustomizeString(Object object,
            String fieldName) {
        final Object methodResult = invokeFieldGettersMethod(object, fieldName);
        if (Objects.isNull(methodResult)) {
            return StringUtils.EMPTY;
        }
        if (methodResult.getClass().isEnum()) {
            return invokeFieldGettersMethod(methodResult, "comment").toString();
        } else if (methodResult instanceof Date) {
            return DateUtilsPlus.formatDateByStyle((Date) methodResult);
        }
        return methodResult.toString();
    }



    /**
     * 获取字段属性说明
     * 
     * @param clazz
     * @return
     */
    @SuppressWarnings("unchecked")
    public static LinkedHashMap<String, String> exportFiledComment(Class<?> clazz) {
        Set<Field> fields = getAllFields(clazz, withAnnotation(ExportFiledComment.class));
        LinkedHashMap<String, String> linkedHashMap =
                new LinkedHashMap<>(Maps.newHashMapWithExpectedSize(fields.size()));
        fields.forEach(field -> {
            String filedComment = field.getAnnotation(ExportFiledComment.class).value();
            if (StringUtils.isEmpty(filedComment)) {
                filedComment = field.getName();
            }
            linkedHashMap.put(field.getName(), filedComment);
        });
        return linkedHashMap;
    }


    /**
     * 获取字段属性说明
     * 
     * @param clazz
     * @param keyPrefix 字段前缀
     * @param valuePrefix 值的前缀
     * @return
     */
    @SuppressWarnings("unchecked")
    public static LinkedHashMap<String, String> exportFiledComment(Class<?> clazz, String keyPrefix,
            String valuePrefix) {
        Set<Field> fields = getAllFields(clazz, withAnnotation(ExportFiledComment.class));
        LinkedHashMap<String, String> linkedHashMap =
                new LinkedHashMap<>(Maps.newHashMapWithExpectedSize(fields.size()));
        fields.forEach(field -> {
            String filedComment = field.getAnnotation(ExportFiledComment.class).value();
            if (StringUtils.isEmpty(filedComment)) {
                filedComment = field.getName();
            }
            linkedHashMap.put(keyPrefix + field.getName(), valuePrefix + filedComment);
        });
        return linkedHashMap;
    }


    ///////////////////////////////////////////////////////////////////////////
    // 下面为通用方法
    ///////////////////////////////////////////////////////////////////////////


    @SuppressWarnings("unchecked")
    public static Set<Method> getMethods(Class<?> clazz,
            final Class<? extends Annotation> annotation) {
        return getMethods(clazz, withAnnotation(annotation));
    }

    /**
     * 反射调用对象字段的Getters方法
     *
     * @param object : 该对象
     * @param fieldName : 该对象字段
     * @return 该对象字段Getters方法结果
     */
    public static Object invokeFieldGettersMethod(Object object, String fieldName) {
        if (Objects.isNull(object)) {
            return null;
        }
        final Method gettersMethod = getFieldGettersMethod(object.getClass(), fieldName);
        if (Objects.isNull(gettersMethod)) {
            return null;
        }
        try {
            return gettersMethod.invoke(object);
        } catch (IllegalAccessException | InvocationTargetException e) {
            LogManager.getLogger().error(e);
            return null;
        }
    }


    /**
     * 反射得到对象字段Getters方法
     *
     * @param clazz : 对象
     * @param fieldName : 对象字段
     * @return <code>getFieldName</code>方法
     */
    @SuppressWarnings("unchecked")
    public static Method getFieldGettersMethod(Class<?> clazz, String fieldName) {
        if (Objects.isNull(clazz) || StringUtils.isEmpty(fieldName)) {
            return null;
        }
        final Set<Method> fieldNameGettersMethods = getMethods(clazz, withModifier(Modifier.PUBLIC),
                withName(buildGetters(fieldName)), withParametersCount(0));
        return fieldNameGettersMethods.parallelStream().findFirst().orElse(null);
    }

    /**
     * 构建字段Getters方法名称
     *
     * @param fieldName 字段
     * @return <code>getFieldName</code>
     */
    private static String buildGetters(String fieldName) {
        return GETTERS_PREFIX + StringPrivateUtils.firstCharToUpperCase(fieldName);
    }

    /**
     * 得到方法参数的名称
     */
    public static Map<String, Integer> getMethodParamList(Class<?> cls, String clazzName,
            String methodName) throws NotFoundException {
        Map<String, Integer> resultMap = new HashMap<>();
        ClassPool pool = ClassPool.getDefault();
        ClassClassPath classPath = new ClassClassPath(cls);
        pool.insertClassPath(classPath);

        CtClass cc = pool.get(clazzName);
        CtMethod cm = cc.getDeclaredMethod(methodName);
        MethodInfo methodInfo = cm.getMethodInfo();
        CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
        LocalVariableAttribute attr =
                (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);
        if (attr == null) {
            return resultMap;
        }
        String[] paramNames = new String[cm.getParameterTypes().length];
        int pos = javassist.Modifier.isStatic(cm.getModifiers()) ? 0 : 1;
        for (int i = 0; i < paramNames.length; i++) {
            // paramNames即参数名
            String paramName = attr.variableName(i + pos);
            resultMap.put(paramName, i + 1);
        }
        return resultMap;
    }


}


