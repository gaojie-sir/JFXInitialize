package com.jack.jfx.helper;

import com.jack.jfx.call.CssUpdateCallBack;
import com.jack.jfx.execption.CssRootNodeParseException;
import com.steadystate.css.parser.CSSOMParser;
import javafx.beans.property.SimpleStringProperty;
import org.w3c.css.sac.InputSource;
import org.w3c.dom.css.CSSRuleList;
import org.w3c.dom.css.CSSStyleRule;
import org.w3c.dom.css.CSSStyleSheet;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author gj
 * @date 2021/4/26 10:48
 */
public class CssParser {
    private static final String ROOT_NAME = ".root", PRE_SUFFIX = "{", REG_PRE_SUFFIX = "\\{", END_SUFFIX = "}",
            VAR_TEXT = "var_", VAL_SPLIT_TAG = ":", VAR_SPLIT_TAG = ";", EMPTY_CHAR = "", QUOTATION_MARK = "\"",
            CSS_SUFFIX = ".css", CSS_ROOT_DIR = "./resources/css/", QUOTATION_MARK_ESC = "\\\\\"";
    private final Map<String, SimpleStringProperty> rootVariable = new HashMap<>();
    private static final Pattern COMPILE = Pattern.compile("\"\\{([\\s\\S]*?)}\"");
    private static final Pattern URL_COMPILE = Pattern.compile("url\\(([\\s\\S]*?)\\)");
    private boolean firstLoad = true;

    /**
     * 原点CSS文件路径
     */
    private final String originCssPath;

    private CssUpdateCallBack cssUpdateCallBack;

    public CssParser(String originCssPath) {
        this.originCssPath = originCssPath;
    }

    public CssUpdateCallBack getCssUpdateCallBack() {
        return cssUpdateCallBack;
    }

    public void setCssUpdateCallBack(CssUpdateCallBack cssUpdateCallBack) {
        this.cssUpdateCallBack = cssUpdateCallBack;
    }

