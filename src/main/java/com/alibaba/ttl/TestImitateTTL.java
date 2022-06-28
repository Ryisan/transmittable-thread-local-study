package com.alibaba.ttl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author ruansm
 * @date 2022/6/28
 * 模仿 TTL 实现
 */
public class TestImitateTTL {


    public static void main(String[] args) {

        ThreadLocal<String> context = new ThreadLocalWrapper();
        context.set("value-set-in-parent");
        System.out.println("main1 get::"+context.get());

        ExecutorService executorService = Executors.newFixedThreadPool(1);

        Runnable task1 = new Runnable() {
            @Override
            public void run() {
                System.out.println("task1 get ::"+context.get());
                context.set("child1");
                System.out.println("task1.1 get::"+context.get());
            }
        };

        Runnable task2 = new Runnable() {
            @Override
            public void run() {
                System.out.println("task2 get::"+context.get());
                context.set("child2");
                System.out.println("task2.1 get::"+context.get());
            }
        };
        RunnableWrapper runnableWrapper1 = new RunnableWrapper(task1);
        RunnableWrapper runnableWrapper2 = new RunnableWrapper(task2);
        executorService.submit(runnableWrapper1);

        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        executorService.submit(runnableWrapper2);

        System.out.println("main1.2 get::"+context.get());
    }

    public static class RunnableWrapper implements Runnable{

        Runnable runnable;
        @Override
        public void run() {
            Snapshot snapshot = ThreadLocalWrapper.capture();
            Snapshot backup = ThreadLocalWrapper.replay(snapshot);
            try {
                runnable.run();
            } finally {
                ThreadLocalWrapper.restore(backup);
            }
        }

        public RunnableWrapper(Runnable runnable) {
            this.runnable = runnable;
        }
    }

    public static class ThreadLocalWrapper extends InheritableThreadLocal{
        static Map<ThreadLocal,Object> holder = new HashMap<>();

        @Override
        public void set(Object value) {
            super.set(value);
            holder.put(this,value);
        }

        public static Snapshot capture() {
            //抓取值时，创建快照
            HashMap<ThreadLocal,Object> map = new HashMap<>();
            for (Map.Entry<ThreadLocal, Object> entry : holder.entrySet()) {
                map.put(entry.getKey(),entry.getValue());
            }
            return new Snapshot(map);
        }

        public static Snapshot replay(Snapshot snapshot){
            Map<ThreadLocal,Object> backup = new HashMap<>();
            backup.putAll(snapshot.holder);
            for (Map.Entry<ThreadLocal, Object> entry : snapshot.holder.entrySet()) {
                ThreadLocal threadLocal = entry.getKey();
                threadLocal.set(entry.getValue());
            }
            return new Snapshot(backup);

        }

        public static void restore(Snapshot snapshot){
            Map<ThreadLocal, Object> holder = ThreadLocalWrapper.holder;
            for (Map.Entry<ThreadLocal, Object> entry : snapshot.holder.entrySet()) {
                holder.put(entry.getKey(),entry.getValue());
            }
        }

    }

    public static class Snapshot{
        Map<ThreadLocal,Object> holder;

        public Snapshot(Map<ThreadLocal, Object> holder) {
            this.holder = holder;
        }
    }

}
