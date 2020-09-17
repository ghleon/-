特别注意:

```
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




删除标签作出提示：会解除标签和所有视频号的关联关系

//创建话题标签
服务:GaaS
method:POST
/v1/merculetManagement/moments/create/tags
{
	"title":"标题",
	"content":"内容",
	"pic_url":"话题图片",
	"extension":"扩展字段",
	"rank":"权重"
}


//话题列表 (分页)
服务:GaaS
method:POST
/v1/merculetManagement/moments/tags/page
{
	"title":"标题",
	"enable":true/false,
	"page_number":1,
	"page_size":10,
}

//话题详情（分页）
服务:GaaS
method:POST
/v1/merculetManagement/moments/tags/summary
{
    "tags_id":xxxx,
    "content":"视频号内容",
    "page_number":1,
    "page_size":10,
}

//删除标签
服务:GaaS
method:POST
/v1/merculetManagement/moments/tags/del

body参数为tagsId数组:
[xxxx,xxxx,xxx]


//启用话题标签
服务:GaaS
method:GET
/v1/merculetManagement/moments/tags/enable?id=xxxx


//停用话题标签
服务:GaaS
method:GET
/v1/merculetManagement/moments/tags/disable?id=xxxx


//视频号内容基本详情
服务:GaaS
method:GET
/v1/merculetManagement/moments/summary?id=xxxx

```






