package com.github.benhaixiao.spi.tool;


import com.github.benhaixiao.spi.tool.ext1.Ext1;
import com.github.benhaixiao.spi.tool.ext1.Ext1Impl1;
import com.github.benhaixiao.spi.tool.ext1.Ext1Impl2;
import com.github.benhaixiao.spi.tool.ext2.Ext2;
import com.github.benhaixiao.spi.tool.ext2.Ext2Impl1;
import com.github.benhaixiao.spi.tool.ext2.Ext2Impl2;
import com.github.benhaixiao.spi.tool.nospi.NoSpiExt;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.allOf;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.containsString;

/**
 * @author xiaobenhai
 * Date: 2016/11/23
 * Time: 17:13
 */
public class SPILoaderTest {

    @Test
    public void test_getSPILoader_Null() throws Exception {
        try {
            SPILoader.getSPILoader(null);
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(),
                    containsString("spi-tool type == null"));
        }
    }

    @Test
    public void test_getSPILoader_NotInterface() throws Exception {
        try {
            SPILoader.getSPILoader(SPILoaderTest.class);
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(),
                    containsString("spi-tool type(class com.github.benhaixiao.spi.tool.SPILoaderTest) is not interface"));
        }
    }

    @Test
    public void test_getSPILoader_NotSpiAnnotation() throws Exception {
        try {
            SPILoader.getSPILoader(NoSpiExt.class);
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(),
                    allOf(containsString("com.github.benhaixiao.spi.tool.nospi.NoSpiExt"),
                            containsString("is not spi-tool"),
                            containsString("WITHOUT @SPI Annotation")));
        }
    }

    @Test
    public void test_getSPI() throws Exception {
        assertTrue(SPILoader.getSPILoader(Ext1.class).getSPI("ext1impl1") instanceof Ext1Impl1);
        assertTrue(SPILoader.getSPILoader(Ext1.class).getSPI("ext1impl2") instanceof Ext1Impl2);
        Assert.assertEquals(SPILoader.getSPILoader(Ext1.class).getSPI("ext1impl1").echo(), "Ext1Impl1");
        Assert.assertEquals(SPILoader.getSPILoader(Ext1.class).getSPI("ext1impl2").echo(), "Ext1Impl2");

        assertTrue(SPILoader.getSPILoader(Ext2.class).getSPI("ext2impl1") instanceof Ext2Impl1);
        assertTrue(SPILoader.getSPILoader(Ext2.class).getSPI("ext2impl2") instanceof Ext2Impl2);
        Assert.assertEquals(SPILoader.getSPILoader(Ext2.class).getSPI("ext2impl1").echo(), "Ext2Impl1");
        Assert.assertEquals(SPILoader.getSPILoader(Ext2.class).getSPI("ext2impl2").echo(), "Ext2Impl2");
    }


}