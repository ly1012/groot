package com.liyunx.groot.util;

import org.apache.commons.lang3.StringUtils;

/**
 * 字符串工具类
 */
public class StringUtil {

    public static String splitAndGetLastString(String text, String regex) {
        String[] splitArr = text.split(regex);
        return splitArr[splitArr.length - 1];
    }

    public static boolean hasValue(String s) {
        return s != null && !s.trim().isEmpty();
    }

    /**
     * 字符串是否以 http:// 或 https:// 开头，不区分大小写
     *
     * @param url URL 字符串
     * @return 如果是 HTTP/HTTPS 协议，返回 true；否则返回 false
     */
    public static boolean isHttpOrHttps(String url) {
        return url.regionMatches(true, 0, "http://", 0, 7)
            || url.regionMatches(true, 0, "https://", 0, 8);
    }

    public static String capitalize(final String str) {
        return StringUtils.capitalize(str);
    }

    /**
     * 当值为 null、空值、一个或多个不可见字符时返回 true
     *
     * @see StringUtils#isBlank(CharSequence)
     */
    public static boolean isBlank(CharSequence cs) {
        return StringUtils.isBlank(cs);
    }

    /**
     * 重复字符串 {@code repeat} 次。
     *
     * <pre>
     * StringUtils.repeat(null, 2) = null
     * StringUtils.repeat("", 0)   = ""
     * StringUtils.repeat("", 2)   = ""
     * StringUtils.repeat("a", 3)  = "aaa"
     * StringUtils.repeat("ab", 2) = "abab"
     * StringUtils.repeat("a", -2) = ""
     * </pre>
     *
     * @param str    被重复的字符串，可能为 null
     * @param repeat 重复次数，非正数将返回空字符串
     * @return 重复 repeat 次的新字符串，如果输入为 null，返回 null
     */
    public static String repeat(final String str, final int repeat) {
        return StringUtils.repeat(str, repeat);
    }

    /**
     * <p>填充字符串到指定长度，使用指定字符，左对齐（右侧填充）。</p>
     *
     * <pre>
     * StringUtils.rightPad(null, *, *)     = null
     * StringUtils.rightPad("", 3, 'z')     = "zzz"
     * StringUtils.rightPad("bat", 3, 'z')  = "bat"
     * StringUtils.rightPad("bat", 5, 'z')  = "batzz"
     * StringUtils.rightPad("bat", 1, 'z')  = "bat"
     * StringUtils.rightPad("bat", -1, 'z') = "bat"
     * </pre>
     *
     * @param str     基础字符串，可能为 null
     * @param size    填充后的长度
     * @param padChar 填充字符
     * @return 填充后的字符串，或者原字符串（如果不需要填充）。如果字符串输入为 null，返回 null。
     */
    public static String rightPad(final String str, final int size, final char padChar) {
        return StringUtils.rightPad(str, size, padChar);
    }

    public static String rightPad(final String str, final int size, final String padChar) {
        return StringUtils.rightPad(str, size, padChar);
    }

    public static String leftPad(final String str, final int size, final char padChar) {
        return StringUtils.leftPad(str, size, padChar);
    }

    public static String leftPad(final String str, final int size, final String padChar) {
        return StringUtils.leftPad(str, size, padChar);
    }

}

