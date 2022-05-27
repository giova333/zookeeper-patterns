package com.gladunalexander.examples.leadership;

import com.gladunalexander.registry.ServiceRegistry;
import com.gladunalexander.zookeper.patterns.leadership.LeaderElection;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.Random;

@Slf4j
public class Application implements Watcher {

    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000;

    private final Object monitor = new Object();
    private ZooKeeper zookeeper;

    @SneakyThrows
    public static void main(String[] args) {
        var application = new Application();
        var zooKeeper = application.connectToZookeeper();

        var leadersRegistry = new ServiceRegistry(zooKeeper, "/leaders");
        var workersRegistry = new ServiceRegistry(zooKeeper, "/workers");
        var onElectionAction = new OnElectionAction(workersRegistry, leadersRegistry, "localhost:" + new Random().nextInt(20000));
        var leaderElection = new LeaderElection(zooKeeper, onElectionAction);
        leaderElection.start();

        application.runUntilDisconnect();
        application.close();
    }

    public ZooKeeper connectToZookeeper() throws IOException {
        this.zookeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, this);
        return zookeeper;
    }

    public void runUntilDisconnect() throws InterruptedException {
        synchronized (monitor) {
            monitor.wait();
        }
    }

    public void close() throws InterruptedException {
        zookeeper.close();
    }

    @Override
    public void process(WatchedEvent event) {
        if (event.getType().equals(Event.EventType.None)) {
            if (event.getState().equals(Event.KeeperState.SyncConnected)) {
                log.info("Connection to Zookeeper established...");
            } else {
                synchronized (monitor) {
                    log.info("Disconnected from Zookeeper");
                    monitor.notify();
                }
            }
        }
    }
}
