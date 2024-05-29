# Java基于内存的消息队列实现

项目地址：https://gitee.com/hyxl-520/memory_message_queue

## 需求背景

需求来源于我写的一个单体应用方便使用的动态任务调度框架（详细内容见Gitee地址：https://gitee.com/hyxl-520/autojob.git），该框架需要实现对运行的任务的日志记录。我采用的方案是Log4j2的自定义Appender和Filter，但是如何有序、高效的采集Appender获得的日志是一个问题，因此我拟采用基于内存的消息队列。本博客只讨论消息队列实现方式，不讨论动态任务调度框架的使用方式。

## 实现的功能

1. 支持可阻塞的消息生产和消费。
2. 支持TTL（即消息可设置过期时长）。
3. 支持单播和多播。

## 拟采用的数据结构

根据需求和功能，拟采用Java的`LinkedBlockingQueue`，具体原因如下：

1. `LinkedBlockingQueue`具有可阻塞、线程安全的优点。
2. `LinkedBlockingQueue`具有链式结构，由于要实现TTL，即会经常的删除消息队列中的已过期消息，相比`ArrayBlockingQueue`，链式结构删除复杂度更低

完整数据结构如下：

```java
Map<String, MessageQueue<MessageEntry<M>>> messageQueues;

public static class MessageQueue<M> {
        private final BlockingQueue<M> messageQueue;

        public MessageQueue() {
            messageQueue = new LinkedBlockingQueue<>();
        }


        public MessageQueue(int maxLength) {
            if (maxLength <= 0) {
                throw new IllegalArgumentException("最大容量应该为非负数");
            }
            messageQueue = new LinkedBlockingDeque<>(maxLength);
        }

        public M takeMessageSync() throws InterruptedException {
            return messageQueue.take();
        }

        public M takeMessage() {
            return messageQueue.poll();
        }

        public M readMessage() {
            return messageQueue.peek();
        }

        public M tryReadMessage() {
            return messageQueue.element();
        }

        public boolean removeIf(Predicate<? super M> predicate) {
            if (predicate == null) {
                return false;
            }
            return messageQueue.removeIf(predicate);
        }

        public int length() {
            return messageQueue.size();
        }

        public boolean remove(M message) {
            if (message == null) {
                return false;
            }
            return messageQueue.remove(message);
        }

        public Iterator<M> iterator() {
            return messageQueue.iterator();
        }

        private boolean publishMessageSync(M message) throws InterruptedException {
            if (message == null) {
                return false;
            }
            try {
                messageQueue.put(message);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }

        private boolean publishMessage(M message) {
            if (message == null) {
                return false;
            }
            try {
                return messageQueue.offer(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

    }

public static class MessageEntry<M> {
        long messageId;
        M message;
        long expiringTime;

        public MessageEntry(long messageId, M message, long expiringTime) {
            this.messageId = messageId;
            this.message = message;
            this.expiringTime = expiringTime;
        }

        public MessageEntry() {
        }
    }
```

Map采用HashTable，确保线程安全。其中`MessageQueue`封装了`LinkedBlockingQueue`，作为容器的内部类，其中生产者方法为私有，保证容器可以访问其生产者方法，但是消费者在外部订阅后只能访问消费者的方法。

`MessageEntry`是消息条目的封装，除了消息内容，还包含消息ID以及过期时间。

## 模块设计

`IMessageQueueContext`：`IMessageQueueContext`是消息容器，所有的消息都将放在这个类里面，其实现了`IProducer`和`ICounsumer`接口，即提供了生产者和消费者的所有功能。并且通过创建守护线程实现了TTL功能。过期监听策略分为两种（`ExpirationListenerPolicy`）：

1. 单线程遍历所有topic的所有消息
2. 每个topic并发遍历

前者适合主题少、总消息数少的情况，后者适合主题较多、消息总数较多的情况。

使用工厂模式，主要配置属性有：

```java
	/**
     * 默认过期时间：ms，对所有消息设置，-1则表示消息均为永久性消息，除非消费者取出，否则将会一直存在。谨慎使用！
     */
    private long defaultExpiringTime = -1;

    /**
     * 是否允许设置单个消息的过期时间
     */
    private boolean isAllowSetEntryExpired = false;

    /**
     * 允许的最大主题数
     */
    private int allowMaxTopicCount = 255;

    /**
     * 允许每个队列的最大消息数
     */
    private int allowMaxMessageCountPerQueue;

    /**
     * 过期监听器的策略
     */
    private ExpirationListenerPolicy listenerPolicy;
```

