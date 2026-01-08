package com.example.warehouse;

import java.util.concurrent.CompletableFuture;

public interface TokenService {
    CompletableFuture<String> getAccessToken();
}
