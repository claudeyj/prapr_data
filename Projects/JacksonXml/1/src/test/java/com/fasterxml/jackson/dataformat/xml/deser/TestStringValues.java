package com.fasterxml.jackson.dataformat.xml.deser;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.XmlTestBase;

public class TestStringValues extends XmlTestBase
{
    protected static class Bean2
    {
        public String a, b;

        @Override
        public String toString() {
            return "[a="+a+",b="+b+"]";
        }
    }

    static class Issue167Bean {
        public String d;
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final XmlMapper MAPPER = new XmlMapper();
    
    public void testSimpleStringElement() throws Exception
    {
        // first, simple one to verify baseline
        StringBean bean = MAPPER.readValue("<StringBean><text>text!</text></StringBean>", StringBean.class);
        assertNotNull(bean);
        assertEquals("text!", bean.text);
    }
    
    public void testEmptyStringElement() throws Exception
    {
        // then with empty element
        StringBean bean = MAPPER.readValue("<StringBean><text></text></StringBean>", StringBean.class);
        assertNotNull(bean);
        // empty String or null?
        // 22-Sep-2012, tatu: Seems to be 'null', but should probably be fixed to ""
        // Also see [dataformat-xml#162]
//        assertEquals("", bean.text);
        assertNull(bean.text);
    }
    
    public void testMissingString() throws Exception
    {
        StringBean baseline = new StringBean();
        // then missing
        StringBean bean = MAPPER.readValue("<StringBean />", StringBean.class);
        assertNotNull(bean);
        assertEquals(baseline.text, bean.text);
    }

    public void testStringWithAttribute() throws Exception
    {
        // and then the money shot: with 'standard' attribute...
        StringBean bean = MAPPER.readValue("<StringBean><text xml:lang='fi'>Pulla</text></StringBean>", StringBean.class);
        assertNotNull(bean);
        assertEquals("Pulla", bean.text);
    }

    public void testStringsWithAttribute() throws Exception
    {
        Bean2 bean = MAPPER.readValue(
                "<Bean2>\n"
                +"<a xml:lang='fi'>abc</a>"
                +"<b xml:lang='en'>def</b>"
//                +"<a>abc</a><b>def</b>"
                +"</Bean2>\n",
                Bean2.class);
        assertNotNull(bean);
        assertEquals("abc", bean.a);
        assertEquals("def", bean.b);
    }
    
    public void testStringArrayWithAttribute() throws Exception
    {
        // should even work for arrays of those
        StringBean[] beans = MAPPER.readValue(
                "<StringBean>\n"
                +"<StringBean><text xml:lang='fi'>Pulla</text></StringBean>"
                +"<StringBean><text xml:lang='se'>Bulla</text></StringBean>"
                +"<StringBean><text xml:lang='en'>Good stuff</text></StringBean>"
                +"</StringBean>",
                StringBean[].class);
        assertNotNull(beans);
        assertEquals(3, beans.length);
        assertEquals("Pulla", beans[0].text);
        assertEquals("Bulla", beans[1].text);
        assertEquals("Good stuff", beans[2].text);
    }

    public void testEmptyElementToString() throws Exception
    {
        final String XML =
"<a xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>\n"+
"<d xsi:nil='true'/>\n"+
"</a>\n";
        Issue167Bean result = MAPPER.readValue(XML, Issue167Bean.class);
        assertNotNull(result);
        assertEquals("", result.d);
    }
}
