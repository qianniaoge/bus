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
package org.aoju.bus.notify.provider.aliyun;

import org.aoju.bus.core.exception.InstrumentException;
import org.aoju.bus.core.lang.Fields;
import org.aoju.bus.core.lang.ZoneId;
import org.aoju.bus.core.toolkit.DateKit;
import org.aoju.bus.core.toolkit.StringKit;
import org.aoju.bus.http.Httpx;
import org.aoju.bus.notify.Context;
import org.aoju.bus.notify.magic.Message;
import org.aoju.bus.notify.provider.generic.GenericProperty;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 阿里云邮件
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class AliyunEmailProvider extends AliyunProvider<AliyunProperty, Context> {

    public AliyunEmailProvider(Context context) {
        super(context);
    }

    /**
     * 发送邮件逻辑处理
     *
     * @param entity 请求对象
     * @return 处理结果响应
     * @throws InstrumentException 异常信息
     */
    @Override
    public Message send(AliyunProperty entity) throws InstrumentException {
        if (StringKit.isEmpty(entity.getContent())) {
            throw new InstrumentException("Email content cannot be empty");
        } else if (StringKit.isEmpty(entity.getReceive())) {
            throw new InstrumentException("Email address cannot be empty");
        } else if (StringKit.isEmpty(entity.getSubject())) {
            throw new InstrumentException("Email subject cannot be empty");
        }

        Map<String, String> bodys = new HashMap<>();
        // 1. 系统参数
        bodys.put("SignatureMethod", "HMAC-SHA1");
        bodys.put("SignatureNonce", UUID.randomUUID().toString());
        bodys.put("AccessKeyId", context.getAppKey());
        bodys.put("SignatureVersion", "1.0");
        bodys.put("Timestamp", DateKit.format(new Date(), Fields.UTC_PATTERN, ZoneId.GMT.name()));
        bodys.put("Format", "JSON");
        // 2. 业务API参数
        bodys.put("Action", "SingleSendMail");
        bodys.put("Version", "2015-11-23");
        bodys.put("RegionId", "cn-hangzhou");

        bodys.put("Subject", entity.getSubject());
        bodys.put("FromAlias", entity.getSender());
        bodys.put("ToAddress", entity.getReceive());

        if (GenericProperty.Type.HTML.equals(entity.getType())) {
            bodys.put("HtmlBody", entity.getContent());
        } else if (GenericProperty.Type.TEXT.equals(entity.getType())) {
            bodys.put("TextBody", entity.getContent());
        }

        bodys.put("ReplyAddress", entity.getSender());
        bodys.put("ReplyToAddress", entity.getSender());
        bodys.put("ReplyAddressAlias", entity.getSender());

        bodys.put("ClickTrace", getSign(bodys));
        bodys.put("Signature", getSign(bodys));

        Map<String, Object> map = new HashMap<>();
        for (String val : bodys.keySet()) {
            map.put(specialUrlEncode(val), specialUrlEncode(bodys.get(val)));
        }
        return checkResponse(Httpx.get(entity.getUrl(), map));
    }

}