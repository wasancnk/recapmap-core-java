package com.recapmap.core.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class UserInfoController {
    @GetMapping("/api/userinfo")
    public Map<String, Object> getUserInfo(Authentication authentication) {
        Map<String, Object> map = new HashMap<>();
        if (authentication != null) {
            map.put("username", authentication.getName());
            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
            map.put("roles", roles);
        } else {
            map.put("username", null);
            map.put("roles", Collections.emptyList());
        }
        return map;
    }
}
