package org.aoju.bus.starter.goalie.filter;

import org.aoju.bus.goalie.reactor.ExchangeContext;
import org.aoju.bus.starter.goalie.ReactorConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Objects;


/**
 * 参数过滤
 *
 * @author Justubborn
 * @since 2020/10/29
 */
@Component
@ConditionalOnBean(ReactorConfiguration.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FirstFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ExchangeContext context = new ExchangeContext();
        if (Objects.equals(request.getMethod(), HttpMethod.GET)) {
            MultiValueMap<String, String> params = request.getQueryParams();
            context.setRequestMap(params);
            exchange.getAttributes().put(ExchangeContext.$, context);
            return chain.filter(exchange);
        } else {
            return exchange.getFormData().flatMap(params -> {
                context.setRequestMap(params);
                exchange.getAttributes().put(ExchangeContext.$, context);
                return chain.filter(exchange);
            });
        }
    }

}