    /**
     * 解析CSS
     * 将HNF自定义的CSS格式转换成JAVAFX CSS
     * eg:
     * .root {
     * var_version: 60px;
     * var_Docker: 20px;
     * var_backColor: #FFFFFF;
     * }
     * <p>
     * #color1 {
     * -fx-background-color: "{version}";
     * -fx-background-color: "{Docker}";
     * }
     *
     * @return
     */
    public String parseCss(boolean reload) throws Exception {
        String newCssPath = null;
        if (!reload) {
            newCssPath = getPreTempCss();
        }
        FileWriter fileWriter = null;
        String tempCssFile = null;
        if (newCssPath == null) {
            tempCssFile = getTempCssFile();
            fileWriter = new FileWriter(new File(tempCssFile));
        }
        try (InputStream orgFis = CssParser.class.getClassLoader().getResourceAsStream(originCssPath)) {
            InputSource source = new InputSource();
            source.setByteStream(orgFis);
            source.setEncoding("UTF-8");
            final CSSOMParser parser = new CSSOMParser();
            CSSStyleSheet sheet = parser.parseStyleSheet(source, null, null);
            CSSRuleList rules = sheet.getCssRules();
            if (rules.getLength() == 0) {
                return null;
            }
            for (int i = 0; i < rules.getLength(); i++) {
                CSSStyleRule rule = (CSSStyleRule) rules.item(i);
                //获取样式名称
                String selectorText = rule.getSelectorText();
                //获取样式内容
                String cssText = rule.getCssText();
                if (".root".equals(selectorText)) {
                    if (firstLoad) {
                        try {
                            parseRoot(cssText);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    firstLoad = false;
                } else {
                    String formatCssText = parseNode(cssText);
                    if (fileWriter != null) {
                        fileWriter.write(formatCssText);
                    }
                }
            }
            if (fileWriter != null) {
                fileWriter.flush();
                return new File(tempCssFile).toURI().toURL().toExternalForm();
            }
            return newCssPath;
        } catch (Exception e) {
            throw e;
        } finally {
            if (fileWriter != null) {
                fileWriter.close();
            }
        }
    }


    /**
     * 解析root结点，缓存root结点的变量集合
     *
     * @param cssText
     * @throws Exception
     */
    private void parseRoot(String cssText) throws Exception {
        if (cssText == null) {
            throw new CssRootNodeParseException();
        }
        if (!cssText.startsWith(ROOT_NAME)) {
            return;
        }
        String nodeVal = cssText.replaceAll(ROOT_NAME, EMPTY_CHAR).trim();
        if (nodeVal.startsWith(PRE_SUFFIX) && nodeVal.endsWith(END_SUFFIX)) {
            String formatVal = nodeVal.replaceAll(REG_PRE_SUFFIX, EMPTY_CHAR).replaceAll(END_SUFFIX, EMPTY_CHAR);
            String[] split = formatVal.split(VAR_SPLIT_TAG);
            for (int i = 0; i < split.length; i++) {
                String var = split[i].trim();
                if (!var.startsWith(VAR_TEXT)) {
                    continue;
                }
                var = var.replaceAll(VAR_TEXT, EMPTY_CHAR);
                String[] splitVal = var.split(VAL_SPLIT_TAG);
                if (splitVal.length != 2) {
                    continue;
                }
                String varKey = splitVal[0].trim();
                String varVal = splitVal[1].trim();
                putVariable(varKey, varVal);
            }
        }
    }

    /**
     * @param cssText
     * @return
     */
    private String parseNode(String cssText) {
        if (cssText == null) {
            return null;
        }
        //匹配url
        List<String> url = new ArrayList<>();
        Matcher urlMatcher = URL_COMPILE.matcher(cssText);
        while (urlMatcher.find()) {
            String group = urlMatcher.group();
            url.add(group);
        }
        for (String urlText : url) {
            String s = urlText.replaceAll("\\(", "\\(\\\"").replaceAll("\\)", "\\\"\\)");
            cssText = cssText.replace(urlText, s);
        }
        //匹配变量值
        Matcher matcher = COMPILE.matcher(cssText);
        while (matcher.find()) {
            String group = matcher.group();
            String varKey = group.replaceAll(QUOTATION_MARK, EMPTY_CHAR).replaceAll(REG_PRE_SUFFIX, EMPTY_CHAR).replaceAll(END_SUFFIX, EMPTY_CHAR);
            String varVal = getVarVal(varKey);
            if (varVal != null) {
                cssText = COMPILE.matcher(cssText).replaceFirst(varVal);
            }
        }
        return cssText;
    }

    /**
     * 向变量池中添加变量
     *
     * @param varKey
     * @param varVal
     */
    public void putVariable(String varKey, String varVal) {
        SimpleStringProperty simpleStringProperty = rootVariable.get(varKey);
        if (simpleStringProperty == null) {
            SimpleStringProperty newSimpleVarProperty = new SimpleStringProperty(varVal);
            rootVariable.put(varKey, newSimpleVarProperty);
            addVarValListener(newSimpleVarProperty);
        } else {
            if (simpleStringProperty.get().equals(varVal)) {
                return;
            }
            simpleStringProperty.set(varVal);
        }
    }


    /**
     * 获取对应变量的值
     *
     * @param varKey
     * @return
     */
    private String getVarVal(String varKey) {
        SimpleStringProperty simpleStringProperty = rootVariable.get(varKey);
        if (simpleStringProperty != null) {
            return simpleStringProperty.get();
        }

        return null;
    }

    /**
     * 对应属性的值添加监听事件
     *
     * @param simpleStringProperty
     */
    private void addVarValListener(SimpleStringProperty simpleStringProperty) {
        simpleStringProperty.addListener(observable -> {
            try {
                String cssPath = parseCss(true);
                if (cssUpdateCallBack != null) {
                    //通知路径
                    cssUpdateCallBack.update(cssPath);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    /**
     * 获取临时CSS文件配置
     *
     * @return
     */
    private String getTempCssFile() {
        File file = new File(originCssPath);
        String name = file.getName().replaceAll(CSS_SUFFIX, EMPTY_CHAR) + "_" + System.currentTimeMillis() + "_temp" + CSS_SUFFIX;
        file = new File(CSS_ROOT_DIR + name);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        File[] files = file.getParentFile().listFiles();
        for (File tempFile : files) {
            tempFile.delete();
        }
        return file.getPath();
    }

    /**
     * 获取根路径下css文件
     *
     * @return
     * @throws Exception
     */
    public String getPreTempCss() throws Exception {
        File[] files = new File(CSS_ROOT_DIR).listFiles(file -> file.getName().endsWith(CSS_SUFFIX));
        if (files == null) {
            return null;
        }
        if (files.length == 0) {
            return null;
        }
        return files[0].toURI().toURL().toExternalForm();
    }


    public static void main(String[] args) {
        String cssText = ".pagination .pagination-control .right-arrow-button {\n" +
                "    -fx-background-image: url(\"/fx/img/tool/page_after.png\");\n" +
                "}";
        CssParser cssParser = new CssParser("");
        String formatCssText = cssParser.parseNode(cssText);
        System.out.println(formatCssText);
        try (FileWriter fileWriter = new FileWriter(new File("./1.css"))) {
            fileWriter.write(formatCssText);
            fileWriter.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
