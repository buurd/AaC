package com.example.order;

import java.util.concurrent.CompletableFuture;

public interface TokenService {
    CompletableFuture<String> getAccessToken();
}
