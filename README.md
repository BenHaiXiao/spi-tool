#  spi-tool
----
## 简介 
Java SPI 全称为 (Service Provider Interface) ,是JDK内置的一种服务提供发现机制；
普遍使用的框架有：

1. java.sql.Driver 数据库驱动（mysql驱动、oracle驱动等）;
2. common-logging 的日志接口实现;
3. dubbo的扩展实现等等;

在自定义组件使用广泛，提供了高扩展性；
该项目主要对Java SPI提供统一封装和规范，便于相关扩展组件的开发，
为基于Java SPI的**可插拔**扩展工具。提供简洁便利的SPI使用。
## spi-tool扩展器使用方法

1. META-INF/spi-tool/  目录命名规则
目录为接口名称(需要进行SPI扩展的接口)，例如：
  
  ```com.github.benhaixiao.spi.tool.ext1.Ext1```
  
目录内容为接口实现，实现名称和具体实现类，例如：

     ```
    impl1=com.github.benhaixiao.spi.tool.Ext1Impl1
    
    impl2=com.github.benhaixiao.spi.tool.Ext1Impl2
     ```

2. 获取实现类方法：
    ```
    Ext1 ext1 = SPILoader.getSPILoader(Ext1.class).getSPI("impl1")
    
    ```
**注意**：需要进行SPI扩展的接口上，必须打上SPI注解
```@SPI
```
