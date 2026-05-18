package com.razonapro.razonaprobackend.security;

import com.razonapro.razonaprobackend.models.Admin;
import com.razonapro.razonaprobackend.models.Student;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@Builder
public class UserPrincipal implements UserDetails {

    private String id;
    private String programId;    // null para admins
    private String email;
    private String password;
    private String userType;     // "ADMIN" | "STUDENT"
    private Collection<? extends GrantedAuthority> authorities;

    public static UserPrincipal fromAdmin(Admin admin) {
        return UserPrincipal.builder()
            .id(admin.getAdminId())
            .email(admin.getEmail())
            .password(admin.getPasswordHash())
            .userType("ADMIN")
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
            .build();
    }

    public static UserPrincipal fromStudent(Student student) {
        return UserPrincipal.builder()
            .id(student.getStudentId())
            .programId(student.getProgramId())
            .email(student.getEmail())
            .password(student.getPasswordHash())
            .userType("STUDENT")
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
            .build();
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword()    { return password; }
    @Override public String getUsername()    { return email; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}
