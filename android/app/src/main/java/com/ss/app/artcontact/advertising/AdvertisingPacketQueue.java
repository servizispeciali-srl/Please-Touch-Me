package com.ss.app.artcontact.advertising;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class AdvertisingPacketQueue {

    LinkedList<AdvertisingPacket> queue = new LinkedList<>();
    Object semaphore = new Object();

    public void push(AdvertisingPacket advertisingPacket) {
        synchronized (semaphore) {
            queue.add(advertisingPacket);
        }
    }

    public AdvertisingPacket pop() {
        AdvertisingPacket advertisingPacket = null;
        synchronized (semaphore) {
            advertisingPacket = queue.pop();
        }
        return advertisingPacket;
    }

    public List<AdvertisingPacket> getAll() {
        List<AdvertisingPacket> packets = new ArrayList<>();
        synchronized (semaphore) {
            while (queue.size() > 0) packets.add(queue.pop());
        }
        return packets;
    }
}
