

```
================ 特别注意 ================

GaaS接口
/v1/merculetManagement/material
替换
/v1/merculetManagement/moments

H5接口
/api/v1/score/community/material
替换
/api/v1/score/community/moments

friend_circle_id
替换
moments_id


任务体系
SHARE_TOPIC_COMMENTS_BE_SCAN_UV
替换成
SHARE_MOMENTS_BE_SCAN_UV

SHARE_SPECIFIED_TOPIC_COMMENTS_BE_SCAN_UV
替换成
SHARE_SPECIFIED_TAGS_MOMENTS_BE_SCAN_UV

SHARE_TOPIC_BE_SCAN_UV
替换成
SHARE_TAGS_BE_SCAN_UV

SHARE_SPECIFIED_TOPIC_BE_SCAN_UV
替换成
SHARE_SPECIFIED_TAGS_BE_SCAN_UV

================ 特别注意 ================
```

* GaaS删除话题标签作出提示：会解除标签和所有视频号的关联关系

* GaaS创建话题标签
```
服务:GaaS
method:POST
/v1/merculetManagement/moments/tags/create
{
	"title":"标题",
	"content":"内容",
	"pic_url":"话题图片",
	"extension":"扩展字段",
	"rank":"权重"
}
```

* GaaS话题标签列表 (分页)
```
服务:GaaS
method:POST
/v1/merculetManagement/moments/tags/page
{
	"title":"标题",
	"enable":true/false,
	"page_number":1,
	"page_size":10,
}
```

* GaaS话题标签详情（分页）
```
服务:GaaS
method:POST
/v1/merculetManagement/moments/tags/summary
{
    "tags_id":xxxx,
    "content":"视频号内容",
    "page_number":1,
    "page_size":10,
}
```

* GaaS删除话题标签
```
服务:GaaS
method:POST
/v1/merculetManagement/moments/tags/del

body参数为tagsId数组:
[xxxx,xxxx,xxx]
```

* GaaS启用话题标签
```
服务:GaaS
method:GET
/v1/merculetManagement/moments/tags/enable?id=xxxx
```

* GaaS停用话题标签
```
服务:GaaS
method:GET
/v1/merculetManagement/moments/tags/disable?id=xxxx
```

* GaaS视频号内容基本详情
```
服务:GaaS
method:GET
/v1/merculetManagement/moments/summary?id=xxxx
```


======================== 
* H5获取话题标签列表 (分页)
```
服务:community
method:GET
/api/v1/score/community/moments/tags/page?uat_short_name=xxx&page_number=1&page_size=10
```

* H5获取单个话题标签基本信息
```
服务:community
method:GET
/api/v1/score/community/moments/tags/summary?uat_short_name=xxx&tags_id=xxxx&invite_id=xxxxxxxx
```

* H5获取话题标签列表 (分页)
```
服务:community
method:GET
/api/v1/score/community/moments/tags/moments/page?uat_short_name=xxx&page_number=1&page_size=10&tags_id=xxxx
```




