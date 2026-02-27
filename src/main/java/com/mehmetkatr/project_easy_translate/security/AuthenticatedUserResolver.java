package com.mehmetkatr.project_easy_translate.security;

import com.mehmetkatr.project_easy_translate.entity.User;
import com.mehmetkatr.project_easy_translate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticatedUserResolver {

    private final UserRepository userRepository;

    public Long resolveUserId(Long requestedUserId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Unauthorized request");
        }

        String username = authentication.getName();
        User authenticatedUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));

        if (requestedUserId != null && !authenticatedUser.getId().equals(requestedUserId)) {
            throw new AccessDeniedException("You cannot access another user's resources");
        }

        return authenticatedUser.getId();
    }
}
