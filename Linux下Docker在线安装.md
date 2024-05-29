# Linux下Docker在线安装

### Docker在线安装

### **1**、安装Docker

(1).安装yum-utils软件包（提供yum-config-manager 实用程序）

```powershell
[root@localhost ]#sudo yum install -y yum-utils
```

(2).设置稳定的存储库。

```powershell
[root@localhost ]#sudo yum-config-manager \

    --add-repo \

https://download.docker.com/linux/centos/docker-ce.repo

-----或者使用阿里云源地址--本次使用此源地址安装-----

[root@localhost ]#sudo yum-config-manager \

    --add-repo \

    http://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo
```

(3).安装DOCKER引擎

安装最新版本的Docker Engine和容器：

```powershell
 [root@localhost ]#sudo yum install docker-ce docker-ce-cli containerd.io
```

**2**、**常用Docker** **命令**

```powershell
-----查看Docker版本-----

[root@localhost ]#docker –v  或者  [root@localhost ]#docker version

------启动Docker -----

[root@localhost ]#systemctl start docker

-----查看Docker状态-----

[root@localhost ]#systemctl status docker

-----重启Docker-----

[root@localhost ]#systemctl restart docker

-----停止Docker-----

[root@localhost ]#systemctl stop docker

-----设置开机自启-----

[root@localhost ]#systemctl enable docker

-----禁止开机自启-----

[root@localhost ]#systemctl disable docker

-----列出所有镜像-----

[root@localhost ]#docker images

-----删除镜像-----

[root@localhost ]#docker rmi IMAGE_ID

-----查看所有容器-----

[root@localhost ]#docker ps

------启动某个容器----

[root@localhost ]#docker start 容器名称

------停止某个容器------

[root@localhost ]#docker stop 容器名

----退出某个容器----

[root@localhost ]#exit

-------删除某个容器-----

[root@localhost ]#docker rm 容器id

-----卸载旧版docker -----

[root@localhost ]#sudo yum remove docker \

                  docker-client \

                  docker-client-latest \

                  docker-common \

                  docker-latest \

                  docker-latest-logrotate \

                  docker-logrotate \

                  docker-engine

或者使用以下方法：

-----列出docker安装过的相关包-----

[root@localhost ]#sudo yum list installed | grep docker

-----删除相关安装包-----

[root@localhost ]#sudo yum -y remove docker-ce.x86_64

[root@localhost ]#sudo yum -y remove docker-ce-selinux.noarch

-----删除相关的镜像与容器-----

[root@localhost ]#sudo rm -rf /var/lib/docker

[root@localhost ]#sudo yum remove docker  docker-common docker-selinux docker-engine


```

**3** **docker run参数说明**

**语法: docker run [OPTIONS] IMAGE [COMMAND] [ARG...]**

**OPTIONS** **说明如下:**

```powershell
a stdin: 指定标准输入输出内容类型，可选 STDIN/STDOUT/STDERR 三项；

-d: 后台运行容器，并返回容器ID；

-i: 以交互模式运行容器，通常与 -t 同时使用；

-P: 随机端口映射，容器内部端口随机映射到主机的端口

-p: 指定端口映射，格式为：主机(宿主)端口:容器端口

-t: 为容器重新分配一个伪输入终端，通常与 -i 同时使用；

--name="nginx-lb": 为容器指定一个名称；

--dns 8.8.8.8: 指定容器使用的DNS服务器，默认和宿主一致；

--dns-search example.com: 指定容器DNS搜索域名，默认和宿主一致；

-h "mars": 指定容器的hostname；

-e username="ritchie": 设置环境变量；

--env-file=[]: 从指定文件读入环境变量；

--cpuset="0-2" or --cpuset="0,1,2": 绑定容器到指定CPU运行；

-m :设置容器使用内存最大值；

--net="bridge": 指定容器的网络连接类型，支持 bridge/host/none/container: 四种类型；

--link=[]: 添加链接到另一个容器；

--expose=[]: 开放一个端口或一组端口；

--volume , -v:    绑定一个卷
```



# Linux远程SSH登陆另外一台服务器教程

**1、远程主机SSH为22端口**
本地SSH连接双栈vps。输入如下命令：

```powershell
ssh 用户名@ip地址
```

然后输入远程密码即可。用户名一般为`root`，下同。

**2、远程主机SSH非22端口**

```powershell
ssh ip地址 -l 用户名 -p 12345
```

-p 12345代表端口号。然后也要输入密码。

ipv6也是一样的，可以直接连接。















