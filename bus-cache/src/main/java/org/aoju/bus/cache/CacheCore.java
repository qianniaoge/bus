/*
 * The MIT License
 *
 * Copyright (c) 2017 aoju.org All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.aoju.bus.cache;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.aoju.bus.cache.annotation.Cached;
import org.aoju.bus.cache.annotation.CachedGet;
import org.aoju.bus.cache.annotation.Invalid;
import org.aoju.bus.cache.entity.CacheHolder;
import org.aoju.bus.cache.entity.CacheMethod;
import org.aoju.bus.cache.entity.Expire;
import org.aoju.bus.cache.entity.Pair;
import org.aoju.bus.cache.invoker.BaseInvoker;
import org.aoju.bus.cache.reader.AbstractCacheReader;
import org.aoju.bus.cache.support.ArgNameGenerator;
import org.aoju.bus.cache.support.CacheInfoContainer;
import org.aoju.bus.cache.support.KeyGenerator;
import org.aoju.bus.cache.support.SpelCalculator;
import org.aoju.bus.logger.Logger;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

/**
 * @author Kimi Liu
 * @version 3.6.5
 * @since JDK 1.8
 */
@Singleton
public class CacheCore {

    @Inject
    private CacheConfig config;

    @Inject
    private CacheManager cacheManager;

    @Inject
    @Named("singleCacheReader")
    private AbstractCacheReader singleCacheReader;

    @Inject
    @Named("multiCacheReader")
    private AbstractCacheReader multiCacheReader;

    public static boolean isSwitchOn(CacheConfig config, Cached cached, Method method, Object[] args) {
        return doIsSwitchOn(config.getCache() == CacheConfig.Switch.ON,
                cached.expire(), cached.condition(),
                method, args);
    }

    public static boolean isSwitchOn(CacheConfig config, Invalid invalid, Method method, Object[] args) {
        return doIsSwitchOn(config.getCache() == CacheConfig.Switch.ON,
                Expire.FOREVER, invalid.condition(),
                method, args);
    }

    public static boolean isSwitchOn(CacheConfig config, CachedGet cachedGet, Method method, Object[] args) {
        return doIsSwitchOn(config.getCache() == CacheConfig.Switch.ON,
                Expire.FOREVER, cachedGet.condition(),
                method, args);
    }

    private static boolean doIsSwitchOn(boolean openStat,
                                        int expire,
                                        String condition, Method method, Object[] args) {
        if (!openStat) {
            return false;
        }

        if (expire == Expire.NO) {
            return false;
        }

        return (boolean) SpelCalculator.calcSpelValueWithContext(condition, ArgNameGenerator.getArgNames(method), args, true);
    }

    public Object read(CachedGet cachedGet, Method method, BaseInvoker baseInvoker) throws Throwable {
        Object result;
        if (isSwitchOn(config, cachedGet, method, baseInvoker.getArgs())) {
            result = doReadWrite(method, baseInvoker, false);
        } else {
            result = baseInvoker.proceed();
        }

        return result;
    }

    public Object readWrite(Cached cached, Method method, BaseInvoker baseInvoker) throws Throwable {
        Object result;
        if (isSwitchOn(config, cached, method, baseInvoker.getArgs())) {
            result = doReadWrite(method, baseInvoker, true);
        } else {
            result = baseInvoker.proceed();
        }

        return result;
    }

    public void remove(Invalid invalid, Method method, Object[] args) {
        if (isSwitchOn(config, invalid, method, args)) {

            long start = System.currentTimeMillis();

            CacheHolder cacheHolder = CacheInfoContainer.getCacheInfo(method).getLeft();
            if (cacheHolder.isMulti()) {
                Map[] pair = KeyGenerator.generateMultiKey(cacheHolder, args);
                Set<String> keys = ((Map<String, Object>) pair[1]).keySet();
                cacheManager.remove(invalid.value(), keys.toArray(new String[keys.size()]));

                Logger.info("multi cache clear, keys: {}", keys);
            } else {
                String key = KeyGenerator.generateSingleKey(cacheHolder, args);
                cacheManager.remove(invalid.value(), key);

                Logger.info("single cache clear, key: {}", key);
            }

            Logger.debug("cache clear total cost [{}] ms", (System.currentTimeMillis() - start));
        }
    }

    private Object doReadWrite(Method method, BaseInvoker baseInvoker, boolean needWrite) throws Throwable {
        long start = System.currentTimeMillis();

        Pair<CacheHolder, CacheMethod> pair = CacheInfoContainer.getCacheInfo(method);
        CacheHolder cacheHolder = pair.getLeft();
        CacheMethod cacheMethod = pair.getRight();

        Object result;
        if (cacheHolder.isMulti()) {
            result = multiCacheReader.read(cacheHolder, cacheMethod, baseInvoker, needWrite);
        } else {
            result = singleCacheReader.read(cacheHolder, cacheMethod, baseInvoker, needWrite);
        }

        Logger.debug("cache read total cost [{}] ms", (System.currentTimeMillis() - start));

        return result;
    }

    public void write() {
        // TODO on @CachedPut
    }

}
