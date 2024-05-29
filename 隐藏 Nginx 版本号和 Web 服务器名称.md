# **如何隐藏 Nginx 版本号和 Web 服务器名称**

如何隐藏 [Nginx](https://so.csdn.net/so/search?q=Nginx&spm=1001.2101.3001.7020) 版本号和 Web 服务器名称

## 1 隐藏 Nginx 版本号

其实隐藏 Nginx 版本号无非就是为了防止“漏洞”被人利用而已，这点 Nginx 其实很早就考虑到了，在 Nginx 的配置文件里只要加上server_tokens off就可以在网页head里隐藏掉 Nginx 的版本号了。

具体操作如下：

### 1.1 关闭server_tokens：

```shell
vim /usr/local/nginx/conf/nginx.conf
```

在http{}中加入

```shell
server_tokens off;
```

### 1.2去掉fastcgi_params里版本显示

```shell
vi /usr/local/nginx/conf/fastcgi_params
```

将里面的

```shell
fastcgi_param SERVER_SOFTWARE nginx/$nginx_version;
```

修改为：

```shell
fastcgi_param SERVER_SOFTWARE nginx;
```

## 2 隐藏 Web 服务器名称

关于如何隐藏Web 服务器名称，目前看除了在编译 Web 服务器名称的时候进行伪装以外没有很好的办法了。下面以 Nginx 为例：

伪装Nginx的具体办法

```shell
vi /src/core/nginx.h
```

修改其中：

```shell
#define NGINX_VERSION “1.0″
#define NGINX_VER “GWS/” NGINX_VERSION
```

重新编译nginx即可。

make & make install  

注：程序重新编译完后，要reload不会生效，需要用kill命令杀死原来的进程，再重新启动 Nginx 。

其实还有一个最简单的隐藏 Web 服务器名称的方法，那就启用 CDN 服务，当客户端访问的是 CDN 节点的时候，看到的当然也就是 CDN 节点的 Web 服务器名称了，至于自己网站的 Web 服务器名称只要没有“真实IP”几乎是不可能获得了，国内很多的免费 CDN 都可以的，这里明月就自己的经验推荐百度云加速、360网站卫士这两个免费的 CDN 。