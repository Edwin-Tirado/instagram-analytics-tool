package ec.ucsg.analytics.infrastructure.security;

import ec.ucsg.analytics.domain.model.AppUser;
import ec.ucsg.analytics.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AppUser appUser = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException(
                "Usuario no encontrado con email: " + email
            ));

        Set<SimpleGrantedAuthority> authorities = appUser.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority(role.getName().name()))
            .collect(Collectors.toSet());

        return User.builder()
            .username(appUser.getEmail())
            .password(appUser.getPassword())
            .authorities(authorities)
            .accountLocked(appUser.isLocked())
            .disabled(!appUser.isEnabled())
            .build();
    }
}
