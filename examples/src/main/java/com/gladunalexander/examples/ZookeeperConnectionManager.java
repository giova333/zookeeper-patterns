package com.gladunalexander.examples;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ZookeeperConnectionManager implements Watcher {

    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000;

    public static final ZookeeperConnectionManager ZOOKEEPER_CONNECTION_MANAGER = new ZookeeperConnectionManager();

    private ZooKeeper zooKeeper;

    public static ZookeeperConnectionManager instance() {
        return ZOOKEEPER_CONNECTION_MANAGER;
    }

    @SneakyThrows
    public synchronized ZooKeeper getConnection() {
        if (zooKeeper == null) {
            this.zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, this);
        }
        return zooKeeper;
    }

    @Override
    @SneakyThrows
    public void process(WatchedEvent event) {
        if (event.getType().equals(Event.EventType.None)) {
            if (event.getState().equals(Event.KeeperState.SyncConnected)) {
                log.info("Connection to Zookeeper established...");
            } else {
                zooKeeper.close();
            }
        }
    }
}
