package com.gladunalexander.lock;

public interface LockManager {

    void lock(String key, Runnable runnable);
}
