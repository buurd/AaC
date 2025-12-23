package com.example.webshop;

import java.util.concurrent.CompletableFuture;

public interface TokenService {
    CompletableFuture<String> getAccessToken();
}
