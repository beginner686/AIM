package com.ailink.security;

import com.ailink.common.ErrorCode;
import com.ailink.common.exception.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityContextUtil {

    private SecurityContextUtil() {
    }

    public static Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return Long.parseLong(authentication.getName());
    }

    public static String currentRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        Optional<? extends GrantedAuthority> authority = authentication.getAuthorities().stream().findFirst();
        if (authority.isEmpty()) {
            return "";
        }
        String value = authority.get().getAuthority();
        if (value.startsWith("ROLE_")) {
            return value.substring(5);
        }
        return value;
    }
}
