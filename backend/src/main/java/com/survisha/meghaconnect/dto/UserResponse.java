package com.survisha.meghaconnect.dto;

import com.survisha.meghaconnect.entity.User;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String username;
    private String fullName;
    private User.UserRole role;
    private String phoneNumber;
    private boolean active;
    private boolean offlineAccess;
}
