package net.sf.xsshtmlfilter;

import net.sf.xsshtmlfilter.HTMLFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 *
 */
public class HTMLFilterTest {
    protected HTMLFilter vFilter;

    @Before
    public void setUp() {
        vFilter = new HTMLFilter(true);
    }

    @After
    public void tearDown() {
        vFilter = null;
    }

    @Test
    public void testBasics() {
        assertThat(vFilter.filter(""), is(""));
        assertThat(vFilter.filter("hello"), is("hello"));
    }

    @Test
    public void testBalancingTags() {
        assertThat(vFilter.filter("<b>hello"), is("<b>hello</b>"));
        assertThat(vFilter.filter("<b>hello"), is("<b>hello</b>"));
        assertThat(vFilter.filter("hello<b>"), is("hello"));
        assertThat(vFilter.filter("hello</b>"), is("hello"));
        assertThat(vFilter.filter("hello<b/>"), is("hello"));
        assertThat(vFilter.filter("<b><b><b>hello"), is("<b><b><b>hello</b></b></b>"));
        assertThat(vFilter.filter("</b><b>"), is(""));
    }

    @Test
    public void testEndSlashes() {
        assertThat(vFilter.filter("<img>"), is("<img />"));
        assertThat(vFilter.filter("<img/>"), is("<img />"));
        assertThat(vFilter.filter("<b/></b>"), is(""));
    }

    @Test
    public void testBalancingAngleBrackets() {
        if (vFilter.isAlwaysMakeTags()) {
            assertThat(vFilter.filter("<img src=\"foo\""), is("<img src=\"foo\" />"));
            assertThat(vFilter.filter("i>"), is(""));
            assertThat(vFilter.filter("<img src=\"foo\"/"), is("<img src=\"foo\" />"));
            assertThat(vFilter.filter(">"), is(""));
            assertThat(vFilter.filter("foo<b"), is("foo"));
            assertThat(vFilter.filter("b>foo"), is("<b>foo</b>"));
            assertThat(vFilter.filter("><b"), is(""));
            assertThat(vFilter.filter("b><"), is(""));
            assertThat(vFilter.filter("><b>"), is(""));
        } else {
            assertThat(vFilter.filter("<img src=\"foo\""), is("&lt;img src=\"foo\""));
            assertThat(vFilter.filter("b>"), is("b&gt;"));
            assertThat(vFilter.filter("<img src=\"foo\"/"), is("&lt;img src=\"foo\"/"));
            assertThat(vFilter.filter(">"), is("&gt;"));
            assertThat(vFilter.filter("foo<b"), is("foo&lt;b"));
            assertThat(vFilter.filter("b>foo"), is("b&gt;foo"));
            assertThat(vFilter.filter("><b"), is("&gt;&lt;b"));
            assertThat(vFilter.filter("b><"), is("b&gt;&lt;"));
            assertThat(vFilter.filter("><b>"), is("&gt;"));
        }
    }

    @Test
    public void testAttributes() {
        assertThat(vFilter.filter("<img src=foo>"), is("<img src=\"foo\" />"));
        assertThat(vFilter.filter("<img asrc=foo>"), is("<img />"));
        assertThat(vFilter.filter("<img src=test test>"), is("<img src=\"test\" />"));
    }

    @Test
    public void testDisallowScriptTags() {
        assertThat(vFilter.filter("<script>"), is(""));
        String result = vFilter.isAlwaysMakeTags() ? "" : "&lt;script";
        assertThat(vFilter.filter("<script"), is(result));
        assertThat(vFilter.filter("<script/>"), is(""));
        assertThat(vFilter.filter("</script>"), is(""));
        assertThat(vFilter.filter("<script woo=yay>"), is(""));
        assertThat(vFilter.filter("<script woo=\"yay\">"), is(""));
        assertThat(vFilter.filter("<script woo=\"yay>"), is(""));
        assertThat(vFilter.filter("<script woo=\"yay<b>"), is(""));
        assertThat(vFilter.filter("<script<script>>"), is(""));
        assertThat(vFilter.filter("<<script>script<script>>"), is("script"));
        assertThat(vFilter.filter("<<script><script>>"), is(""));
        assertThat(vFilter.filter("<<script>script>>"), is(""));
        assertThat(vFilter.filter("<<script<script>>"), is(""));
    }

    @Test
    public void testProtocols() {
        assertThat(vFilter.filter("<a href=\"http://foo\">bar</a>"), is("<a href=\"http://foo\">bar</a>"));
        // we don't allow ftp. t("<a href=\"ftp://foo\">bar</a>", "<a href=\"ftp://foo\">bar</a>");
        assertThat(vFilter.filter("<a href=\"mailto:foo\">bar</a>"), is("<a href=\"mailto:foo\">bar</a>"));
        assertThat(vFilter.filter("<a href=\"javascript:foo\">bar</a>"), is("<a href=\"#foo\">bar</a>"));
        assertThat(vFilter.filter("<a href=\"java script:foo\">bar</a>"), is("<a href=\"#foo\">bar</a>"));
        assertThat(vFilter.filter("<a href=\"java\tscript:foo\">bar</a>"), is("<a href=\"#foo\">bar</a>"));
        assertThat(vFilter.filter("<a href=\"java\nscript:foo\">bar</a>"), is("<a href=\"#foo\">bar</a>"));
        assertThat(vFilter.filter("<a href=\"java" + HTMLFilter.chr(1) + "script:foo\">bar</a>"), is("<a href=\"#foo\">bar</a>"));
        assertThat(vFilter.filter("<a href=\"jscript:foo\">bar</a>"), is("<a href=\"#foo\">bar</a>"));
        assertThat(vFilter.filter("<a href=\"vbscript:foo\">bar</a>"), is("<a href=\"#foo\">bar</a>"));
        assertThat(vFilter.filter("<a href=\"view-source:foo\">bar</a>"), is("<a href=\"#foo\">bar</a>"));
    }

