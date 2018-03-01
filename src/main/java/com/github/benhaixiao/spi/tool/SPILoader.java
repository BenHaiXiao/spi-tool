package com.github.benhaixiao.spi.tool;

import com.github.benhaixiao.spi.tool.util.Holder;
import com.github.benhaixiao.spi.tool.util.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

/**
 * spi加载器
 * @author xiaobenhai
 * Date: 2016/11/23
 * Time: 12:51
 */
public class SPILoader<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SPILoader.class);

    private static final String SPI_DIRECTORY = "META-INF/spi-tool/";
    private static final Pattern NAME_SEPARATOR = Pattern.compile("\\s*[,]+\\s*");

    /**
     * key:spi扩展点接口
     * value: spi加载器
     */
    private static final ConcurrentMap<Class<?>, SPILoader<?>> SPI_LOADERS = new ConcurrentHashMap<Class<?>, SPILoader<?>>();

    /**
     * key:spi具体实现类class
     * value: spi具体实现类实例
     */
    private static final ConcurrentMap<Class<?>, Object> SPI_INSTANCES = new ConcurrentHashMap<Class<?>, Object>();

    /**
     * key:spi扩展点接口
     * value:spi扩展点名称
     */
    private final ConcurrentMap<Class<?>, String> cachedNames = new ConcurrentHashMap<Class<?>, String>();

    /**
     * key: spi扩展点名称
     * value:spi扩展点实现类class
     */
    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<Map<String, Class<?>>>();

    /**
     * key:spi扩展点名称
     * value；spi扩展点实现类实例
     */
    private final ConcurrentMap<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<String, Holder<Object>>();
    /**
     * 当前sp扩展点接口
     */
    private final Class<?> type;
    /**
     * spi默认名称
     */
    private String cachedDefaultName;
    /**
     * key:扩展点实现类class类名
     * value：异常
     */

    private Map<String, IllegalStateException> exceptions = new ConcurrentHashMap<String, IllegalStateException>();

    private SPILoader(Class<?> type) {
        this.type = type;
    }

    /**
     * 验证是否spi标识
     */
    private static <T> boolean isSPIAnnotaions(Class<T> type) {
        return type.isAnnotationPresent(SPI.class);
    }

    /**
     * 获取SPI加载器
     */
    @SuppressWarnings("unchecked")
    public static <T> SPILoader<T> getSPILoader(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("spi-tool type == null");
        }
        if (!type.isInterface()) {
            throw new IllegalArgumentException("spi-tool type(" + type + ") is not interface!");
        }
        if (!isSPIAnnotaions(type)) {
            throw new IllegalArgumentException(
                    "spi-tool type(" + type + ") is not spi-tool, because WITHOUT @" + SPI.class.getSimpleName() + " Annotation!");
        }

        SPILoader<T> loader = (SPILoader<T>) SPI_LOADERS.get(type);
        if (loader == null) {
            SPI_LOADERS.putIfAbsent(type, new SPILoader<T>(type));
            loader = (SPILoader<T>) SPI_LOADERS.get(type);
        }
        return loader;
    }

    private static ClassLoader findClassLoader() {
        return SPILoader.class.getClassLoader();
    }

    /**
     * 获取SPI实例
     *
     * @param name spi扩展点名字
     */
    @SuppressWarnings("unchecked")
    public T getSPI(String name) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("spi-tool name == null");
        }
        if ("true".equals(name)) {
            return getDefaultSPI();
        }
        Holder<Object> holder = cachedInstances.get(name);
        if (holder == null) {
            cachedInstances.putIfAbsent(name, new Holder<Object>());
            holder = cachedInstances.get(name);
        }
        Object instance = holder.get();
        if (instance == null) {
            synchronized (holder) {
                instance = holder.get();
                if (instance == null) {
                    instance = createSPI(name);
                    holder.set(instance);
                }
            }
        }
        return (T) instance;
    }

    /**
     * 新建一个SPI实例
     *
     * @param name SPI扩展点名字
     */
    @SuppressWarnings("unchecked")
    private T createSPI(String name) {
        Class<?> clazz = getSPIClasses().get(name);
        if (clazz == null) {
            throw findException(name);
        }
        try {
            T instance = (T) SPI_INSTANCES.get(clazz);
            if (instance == null) {
                SPI_INSTANCES.putIfAbsent(clazz, (T) clazz.newInstance());
                instance = (T) SPI_INSTANCES.get(clazz);
            }
            return instance;
        } catch (Throwable t) {
            throw new IllegalStateException(
                    "spi-tool instance(name: " + name + ", class: " + type + ")  could not be instantiated: " + t.getMessage(), t);
        }
    }

    /**
     * 查询异常
     *
     * @param name 扩展点class类全名
     */
    private IllegalStateException findException(String name) {
        for (Map.Entry<String, IllegalStateException> entry : exceptions.entrySet()) {
            if (entry.getKey().toLowerCase().contains(name.toLowerCase())) {
                return entry.getValue();
            }
        }
        StringBuilder buf = new StringBuilder("No such spi-tool " + type.getName() + " by name " + name);


        int i = 1;
        for (Map.Entry<String, IllegalStateException> entry : exceptions.entrySet()) {
            if (i == 1) {
                buf.append(", possible causes: ");
            }

            buf.append("\r\n(");
            buf.append(i++);
            buf.append(") ");
            buf.append(entry.getKey());
            buf.append(":\r\n");
            buf.append(StringUtils.toString(entry.getValue()));
        }
        return new IllegalStateException(buf.toString());
    }

    private Class<?> getSPIClass(String name) {
        if (type == null) {
            throw new IllegalArgumentException("spi-tool type == null");
        }
        if (name == null) {
            throw new IllegalArgumentException("spi-tool name == null");
        }
        Class<?> clazz = getSPIClasses().get(name);
        if (clazz == null) {
            throw new IllegalStateException("No such spi-tool \"" + name + "\" for " + type.getName() + "!");
        }
        return clazz;
    }

    private Map<String, Class<?>> getSPIClasses() {
        Map<String, Class<?>> classes = cachedClasses.get();
        if (classes == null) {
            synchronized (cachedClasses) {
                classes = cachedClasses.get();
                if (classes == null) {
                    classes = loadExtensionClasses();
                    cachedClasses.set(classes);
                }
            }
        }
        return classes;
    }

    /**
     * 加载所有spi数据
     */

    private Map<String, Class<?>> loadExtensionClasses() {
        final SPI defaultAnnotation = type.getAnnotation(SPI.class);
        if (defaultAnnotation != null) {
            String value = defaultAnnotation.value();
            if (value != null && (value = value.trim()).length() > 0) {
                String[] names = NAME_SEPARATOR.split(value);
                if (names.length > 1) {
                    throw new IllegalStateException("more than 1 default spi-tool name on spi " + type.getName() + ": " + Arrays.toString(names));
                }
                if (names.length == 1) {
                    cachedDefaultName = names[0];
                }
            }
        }

        Map<String, Class<?>> extensionClasses = new HashMap<String, Class<?>>();
        loadFile(extensionClasses, SPI_DIRECTORY);
        return extensionClasses;
    }

    public T getDefaultSPI() {
        getSPIClasses();
        if (null == cachedDefaultName || cachedDefaultName.length() == 0
            || "true".equals(cachedDefaultName)) {
            return null;
        }
        return getSPI(cachedDefaultName);
    }

    /**
     * 读取文件配置
     */
    private void loadFile(Map<String, Class<?>> extensionClasses, String dir) {
        //文件全路径
        String fileName = dir + type.getName();
        try {
            Enumeration<java.net.URL> urls;
            ClassLoader classLoader = findClassLoader();
            if (classLoader != null) {
                urls = classLoader.getResources(fileName);
            } else {
                urls = ClassLoader.getSystemResources(fileName);
            }
            if (urls == null) {
                return;
            }
            while (urls.hasMoreElements()) {
                java.net.URL url = urls.nextElement();
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"));
                    try {
                        String line = null;
                        while ((line = reader.readLine()) != null) {
                            final int ci = line.indexOf('#');
                            if (ci >= 0) {
                                line = line.substring(0, ci);
                            }
                            line = line.trim();
                            if (line.length() > 0) {
                                try {
                                    String name = null;
                                    int i = line.indexOf('=');
                                    if (i > 0) {
                                        name = line.substring(0, i).trim();
                                        line = line.substring(i + 1).trim();
                                    }
                                    if (line.length() > 0) {
                                        Class<?> clazz = Class.forName(line, true, classLoader);
                                        if (!type.isAssignableFrom(clazz)) {
                                            throw new IllegalStateException("Error when load spi class(interface: " +
                                                                            type + ", class line: " + clazz.getName() + "), class "
                                                                            + clazz.getName() + "is not subtype of interface.");
                                        }
                                        if (name != null && name.length() > 0) {
                                            if (!cachedNames.containsKey(clazz)) {
                                                cachedNames.put(clazz, name);
                                            }
                                            Class<?> c = extensionClasses.get(name);
                                            if (c == null) {
                                                extensionClasses.put(name, clazz);
                                            } else if (c != clazz) {
                                                throw new IllegalStateException(
                                                        "Duplicate spi-tool " + type.getName() + " name " + name + " on " + c.getName() + " and "
                                                        + clazz.getName());
                                            }
                                        }
                                    }
                                } catch (Throwable t) {
                                    IllegalStateException
                                            e =
                                            new IllegalStateException(
                                                    "Failed to load spi-tool class(interface: " + type + ", class line: " + line + ") in " + url
                                                    + ", cause: " + t.getMessage(), t);
                                    exceptions.put(line, e);
                                }
                            }
                        }
                    } finally {
                        reader.close();
                    }
                } catch (Throwable t) {
                    LOGGER.error("Exception when load spi-tool class(interface: " + type + ", class file: " + url + ") in " + url, t);
                }
            } // end of while urls
        } catch (Throwable t) {
            LOGGER.error("Exception when load spi-tool class(interface: " +
                         type + ", description file: " + fileName + ").", t);
        }
    }

    @Override
    public String toString() {
        return this.getClass().getName() + "[" + type.getName() + "]";
    }

}
