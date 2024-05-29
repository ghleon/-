# netstat监控大量ESTABLISHED连接与Time_Wait连接问题

问题描述：

在不考虑系统负载、CPU、[内存](https://so.csdn.net/so/search?q=内存&spm=1001.2101.3001.7020)等情况下，netstat监控大量ESTABLISHED连接与Time_Wait连接。

```
# netstat -n | awk '/^tcp/ {++y[$NF]} END {for(w in y) print w, y[w]}'
CLOSE_WAIT         348
ESTABLISHED        1240
TIME_WAIT          5621
```

监控Apache与tomcat之间的链接端口

```
#netstat -n | grep 8009 | wc -l

7198 
```

问题1：怎么解决大量*Time_Wait*

通过调整内核参数：

```shell
vim /etc/sysctl.conf
#编辑文件，加入以下内容：
net.ipv4.tcp_syncookies = 1
net.ipv4.tcp_tw_reuse = 1
net.ipv4.tcp_tw_recycle = 1
net.ipv4.tcp_fin_timeout = 30
#然后执行 /sbin/sysctl -p 让参数生效。
 /sbin/sysctl -p
```

配置说明：

*net.ipv4.tcp_syncookies = 1* 表示开启*SYN Cookies*。当出现*SYN*等待队列溢出时，启用*cookies*来处理，可防范少量*SYN*攻击，默认为*0*，表示关闭；

*net.ipv4.tcp_tw_reuse = 1*   表示开启重用。允许将*TIME-WAIT sockets*重新用于新的*TCP*连接，默认为*0*，表示关闭；

*net.ipv4.tcp_tw_recycle = 1* 表示开启*TCP*连接中*TIME-WAIT sockets*的快速回收，默认为*0*，表示关闭；

*net.ipv4.tcp_fin_timeout=30*修改系統默认的 *TIMEOUT* 时间。

如果以上配置调优后性能还不理想，可继续修改一下配置：

```shell
vi /etc/sysctl.conf
net.ipv4.tcp_keepalive_time = 1200 
#表示当keepalive起用的时候，TCP发送keepalive消息的频度。缺省是2小时，改为20分钟。
net.ipv4.ip_local_port_range = 1024 65000 
#表示用于向外连接的端口范围。缺省情况下很小：32768到61000，改为1024到65000。
net.ipv4.tcp_max_syn_backlog = 8192 
#表示SYN队列的长度，默认为1024，加大队列长度为8192，可以容纳更多等待连接的网络连接数。
net.ipv4.tcp_max_tw_buckets = 5000 
#表示系统同时保持TIME_WAIT套接字的最大数量，如果超过这个数字，TIME_WAIT套接字将立刻被清除并打印警告信息。
默认为180000，改为5000。对于Apache、Nginx等服务器，上几行的参数可以很好地减少TIME_WAIT套接字数量，但是对于 Squid，效果却不大。此项参数可以控制TIME_WAIT套接字的最大数量，避免Squid服务器被大量的TIME_WAIT套接字拖死。


```



调优完毕，再压一下看看效果吧。

```
# netstat -n | awk '/^tcp/ {++y[$NF]} END {for(w in y) print w, y[w]}'

ESTABLISHED        968
```

问题1：怎么解决请求结束后依然存在大量ESTABLISHED没有被释放