package com.uracer.racer.service;

public interface TaskExecutor<E, V> {

    V execute(E... targets) throws Exception;
}
