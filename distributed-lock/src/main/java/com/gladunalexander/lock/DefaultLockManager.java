package com.gladunalexander.lock;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

import java.util.Collections;
import java.util.List;

@Slf4j
public class DefaultLockManager implements LockManager, Watcher {

    private static final String LOCK_NAMESPACE = "/locks";

    private final ZooKeeper zooKeeper;
    private final Object monitor = new Object();

    public DefaultLockManager(ZooKeeper zooKeeper) {
        this.zooKeeper = zooKeeper;
        createZnodeIfNotExists(LOCK_NAMESPACE);
    }

    @Override
    @SneakyThrows
    public void lock(String key, Runnable job) {
        var rootPath = LOCK_NAMESPACE + "/" + key;
        createZnodeIfNotExists(rootPath);
        var lockPath = zooKeeper.create(rootPath + "/",
                                        new byte[]{},
                                        ZooDefs.Ids.OPEN_ACL_UNSAFE,
                                        CreateMode.EPHEMERAL_SEQUENTIAL);
        var node = new Node(lockPath);
        while (true) {
            var children = zooKeeper.getChildren(rootPath, false);
            Collections.sort(children);
            if (node.isLockOwner(children.get(0))) {
                log.info("Acquired lock: {}", lockPath);
                runJob(job, rootPath, lockPath);
                return;
            }
            waitAndWatchPredecessor(rootPath, node, children);
        }
    }

    private void waitAndWatchPredecessor(String rootPath, Node node, List<String> children) throws KeeperException, InterruptedException {
        var predecessorIndex = Collections.binarySearch(children, node.getName()) - 1;
        var predecessorPath = rootPath + "/" + children.get(predecessorIndex);
        if (zooKeeper.exists(predecessorPath, this) != null) {
            synchronized (monitor) {
                log.info("Waiting for lock release from: {}", predecessorPath);
                monitor.wait();
            }
        }
    }

    private void runJob(Runnable job, String rootPath, String lockPath) throws InterruptedException {
        try {
            job.run();
        } finally {
            try {
                zooKeeper.delete(lockPath, -1);
                var keeperChildren = zooKeeper.getChildren(rootPath, false);
                if (keeperChildren.isEmpty()) {
                    zooKeeper.delete(rootPath, -1);
                }
            } catch (KeeperException e) {
                log.info("Unable to remove nod");
            }
        }
    }

    @Override
    public void process(WatchedEvent event) {
        synchronized (monitor) {
            monitor.notify();
        }
    }

    @SneakyThrows
    private void createZnodeIfNotExists(String namespace) {
        try {
            zooKeeper.create(namespace,
                             new byte[]{},
                             ZooDefs.Ids.OPEN_ACL_UNSAFE,
                             CreateMode.PERSISTENT);
        } catch (KeeperException e) {
            log.info("{} namespace already exists", namespace);
        }
    }
}
