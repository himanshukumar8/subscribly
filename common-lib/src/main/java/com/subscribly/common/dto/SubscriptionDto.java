package com.subscribly.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionDto implements Serializable {
    private Long id;
    private Long userId;
    private String userEmail;
    private String plan;
    private Double price;
    private String billingCycle;
    private String status;
    private String startDate;
    private String endDate;
}
