package com.wailsaid.youClone.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/**
 * VideoService
 */

@Service
public class VideoService {

    @Autowired
    private ResourceLoader loader;

    @Value("${userBucket}")
    private String userBucketPath;

    public Mono<Resource> StreamVideo() {

        return Mono.fromSupplier(() -> loader.getResource(userBucketPath));
    }

}