通过修改属性值可以构建出单播和多播的生产者消费者。

一个消息容器可供多个生产者和消费者使用。

提供topic过期功能，当消费者订阅后会统计一个订阅数目，当消费者取消订阅时如果当前topic的订阅数和消息堆积数为0，若指定时间内没有生产者生产消息将会移除该主题。

`MessageProducer`：`MessageProducer`为生产者，实现了`IProducer`接口（其实最终实现来自于`IMessageQueueContext`类），其提供如图上的方法。构建时需要指定所使用的消息容器。

`MessageConsumer`：`MessageConsumer`为消费者，实现了`IConsumer`接口（其实最终实现来自于`IMessageQueueContext`类）。对于消息消费强烈建议不要使用如下参数列表`String topic`的方法，这个方法将不能使用消息容器提供的topic过期功能，建议先订阅指定队列，再从该队列获取消息，生产者对于队列订阅也提供了阻塞功能。

## 拓展

消息容器的队列可采用多种实现，如采用`DelayQueue`可以实现死信队列。

### 完整源代码

`IMessageQueueContext`

```java
package com.example.autojob.skeleton.mq;


/**
 * @Description
 * @Auther Huang Yongxiang
 * @Date 2022/03/20 9:57
 */
public interface IMessageQueueContext<M> {

    int length(String topic);

    /**
     * 使得消息立即过期
     *
     * @param topic 主题
     * @return void
     * @throws ErrorExpiredException 过期时发生异常抛出
     * @author Huang Yongxiang
     * @date 2022/3/20 11:31
     */
    void expire(String topic, MessageQueueContext.MessageEntry<M> messageEntry) throws ErrorExpiredException;

    /**
     * 摧毁消息容器并启动垃圾清理
     *
     * @return void
     * @author Huang Yongxiang
     * @date 2022/3/22 14:33
     */
    void destroy();

}

```

`IConsumer`

```java
package com.example.autojob.skeleton.mq;

import java.util.concurrent.TimeUnit;

/**
 * @Description
 * @Auther Huang Yongxiang
 * @Date 2022/03/21 14:20
 */
public interface IConsumer<M> {
    /**
     * 阻塞的获取一条消息，可以决定是否将该消息取出，即移出队列
     *
     * @param topic     主题
     * @param isTakeout 是否移出队列，当为false时该方法将退化成非阻塞的
     * @return M
     * @author Huang Yongxiang
     * @date 2022/3/20 10:55
     */
    M takeMessageBlock(final String topic, final boolean isTakeout);

    M takeMessageNoBlock(final String topic, final boolean isTakeout);

    MessageQueueContext.MessageQueue<MessageQueueContext.MessageEntry<M>> subscriptionMessage(String topic);

    /**
     * 阻塞的尝试订阅指定消息队列，如果存在则立即返回，否则将会等待指定时长，若期间创建则会立即返回，否则等
     * 到结束返回null
     *
     * @param topic 主题
     * @param wait  要阻塞获取的时长
     * @param unit  wait的时间单位
     * @return com.example.autojob.skeleton.mq.MessageQueueContext.MessageQueue<com.example.autojob.skeleton.mq.MessageQueueContext.MessageEntry < M>>
     * @author Huang Yongxiang
     * @date 2022/3/22 14:32
     */
    MessageQueueContext.MessageQueue<MessageQueueContext.MessageEntry<M>> subscriptionMessage(String topic, long wait, TimeUnit unit);

    void unsubscribe(String topic);

    void unsubscribe(String topic,long wait,TimeUnit unit);
}

```

`IProducer`

```java
package com.example.autojob.skeleton.mq;

import java.util.concurrent.TimeUnit;

/**
 * @Description
 * @Auther Huang Yongxiang
 * @Date 2022/03/21 14:19
 */
public interface IProducer<M> {

    /**
     * 注册一个消息队列
     *
     * @param topic 主题名
     * @return boolean
     * @author Huang Yongxiang
     * @date 2022/3/20 9:59
     */
    boolean registerMessageQueue(String topic);

    boolean hasTopic(String topic);

    boolean removeMessageQueue(String topic);
    /**
     * 非阻塞的发布一条消息，当容量已满时立即返回
     *
     * @param message 消息内容
     * @param topic   队列主题
     * @return boolean
     * @author Huang Yongxiang
     * @date 2022/3/20 9:59
     */
    boolean publishMessageNoBlock(final M message, final String topic);

    /**
     * 非阻塞的发布一条消息，同时指定其过期时间，当容量已满时立即返回
     *
     * @param message      消息内容
     * @param topic        主题
     * @param expiringTime 过期时长
     * @param unit         时间单位
     * @return boolean
     * @author Huang Yongxiang
     * @date 2022/3/20 10:09
     */
    boolean publishMessageNoBlock(final M message, final String topic, final long expiringTime, final TimeUnit unit);

    /**
     * 阻塞的发布一条消息，当容量已满时等待空出
     *
     * @param message 消息内容
     * @param topic   队列主题
     * @return boolean
     * @author Huang Yongxiang
     * @date 2022/3/20 10:03
     */
    boolean publishMessageBlock(final M message, final String topic);

    boolean publishMessageBlock(final M message, final String topic, final long expiringTime, final TimeUnit unit);
}

```

