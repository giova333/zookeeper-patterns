package com.gladunalexander.registry;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ServiceRegistry implements Watcher {

    private final ZooKeeper zooKeeper;
    private final String registryZnode;
    private List<String> addresses = new ArrayList<>();
    private Node node;

    public ServiceRegistry(ZooKeeper zooKeeper, String registryZnode) {
        this.zooKeeper = zooKeeper;
        this.registryZnode = registryZnode;
        createServiceRegistryNode();
    }

    @SneakyThrows
    public void registerNode(String host) {
        var znodeName = zooKeeper.create(registryZnode + "/n_",
                                         host.getBytes(StandardCharsets.UTF_8),
                                         ZooDefs.Ids.OPEN_ACL_UNSAFE,
                                         CreateMode.EPHEMERAL_SEQUENTIAL);
        log.info("Registered znode {} with host {}", znodeName, host);
        node = new Node(znodeName);
    }

    @SneakyThrows
    public void unregisterNode() {
        if (node != null && zooKeeper.exists(node.getName(), false) != null) {
            zooKeeper.delete(node.getName(), -1);
        }
    }

    @SneakyThrows
    public List<String> getAllAddresses() {
        updateAddresses();
        return this.addresses;
    }

    @SneakyThrows
    private synchronized void updateAddresses() {
        var children = zooKeeper.getChildren(registryZnode, this);
        var addresses = new ArrayList<String>();
        for (String child : children) {
            var serviceFullPath = registryZnode + "/" + child;
            var stats = zooKeeper.exists(serviceFullPath, false);
            if (stats == null) continue;
            byte[] hostInBytes = zooKeeper.getData(serviceFullPath, false, stats);
            addresses.add(new String(hostInBytes));
        }
        this.addresses = addresses;
    }

    @SneakyThrows
    private void createServiceRegistryNode() {
        if (zooKeeper.exists(registryZnode, false) == null) {
            zooKeeper.create(registryZnode, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
    }

    @Override
    public void process(WatchedEvent event) {
        updateAddresses();
    }
}
