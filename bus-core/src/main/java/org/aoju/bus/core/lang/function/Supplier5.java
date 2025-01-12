/*********************************************************************************
 *                                                                               *
 * The MIT License (MIT)                                                         *
 *                                                                               *
 * Copyright (c) 2015-2022 aoju.org and other contributors.                      *
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
package org.aoju.bus.core.lang.function;

import java.util.function.Supplier;

/**
 * 参数Supplier
 *
 * @param <T>  目标   类型
 * @param <P1> 参数一 类型
 * @param <P2> 参数二 类型
 * @param <P3> 参数三 类型
 * @param <P4> 参数四 类型
 * @param <P5> 参数五 类型
 * @author Kimi Liu
 * @since Java 17+
 */
@FunctionalInterface
public interface Supplier5<T, P1, P2, P3, P4, P5> {

    /**
     * 生成实例的方法
     *
     * @param p1 参数一
     * @param p2 参数二
     * @param p3 参数三
     * @param p4 参数四
     * @param p5 参数五
     * @return 目标对象
     */
    T get(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5);

    /**
     * 将带有参数的Supplier转换为无参{@link Supplier}
     *
     * @param p1 参数1
     * @param p2 参数2
     * @param p3 参数3
     * @param p4 参数4
     * @param p5 参数5
     * @return {@link Supplier}
     */
    default Supplier<T> toSupplier(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5) {
        return () -> get(p1, p2, p3, p4, p5);
    }

}
