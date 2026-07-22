package com.company.blog.article.api;

public interface ReviewPolicyClient {
    boolean reviewRequired(SubmitPublishRequest request);
}