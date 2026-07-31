package com.company.blog.stats.api;

import com.company.blog.stats.InteractionType;
import java.util.List;

public interface PersonalInteractionRepository {
    List<PersonalInteractionItem> findByUser(String userId, InteractionType type, int limit);
}
