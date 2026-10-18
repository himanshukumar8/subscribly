package com.subscribly.paymentservice.client;

import com.subscribly.common.dto.SubscriptionDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "subscription-service", path = "/api/v1/internal/subscriptions")
public interface SubscriptionClient {

    @GetMapping("/{id}")
    SubscriptionDto getSubscriptionById(@PathVariable("id") Long id);
}
