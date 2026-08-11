package com.company.blog.article.api;

import java.util.List;
import java.util.Optional;

public interface KnowledgeCollectionRepository {
    KnowledgeCollection save(KnowledgeCollection collection);

    Optional<KnowledgeCollection> findById(String collectionId);

    List<KnowledgeCollection> findRecent(int limit);

    List<KnowledgeCollection> findByOwnerId(String ownerId, int limit);

    boolean deleteById(String collectionId);
}
