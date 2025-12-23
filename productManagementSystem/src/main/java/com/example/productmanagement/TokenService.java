package com.example.productmanagement;

import java.util.concurrent.CompletableFuture;

public interface TokenService {
    CompletableFuture<String> getAccessToken();
}
