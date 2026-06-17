package com.razonapro.razonaprobackend.infrastructure.security;

import com.razonapro.razonaprobackend.domain.admin.repository.AdminRepository;
import com.razonapro.razonaprobackend.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AdminRepository adminRepository;
    private final StudentRepository studentRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Try admin first, then student
        var admin = adminRepository.findByEmail(username);
        if (admin.isPresent()) {
            return UserPrincipal.fromAdmin(admin.get());
        }

        var student = studentRepository.findByEmail(username);
        if (student.isPresent()) {
            return UserPrincipal.fromStudent(student.get());
        }

        throw new UsernameNotFoundException("Usuario no encontrado: " + username);
    }
}
