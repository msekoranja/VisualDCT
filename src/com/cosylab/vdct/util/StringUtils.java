package com.cosylab.vdct.util;

import java.awt.Color;

/**
 * This type was created in VisualAge.
 */
public class StringUtils {
	private static final String ZERO = "0";
	private static final String ONE = "1";
	private static final String HEX = "0x";

	private static final String nullString = "";

	private static final String QUOTE = "\"";
	private static final String nonMacroChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-:[]<>;";
/**
 * This method was created in VisualAge.
 * @return java.lang.String
 * @param state boolean
 */
public static String boolean2str(boolean state) {
	if (state) return ONE;
	else return ZERO;
}
/**
 * Insert the method's description here.
 * Creation date: (23.4.2001 18:49:45)
 * @return java.lang.String
 * @param color java.awt.Color
 */
public static String color2string(java.awt.Color color) {
//	return HEX+Integer.toHexString(color.getRGB() & 0xffffff);
	if (color==null)
		return ZERO;
	else
		return Integer.toString(color.getRGB() & 0xffffff);
}
/**
 * This method was created in VisualAge.
 * @param fileName java.lang.String
 * @param newFN java.lang.String
 */
public static String getFileName(String fileName) {
	// fileName can contain path!

	int pos = fileName.lastIndexOf(java.io.File.separatorChar);
	if (pos<0) return fileName;

	return fileName.substring(pos+1);
	
}
/**
 * Insert the method's description here.
 * Creation date: (23.4.2001 18:52:04)
 * @return java.awt.Color
 * @param rgb int
 */
public static java.awt.Color int2color(int rgb) {
	// !!! add more or use flyweight
	switch (rgb) {
		case 0x000000 : return Color.black;
		case 0x0000ff : return Color.blue;
		case 0x00ff00 : return Color.green;
		case 0xff0000 : return Color.red;
		case 0xffffff : return Color.white;
		default: return new Color(rgb);
	}
	
}
/**
 * This method was created in VisualAge.
 * @return java.lang.String
 * @param str java.lang.String
 */
public static String quoteIfMacro(String str) {

	boolean needsQuotes = false;
	int len = str.length();

	for (int i=0; (i<len) && !needsQuotes; i++)
		if (nonMacroChars.indexOf(str.charAt(i))<0) needsQuotes=true;
			
	if (needsQuotes) return QUOTE+str+QUOTE;
	else return str;
}
/**
 * This method was created in VisualAge.
 * @param str java.lang.String
 * @param begining java.lang.String
 */
public static String removeBegining(String str, String begining) {
	if (begining.equals(nullString)) return str;
	else if (str.startsWith(begining)) return str.substring(begining.length());
	else return str;
}
/**
 * This method was created in VisualAge.
 * @param str java.lang.String
 * @param s1 java.lang.String
 * @param s2 java.lang.String
 */
public static String replace(String str, String s1, String s2) {
	if (str.equals(s1)) return s2;
	int pos = str.indexOf(s1);
	if (pos<0) return str;
	String p1 = str.substring(0, pos);
	String p2 = str.substring(pos+s1.length());
	return p1+s2+p2;
}
/**
 * This method was created in VisualAge.
 * @param fileName java.lang.String
 * @param newFN java.lang.String
 */
public static String replaceFileName(String fileName, String newFN) {
	// fileName can contain path!

	int pos = fileName.lastIndexOf(java.io.File.separatorChar);
	if (pos<0) return newFN;

	String onlyFN = fileName.substring(pos+1);
	
	return replace(fileName, onlyFN, newFN);
	
}
/**
 * This method was created in VisualAge.
 * @return boolean
 * @param str java.lang.String
 */
public static boolean str2boolean(String str) {
	return str.trim().equals(ONE);
}
/**
 * Insert the method's description here.
 * Creation date: (23.4.2001 18:52:04)
 * @return java.awt.Color
 * @param str java.lang.String
 */
public static java.awt.Color string2color(String str) {
	int rgb = Integer.parseInt(str);
	// !!! add more or use flyweight
	switch (rgb) {
		case 0x000000 : return Color.black;
		case 0x0000ff : return Color.blue;
		case 0x00ff00 : return Color.green;
		case 0xff0000 : return Color.red;
		case 0xffffff : return Color.white;
		default: return new Color(rgb);
	}
	
}
}