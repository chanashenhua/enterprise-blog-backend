package com.company.blog.article.api;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class KnowledgeCollectionMemoryRepository implements KnowledgeCollectionRepository {
    private final ConcurrentMap<String, KnowledgeCollection> collections = new ConcurrentHashMap<>();

    @Override
    public KnowledgeCollection save(KnowledgeCollection collection) {
        collections.put(collection.id(), collection);
        return collection;
    }

    @Override
    public Optional<KnowledgeCollection> findById(String collectionId) {
        return Optional.ofNullable(collections.get(collectionId));
    }

    @Override
    public List<KnowledgeCollection> findRecent(int limit) {
        return sorted().stream().limit(limit).toList();
    }

    @Override
    public List<KnowledgeCollection> findByOwnerId(String ownerId, int limit) {
        return sorted().stream()
                .filter(collection -> collection.ownerId().equals(ownerId))
                .limit(limit)
                .toList();
    }

    @Override
    public boolean deleteById(String collectionId) {
        return collections.remove(collectionId) != null;
    }

    private List<KnowledgeCollection> sorted() {
        return collections.values().stream()
                .sorted(Comparator.comparing(KnowledgeCollection::updatedAt).reversed()
                        .thenComparing(KnowledgeCollection::id))
                .toList();
    }
}
