package com.gladunalexander.zookeper.patterns.leadership;

import lombok.Value;

@Value
public class Node {

    String name;

    public boolean isLeader(String leaderName) {
        return name.equals(leaderName);
    }
}
