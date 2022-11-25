package com.jack.jfx;

import com.jack.jfx.execption.CssRootNodeParseException;
import com.steadystate.css.parser.CSSOMParser;
import org.w3c.css.sac.InputSource;
import org.w3c.dom.css.CSSRuleList;
import org.w3c.dom.css.CSSStyleRule;
import org.w3c.dom.css.CSSStyleSheet;

import java.io.*;

/**
 * @author gj
 * @date 2021/4/26 10:16
 */
public class CssParser {
    public static void main(String[] args) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(new File("src\\main\\resources\\css\\demo.css"));
            parseRoot("inputStream");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * * 打印样式文件内容
     * * @param filePath 样式本地文件路径
     * * @param selectorText 属性名称
     * * @return
     */


    public static boolean showCssText(InputStream inStream) {
        try {
            InputSource source = new InputSource();
            source.setByteStream(inStream);
            source.setEncoding("UTF-8");
            final CSSOMParser parser = new CSSOMParser();
            CSSStyleSheet sheet = parser.parseStyleSheet(source, null, null);
            CSSRuleList rules = sheet.getCssRules();
            if (rules.getLength() == 0) {
                return false;
            }
            for (int i = 0; i < rules.getLength(); i++) {
                CSSStyleRule rule = (CSSStyleRule) rules.item(i);
                //获取样式名称
                String selectorText = rule.getSelectorText();
                System.out.println(selectorText);
                String cssText = rule.getCssText();
                System.out.println(cssText);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
        return false;
    }

    /**
     * 解析root结点，缓存root结点的变量集合
     *
     * @param cssText
     * @throws Exception
     */
    private static void parseRoot(String cssText) throws Exception {
        cssText = ".root { var_version: 60; var_Docker: 20; var_backColor: rgb(255, 255, 255) }";
        if (cssText == null) {
            throw new CssRootNodeParseException();
        }
        if (!cssText.startsWith(".root")) {
            return;
        }
        String nodeVal = cssText.replaceAll("\\.root", "").trim();
        if (nodeVal.startsWith("{") && nodeVal.endsWith("}")) {
            String formatVal = nodeVal.replaceAll("\\{", "").replaceAll("}", "");
            String[] split = formatVal.split(";");
            for (int i = 0; i < split.length; i++) {
                String var = split[i].trim();
                if (!var.startsWith("var_")) {
                    continue;
                }
                var = var.replaceAll("var_", "");
                String[] splitVal = var.split(":");
                if (splitVal.length != 2) {
                    continue;
                }
                String valKey = splitVal[0].trim();
                String valVal = splitVal[1].trim();
            }
        }
    }

}
