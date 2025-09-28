package com.gym.security.dto;

import org.springframework.security.core.GrantedAuthority;
// import org.springframework.security.core.authority.SimpleGrantedAuthority; // [삭제] 내장 타입 대신 우리 DTO 사용
import org.springframework.security.core.userdetails.UserDetails;

import lombok.ToString;

import java.util.Collection;
import java.util.List;

/** 보안 전용 사용자 DTO (기존 도메인 DTO 불변) */
@ToString // 이거 누락되서 시간 날려먹었음..ㅜㅜ
public class SecuUserDTO implements UserDetails {

    private final String userId; // = member_id (username은 userId로 고정)
    private final String passwordHash; // = member_pw(BCrypt 해시)
    // private final List<SimpleGrantedAuthority> authorities; // ["ROLE_USER","ROLE_ADMIN"] // [삭제] 내장 권한 컬렉션 제거
    private final List<SecuRoleDTO> authorities; // [수정] role을 SecuRoleDTO에 보관하도록 변경 

    // private SecuUserDTO(String userId, String passwordHash, List<String> roles){ // [삭제] 파라미터 타입 불일치(List<String> → List<SecuRoleDTO> 필요)
    //     this.userId = userId;
    //     this.passwordHash = passwordHash;
    //     this.authorities = roles.stream().map(SimpleGrantedAuthority::new).toList();
    // }
    private SecuUserDTO(String userId, String passwordHash, List<SecuRoleDTO> authorities){ // [수정] 생성자 시그니처를 필드 타입과 일치
        this.userId = userId;
        this.passwordHash = passwordHash;
        this.authorities = authorities; // [수정] 변환된 권한 리스트를 그대로 주입
    }

    public static SecuUserDTO of(String userId, String passwordHash, List<String> roles){
        // return new SecuUserDTO(userId, passwordHash, roles); // [삭제] 문자열 리스트를 그대로 넣으면 타입 불일치
        return new SecuUserDTO( // [수정] 문자열 권한을 SecuRoleDTO로 변환하여 생성자에 전달
                userId,
                passwordHash,
                roles.stream().map(SecuRoleDTO::new).toList()
        );
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities(){ return authorities; } // 시큐리티는 GrantedAuthority 컬렉션만 요구
    @Override public String getPassword(){ return passwordHash; } 
    @Override public String getUsername(){ return userId; } //  username=userId 규칙
    @Override public boolean isAccountNonExpired(){ return true; }  
    @Override public boolean isAccountNonLocked(){ return true; }  
    @Override public boolean isCredentialsNonExpired(){ return true; } 
    @Override public boolean isEnabled(){ return true; }  
}
