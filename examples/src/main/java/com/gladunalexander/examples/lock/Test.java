package com.gladunalexander.examples.lock;

import com.gladunalexander.examples.ZookeeperConnectionManager;
import com.gladunalexander.lock.DefaultLockManager;

public class Test {

    public static void main(String[] args) {
        var zooKeeper = ZookeeperConnectionManager.instance().getConnection();

        var lockManager = new DefaultLockManager(zooKeeper);

        lockManager.lock("key", () -> {
            var end = System.currentTimeMillis() + 40000;
            while (System.currentTimeMillis() < end) {
                System.out.println("Processing...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException();
                }
            }
        });
    }
}
