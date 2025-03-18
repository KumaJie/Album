//
// Created by 67561 on 2025/3/18.
//

#include "UnionFind.h"

UnionFind::UnionFind(int n) : parent(n) {
    std::iota(parent.begin(), parent.end(), 0);
}

int UnionFind::find(int x) {
    if (parent[x] != x) {
        parent[x] = find(parent[x]);
    }
    return parent[x];
}

void UnionFind::merge(int x, int y) {
    int rootX = find(x);
    int rootY = find(y);
    if (rootX == rootY) {
        return;
    }
    parent[rootX] = rootY;
}
