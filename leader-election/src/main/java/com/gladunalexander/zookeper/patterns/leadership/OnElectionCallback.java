package com.gladunalexander.zookeper.patterns.leadership;

public interface OnElectionCallback {

    void onLeader();

    void onWorker();
}
