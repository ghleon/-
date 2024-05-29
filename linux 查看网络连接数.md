# linux 查看网络连接数

### 1.统计80端口连接数

```bash
netstat -nat | grep -i "80" | wc -l
```

### 2.统计已连接上的，状态为 [established](https://so.csdn.net/so/search?q=established&spm=1001.2101.3001.7020)

```bash
netstat -na | grep ESTABLISHED | wc -l
```

### 3.查出 ESTABLISHED 连接 哪个IP地址连接最多

```bash
netstat -na | grep ESTABLISHED | awk '{print $5}' | awk -F':' '{print $1}' | sort | uniq -c
```

### 4.查出 [SYN](https://so.csdn.net/so/search?q=SYN&spm=1001.2101.3001.7020) 连接 哪个IP地址连接最多

```bash
netstat -na | grep SYN | awk '{print $5}' | awk -F':' '{print $1}' | sort | uniq -c
```

### 5.[tcp连接](https://so.csdn.net/so/search?q=tcp连接&spm=1001.2101.3001.7020)状态数量统计

```bash
netstat -n | awk '/^tcp/ {++S[$NF]} END {for(a in S) print a, S[a]}'
```

返回结果示例：

```bash
LAST_ACK 5
SYN_RECV 30
ESTABLISHED 1597
FIN_WAIT1 51
FIN_WAIT2 504
TIME_WAIT 1057
```

### 6.tcp连接状态简介

LISTEN： 侦听来自远方的TCP端口的连接请求
SYN-SENT： 再发送连接请求后等待匹配的连接请求
SYN-RECEIVED：再收到和发送一个连接请求后等待对方对连接请求的确认
ESTABLISHED： 代表一个打开的连接
FIN-WAIT-1： 等待远程TCP连接中断请求，或先前的连接中断请求的确认
FIN-WAIT-2： 从远程TCP等待连接中断请求
CLOSE-WAIT： 等待从本地用户发来的连接中断请求
CLOSING： 等待远程TCP对连接中断的确认
LAST-ACK： 等待原来的发向远程TCP的连接中断请求的确认
TIME-WAIT： 等待足够的时间以确保远程TCP接收到连接中断请求的确认
CLOSED： 没有任何连接状态