package com.hmdp.utils;

public interface Ilock {
    boolean tryLock(long timeOutSec);
    void unLock();
}
