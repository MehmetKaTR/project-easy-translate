package com.mehmetkatr.project_easy_translate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountLifecycleService {

    private final UserService userService;

    @Scheduled(cron = "0 */30 * * * *")
    public void purgeExpiredPendingDeletions() {
        userService.purgeExpiredPendingDeletions();
    }
}
