package com.gym.security;

import com.gym.security.dto.SecuUserDTO;
import com.gym.security.mapper.LoginQueryMapper;  // 매퍼 함께 추가
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/** MyBatis로 회원/권한 로딩 → SecuUserDTO 구성 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final LoginQueryMapper loginQueryMapper;

    public UserDetailsServiceImpl(LoginQueryMapper loginQueryMapper){
        this.loginQueryMapper = loginQueryMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String memberId) throws UsernameNotFoundException {
        var row = loginQueryMapper.findUser(memberId); // member_id, member_pw(해시)
        if (row == null) throw new UsernameNotFoundException("존재하지 않는 회원: " + memberId);

        // DB 원본 role(user/admin/한글)을 Security 권한으로 변환
        List<String> rawRoles = loginQueryMapper.findRoles(memberId);      // ex) ["user"] or ["admin"]
        List<String> secuRoles = rawRoles.stream().map(this::toSecurityRole).toList();

        return SecuUserDTO.of(row.getMemberId(), row.getPasswordHash(), secuRoles);
    }

    private String toSecurityRole(String raw){
        return switch (raw) {
            //case "admin", "관리자", "최고관리자", "담당자" -> "ROLE_ADMIN"; // CMS권한은 ADMIN으로 통합
        	// 책임자로 변경
        	case "admin", "관리자", "책임자", "강사" -> "ROLE_ADMIN"; // CMS권한은 ADMIN으로 통합 
            case "user",  "회원" -> "ROLE_USER";
            default -> "ROLE_USER";
        };
    }
}

