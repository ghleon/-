# springboot 集成jetcache实现redis远程缓存和本地缓存



### 一、添加pom依赖

```XML
        <dependency>
            <groupId>com.alicp.jetcache</groupId>
            <artifactId>jetcache-starter-redis</artifactId>
            <version>2.5.11</version>
        </dependency>
 
        <dependency>
            <groupId>redis.clients</groupId>
            <artifactId>jedis</artifactId>
            <version>2.9.0</version>
        </dependency>
```

### 二、yml文件配置如下

可同时实现远程和[本地缓存](https://so.csdn.net/so/search?q=本地缓存&spm=1001.2101.3001.7020)的使用，并且支持自定义，非常方便！

```yaml
spring: 
#      解决jetcache循环依赖问题
  main:
    allow-circular-references: true

jetcache:
  statIntervalMinutes: 15 #每过15分钟在控制台汇总一次数据
  areaInCacheName: false
  local:
    default:
      type: linkedhashmap
      keyConvertor: fastjson
      limit: 100
  remote:
    default:
      keyPrefix: sms_ # 全局key前缀
      type: redis
      host: localhost
      port: 6379
      keyConvertor: fastjson
      valueEncoder: java
      valueDecoder: java
      poolConfig:
        maxTotal: 50
        minIdle: 5
        maxIdle: 20
```

### 三、开启注解缓存使用设置

@EnableCreateCacheAnnotation在启动类上添加

```java
@SpringBootApplication
//jetCache开启注解缓存的开关
@EnableCreateCacheAnnotation
//开启注解缓存方法的包
@EnableMethodCache(basePackages = "com.example.demo")
public class DemoApplication {
 
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
 
}
```

### 四、使用注解类

@CreateCache声明jetcache缓存对象，类似于hashmap的升级版。

```java
package com.example.demo.service;
 
import com.alicp.jetcache.Cache;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.CreateCache;
import com.example.demo.dao.TestDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
 
import java.util.concurrent.TimeUnit;
 
@Service
public class TestService {
    @Autowired
    private TestDao testDao;
 
    //创建缓存对象
    //name是自定义空间名，expire是时间，timeUnit是时间单位默认为秒,area为范围前缀,cacheType使用远程或本地方案默认为远程
    //cacheType 有 local 本地, remote 线上远程, both(两级缓存）
    @CreateCache(name="jetCache_", expire = 3600, timeUnit = TimeUnit.SECONDS, cacheType = CacheType.LOCAL)
    private Cache<String, String> jetCache;
 
    public String gainData(){
        return jetCache.get("name");//获取缓存
    }
 
    public void putData(String s){
        jetCache.put("name", s);//放入缓存
    }
}
```

### 五、@Cached使用

@Cached的使用方法和@[Cacheable](https://so.csdn.net/so/search?q=Cacheable&spm=1001.2101.3001.7020)使用方法差不多，可以说是@Cacheable的升级版。

```java
package com.example.demo.service;
 
import com.alicp.jetcache.anno.Cached;
import com.example.demo.pojo.Person;
import org.springframework.stereotype.Service;
 
@Service
public class TestService {
    //默认的cacheType为Remote
    @Cached(name="person",key="#id",expire = 3600,cacheType = CacheType.LOCAL)
    //@CacheRefresh(refresh=5)//几秒刷新一次
    public Person gainData(Integer id){
        System.out.println("数据库操作了");
    }
}

//为了让系统同步可以设置@CacheRefresh(refresh=5)//几秒刷新一次
```

**使用效果就是先写入再读取，在缓存中存在key时直接读取不再访问数据库，当缓存里不存在key时会放行接下来的操作。使用时注意：实体类需要扩展一个接口：**

**必须保证缓存的操作对象是可序列化的，如下：**

```java
package com.example.demo.pojo;
 
import java.io.Serializable;
 
public class Person implements Serializable {
    private Integer id;
    private String name;
    private Integer age;
    private String address;
}
```

### 六、@CacheInvalidate使用

一旦[数据库删除](https://so.csdn.net/so/search?q=数据库删除&spm=1001.2101.3001.7020)对应的值以后，缓存中也会对应的删除该值。

name为存储库名，key为键名

```java
package com.example.demo.service;
 
import com.alicp.jetcache.anno.CacheInvalidate;
import org.springframework.stereotype.Service;
 
@Service
public class TestService {
    @CacheInvalidate(name="person_", key="#id")
    public boolean delete(Integer id){
        System.out.println("数据库删除对应的id:"+id);
        return true;
    }
}
```

### 七、@CacheUpdate使用

数据库被修改后，缓存也会被修改。

```java
package com.example.demo.service;
 
import com.alicp.jetcache.anno.CacheUpdate;
import com.example.demo.pojo.Person;
import org.springframework.stereotype.Service;
 
@Service
public class TestService {
    @CacheUpdate(name="person_", key="#person.id", value="#person")
    public boolean update(Person person){
        System.out.println("数据库修改成功！");
        return true;
    }
}
```





















