package com.uracer.racer.service;

public interface Subscriber {

    void doWhenReceivedUpdates(Object... messages) throws Exception;
}
