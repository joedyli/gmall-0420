package com.atguigu.gmall.item.service;

import java.io.IOException;
import java.util.concurrent.*;

public class ThreadTest {

    public static void main(String[] args) throws IOException {

//        CompletableFuture.runAsync(() -> {
//            System.out.println("这是通过runAsync初始化的子任务程序");
//        });

        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("这是通过supplyAsync初始化的子任务程序");
            //int i = 1/0;
            return "hello supplyAsync";
        });

        CompletableFuture<String> future1 = future.thenApplyAsync(t -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("================thenApplyAsync==a============");
            System.out.println("t: " + t);
            return "hello thenApplyAsync a";
        });
        CompletableFuture<String> future2 = future.thenApplyAsync(t -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("================thenApplyAsync==b============");
            System.out.println("t: " + t);
            return "hello thenApplyAsync b";
        });
        CompletableFuture<Void> future3 = future.thenAcceptAsync(t -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("================thenAcceptAsync==c============");
            System.out.println("t: " + t);
        });
        CompletableFuture<Void> future4 = future.thenRunAsync(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("================thenRunAsync==d============");
        });

        CompletableFuture.anyOf(future1, future2, future3, future4).join();

        System.out.println("这是主任务执行了。。。。。。");

//    .whenCompleteAsync((t, u) -> { // 上一个任务不管是正常还是异常都会执行
//            // 上一个任务的返回结果集
//            System.out.println("t: " + t);
//            // 上一个任务的异常信息
//            System.out.println("u: " + u);
//        }).exceptionally(t -> { // 上一个任务出现异常时，才会执行
//            System.out.println("t: " + t);
//            return "hello exceptionally";
//        });

//        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(3, 5, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10));
//        threadPoolExecutor.execute(() -> {
//            System.out.println("这是自定义线程池执行子任务");
//        });

//        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(3);
//        scheduledExecutorService.scheduleAtFixedRate(() -> {
//            System.out.println("这是一个定时任务");
//        }, 5, 10, TimeUnit.SECONDS);
//
//        ExecutorService executorService = Executors.newFixedThreadPool(3);
//        for (int i = 0; i < 10; i++) {
//            executorService.execute(() -> {
//                System.out.println("这是通过线程池初始化多线程程序" + Thread.currentThread().getName());
//            });
//        }

        //new MyThread().start();
        //new Thread(new MyRunnable()).start();
        // futureTask实现了Future、Runnable
//        FutureTask futureTask = new FutureTask<>(new MyCallable());
//        new Thread(futureTask).start();
//        try {
//            System.out.println(futureTask.get());
//        } catch (Exception e) {
//            System.out.println("这是主线程中捕获的子任务异常：");
//            e.printStackTrace();
//        }
    }
}

class MyCallable implements Callable<String> {
    @Override
    public String call() throws Exception {
        System.out.println("这是通过实现Callable接口方式实现多线程程序");
        int i = 1/0;
        return "hello Callable";
    }
}

class MyRunnable implements Runnable{
    @Override
    public void run() {
        System.out.println("这是通过实现Runnable接口实现了多线程程序");
    }
}

class MyThread extends Thread{
    @Override
    public void run() {
        System.out.println("这是通过继承Thread类实现多线程程序");
    }
}