`ErrorExpiredException`

```java
package com.example.autojob.skeleton.mq;

/**
 * @Description 消息过期异常类
 * @Auther Huang Yongxiang
 * @Date 2022/03/20 11:32
 */
public class ErrorExpiredException extends RuntimeException{
    public ErrorExpiredException() {
        super();
    }

    public ErrorExpiredException(String message) {
        super(message);
    }

    public ErrorExpiredException(String message, Throwable cause) {
        super(message, cause);
    }

    public ErrorExpiredException(Throwable cause) {
        super(cause);
    }

    protected ErrorExpiredException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

```

`ExpirationListenerPolicy`

```java
package com.example.autojob.skeleton.mq;

/**
 * @Description 过期策略
 * @Auther Huang Yongxiang
 * @Date 2022/03/21 9:30
 */
public enum ExpirationListenerPolicy {
    /**
     * 单线程监听过期
     */
    SINGLE_THREAD,
    /**
     * 按照主题并发监听过期，总消息数目过多时采取该方式可以使得效率更高，注意topic过多会占用大量线程资源
     */
    TOPIC_CONCURRENCY
}

```

`MessageConsumer`

```java
package com.example.autojob.skeleton.mq;

import java.util.concurrent.TimeUnit;

/**
 * @Description 消费者
 * @Auther Huang Yongxiang
 * @Date 2022/03/21 12:53
 */
public class MessageConsumer<M> implements IConsumer<M> {
    private final MessageQueueContext<M> messageQueueContext;

    public MessageConsumer(MessageQueueContext<M> messageQueueContext) {
        this.messageQueueContext = messageQueueContext;
    }


    @Override
    public M takeMessageBlock(String topic, boolean isTakeout) {
        if (messageQueueContext == null) {
            throw new NullPointerException("消息容器为空");
        }
        return messageQueueContext.takeMessageBlock(topic, isTakeout);
    }

    @Override
    public M takeMessageNoBlock(String topic, boolean isTakeout) {
        if (messageQueueContext == null) {
            throw new NullPointerException("消息容器为空");
        }
        return messageQueueContext.takeMessageNoBlock(topic, isTakeout);
    }

    @Override
    public MessageQueueContext.MessageQueue<MessageQueueContext.MessageEntry<M>> subscriptionMessage(String topic) {
        return messageQueueContext.subscriptionMessage(topic);
    }

    @Override
    public MessageQueueContext.MessageQueue<MessageQueueContext.MessageEntry<M>> subscriptionMessage(String topic, long wait, TimeUnit unit) {
        return messageQueueContext.subscriptionMessage(topic, wait, unit);
    }

    @Override
    public void unsubscribe(String topic) {
        messageQueueContext.unsubscribe(topic);
    }

    @Override
    public void unsubscribe(String topic, long wait, TimeUnit unit) {
        messageQueueContext.unsubscribe(topic, wait, unit);
    }
}

```

`MessageProducer`

