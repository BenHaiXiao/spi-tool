package com.github.benhaixiao.spi.tool.util;

/**
 * @author xiaobenhai
 * Date: 2016/11/23
 * Time: 12:54
 */
public class Holder<T> {
private volatile T value;

public void set(T value) {
        this.value = value;
        }

public T get() {
        return value;
        }
}
