

> 查看安装的docker服务

```
docker ps

docker exec -it 6d4809f9703f /bin/bash

rabbitmq-plugins enable rabbitmq_management
```

> 安装服务

```
docker pull rocketmqinc/rocketmq

mkdir -p /Users/leon/data/rocketmq/data/namesrv/logs

mkdir -p /Users/leon/data/rocketmq/data/namesrv/store


docker run -d \
--restart=always \
--name rmqnamesrv \
-p 9876:9876 \
-v /Users/leon/data/rocketmq/data/namesrv/logs:/root/logs \
-v /Users/leon/data/rocketmq/data/namesrv/store:/root/store \
-e "MAX_POSSIBLE_HEAP=100000000" \
rocketmqinc/rocketmq \
sh mqnamesrv

```

> 安装broken

```
mkdir -p /Users/leon/data/rocketmq/data/broker/logs
mkdir -p /Users/leon/droata/rocketmq/data/broker/store
mkdir -p /Users/leon/data/rocketmq/conf

vi /Users/leon/rocketmq/conf/broker.conf
# 所属集群名称，如果节点较多可以配置多个
brokerClusterName = DefaultCluster
#broker名称，master和slave使用相同的名称，表明他们的主从关系
brokerName = broker-a
#0表示Master，大于0表示不同的slave
brokerId = 0
#表示几点做消息删除动作，默认是凌晨4点
deleteWhen = 04
#在磁盘上保留消息的时长，单位是小时
fileReservedTime = 48
#有三个值：SYNC_MASTER，ASYNC_MASTER，SLAVE；同步和异步表示Master和Slave之间同步数据的机制；
brokerRole = ASYNC_MASTER
#刷盘策略，取值为：ASYNC_FLUSH，SYNC_FLUSH表示同步刷盘和异步刷盘；SYNC_FLUSH消息写入磁盘后才返回成功状态，ASYNC_FLUSH不需要；
flushDiskType = ASYNC_FLUSH
# 设置broker节点所在服务器的ip地址
brokerIP1 = 192.168.200.192


docker run -d \
--restart=always \
--name rmqbroker \
--link rmqnamesrv:namesrv \
-p 10911:10911 \
-p 10909:10909 \
-p 10912:10912 \
-v /Users/leon/data/rocketmq/data/broker/logs:/root/logs \
-v /Users/leon/data/rocketmq/data/broker/store:/root/store \
-v /Users/leon/data/rocketmq/conf/broker.conf:/opt/rocketmq-4.4.0/conf/broker.conf \
-e "NAMESRV_ADDR=namesrv:9876" \
-e "MAX_POSSIBLE_HEAP=200000000" \
rocketmqinc/rocketmq:latest \
sh mqbroker -c /opt/rocketmq-4.4.0/conf/broker.conf

```

> 安装控制台

```
docker run \
-e "JAVA_OPTS=-Drocketmq.config.namesrvAddr=192.168.200.192:9876 -Drocketmq.config.isVIPChannel=false" \
-p 9999:8080 \
-t styletang/rocketmq-console-ng
```



