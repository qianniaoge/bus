/*********************************************************************************
 *                                                                               *
 * The MIT License (MIT)                                                         *
 *                                                                               *
 * Copyright (c) 2015-2021 aoju.org mybatis.io and other contributors.           *
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
package org.aoju.bus.mapper.additional.aggregation;

import org.aoju.bus.mapper.annotation.RegisterMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;

import java.util.List;

/**
 * 通用聚合查询接口,特殊方法
 *
 * @author Kimi Liu
 * @version 6.3.0
 * @since JDK 1.8+
 */
@RegisterMapper
public interface AggregationMapper<T> {

    /**
     * 根据condition和aggregateCondition进行聚合查询
     * 分组不支持having条件过滤， 如需要建议使用xml文件
     *
     * @param condition          条件
     * @param aggregateCondition 可以设置聚合查询的属性和分组属性
     * @return 返回聚合查询属性和分组属性的值
     */
    @SelectProvider(type = AggregationProvider.class, method = "dynamicSQL")
    List<T> selectAggregationByCondition(@Param("condition") Object condition, @Param("aggregateCondition") AggregateCondition aggregateCondition);

}