# Controller 层编码规范

**1、Controller层**

controller层在MVC设计中属于控制层；设计初衷：接受请求并响应请求；所以，该层尽量轻薄，避免编写涉及业务处理的代码。

前后端分离的开发设计模式下，推荐使用@RestController注解，它相当于@ResponseBody ＋ @Controller的组合使用。

1) 如果只是使用@RestController注解Controller类，则Controller中的方法无法返回jsp页面，或者html，配置的视图解析器InternalResourceViewResolver不起作用，返回的内容就是Return 里的内容，默认转成json串。

2) 如果需要返回到指定页面，则需要用 @Controller配合视图解析器InternalResourceViewResolver才行。

如果需要返回JSON，XML或自定义mediaType内容到页面，则需要在对应的方法上加上@ResponseBody注解。

总之，使用@Controller 注解，在对应的方法上，视图解析器可以解析return 的jsp,html页面，并且跳转到相应页面；若返回json等内容到页面，则需要加@ResponseBody注解



### 1）设定请求路径

使用注解`@PostMapping("/page")`，类命名和方法命名之上都可以加

> 注意按照不同业务划分使用，避免乱写乱用。

### 2）设置请求方式

常用的POST/GET。使用注解：`@RequestMapping` 和  `@GetMapping` `@PostMapping`。

Spring4.3中引进了`｛@GetMapping、@PostMapping、@PutMapping、@DeleteMapping、@PatchMapping｝` 来帮助简化常用的HTTP方法的映射 并更好地表达被注解方法的语义

该注解`HTTP Get`方法将映射到特定的处理上

- `@GetMapping`是一个组合注解，它是`@RequestMapping(method = RequestMethod.GET)`的缩写
- `@PostMapping`是一个组合注解，它是`@RequestMapping(method = RequestMethod.POST)`的缩写

### 3）设置请求参数方式

**①提交，直接使用vo类或具体参数名接收；**

```java
@Controller
public class LoginController {
 
    @RequestMapping(value = "login", method = RequestMethod.POST)
    public String login(UserVO user){
        System.out.println("POJO: " + user.getClass().getName() + 
                ", hash code: " + user.hashCode() + ", " + user.toString());
        return "redirect:/";
    }
 
}
```

**②@RequestParam**

```java
@RequestParam(value="", required=true, defaultValue="")
```

`@RequestParam`有三个属性：

- **value：** 请求参数名（必须配置）
- **required：** 必须，默认为true 请求中必须包含该参数，如果包含没有，即抛出异常（可选配置）
- **defaultValue：** 默认值，如果设置了该值，必填 将自动设为 false

```java
@ApiOperation(value = "根据id查询") 
@PostMapping("/show")
public Responses show(@RequestParam(value="userId",defaultValue="-1") Long userId) {
    Record data = recordService.getOne(vo.getId());
    return Responses.success(data);
 
}
```

**③n提交，使用注解@RequestBody**

```java
@RequestBody`主要接收接收端以POST方式传递给使用`@RequestBody`数据时（JSON字符串中的数据请求体中的数据的）；GET方式无请求体，所以接收端不能使用GET方式提交数据，只是可以用同一个POST方式进行提交。可以在同一个接收方法中，`@RequestBody`最多可以有一个，并且有多个。`@RequestParam()` `@RequestBody` `@RequestParam()`
```

> 注：一个请求，只有一个`RequestBody`请求，可以有多个`RequestParam`。

```java
 @ApiOperation(value = "根据id查询") 
 @PostMapping("/get")
 public Responses getOne(@Validated @RequestBody IdVO vo){
     Record data = recordService.getOne(vo.getId());
     return Responses.success(data);
 }
```

**④PathVariable**

```java
@RestController
@RequestMapping("/")
public class ChineseDrugController {
 @ResponseBody
 @RequestMapping(value = "/{name}")
 public String showName(@PathVariable String name, @PathVariable(value = "name", required = false) String sex) {
  return "Hello " + name + sex;
 }
```

**⑤@PathParam**

> ```properties
> url：http://127.0.0.1:8080/sexvalue/namevalue?name=唐&sex=男
> ```

```java
@RestController
@RequestMapping(value = "/{sex}")
public class ChineseDrugController {
 @ResponseBody
 @RequestMapping(value = "/{name}")
 public String showName(@PathVariable(value = "name") String name, @PathParam(value = "sex") String sex) {
  return "Hello " + name + " " + sex;
 }
}
```

说明：以上代码仅仅展示功能上有很好的灵活性，实际开发中避免如此任意使用 。

### 4）校验请求参数

参数校验

- 使用注解`@Validated`，使得参数自动校验生效，它是`spring-contex`中的注解；
- vo类中自定义各类校验，比如`@NotNull`等，他是javax`validation-api`中的注解这里不赘述；
- 程序层面的校验。

示例方法如下

```java
@ApiOperation(value = "应用类型和应用关系绑定")
@PostMapping("/applicationTypeBind")
public Boolean applicationTypeBind(@Validated @RequestBody ApplicationBindVO vo){
    applicationTypeService.applicationTypeBind(vo);
    return true;
}
```

对应 VO 类示例

```java
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
 
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;
 
@Data
@ApiModel(value = "ApplicationBindVO",description = "关系绑定vo")
public class ApplicationBindVO {
 
    @NotNull
    @ApiModelProperty("应用类型id")
    private Long typeId;
 
