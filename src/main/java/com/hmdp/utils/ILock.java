package com.hmdp.utils;

/**
 * ClassName: ILock
 * Package: com.hmdp.utils
 * Description:
 *
 * @Author xzh
 * @Create 2023/4/27 15:21
 */
public interface ILock {
    //获取锁
    boolean tryLock(long timeoutSec);

    //释放锁
    void unlock();
}
