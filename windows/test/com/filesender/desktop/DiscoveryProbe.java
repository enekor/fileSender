package com.filesender.desktop;

import com.filesender.desktop.net.DeviceInfo;
import com.filesender.desktop.net.DiscoveryClient;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DiscoveryProbe {

    public static void main(String[] args) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicBoolean found = new AtomicBoolean();
        DiscoveryClient client = new DiscoveryClient(new DiscoveryClient.Listener() {
            @Override
            public void onDevice(DeviceInfo device) {
                System.out.println("FOUND " + device.name + " " + device.address + ":" + device.port);
                found.set(true);
            }

            @Override
            public void onFinished(String errorMessage) {
                System.out.println("FINISHED error=" + errorMessage);
                done.countDown();
            }
        });
        client.start();
        done.await(10, TimeUnit.SECONDS);
        client.stop();
        if (!found.get()) {
            System.exit(2);
        }
    }
}
