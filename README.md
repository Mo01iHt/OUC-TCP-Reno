# OUC计算机网络大作业——TCP Reno的迭代设计

姓名：郝文轩

课程：中国海洋大学2023年秋季学期计算机网络

授课教师：洪锋

完成时间：2024.1.6

## 项目说明

| 文件名称                | 内容                                     | 备注                                                         |
| ----------------------- | ---------------------------------------- | ------------------------------------------------------------ |
| .settings/              | Eclipse IDE                              | 自动生成文件                                                 |
| bin/                    | java类文件编译                           | 自动生成文件                                                 |
| doc/                    | API文档                                  | 实验系统环境自带                                             |
| **Iteration-versions/** | 所实现的各迭代版本源代码及log            | RDT2.0+2.1 RDT2.2 RDT3.0 Selective-Response TCP-Tahoe TCP-Reno |
| lib/                    | 依赖外部库文件目录                       | TCP_TestSys_Linux.jar                                        |
| **src/**                | 主要的开发目录                           | 代码测试：TCP Reno                                           |
| .classpath              | 项目路径配置信息                         | 自动生成文件                                                 |
| .project                | Eclipse项目文件                          | 自动生成文件                                                 |
| **Log.txt**             | 发送端与接收端发包及ACK、网络状况反馈log | 实验系统环境自带                                             |
| **recvData.txt**        | 接收端接收数据结果                       | 实验系统环境自带                                             |
| **Sender.log**          | 发送方输出反馈日志                       | 记录拥塞窗口状态信息                                         |

## 项目环境

| 环境        | 版本                  |
| ----------- | --------------------- |
| OS          | Windows 11            |
| Jdk         | jdk-6u45-windows-i586 |
| Jre         | jre-6                 |
| IDE         | Eclipse 4.29.0        |
| TCP-TestSys | TCP_TestSys_Linux.jar |