    @Test
    public void testSelfClosingTags() {
        assertThat(vFilter.filter("<img src=\"a\">"), is("<img src=\"a\" />"));
        assertThat(vFilter.filter("<img src=\"a\">foo</img>"), is("<img src=\"a\" />foo"));
        assertThat(vFilter.filter("</img>"), is(""));
    }

    @Test
    public void testComments() {
        if (vFilter.isStripComments()) {
            assertThat(vFilter.filter("<!-- a<b --->"), is(""));
        } else {
            assertThat(vFilter.filter("<!-- a<b --->"), is("<!-- a&lt;b --->"));
        }
    }

    @Test
    public void testEntities() {
        assertThat(vFilter.filter("&nbsp;"), is("&amp;nbsp;"));
        assertThat(vFilter.filter("&amp;"), is("&amp;"));
        assertThat(vFilter.filter("test &nbsp; test"), is("test &amp;nbsp; test"));
        assertThat(vFilter.filter("test &amp; test"), is("test &amp; test"));
        assertThat(vFilter.filter("&nbsp;&nbsp;"), is("&amp;nbsp;&amp;nbsp;"));
        assertThat(vFilter.filter("&amp;&amp;"), is("&amp;&amp;"));
        assertThat(vFilter.filter("test &nbsp;&nbsp; test"), is("test &amp;nbsp;&amp;nbsp; test"));
        assertThat(vFilter.filter("test &amp;&amp; test"), is("test &amp;&amp; test"));
        assertThat(vFilter.filter("&amp;&nbsp;"), is("&amp;&amp;nbsp;"));
        assertThat(vFilter.filter("test &amp;&nbsp; test"), is("test &amp;&amp;nbsp; test"));
    }

    @Test
    public void testDollar() {
        String text = "modeling & US MSRP $81.99, (Not Included)";
        String result = "modeling &amp; US MSRP $81.99, (Not Included)";

        assertThat(vFilter.filter(text), is(result));
    }

    @Test
    public void testBr() {
        final Map<String, List<String>> allowed = new HashMap<String, List<String>>();
        for(String allow : "span;br;b;strong;em;u;i".split("\\s*;\\s*")){
            if(0 < allow.indexOf(':')){
                final String name = allow.split(":")[0];
                final String[] attributes = allow.split(":")[0].split("\\s*,\\s*");
                allowed.put(name, Arrays.asList(attributes));
            }else{
                allowed.put(allow, Collections.<String>emptyList());
            }
        }

        Map<String,Object> config = new HashMap<String,Object>(){{
        put("vAllowed", allowed);
        put("vSelfClosingTags", "img,br".split("\\s*,\\s*"));
        put("vNeedClosingTags", "a,b,strong,i,em".split("\\s*,\\s*"));
        put("vDisallowed", "".split("\\s*,\\s*"));
        put("vAllowedProtocols", "src,href".split("\\s*,\\s*"));
        put("vProtocolAtts", "".split("\\s*,\\s*"));
        put("vRemoveBlanks", "a,b,strong,i,em".split("\\s*,\\s*"));
        put("vAllowedEntities", "mdash,euro,quot,amp,lt,gt,nbsp,iexcl,cent,pound,curren,yen,brvbar,sect,uml,copy,ordf,laquo,not,shy,reg,macr,deg,plusmn,sup2,sup3,acute,micro,para,middot,cedil,sup1,ordm,raquo,frac14,frac12,frac34,iquest,Agrave,Aacute,Acirc,Atilde,Auml,Aring,AElig,Ccedil,Egrave,Eacute,Ecirc,Euml,Igrave,Iacute,Icirc,Iuml,ETH,Ntilde,Ograve,Oacute,Ocirc,Otilde,Ouml,times,Oslash,Ugrave,Uacute,Ucirc,Uuml,Yacute,THORN,szlig,agrave,aacute,acirc,atilde,auml,aring,aelig,ccedil,egrave,eacute,ecirc,euml,igrave,iacute,icirc,iuml,eth,ntilde,ograve,oacute,ocirc,otilde,ouml,divide,oslash,ugrave,uacute,ucirc,uuml,yacute,thorn,yuml,#34,#38,#60,#62,#160,#161,#162,#163,#164,#165,#166,#167,#168,#169,#170,#171,#172,#173,#174,#175,#176,#177,#178,#179,#180,#181,#182,#183,#184,#185,#186,#187,#188,#189,#190,#191,#192,#193,#194,#195,#196,#197,#198,#199,#200,#201,#202,#203,#204,#205,#206,#207,#208,#209,#210,#211,#212,#213,#214,#215,#216,#217,#218,#219,#220,#221,#222,#223,#224,#225,#226,#227,#228,#229,#230,#231,#232,#233,#234,#235,#236,#237,#238,#239,#240,#241,#242,#243,#244,#245,#246,#247,#248,#249,#250,#251,#252,#253,#254,#255".split("\\s*,\\s*"));
        put("stripComment", Boolean.TRUE);
        put("alwaysMakeTags", Boolean.TRUE);
        }};

        vFilter = new HTMLFilter(config);

        assertThat(vFilter.filter("test <br> test <br>"), is("test <br /> test <br />"));
    }

}
