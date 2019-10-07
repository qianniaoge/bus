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
package org.aoju.bus.proxy.provider;

import org.aoju.bus.core.lang.exception.InstrumentException;
import org.aoju.bus.proxy.Provider;

/**
 * @author Kimi Liu
 * @version 3.6.5
 * @since JDK 1.8
 */
public class BeanProvider implements Provider {

    private Class beanClass;

    public BeanProvider() {
    }

    public BeanProvider(Class beanClass) {
        this.beanClass = beanClass;
    }

    public Object getObject() {
        try {
            if (beanClass == null) {
                throw new InstrumentException("No bean class provided.");
            }
            return beanClass.newInstance();
        } catch (InstantiationException e) {
            throw new InstrumentException("Class " + beanClass.getName() + " is not concrete.", e);
        } catch (IllegalAccessException e) {
            throw new InstrumentException("Constructor for class " + beanClass.getName() + " is not accessible.",
                    e);
        }
    }

    public void setBeanClass(Class beanClass) {
        this.beanClass = beanClass;
    }

}

