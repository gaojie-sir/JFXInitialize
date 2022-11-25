package com.jack.jfx.execption;

/**
 * @author gj
 * @date 2021/4/26 11:44
 */
public class CssRuleNodeParseException extends Exception {
    private int line;
    private int index;

    public CssRuleNodeParseException(int line, int index) {
        super();
        this.line = line;
        this.index = index;
    }

}
