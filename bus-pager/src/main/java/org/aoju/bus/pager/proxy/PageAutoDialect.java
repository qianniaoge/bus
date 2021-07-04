/*********************************************************************************
 *                                                                               *
 * The MIT License (MIT)                                                         *
 *                                                                               *
 * Copyright (c) 2015-2021 aoju.org and other contributors.                      *
 *                                                                               *
 * Permission is hereby granted, free of charge, to any person obtaining a copy  *
 * of this software and associated documentation files (the "Software"), to deal *
 * in the Software without restriction, including without limitation the rights  *
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell     *
 * copies of the Software, and to permit persons to whom the Software is         *
 * furnished to do so, subject to the following conditions:                      *
 *                                                                               *
 * The above copyright notice and this permission notice shall be included in    *
 * all copies or substantial portions of the Software.                           *
 *                                                                               *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR    *
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,      *
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE   *
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER        *
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, *
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN     *
 * THE SOFTWARE.                                                                 *
 *                                                                               *
 ********************************************************************************/
package org.aoju.bus.pager.proxy;

import org.aoju.bus.core.lang.Symbol;
import org.aoju.bus.pager.PageException;
import org.aoju.bus.pager.dialect.AbstractDialect;
import org.aoju.bus.pager.dialect.Dialect;
import org.aoju.bus.pager.dialect.general.*;
import org.aoju.bus.pager.plugin.PageFromObject;
import org.apache.ibatis.mapping.MappedStatement;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 基础方言信息
 *
 * @author Kimi Liu
 * @version 6.2.3
 * @since JDK 1.8+
 */
public class PageAutoDialect {

    private static final Map<String, Class<? extends Dialect>> DIALECT_ALIAS_MAP = new HashMap<>();

    static {
        registerDialectAlias("db2", Db2.class);
        registerDialectAlias("herddb", HerdDB.class);

        registerDialectAlias("hsqldb", Hsqldb.class);
        registerDialectAlias("h2", Hsqldb.class);
        registerDialectAlias("postgresql", PostgreSql.class);
        registerDialectAlias("phoenix", Hsqldb.class);

        registerDialectAlias("informix", Informix.class);
        registerDialectAlias("informix-sqli", Informix.class);

        registerDialectAlias("mysql", MySql.class);
        registerDialectAlias("mariadb", MySql.class);
        registerDialectAlias("sqlite", MySql.class);
        // 神通数据库
        registerDialectAlias("oscar", MySql.class);
        registerDialectAlias("clickhouse", MySql.class);

        registerDialectAlias("oracle9i", Oracle9i.class);
        registerDialectAlias("oracle", Oracle.class);
        // 达梦数据库
        registerDialectAlias("dm", Oracle.class);
        // 阿里云PPAS数据库
        registerDialectAlias("edb", Oracle.class);
        // 神通数据库
        registerDialectAlias("oscar", Oscar.class);
        registerDialectAlias("clickhouse", MySql.class);
        // 瀚高数据库
        registerDialectAlias("highgo", Hsqldb.class);
        // 虚谷数据库
        registerDialectAlias("xugu", Hsqldb.class);

        registerDialectAlias("sqlserver", SqlServer.class);
        registerDialectAlias("sqlserver2012", SqlServer2012.class);
        registerDialectAlias("derby", SqlServer2012.class);
    }

    /**
     * 缓存
     */
    private final Map<String, AbstractDialect> urlDialectMap = new ConcurrentHashMap<>();
    private final ThreadLocal<AbstractDialect> dialectThreadLocal = new ThreadLocal<>();
    //
    private final ReentrantLock lock = new ReentrantLock();
    /**
     * 自动获取dialect,如果没有setProperties或setSqlUtilConfig,也可以正常进行
     */
    private boolean autoDialect = true;
    /**
     * 多数据源时,获取jdbcurl后是否关闭数据源
     */
    private boolean closeConn = true;
    /**
     * 属性配置
     */
    private Properties properties;
    private AbstractDialect delegate;

    public static void registerDialectAlias(String alias, Class<? extends Dialect> dialectClass) {
        DIALECT_ALIAS_MAP.put(alias, dialectClass);
    }

    /**
     * 多数据动态获取时,每次需要初始化
     *
     * @param ms 执行映射的语句
     */
    public void initDelegateDialect(MappedStatement ms) {
        if (null == delegate) {
            if (autoDialect) {
                this.delegate = getDialect(ms);
            } else {
                dialectThreadLocal.set(getDialect(ms));
            }
        }
    }

    // 获取当前的代理对象
    public AbstractDialect getDelegate() {
        if (null != delegate) {
            return delegate;
        }
        return dialectThreadLocal.get();
    }

    // 移除代理对象
    public void clearDelegate() {
        dialectThreadLocal.remove();
    }

    private String fromJdbcUrl(String jdbcUrl) {
        for (String dialect : DIALECT_ALIAS_MAP.keySet()) {
            if (jdbcUrl.indexOf(Symbol.COLON + dialect + Symbol.COLON) != -1) {
                return dialect;
            }
        }
        return null;
    }

