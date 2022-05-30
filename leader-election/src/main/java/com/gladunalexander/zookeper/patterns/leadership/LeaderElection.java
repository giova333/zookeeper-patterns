package com.gladunalexander.zookeper.patterns.leadership;

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

import static org.apache.zookeeper.Watcher.Event.EventType.NodeDeleted;

@Slf4j
public class LeaderElection implements Watcher {

    private static final String ELECTION_NAMESPACE = "/election";

    private final ZooKeeper zookeeper;
    private final OnElectionCallback onElectionCallback;
    private Node node;

    public LeaderElection(ZooKeeper zookeeper, OnElectionCallback onElectionCallback) {
        this.zookeeper = zookeeper;
        this.onElectionCallback = onElectionCallback;
        createZnodeIfNotExists(ELECTION_NAMESPACE, CreateMode.PERSISTENT);
    }

    public void start() {
        volunteerForLeadership();
        electLeader();
    }

    @SneakyThrows
    private void volunteerForLeadership() {
        var znodePrefix = ELECTION_NAMESPACE + "/c_";
        var znodeName = zookeeper.create(znodePrefix,
                                         new byte[]{},
                                         ZooDefs.Ids.OPEN_ACL_UNSAFE,
                                         CreateMode.EPHEMERAL_SEQUENTIAL);
        this.node = new Node(znodeName.replace(ELECTION_NAMESPACE + "/", ""));
        log.info("Created znode: {}", node.getName());
    }

    @Override
    public void process(WatchedEvent event) {
        if (event.getType().equals(NodeDeleted)) {
            electLeader();
        }
    }

    @SneakyThrows
    private void electLeader() {
        while (true) {
            var children = zookeeper.getChildren(ELECTION_NAMESPACE, false);

            Collections.sort(children);
            var leaderName = children.get(0);
            log.info("The leader is {}", leaderName);

            if (node.isLeader(leaderName)) {
                onElectionCallback.onLeader();
                return;
            }
            if (watchPredecessor(children)) {
                onElectionCallback.onWorker();
                return;
            }
        }
    }

    private boolean watchPredecessor(List<String> children) throws KeeperException, InterruptedException {
        var predecessorIndex = Collections.binarySearch(children, node.getName()) - 1;
        var predecessorName = children.get(predecessorIndex);
        var stat = zookeeper.exists(ELECTION_NAMESPACE + "/" + predecessorName, this);
        return stat != null;
    }

    @SneakyThrows
    private void createZnodeIfNotExists(String namespace, CreateMode type) {
        try {
            zookeeper.create(namespace,
                             new byte[]{},
                             ZooDefs.Ids.OPEN_ACL_UNSAFE,
                             type);
        } catch (KeeperException e) {
            log.info("{} namespace already exists", namespace);
        }
    }
}
