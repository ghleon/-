1.git log命令，查看分支之间的关系图

```
git log --graph --decorate --oneline --simplify-by-decoration --all
```

>说明：
>--decorate 标记回让git log显示每个commit的引用（如：分支、tag等）
>--oneline 一行显示
>--simplify-by-decoration只显示被branch或tag引用的commit
>-- all 表示嫌疑所有的branch，这里也可以选择，比如我只想显示分支ABC的关系，则将-- all替换成branchA branchB branchC

2.查看远程分支
```
git branch -a
```

3.版本回退到某一个version

回退到上一个版本
```
git reset --hard HEAD^
```

回退到某一个版本
```
git reset --hard [提交commit的id]
```

* 穿梭前，用git log可以查看提交历史，以便确定要回退到哪个版本。
* 要重返未来，用git reflog查看命令历史，以便确定要回到未来的哪个版本。
