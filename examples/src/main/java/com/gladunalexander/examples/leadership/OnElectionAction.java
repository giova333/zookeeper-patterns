package com.gladunalexander.examples.leadership;

import com.gladunalexander.registry.ServiceRegistry;
import com.gladunalexander.zookeper.patterns.leadership.OnElectionCallback;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OnElectionAction implements OnElectionCallback {

    private final ServiceRegistry workersRegistry;
    private final ServiceRegistry leadersRegistry;
    private final String host;

    @Override
    public void onLeader() {
        workersRegistry.unregisterNode();
        leadersRegistry.registerNode(host);
    }

    @Override
    public void onWorker() {
        leadersRegistry.unregisterNode();
        workersRegistry.registerNode(host);
    }
}