    @ApiModelProperty("应用id集合")
    private List<Long> applicationIdList;
}
```

### 5）入参出参设计

依据业务而定，格式尽量做到统一；

响应前端（APP/PC）的参数，一般再封装一层，方便前端统一处理，如下返回

```java
Responses.success(data);
import com.fasterxml.jackson.annotation.JsonView;
import com.myfutech.common.util.enums.ResponseCode;
import com.myfutech.common.util.vo.BaseView;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
 
@ApiModel(value = "Responses",description = "响应信息")
public class Responses<T> {
   
 @JsonView({BaseView.class})
    @ApiModelProperty("响应编码")
    private String code;
    
    @JsonView({BaseView.class})
    @ApiModelProperty("响应消息")
    private String msg;
    
    @JsonView({BaseView.class})
    @ApiModelProperty("响应体")
    private T result;
    
    public static <T> Responses<T> success() {
        return new Responses(ResponseCode.SUCCESS_CODE, "", (Object)null);
    }
 
    public static <T> Responses<T> success(T result) {
        return new Responses(ResponseCode.SUCCESS_CODE, "", result);
    }
 
    public static <T> Responses<T> success(String msg, T result) {
        return new Responses(ResponseCode.SUCCESS_CODE, msg, result);
    }
 
    public static <T> Responses<T> error(String msg) {
        return new Responses(ResponseCode.ERROR_CODE, msg, (Object)null);
    }
 
    public static <T> Responses<T> error(ResponseCode code) {
        return new Responses(code, code.getDefaultMsg(), (Object)null);
    }
 
    public static <T> Responses<T> error(ResponseCode code, String msg) {
        return new Responses(code, msg, (Object)null);
    }
 
    public Responses() {
    }
 
    private Responses(ResponseCode code, String msg, T result) {
        this.code = code.getCode();
        this.msg = msg;
        this.result = result;
    }
 
    public String getCode() {
        return this.code;
    }
 
    public boolean notSuccess() {
        return !ResponseCode.SUCCESS_CODE.getCode().equals(this.code);
    }
 
    public String getMsg() {
        return this.msg;
    }
 
    public T getResult() {
        return this.result;
    }
 
    public void setCode(String code) {
        this.code = code;
    }
 
    public void setMsg(String msg) {
        this.msg = msg;
    }
 
    public void setResult(T result) {
        this.result = result;
    }
    
    
}
```

### 6） 自动生成接口文档

使用SwaggerAPI，常用注解

```java
//加载类名之上
@Api(tags = "日志相关接口", description="操作日志",
        consumes= MediaType.APPLICATION_JSON_UTF8_VALUE,
        produces=MediaType.APPLICATION_JSON_UTF8_VALUE)
 
 
//加在方法名之上
@ApiOperation(value = "查询分页列表")
 
//加载实体或VO类名之上
@Data
@ApiModel(value = "ApprovalRoleModifyVO",description = "审批角色修改信息")
public class ApprovalRoleModifyVO{
```

**@Api：** 作用在类上，用来标注该类具体实现内容。表示该类是swagger的资源。

参数：

- **tags：** 可以使用`tags()`允许您为操作设置多个标签的属性，而不是使用该属性。
- **description：** 可描述描述该类的作用。

**@ApiOperation：** 用于方法，表示一个http请求的操作。

**@ApiModel：** 用于方法，字段，表示对model属性的说明或者数据操作更改



> **2、相对标准控制器类示例**



```java
package com.myfutech.employee.service.provider.ctrl;
 
import com.myfutech.common.util.Responses;
import com.myfutech.common.util.vo.IdVO;
import com.myfutech.common.util.vo.Page;
import com.myfutech.common.util.vo.Pageable;
import com.myfutech.employee.service.api.vo.response.record.RecordListVo;
import com.myfutech.employee.service.provider.model.Record;
import com.myfutech.employee.service.provider.service.RecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
 
import javax.annotation.Resource;
 
/**
 *  相关接口
 */
@Api(tags = "日志相关接口", description="操作日志",
        consumes= MediaType.APPLICATION_JSON_UTF8_VALUE,
        produces=MediaType.APPLICATION_JSON_UTF8_VALUE)
@RestController
@RequestMapping("/record")
public class RecordCtrl {
 
 
    private static final Logger log = LoggerFactory.getLogger(RecordCtrl.class);
 
    @Resource(name="recordService") 
    private RecordService recordService;
 
    @ApiOperation(value = "查询分页列表")
    @PostMapping("/page")
    public Page<RecordListVo> page( @RequestBody Pageable pageable){
        Page<RecordListVo> list = recordService.findConditionPage(pageable);
        return list;
    }
    @ApiOperation(value = "根据id查询") 
    @PostMapping("/get")
    public Responses getOne(@Validated @RequestBody IdVO vo){
        Record data = recordService.getOne(vo.getId());
        return Responses.success(data);
    }
    @ApiOperation(value = "新增") 
    @PostMapping("/add")
    public Responses add(@Validated(Record.Create.class) @RequestBody Record data){
        recordService.save(data);
        return Responses.success();
    }
    @ApiOperation(value = "更新") 
    @PostMapping("/update")
    public Responses update(@Validated(Record.Update.class) @RequestBody Record data){
        recordService.save(data);
        return Responses.success();
    }
    @ApiOperation(value = "删除") 
    @PostMapping("/delete")
    public Responses delete(@Validated @RequestBody IdVO vo){
        recordService.deleteById(vo.getId());
        return Responses.success();
    }
}

```