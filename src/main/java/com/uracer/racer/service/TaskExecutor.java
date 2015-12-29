package com.uracer.racer.service;

public interface TaskExecutor<E> {

    void execute(E... targets) throws Exception;
}
