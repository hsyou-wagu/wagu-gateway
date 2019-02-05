package com.hsyou.wagugateway.model;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TokenClaim {

    private long userId;
    private String userEmail;
}
