package com.goservi.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummary {
    private Long id;
    private String email;
    private String fullName;
    private String photoUrl;
    private String phone;
    private String role;
    private boolean firstAdCreated;
    private boolean onboardingSeen;
}