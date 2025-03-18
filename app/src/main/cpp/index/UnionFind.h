//
// Created by 67561 on 2025/3/18.
//

#ifndef ALBUM_UNIONFIND_H
#define ALBUM_UNIONFIND_H

#include <vector>
#include <numeric>

class UnionFind {
private:
    std::vector<int> parent;
public:
    explicit UnionFind(int n);

    int find(int x);

    void merge(int x, int y);
};


#endif //ALBUM_UNIONFIND_H
