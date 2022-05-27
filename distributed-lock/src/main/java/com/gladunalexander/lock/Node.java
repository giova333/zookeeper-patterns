package com.gladunalexander.lock;

import lombok.Value;

@Value
public class Node {
    String path;
    String name;

    public Node(String path) {
        this.path = path;
        var split = path.split("/");
        this.name = split[split.length - 1];
    }

    public boolean isLockOwner(String owner) {
        return name.equals(owner);
    }
}
