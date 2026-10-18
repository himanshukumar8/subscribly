package com.subscribly.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserRegisteredEvent extends DomainEvent {
    private Long userId;
    private String email;
    private String fullName;
    private String role;

    public UserRegisteredEvent(Long userId, String email, String fullName, String role) {
        super("USER_REGISTERED");
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }
}
