# Spring Boot中内置Tomcat最大连接数、线程数与等待数 实践调优

在 Spring Boot 框架中，我们使用最多的是Tomcat，这是 Spring Boot 默认的[容器技术](https://so.csdn.net/so/search?q=容器技术&spm=1001.2101.3001.7020)，而且是内嵌式的 Tomcat。Tomcat 是 Apache 基金下的一个轻量级的Servlet 容 器 ， 支 持 Servlet 和 JSP 。Tomcat服务器本身具有Web服务器的功能，可以作为独立的Web服务器来使用。

# 一、Spring Boot应用中Tomcat建议配置

Spring Boot 能支持的最大并发量主要看其对Tomcat的设置，可以在配置文件中对其进行更改。要了解具体参数的默认值，一个**简单的方法是**在application.properties 配置文件中输入配置项，默认值就会显示出来。

```properties
最大工作线程数，默认200。
server.tomcat.max-threads=200
最大连接数默认是10000
server.tomcat.max-connections=10000
等待队列长度，默认100。
server.tomcat.accept-count=100
最小工作空闲线程数，默认10。
server.tomcat.min-spare-threads=100
```

对应application.yml 配置文件如下所示：

```yaml
server:
  port: 9000
  tomcat:
    uri-encoding: UTF-8
    max-threads: 800 #最大工作线程数量
    min-spare-threads: 20 #最小工作线程数量
    #max-connections: 10000 #一瞬间最大支持的并发的连接数
    accept-count: 200 #等待队列长度
```

## 参数解释 

- 线程数的经验值为：***\**\*1核2G\*\**\**\*内存，线程数经验值\**\**\*\*200\*\**\**\*；\**\**\*\*4核8G\*\**\**\*内存， 线程数经验值\**\**\*\*800\*\**\***。
  （4核8G内存单进程调度线程数800-1000，超过这个并发数之后，将会花费巨大的时间在CPU调度上）
- 等待队列长度：队列做缓冲池用，但也不能无限长，消耗内存，出入队列也耗CPU。
- maxThreads规定的是最大的线程数目，并不是实际running的CPU数量；实际上，maxThreads的大小比CPU核心数量要大得多。这是因为，处理请求的线程真正用于计算的时间可能很少，大多数时间可能在阻塞，如等待数据库返回数据、等待硬盘读写数据等。因此，在某一时刻，只有少数的线程真正的在使用物理CPU，大多数线程都在等待；因此线程数远大于物理核心数才是合理的。也就是说，Tomcat通过使用比CPU核心数量多得多的线程数，可以使CPU忙碌起来，大大提高CPU的利用率。

**针对4C8G配置，可以参考建议值：**

```yaml
tomcat:
accept-count: 1000
max-connections: 10000
max-threads: 800
min-spare-threads: 100
```

Spring Boot的默认配置信息，都在 springboot-autoconfigure-版本号.jar 这个包中。
其 中 上 述 Tomcat 的 配 置在/web/ServerProperties.java中。

# 二、最大并发量-maxThreads和maxConnections参数

比较容易弄混的是maxThreads和maxConnections这两个参数:

- maxThreads是指Tomcat线程池最多能起的线程数
- maxConnections则是Tomcat一瞬间最多能够处理的并发连接数。

比如 maxThreads=1000,maxConnections=800，
假设某一瞬间的并发是1000，那么最终Tomcat的线程数将会是800，即同时处理800个请求，剩余200进入队列“排队”，如果acceptCount=100 (100个请求进入排队），另外100个请求会被拒掉。

并发量指的是连接数,还是线程数?当然是连接数(maxConnections) 。

200个线程如何处理10000条连接?
Tomcat有两种处理连接的模式

- 一种是BIO，一个线程只处理一个Socket连接;
- 另一种就是NIO,一个线程处理多个Socket连接。

由于HTTP请求不会太耗时,而且多个连接一般不会同时来消息，所以一个线程处理多个连接没有太大问题。

Tomcat的最大连接数参数是maxConnections,这个值表示最多可以有多少个Socket 连接到Tomcat上。
BIO模式下默认最大连接数是它的最大线程数(缺省是200)，
NIO模式下默认是10000 ,APR模式则是8192。

为什么不开更多线程?
多开线程的代价就是增加上下文切换的时间，浪费CPU时间。另外还有就是线程数增多，每个线程分配到的时间片就变少。多开线程并不等于提高处理效率。
那增加最大连接数(maxConnections)呢?
增加最大连接数,支持的并发量确实可以上去。但是在没有改变硬件条件的情况下，这种并发量的提升必定以牺牲响应时间为代价。

# 三、maxConnections和acceptCount参数

maxConnections 和acceptCount的关系为:当连接数达到最大值maxConnections后,系统会继续接收连接,进行排队，但不会超过acceptCount的值。
Tomcat最大连接数取决于maxConnections这个值加上acceptCount这个值,在连接数达到了maxConenctions之后,Tomcat仍会保持住连接,但是不处理，等待其它请求处理完毕之后才会处理这个请求。
当队列(acceptCount)已满时,任何的连接请求都将被拒绝。acceptCount的默认值为100。简而言之，当调用HTTP请求数达到Tomcat的最大连接数时,还有新的HTTP请求到来,这时Tomcat会将该请求放在等待队列中,这个acceptCount就是指能够接受的最大等待数，默认100。如果等待队列也被放满了，这个时候再来新的请求就会被Tomcat拒绝(connection refused)。
用户端(浏览器端)也会报错



------