# spi-tool
----
基于Java SPI的**可插拔**扩展工具。提供简洁便利的SPI使用。
## spi-tool扩展器使用方法
1、META-INF/spi-tool/  目录命名规则
目录为接口名称，例如：com.github.benhaixiao.spi.tool.ext1.Ext1
目录内容为接口实现，实现名称和具体实现类，例如：
impl1=com.github.benhaixiao.spi.tool.Ext1Impl1
impl2=com.github.benhaixiao.spi.tool.Ext1Impl2

2、获取实现类方法：Ext1 ext1 = SPILoader.getSPILoader(Ext1.class).getSPI("impl1")