```java
package com.example.autojob.skeleton.mq;

import lombok.Data;
import java.util.concurrent.TimeUnit;

/**
 * @Description 生产者
 * @Auther Huang Yongxiang
 * @Date 2022/03/21 12:54
 */
@Data
public class MessageProducer<M> implements IProducer<M> {
    private MessageQueueContext<M> queueContext;
    private static final int maxMessageCount = 1000;
    private static final long expiringTime = -1;

    public MessageProducer(final MessageQueueContext<M> messageQueueContext) {
        this.queueContext = messageQueueContext;
    }

    public MessageProducer() {
        queueContextBuild();
    }

    public MessageQueueContext<M> getMessageQueueContext() {
        if (queueContext == null) {
            queueContextBuild();
        }
        return queueContext;
    }

    private void queueContextBuild() {
        queueContext = MessageQueueContext.builder().setAllowMaxMessageCountPerQueue(maxMessageCount).setDefaultExpiringTime(expiringTime).setAllowMaxTopicCount(1).setListenerPolicy(ExpirationListenerPolicy.SINGLE_THREAD).setAllowSetEntryExpired(true).build();
    }


    @Override
    public boolean registerMessageQueue(String topic) {
        return queueContext.registerMessageQueue(topic);
    }

    @Override
    public boolean hasTopic(String topic) {
        return queueContext.hasTopic(topic);
    }

    @Override
    public boolean removeMessageQueue(String topic) {
        return queueContext.removeMessageQueue(topic);
    }

    @Override
    public boolean publishMessageNoBlock(M message, String topic) {
        return queueContext.publishMessageNoBlock(message, topic);
    }

    @Override
    public boolean publishMessageNoBlock(M message, String topic, long expiringTime, TimeUnit unit) {
        return queueContext.publishMessageNoBlock(message, topic, expiringTime, unit);
    }

    @Override
    public boolean publishMessageBlock(M message, String topic) {
        return queueContext.publishMessageBlock(message, topic);
    }

    @Override
    public boolean publishMessageBlock(M message, String topic, long expiringTime, TimeUnit unit) {
        return queueContext.publishMessageBlock(message, topic, expiringTime, unit);
    }
}

```

`MessageQueueContext`

