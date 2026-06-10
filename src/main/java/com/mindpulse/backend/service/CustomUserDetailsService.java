package com.mindpulse.backend.service;

import com.mindpulse.backend.entity.User;
import com.mindpulse.backend.mapper.UserMapper;
import com.mindpulse.backend.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user;

        if (EMAIL_PATTERN.matcher(username).matches()) {
            user = userMapper.findByEmail(username);
            if (user == null) {
                throw new UsernameNotFoundException("User not found with email: " + username);
            }
        } else {
            user = userMapper.findByUsername(username);
            if (user == null) {
                throw new UsernameNotFoundException("User not found with username: " + username);
            }
        }

        return new CustomUserDetails(user);
    }
}
