package com.gladunalexander.examples.leadership;

import com.gladunalexander.examples.ZookeeperConnectionManager;
import com.gladunalexander.registry.ServiceRegistry;
import com.gladunalexander.zookeper.patterns.leadership.LeaderElection;
import lombok.SneakyThrows;

import java.util.Random;

public class Test {

    @SneakyThrows
    public static void main(String[] args) {
        var zooKeeper = ZookeeperConnectionManager.instance().getConnection();

        var leadersRegistry = new ServiceRegistry(zooKeeper, "/leaders");
        var workersRegistry = new ServiceRegistry(zooKeeper, "/workers");
        var onElectionAction = new OnElectionAction(workersRegistry, leadersRegistry, "localhost:" + new Random().nextInt(20000));
        var leaderElection = new LeaderElection(zooKeeper, onElectionAction);
        leaderElection.start();
    }

}
