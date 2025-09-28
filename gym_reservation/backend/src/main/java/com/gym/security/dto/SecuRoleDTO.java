package com.gym.security.dto;

import org.springframework.security.core.GrantedAuthority;

import lombok.ToString;

/** 보안 권한 DTO : "ROLE_ADMIN", "ROLE_USER" 등 한 건의 권한 표현 */
@ToString // 이거 누락되서 시간 날려먹었음..ㅜㅜ
public class SecuRoleDTO implements GrantedAuthority {

    private final String roleName;  // 예: "ROLE_ADMIN"

    public SecuRoleDTO(String roleName) {
        this.roleName = roleName;  // 생성 시 불변 세팅
    }
    @Override
    public String getAuthority() { // 시큐리티 규약 메서드
        return roleName; // 내부 문자열 그대로 반환
    }
}

