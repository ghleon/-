# vmoptions

```properties
-Xms2048m
-Xmx4096m
-Xverify:none
-XX:+DisableExplicitGC
-XX:ReservedCodeCacheSize=720m
-XX:SoftRefLRUPolicyMSPerMB=50
-ea
-Dsun.io.useCanonCaches=false
-Djava.net.preferIPv4Stack=true
-Djdk.http.auth.tunneling.disabledSchemes=""
-XX:+HeapDumpOnOutOfMemoryError
-XX:-OmitStackTraceInFastThrow
# JIT 参数
# 设置用于编译的编译器线程
-XX:CICompilerCount=2
# 开启分层编码
-XX:TieredStopAtLevel=1
# 控制最大数量嵌套调用内存
-XX:MaxInlineLevel=3
# 即时编译的东西
-XX:Tier4MinInvocationThreshold=100000
-XX:Tier4InvocationThreshold=110000
-XX:Tier4CompileThreshold=120000



-XX:ErrorFile=$USER_HOME/java_error_in_idea_%p.log
-XX:HeapDumpPath=$USER_HOME/java_error_in_idea.hprof
-javaagent:/Users/leon/tools/jetbrains-agent/lib/jetbrains-agent.jar

```





