package mexa.club.authservice.service;

import mexa.club.authservice.entity.Role;
import mexa.club.authservice.entity.User;
import mexa.club.authservice.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // Username yoki email orqali qidirish
        User user = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmailIgnoreCase(usernameOrEmail))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + usernameOrEmail));

        List<GrantedAuthority> authorities = new ArrayList<>();

        if (user.getRoles() != null) {
            for (Role role : user.getRoles()) {
                // Role name (e.g. ROLE_SUPER_ADMIN) — used by hasRole() and hasAuthority()
                authorities.add(new SimpleGrantedAuthority(role.getName()));
                // Permission names from the role — used by hasAuthority('PERMISSION_NAME')
                if (role.getPermissions() != null) {
                    role.getPermissions().forEach(perm ->
                            authorities.add(new SimpleGrantedAuthority(perm.name()))
                    );
                }
            }
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(), // Spring Security uchun har doim username ishlatiladi
                user.getPassword(),
                user.isEnabled() && user.isVerified(),
                true, true, true,
                authorities
        );
    }
}
