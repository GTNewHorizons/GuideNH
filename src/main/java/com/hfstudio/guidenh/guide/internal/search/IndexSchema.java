package com.hfstudio.guidenh.guide.internal.search;

public class IndexSchema {

    public static final String FIELD_GUIDE_ID = "guide_id";
    public static final String FIELD_PAGE_ID = "page_id";
    public static final String FIELD_TEXT = "page_content";
    public static final String FIELD_TITLE = "page_title";
    public static final String FIELD_LANG = "lang";
    public static final String FIELD_SEARCH_LANG = "search_lang";

    public static String getTitleField(String language) {
        return "page_title_" + language;
    }

    public static String getTextField(String language) {
        return "page_text_" + language;
    }

    public static String getFilenameField(String language) {
        return "page_filename_" + language;
    }

    public static String getKeywordField(String language) {
        return "page_keyword_" + language;
    }
}
