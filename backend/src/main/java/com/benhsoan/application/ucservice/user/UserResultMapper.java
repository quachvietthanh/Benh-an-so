package com.benhsoan.application.ucservice.user;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.port.dto.result.UserResult;

@Component
public class UserResultMapper {

    public UserResult toResult(
            User user,
            Role role
    ) {

        return new UserResult(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                role.getName(),
                user.isActive()
        );
    }

}