    /**
     * 反射类
     *
     * @param className 类名称
     * @return 实体类
     * @throws Exception 异常
     */
    private Class resloveDialectClass(String className) throws Exception {
        if (DIALECT_ALIAS_MAP.containsKey(className.toLowerCase())) {
            return DIALECT_ALIAS_MAP.get(className.toLowerCase());
        } else {
            return Class.forName(className);
        }
    }

    /**
     * 初始化 general
     *
     * @param dialectClass 方言
     * @param properties   属性
     */
    private AbstractDialect initDialect(String dialectClass, Properties properties) {
        AbstractDialect dialect;
        if (PageFromObject.isEmpty(dialectClass)) {
            throw new PageException("使用 PageContext 分页插件时,必须设置 general 属性");
        }
        try {
            Class sqlDialectClass = resloveDialectClass(dialectClass);
            if (AbstractDialect.class.isAssignableFrom(sqlDialectClass)) {
                dialect = (AbstractDialect) sqlDialectClass.newInstance();
            } else {
                throw new PageException("使用 PageContext 时,方言必须是实现 " + AbstractDialect.class.getCanonicalName() + " 接口的实现类!");
            }
        } catch (Exception e) {
            throw new PageException("初始化 general [" + dialectClass + "]时出错:" + e.getMessage(), e);
        }
        dialect.setProperties(properties);
        return dialect;
    }

    /**
     * 获取url
     *
     * @param dataSource 数据源
     * @return url
     */
    private String getUrl(DataSource dataSource) {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            return conn.getMetaData().getURL();
        } catch (SQLException e) {
            throw new PageException(e);
        } finally {
            if (null != conn) {
                try {
                    if (closeConn) {
                        conn.close();
                    }
                } catch (SQLException e) {
                    //ignore
                }
            }
        }
    }

    /**
     * 根据 jdbcUrl 获取数据库方言
     *
     * @param ms the MappedStatement
     * @return dialect
     */
    private AbstractDialect getDialect(MappedStatement ms) {
        // 改为对dataSource做缓存
        DataSource dataSource = ms.getConfiguration().getEnvironment().getDataSource();
        String url = getUrl(dataSource);
        if (urlDialectMap.containsKey(url)) {
            return urlDialectMap.get(url);
        }
        try {
            lock.lock();
            if (urlDialectMap.containsKey(url)) {
                return urlDialectMap.get(url);
            }
            if (PageFromObject.isEmpty(url)) {
                throw new PageException("无法自动获取jdbcUrl,请在分页插件中配置dialect参数!");
            }
            String dialectStr = fromJdbcUrl(url);
            if (null == dialectStr) {
                throw new PageException("无法自动获取数据库类型,请通过 helperDialect 参数指定!");
            }
            AbstractDialect dialect = initDialect(dialectStr, properties);
            urlDialectMap.put(url, dialect);
            return dialect;
        } finally {
            lock.unlock();
        }
    }

    public void setProperties(Properties properties) {
        // 多数据源时,获取 jdbcurl 后是否关闭数据源
        String closeConn = properties.getProperty("closeConn");
        if (PageFromObject.isNotEmpty(closeConn)) {
            this.closeConn = Boolean.parseBoolean(closeConn);
        }
        // 使用 sqlserver2012 作为默认分页方式,这种情况在动态数据源时方便使用
        String useSqlserver2012 = properties.getProperty("useSqlserver2012");
        if (PageFromObject.isNotEmpty(useSqlserver2012) && Boolean.parseBoolean(useSqlserver2012)) {
            registerDialectAlias("sqlserver", SqlServer2012.class);
            registerDialectAlias("sqlserver2008", SqlServer.class);
        }
        String dialectAlias = properties.getProperty("dialectAlias");
        if (PageFromObject.isNotEmpty(dialectAlias)) {
            String[] alias = dialectAlias.split(Symbol.SEMICOLON);
            for (int i = 0; i < alias.length; i++) {
                String[] kv = alias[i].split(Symbol.EQUAL);
                if (kv.length != 2) {
                    throw new IllegalArgumentException("dialectAlias 参数配置错误," +
                            "请按照 alias1=xx.dialectClass;alias2=dialectClass2 的形式进行配置!");
                }
                for (int j = 0; j < kv.length; j++) {
                    try {
                        Class<? extends Dialect> diallectClass = (Class<? extends Dialect>) Class.forName(kv[1]);
                        // 允许覆盖已有的实现
                        registerDialectAlias(kv[0], diallectClass);
                    } catch (ClassNotFoundException e) {
                        throw new IllegalArgumentException("请确保 dialectAlias 配置的 Dialect 实现类存在!", e);
                    }
                }
            }
        }
        // 指定的数据库方言
        String dialect = properties.getProperty("delegate");
        // 运行时获取数据源
        String runtimeDialect = properties.getProperty("autoRuntimeDialect");
        // 1.动态多数据源
        if (PageFromObject.isNotEmpty(runtimeDialect) && "TRUE".equalsIgnoreCase(runtimeDialect)) {
            this.autoDialect = false;
            this.properties = properties;
        }
        // 2.动态获取方言
        else if (PageFromObject.isEmpty(dialect)) {
            autoDialect = true;
            this.properties = properties;
        }
        // 3.指定方言
        else {
            autoDialect = false;
            this.delegate = initDialect(dialect, properties);
        }
    }

}
