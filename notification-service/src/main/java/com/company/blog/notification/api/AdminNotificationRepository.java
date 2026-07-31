package com.company.blog.notification.api;

import java.util.List;

public interface AdminNotificationRepository {
    NotificationGovernanceOverview overview();

    List<AdminNotificationRecord> search(NotificationGovernanceQuery query);
}
