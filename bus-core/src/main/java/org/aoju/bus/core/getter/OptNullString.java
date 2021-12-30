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
package org.aoju.bus.core.getter;

import org.aoju.bus.core.convert.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

/**
 * 基本类型的getter接口抽象实现,所有类型的值获取都是通过将String转换而来
 * 用户只需实现getStr方法即可,其他类型将会从String结果中转换 在不提供默认值的情况下, 如果值不存在或获取错误,返回null
 *
 * @author Kimi Liu
 * @version 6.3.3
 * @since JDK 1.8+
 */
public interface OptNullString<K> extends OptNullType<K> {

    @Override
    default Object getObj(K key, Object defaultValue) {
        return getStr(key, null == defaultValue ? null : defaultValue.toString());
    }

    @Override
    default Integer getInt(K key, Integer defaultValue) {
        return Convert.toInt(getStr(key), defaultValue);
    }

    @Override
    default Short getShort(K key, Short defaultValue) {
        return Convert.toShort(getStr(key), defaultValue);
    }

    @Override
    default Boolean getBool(K key, Boolean defaultValue) {
        return Convert.toBool(getStr(key), defaultValue);
    }

    @Override
    default Long getLong(K key, Long defaultValue) {
        return Convert.toLong(getStr(key), defaultValue);
    }

    @Override
    default Character getChar(K key, Character defaultValue) {
        return Convert.toChar(getStr(key), defaultValue);
    }

    @Override
    default Float getFloat(K key, Float defaultValue) {
        return Convert.toFloat(getStr(key), defaultValue);
    }

    @Override
    default Double getDouble(K key, Double defaultValue) {
        return Convert.toDouble(getStr(key), defaultValue);
    }

    @Override
    default Byte getByte(K key, Byte defaultValue) {
        return Convert.toByte(getStr(key), defaultValue);
    }

    @Override
    default BigDecimal getBigDecimal(K key, BigDecimal defaultValue) {
        return Convert.toBigDecimal(getStr(key), defaultValue);
    }

    @Override
    default BigInteger getBigInteger(K key, BigInteger defaultValue) {
        return Convert.toBigInteger(getStr(key), defaultValue);
    }

    @Override
    default <E extends Enum<E>> E getEnum(Class<E> clazz, K key, E defaultValue) {
        return Convert.toEnum(clazz, getStr(key), defaultValue);
    }

    @Override
    default Date getDate(K key, Date defaultValue) {
        return Convert.toDate(getStr(key), defaultValue);
    }

}