```java
package com.example.autojob.skeleton.mq;

import com.example.autojob.util.convert.StringUtils;
import com.example.autojob.util.id.IdGenerator;
import com.example.autojob.util.id.SystemClock;
import com.example.autojob.util.thread.ScheduleTaskUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.swing.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/**
 * @Description 内存消息队列的context
 * @Auther Huang Yongxiang
 * @Date 2022/03/18 17:14
 */
@Slf4j
public class MessageQueueContext<M> implements IMessageQueueContext<M>, IProducer<M>, IConsumer<M> {

    /**
     * 默认过期时间：ms，对所有消息设置，-1则表示消息均为永久性消息，除非消费者取出，否则将会一直存在。谨慎使用！
     */
    private long defaultExpiringTime = -1;

    /**
     * 是否允许设置单个消息的过期时间
     */
    private boolean isAllowSetEntryExpired = false;

    /**
     * 允许的最大主题数
     */
    private int allowMaxTopicCount = 255;

    /**
     * 允许每个队列的最大消息数
     */
    private int allowMaxMessageCountPerQueue;

    /**
     * 过期监听器的策略
     */
    private ExpirationListenerPolicy listenerPolicy;

    //存储消息的数据结构
    private Map<String, MessageQueue<MessageEntry<M>>> messageQueues;

    private boolean isOpenListener = false;

    /**
     * 守护线程
     */
    private ScheduledExecutorService executorService;

    /**
     * 各个主题的订阅数
     */
    private Map<String, AtomicLong> subscriptionCount;

    public static Builder<Object> builder() {
        return new Builder<>();
    }

    private MessageQueueContext() {

    }

    @PostConstruct
    public void init() {

    }

    @Override
    public boolean registerMessageQueue(String topic) {
        if (StringUtils.isEmpty(topic)) {
            log.error("创建队列失败，主题为空");
            return false;
        }
        if (messageQueues.containsKey(topic)) {
            return false;
        }
        if (messageQueues.size() >= allowMaxTopicCount) {
            log.error("当前消息容器最大支持{}个主题", allowMaxTopicCount);
            return false;
        }
        try {
            MessageQueue<MessageEntry<M>> messageQueue = new MessageQueue<>(allowMaxMessageCountPerQueue);
            messageQueues.put(topic, messageQueue);
            if (!isOpenListener) {
                if (listenerPolicy == ExpirationListenerPolicy.SINGLE_THREAD) {
                    scheduleExpiringCheckSingleThread();
                } else {
                    scheduleExpiringCheckTopicConcurrency();
                }
                isOpenListener = true;
            }
            if (subscriptionCount == null) {
                subscriptionCount = new Hashtable<>();
            }
            subscriptionCount.put(topic, new AtomicLong(0));
            return true;
        } catch (Exception e) {
            log.error("创建队列发生异常：{}", e.getMessage());
        }
        return false;
    }

    @Override
    public boolean hasTopic(String topic) {
        return messageQueues.containsKey(topic);
    }

    @Override
    public boolean removeMessageQueue(String topic) {
        try {
            messageQueues.remove(topic);
            System.gc();
            return true;
        } catch (Exception e) {
            log.error("移除消息队列时发生异常：{}", e.getMessage());
        }
        return false;
    }

    @Override
    public boolean publishMessageNoBlock(M message, String topic, long expiringTime, TimeUnit unit) {
        if (!isAllowSetEntryExpired) {
            log.error("不允许设置单个消息的过期时间");
            return false;
        }
        if (!messageQueues.containsKey(topic)) {
            log.error("发布非阻塞消息失败，所要发布到的队列：{}不存在", topic);
            return false;
        }
        if (expiringTime <= 0 || unit == null) {
            log.error("非法过期时间");
            return false;
        }
        if (message == null) {
            log.error("禁止发布空消息");
            return false;
        }
        MessageEntry<M> messageEntry = new MessageEntry<>();
        messageEntry.setMessageId(IdGenerator.getNextIdAsLong());
        messageEntry.setMessage(message);
        messageEntry.setExpiringTime(unit.toMillis(expiringTime) + SystemClock.now());
        return messageQueues.get(topic).publishMessage(messageEntry);
    }

    @Override
    public boolean publishMessageBlock(M message, String topic, long expiringTime, TimeUnit unit) {
        if (!isAllowSetEntryExpired) {
            log.error("不允许设置单个消息的过期时间");
            return false;
        }
        if (!messageQueues.containsKey(topic)) {
            log.error("发布非阻塞消息失败，所要发布到的队列：{}不存在", topic);
            return false;
        }
        if (expiringTime <= 0 || unit == null) {
            log.error("非法过期时间");
            return false;
        }
        if (message == null) {
            log.error("禁止发布空消息");
            return false;
        }
        MessageEntry<M> messageEntry = new MessageEntry<>();
        messageEntry.setMessageId(IdGenerator.getNextIdAsLong());
        messageEntry.setMessage(message);
        messageEntry.setExpiringTime(unit.toMillis(expiringTime) + SystemClock.now());
        try {
            return messageQueues.get(topic).publishMessageSync(messageEntry);
        } catch (InterruptedException e) {
            log.warn("发布可阻塞消息发生异常，等待时被异常占用：{}", e.getMessage());
        }
        return false;
    }

    /**
     * 阻塞的获取一条消息，可以决定是否将该消息取出，即移出队列，当多播时最好不要移出队列
     *
     * @param topic     主题
     * @param isTakeout 是否移出队列，当为false时该方法将退化成非阻塞的
     * @return M
     * @author Huang Yongxiang
     * @date 2022/3/20 10:55
     */
    @Override
    public M takeMessageBlock(String topic, boolean isTakeout) {
        if (!messageQueues.containsKey(topic)) {
            return null;
        }
        if (isTakeout) {
            try {
                return messageQueues.get(topic).takeMessageSync().message;
            } catch (InterruptedException e) {
                log.warn("可阻塞获取消息发生异常，等待时被异常占用：{}", e.getMessage());
            }
            return null;
        }
        return messageQueues.get(topic).readMessage().message;
    }

    @Override
    public M takeMessageNoBlock(String topic, boolean isTakeout) {
        if (!messageQueues.containsKey(topic)) {
            return null;
        }
        if (isTakeout) {
            return messageQueues.get(topic).takeMessage().message;
        }
        return messageQueues.get(topic).readMessage().message;
    }

    @Override
    public MessageQueue<MessageEntry<M>> subscriptionMessage(String topic) {
        MessageQueue<MessageEntry<M>> messageQueue = messageQueues.get(topic);
        if (messageQueue != null && subscriptionCount != null) {
            AtomicLong atomicLong = subscriptionCount.get(topic);
            atomicLong.incrementAndGet();
        }
        return messageQueue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public MessageQueue<MessageEntry<M>> subscriptionMessage(String topic, long wait, TimeUnit unit) {
        MessageQueue<MessageEntry<M>> messageQueue = subscriptionMessage(topic);
        if (messageQueue != null) {
            return messageQueue;
        }
        //进行阻塞获取
        ScheduledFuture<Object> future = ScheduleTaskUtil.build(false, "subscriptionBlock").EOneTimeTask(() -> {
            long blockTime = unit.toMillis(wait);
            int i = 0;
            try {
                do {
                    if (hasTopic(topic)) {
                        return messageQueues.get(topic);
                    }
                    Thread.sleep(1);
                } while (i++ <= blockTime);
                return null;
            } catch (Exception e) {
                log.error("阻塞订阅时发生异常：{}", e.getMessage());
            }
            return null;
        }, 1, TimeUnit.MILLISECONDS);
        try {
            return (MessageQueue<MessageEntry<M>>) future.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("阻塞订阅时发生异常：{}", e.getMessage());
        }
        return null;
    }

    @Override
    public void unsubscribe(String topic) {
        unsubscribe(topic, 5, TimeUnit.SECONDS);
    }

    @Override
    public void unsubscribe(String topic, long wait, TimeUnit unit) {
        if (subscriptionCount != null) {
            AtomicLong atomicLong = subscriptionCount.get(topic);
            atomicLong.decrementAndGet();
            if (atomicLong.get() < 0) {
                atomicLong.set(0);
            }
            //当有队列取消订阅，且目前消息数为0，则对指定队列监视5秒，5秒后依然没有生产者发布消息则直接移除主题
            if (atomicLong.get() == 0 && length(topic) == 0) {
                long w = unit.toMillis(wait);
                log.debug("主题为{}的消息队列目前订阅数为0且积压消息为0，当{}ms后若无生产者发布消息将自动删除该主题队列", topic, w);
                ScheduleTaskUtil.build(true, "temporaryProducerListener").EOneTimeTask(() -> {
                    int i = 0;
                    boolean flag = true;
                    do {
                        if (length(topic) > 0) {
                            flag = false;
                            break;
                        }
                        Thread.sleep(1);
                    } while (i++ <= w);
                    if (flag) {
                        log.debug("主题：{}自动删除完成", topic);
                        removeMessageQueue(topic);
                    }
                    return flag;
                }, 1, TimeUnit.MILLISECONDS);
            }
        }
    }

    @Override
    public int length(String topic) {
        if (messageQueues.containsKey(topic)) {
            return messageQueues.get(topic).length();
        }
        return 0;
    }

    @Override
    public void expire(String topic, MessageQueueContext.MessageEntry<M> messageEntry) throws ErrorExpiredException {
        if (messageEntry == null || !messageQueues.containsKey(topic)) {
            throw new IllegalArgumentException("参数有误，ID非法或主题不存在");
        }
        try {
            boolean flag = messageQueues.get(topic).remove(messageEntry);
            if (!flag) {
                throw new ErrorExpiredException("移出失败");
            }
        } catch (Exception e) {
            log.error("过期时发生异常");
            throw new ErrorExpiredException(e.getMessage());
        }
    }

    @Override
    public void destroy() {
        messageQueues = null;
        if (isOpenListener) {
            try {
                executorService.shutdown();
                isOpenListener = false;
            } catch (Exception e) {
                log.error("关闭守护线程发生异常：{}", e.getMessage());
            }
        }
        System.gc();
    }

    /**
     * <p>根据迭代器位置来使得一个元素过期，由于迭代器的弱一致性，多线程环境下可能会出现使用迭代器时
     * 发生插入\删除操作，由于该消息队列对于操作严格从队尾执行，因此对于插入修改能检测到，但是由于
     * 删除从队首进行，可能发生当迭代器获取下一个元素时为空，这时应该立即停止遍历，等待下一次</p>
     *
     * @param iterator 迭代器
     * @return void
     * @author Huang Yongxiang
     * @date 2022/3/21 11:42
     */
    public void expire(Iterator<MessageEntry<M>> iterator) throws ErrorExpiredException {
        if (iterator == null) {
            throw new ErrorExpiredException("过期失败，迭代器为空");
        }
        try {
            iterator.remove();
        } catch (UnsupportedOperationException e) {
            throw new ErrorExpiredException("过期失败，该迭代器不支持移除操作");
        } catch (IllegalStateException e) {
            throw new ErrorExpiredException("过期失败，可能发生删除操作");
        }
    }

    @Override
    public boolean publishMessageNoBlock(M message, String topic) {
        if (!messageQueues.containsKey(topic)) {
            log.error("发布非阻塞消息失败，所要发布到的队列主题：{}不存在", topic);
            return false;
        }
        if (message == null) {
            log.error("禁止发布空消息");
            return false;
        }
        MessageEntry<M> messageEntry = new MessageEntry<>();
        messageEntry.setMessageId(IdGenerator.getNextIdAsLong());
        messageEntry.setMessage(message);
        messageEntry.setExpiringTime(defaultExpiringTime > 0 ? defaultExpiringTime + SystemClock.now() : -1);
        return messageQueues.get(topic).publishMessage(messageEntry);
    }

    public boolean publishMessageBlock(M message, String topic) {
        if (!messageQueues.containsKey(topic)) {
            log.error("发布阻塞消息失败，所要发布到的队列主题：{}不存在", topic);
            return false;
        }
        if (message == null) {
            log.error("禁止发布空消息");
            return false;
        }
        try {
            MessageEntry<M> messageEntry = new MessageEntry<>();
            messageEntry.setMessageId(IdGenerator.getNextIdAsLong());
            messageEntry.setMessage(message);
            messageEntry.setExpiringTime(defaultExpiringTime > 0 ? defaultExpiringTime + SystemClock.now() : -1);
            return messageQueues.get(topic).publishMessageSync(messageEntry);
        } catch (InterruptedException e) {
            log.warn("发布可阻塞消息发生异常，等待时被异常占用：{}", e.getMessage());
        }
        return false;
    }

    private void scheduleExpiringCheckSingleThread() {
        executorService = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "ExpiringCheckSingleThread");
            thread.setDaemon(true);
            return thread;
        });
        executorService.scheduleAtFixedRate(() -> {
            for (Map.Entry<String, MessageQueue<MessageEntry<M>>> entry : messageQueues.entrySet()) {
                for (Iterator<MessageEntry<M>> it = entry.getValue().iterator(); it.hasNext(); ) {
                    MessageEntry<M> message = it.next();
                    //如果达到过期时间则通知其过期
                    if (message.expiringTime >= 0 && message.expiringTime <= SystemClock.now()) {
                        try {
                            //log.info("messageId：{}已过期", message.messageId);
                            expire(it);
                        } catch (ErrorExpiredException e) {
                            log.error("主题：{}，消息ID：{}过期失败：{}", entry.getKey(), message.getMessageId(), e.getMessage());
                        }
                    }
                }
            }
        }, 1, 1, TimeUnit.MILLISECONDS);
    }

    private void scheduleExpiringCheckTopicConcurrency() {
        executorService = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "ExpiringCheckTopicConcurrencyThread");
            thread.setDaemon(true);
            return thread;
        });
        executorService.scheduleAtFixedRate(() -> {
            for (Map.Entry<String, MessageQueue<MessageEntry<M>>> entry : messageQueues.entrySet()) {
                ScheduledExecutorService queueScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
                    Thread thread = new Thread(runnable, String.format("%sQueueListener", entry.getKey()));
                    thread.setDaemon(true);
                    return thread;
                });
                synchronized (MessageEntry.class) {
                    queueScheduler.schedule(() -> {
                        for (Iterator<MessageEntry<M>> it = entry.getValue().iterator(); it.hasNext(); ) {
                            MessageEntry<M> message = it.next();

                            //如果达到过期时间则通知其过期
                            if (message.expiringTime >= 0 && message.expiringTime <= SystemClock.now()) {
                                try {
                                    log.info("messageId：{}已过期", message.messageId);
                                    expire(it);
                                } catch (ErrorExpiredException e) {
                                    log.error("主题：{}，消息ID：{}过期失败：{}", entry.getKey(), message.getMessageId(), e.getMessage());
                                }
                            }
                        }
                    }, 1, TimeUnit.MILLISECONDS);
                }
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS);
    }


    @Setter
    @Accessors(chain = true)
    public static class Builder<M> {
        /**
         * 默认过期时间，对所有消息设置，-1则表示消息均为永久性消息，除非消费者取出，否则将会一直存在。谨慎使用！
         */
        private long defaultExpiringTime = -1;

        /**
         * 是否允许设置单个消息的过期时间
         */
        private boolean isAllowSetEntryExpired = false;

        /**
         * 允许的最大主题数
         */
        private int allowMaxTopicCount = 255;

        /**
         * 允许每个队列的最大消息数
         */
        private int allowMaxMessageCountPerQueue = 1000;

        /**
         * 过期监听器的策略
         */
        private ExpirationListenerPolicy listenerPolicy = ExpirationListenerPolicy.SINGLE_THREAD;

        public Builder<M> setDefaultExpiringTime(long defaultExpiringTime, TimeUnit unit) {
            if (unit == TimeUnit.MICROSECONDS) {
                log.error("最小支持毫秒级");
                throw new IllegalArgumentException("最小支持毫秒级");
            }
            this.defaultExpiringTime = unit.toMillis(defaultExpiringTime);
            return this;
        }

        public <M1 extends M> MessageQueueContext<M1> build() {
            MessageQueueContext<M1> messageQueueContext = new MessageQueueContext<>();
            messageQueueContext.messageQueues = new Hashtable<>(this.allowMaxTopicCount);
            messageQueueContext.isAllowSetEntryExpired = this.isAllowSetEntryExpired;
            messageQueueContext.allowMaxMessageCountPerQueue = this.allowMaxMessageCountPerQueue;
            messageQueueContext.defaultExpiringTime = this.defaultExpiringTime;
            messageQueueContext.allowMaxTopicCount = this.allowMaxTopicCount;
            messageQueueContext.listenerPolicy = this.listenerPolicy;
            return messageQueueContext;
        }

    }

    public static class MessageQueue<M> {
        private final BlockingQueue<M> messageQueue;

        public MessageQueue() {
            messageQueue = new LinkedBlockingQueue<>();
        }


        public MessageQueue(int maxLength) {
            if (maxLength <= 0) {
                throw new IllegalArgumentException("最大容量应该为非负数");
            }
            messageQueue = new LinkedBlockingDeque<>(maxLength);
        }

        public M takeMessageSync() throws InterruptedException {
            return messageQueue.take();
        }

        public M takeMessage() {
            return messageQueue.poll();
        }

        public M readMessage() {
            return messageQueue.peek();
        }

        public M tryReadMessage() {
            return messageQueue.element();
        }

        public boolean removeIf(Predicate<? super M> predicate) {
            if (predicate == null) {
                return false;
            }
            return messageQueue.removeIf(predicate);
        }

        public int length() {
            return messageQueue.size();
        }

        public boolean remove(M message) {
            if (message == null) {
                return false;
            }
            return messageQueue.remove(message);
        }

        public Iterator<M> iterator() {
            return messageQueue.iterator();
        }

        private boolean publishMessageSync(M message) throws InterruptedException {
            if (message == null) {
                return false;
            }
            try {
                messageQueue.put(message);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }

        private boolean publishMessage(M message) {
            if (message == null) {
                return false;
            }
            try {
                return messageQueue.offer(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

    }

    @Getter
    @Setter(AccessLevel.PRIVATE)
    public static class MessageEntry<M> {
        long messageId;
        M message;
        long expiringTime;

        public MessageEntry(long messageId, M message, long expiringTime) {
            this.messageId = messageId;
            this.message = message;
            this.expiringTime = expiringTime;
        }

        public MessageEntry() {
        }
    }

    public static void main(String[] args) {
        int producerCount = 50;
        int consumerCount = 50;
        long maxMessageCount = 10000000;
        AtomicLong getMessageAccount = new AtomicLong(0);
        AtomicBoolean isFinished = new AtomicBoolean(false);
        int perProducerCreate = (int) (maxMessageCount / producerCount);
        MessageQueueContext<String> messageQueueContext = MessageQueueContext.builder().setAllowMaxTopicCount(producerCount).setListenerPolicy(ExpirationListenerPolicy.TOPIC_CONCURRENCY).setDefaultExpiringTime(30, TimeUnit.SECONDS).setAllowSetEntryExpired(true).build();
        for (int i = 0; i < producerCount; i++) {
            messageQueueContext.registerMessageQueue(i + "");
        }

        Runnable create = () -> {
            MessageProducer<String> producer = new MessageProducer<>(messageQueueContext);
            String topicName = Thread.currentThread().getName();
            for (int i = 0; i < perProducerCreate; i++) {
                producer.publishMessageBlock(StringUtils.getRandomStr(10), topicName);
            }
        };

        Runnable get = () -> {
            MessageConsumer<String> consumer = new MessageConsumer<>(messageQueueContext);
            String topicName = Thread.currentThread().getName();
            while (true) {
                if (getMessageAccount.get() % 10000 == 0) {
                    log.info("已消费消息：{}条", getMessageAccount.get());
                }
                consumer.takeMessageBlock(topicName, true);
                getMessageAccount.incrementAndGet();
                if (getMessageAccount.get() == maxMessageCount) {
                    isFinished.set(true);
                    break;
                }
            }
        };

        long start = System.currentTimeMillis();

        for (int i = 0; i < producerCount; i++) {
            Thread thread = new Thread(create);
            thread.setName("" + i);
            thread.start();
        }

        for (int i = 0; i < consumerCount; i++) {
            Thread thread = new Thread(get);
            thread.setName("" + i);
            thread.start();
        }

        Thread thread = new Thread(() -> {
            while (true) {
                if (isFinished.get()) {
                    break;
                }
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            long end = System.currentTimeMillis();
            log.info("测试完成！消费者数目：{}，生产者数目：{}，消息总数：{}，总计耗时：{}s", consumerCount, producerCount, maxMessageCount, (end - start) / 1000);
        });
        thread.start();


    }
}

```