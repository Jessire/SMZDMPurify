package com.smzdm.pure;

import android.graphics.Bitmap;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import io.github.libxposed.api.XposedInterface;

public final class SmzdmCommentFilter {
    private static final String TAG = "SMZDMPure";
    public static final String MARKER = "__SMZDM_PURE_COMMENT_FILTER__";

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern EMOTION_BRACKET_PATTERN = Pattern.compile("\\[[^\\[\\]]{1,8}\\]");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("[\\s\\u00A0\\u200B-\\u200D\\uFEFF]+");
    private static final Pattern LEADING_INVITE_CODE_PATTERN = Pattern.compile(
            "^\\s*(?:邀请码?[:：\\s]*)?(?!202[0-6][01]\\d[0-3]\\d(?!\\d))\\d{8,10}[\\s,，。.:：;；\\-]*"
    );
    private static final Pattern LEADING_RAW_INVITE_CODE_PATTERN = Pattern.compile(
            "^(\\s*(?:<[^>]*>\\s*)*)(?:邀请码?[:：\\s]*)?(?!202[0-6][01]\\d[0-3]\\d(?!\\d))\\d{8,10}[\\s,，。.:：;；\\-]*"
    );

    // Pure 6-12 digit invite code (or Chinese numerals / homophones), or punctuation + digits
    private static final Pattern PURE_INVITE_CODE_PATTERN = Pattern.compile(
            "^[\\p{Punct}\\p{P}\\s]*(?:\\d{6,12}|[\\d幺要依有洞零一二三四五六七八九十壹贰叁肆伍陆柒捌玖〇把]{7,12})[来來\\p{Punct}\\p{P}\\s]*$"
    );
    // Standalone 8-11 digit PDD invite code (excluding 2020-2026 YYYYMMDD dates) embedded with filler text
    private static final Pattern STANDALONE_PDD_CODE_PATTERN = Pattern.compile("(?<!\\d)(?!202[0-6][01]\\d[0-3]\\d(?!\\d))\\d{8,11}(?!\\d)");
    // 8-10 digit invite code accompanied by PDD/team-up keywords
    private static final Pattern INVITE_CODE_WITH_CONTEXT_PATTERN = Pattern.compile(
            "(?:(?:邀请码?|助力码?|立减码?|口令|多多|拼多多|搜索|搜|复制|组队|组团|三人|3人|成团|红包|福袋|五折|5折|百亿补贴|有码|新客)[^\\d]{0,12}\\d{6,12})" +
            "|(?:\\d{6,12}[^\\d]{0,12}(?:邀请码?|助力码?|立减码?|口令|多多|拼多多|搜索|搜|复制|组队|组团|三人|3人|成团|红包|福袋|五折|5折|百亿补贴|有码|新客))"
    );
    // Explicit PDD / red-packet / 3-person group-buy spam text patterns
    private static final Pattern HARD_SPAM_TEXT_PATTERN = Pattern.compile(
            "(?:#小程序://拼多多)" +
            "|(?:[⇥⇤💲🎵🔐]{1,2}[A-Za-z0-9_\\-]{8,}[⇥⇤💲🎵🔐]{1,2})" +
            "|(?:打不开[？?]点击上方空白处返回)" +
            "|(?:第二张的?领完之后|等下再领第三张|领完之后.*再领第[二三23]张)" +
            "|(?:三个人组个?团|三人团|3人团|互相领红包|互领红包|组团领红包|组队领红包|百补红包|补贴红包|百亿红包|瓜分红包)" +
            "|(?:有码立减|品牌新客立减|新客立减码|互助码|助力码)" +
            "|(?:打开拼多多APP|搜索以下数字邀请码|抽福袋.*邀请码|福袋专享.*百亿补)" +
            "|(?:下面[一二三两123]个都可以|就差你了)" +
            "|(?:取件码|极兔速递|菜鸟驿站)" +
            "|(?:^[A-Za-z0-9]{6,10}$)" +
            "|(?:[A-Za-z0-9]{5,}[^\\w\\s]+[A-Za-z0-9]{5,})" +
            "|(?:第一步.*(?:第二步|第三步|第三部|领红包|扫码))" +
            "|(?:(?:首单礼金|首购礼金|首单金)[^。！？!?]{0,60}(?:私我|私信|加我|加V|退款|退货|分成|对半|返现|返利|到账|账户))" +
            "|(?:(?:私我|私信我|加我|加V|加微信|加企鹅|QQ群)[^。！？!?]{0,50}(?:首单|礼金|红包|退款|分成|返现|返利|上车))" +
            "|(?:(?:退款|退货|刷单|代拍|薅羊毛|撸货)[^。！？!?]{0,50}(?:分成|对半|五五|返现|返利|账户|到账|私我))"
    );
    // Short team-up / red-packet filler when comment also contains an image (<= 35 chars)
    private static final Pattern IMAGE_SHORT_SPAM_PHRASE_PATTERN = Pattern.compile(
            "(?:第[一二三123][步部]|图[一二三123]|第[二三23]张|借楼|来个人|来人|来[一二两12]个?人?|差[一二两12]个?人?|二缺一|三缺一|一缺二|" +
            "组队|组团|拼团|成团|互助|领红包|红包|福袋|五折|5折|立减|新客|秒成|热乎|刚开|最新|搜就行|复制|速来|速度|上车|扫码|扫一扫)"
    );
    // Short solicitation / team-up filler when comment is text-only (<= 12 chars), excluding normal coupon/deal words like 五折/福袋/上车
    private static final Pattern TEXT_SHORT_SPAM_PHRASE_PATTERN = Pattern.compile(
            "(?:第[一二三123][步部]|图[一二三123]|第[二三23]张|借楼|来个人|来人|来[一二两12]个?人?|差[一二两12]个?人?|差一|差1|新车|二缺一|三缺一|一缺二|" +
            "组队|组团|拼团|成团|互助|领红包|一起抽福袋|抽5折|热火的|秒成|热乎|刚开|搜就行|复制|速来|扫码|扫一扫)"
    );
    // Image commentary where text is only meaningless filler like "刚刚", "111", ".", etc.
    private static final Pattern IMAGE_FILLER_TEXT_PATTERN = Pattern.compile(
            "^(?:刚刚|刚|1+|2+|3+|6+|8+|9+|\\.+|，+|。+|！+|\\?+|？+|~+|up|dd|kk|ok|顶|同|同上|如图|看图|图片|上图|来|冲|好|可|加油|复制)+$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern NON_WORD_OR_DIGIT_ONLY_PATTERN = Pattern.compile("^[\\p{Punct}\\p{P}\\s\\d]+$");
    private static final Pattern DIGIT_GAP_PATTERN = Pattern.compile("(?<=\\d)[\\s\\-.点的_号楼月日年个分秒钟哦是来來]+(?=\\d)");
    private static final Pattern SHARE_IMAGE_URL_PATTERN = Pattern.compile(
            "(?i)(pinduoduo|yangkeduo|pdd|redpacket|red_packet|share|红包|助力|拼团)"
    );

    private SmzdmCommentFilter() {
    }

    public static void install(XposedInterface xposed, ClassLoader cl) {
        try {
            WebView.setWebContentsDebuggingEnabled(true);
        } catch (Throwable ignored) {
        }

        // 1. Hook DetailWebViewClient.loadUrl(String, WebView)
        Class<?> detailClientClass = ReflectUtils.findClass("com.smzdm.core.detail_js.DetailWebViewClient", cl);
        if (detailClientClass != null) {
            ReflectUtils.hookMethod(xposed, detailClientClass, "loadUrl",
                    new Class<?>[]{String.class, WebView.class}, chain -> {
                        Object htmlObj = chain.getArg(0);
                        Object wvObj = chain.getArg(1);
                        if (htmlObj instanceof String) {
                            String injected = injectHtml((String) htmlObj);
                            return chain.proceed(new Object[]{injected, wvObj});
                        }
                        return chain.proceed();
                    });

            ReflectUtils.hookMethod(xposed, detailClientClass, "onPageStarted",
                    new Class<?>[]{WebView.class, String.class, Bitmap.class}, chain -> {
                        Object res = chain.proceed();
                        try {
                            WebView.setWebContentsDebuggingEnabled(true);
                            WebView wv = (WebView) chain.getArg(0);
                            if (wv != null) {
                                wv.evaluateJavascript(getCommentFilterJs(), null);
                            }
                        } catch (Throwable ignored) {
                        }
                        return res;
                    });

            ReflectUtils.hookMethod(xposed, detailClientClass, "onPageFinished",
                    new Class<?>[]{WebView.class, String.class}, chain -> {
                        Object res = chain.proceed();
                        try {
                            WebView wv = (WebView) chain.getArg(0);
                            if (wv != null) {
                                wv.evaluateJavascript(getCommentFilterJs(), null);
                            }
                        } catch (Throwable ignored) {
                        }
                        return res;
                    });
        }

        // 2. Hook android.webkit.WebView.loadDataWithBaseURL directly as a universal guarantee
        ReflectUtils.hookMethod(xposed, WebView.class, "loadDataWithBaseURL",
                new Class<?>[]{String.class, String.class, String.class, String.class, String.class}, chain -> {
                    try {
                        WebView.setWebContentsDebuggingEnabled(true);
                    } catch (Throwable ignored) {
                    }
                    Object baseUrl = chain.getArg(0);
                    Object data = chain.getArg(1);
                    Object mimeType = chain.getArg(2);
                    Object encoding = chain.getArg(3);
                    Object historyUrl = chain.getArg(4);
                    if (data instanceof String) {
                        String html = (String) data;
                        if (html.contains("<head") || html.contains("<html") || html.contains("smzdm")) {
                            String injected = injectHtml(html);
                            return chain.proceed(new Object[]{baseUrl, injected, mimeType, encoding, historyUrl});
                        }
                    }
                    return chain.proceed();
                });

        // 3. Hook WebViewClient.onPageFinished for any other WebViewClient subclasses
        ReflectUtils.hookMethod(xposed, WebViewClient.class, "onPageFinished",
                new Class<?>[]{WebView.class, String.class}, chain -> {
                    Object res = chain.proceed();
                    try {
                        WebView wv = (WebView) chain.getArg(0);
                        if (wv != null) {
                            wv.evaluateJavascript(getCommentFilterJs(), null);
                        }
                    } catch (Throwable ignored) {
                    }
                    return res;
                });

        // 4. Hook native CommentNewBean & CommentNewBean$Data
        Class<?> commentNewBeanClass = ReflectUtils.findClass("com.smzdm.client.android.bean.CommentNewBean", cl);
        if (commentNewBeanClass != null) {
            ReflectUtils.hookAllMethods(xposed, commentNewBeanClass, "getCommentNewRows", chain -> {
                Object listObj = chain.proceed();
                if (listObj instanceof List<?>) {
                    filterCommentItemList((List<?>) listObj);
                }
                return listObj;
            });
            ReflectUtils.hookAllMethods(xposed, commentNewBeanClass, "getDatas", chain -> {
                Object dataObj = chain.proceed();
                filterNativeCommentData(dataObj);
                return dataObj;
            });
        }

        Class<?> commentDataClass = ReflectUtils.findClass("com.smzdm.client.android.bean.CommentNewBean$Data", cl);
        if (commentDataClass != null) {
            ReflectUtils.hookAllMethods(xposed, commentDataClass, "getRows", chain -> {
                Object listObj = chain.proceed();
                if (listObj instanceof List<?>) {
                    filterCommentItemList((List<?>) listObj);
                }
                return listObj;
            });
            ReflectUtils.hookAllMethods(xposed, commentDataClass, "getTop", chain -> {
                Object listObj = chain.proceed();
                if (listObj instanceof List<?>) {
                    filterCommentItemList((List<?>) listObj);
                }
                return listObj;
            });
        }

        Log.i(TAG, "SmzdmCommentFilter hooks installed.");
    }

    public static String cleanCommentText(String rawContent) {
        if (rawContent == null || rawContent.isEmpty()) {
            return "";
        }
        String s = HTML_TAG_PATTERN.matcher(rawContent).replaceAll("");
        s = s.replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">");
        s = EMOTION_BRACKET_PATTERN.matcher(s).replaceAll("");
        s = WHITESPACE_PATTERN.matcher(s).replaceAll(" ").trim();
        return s;
    }

    public static String stripLeadingInviteCodeIfLongReview(String rawContent) {
        if (rawContent == null || rawContent.isEmpty()) return rawContent;
        String clean = cleanCommentText(rawContent);
        if (clean.length() < 30) return rawContent;
        String strippedClean = LEADING_INVITE_CODE_PATTERN.matcher(clean).replaceFirst("").trim();
        if (strippedClean.length() < 20 || strippedClean.equals(clean)) {
            return rawContent;
        }
        return LEADING_RAW_INVITE_CODE_PATTERN.matcher(rawContent).replaceFirst("$1");
    }

    public static boolean isPddPosterDimension(int width, int height) {
        if (width <= 0 || height <= 0) return false;
        if (width == 1200 && (height == 1500 || height == 1770 || height == 1560 || height == 1600 || height == 2670)) {
            return true;
        }
        if (width == 560 && height == 646) {
            return true;
        }
        if (width == 1536 && (height == 2048 || height == 2275)) {
            return true;
        }
        if (width == 1152 && height == 2048) {
            return true;
        }
        return width == 480 && height == 854;
    }

    public static boolean isSpamComment(String rawContent, boolean hasImage, int imageWidth, int imageHeight) {
        String clean = cleanCommentText(stripLeadingInviteCodeIfLongReview(rawContent));
        int len = clean.length();
        String compactDigits = DIGIT_GAP_PATTERN.matcher(clean).replaceAll("");

        // Rule 1: Empty text, ultra-short filler (<= 2 chars), filler words, or digits/punctuation only -> block
        if (len <= 2
                || IMAGE_FILLER_TEXT_PATTERN.matcher(clean).matches()
                || NON_WORD_OR_DIGIT_ONLY_PATTERN.matcher(clean).matches()) {
            return true;
        }
        // Rule 2: PDD generated share poster / red-packet card image dimensions
        if (hasImage && isPddPosterDimension(imageWidth, imageHeight)) {
            return true;
        }
        // Rule 3: Image with ultra-short stub (<= 6 chars like "五折衷", "搜就行", "5元")
        if (hasImage && len <= 6) {
            return true;
        }
        // Rule 4: Pure 6-12 digit invite code (including space/hyphen-split codes like "200 959454")
        if (PURE_INVITE_CODE_PATTERN.matcher(clean).matches()
                || PURE_INVITE_CODE_PATTERN.matcher(compactDigits).matches()) {
            return true;
        }
        // Rule 4b: Standalone 8-11 digit PDD invite code (excluding YYYYMMDD dates)
        if (STANDALONE_PDD_CODE_PATTERN.matcher(clean).find()
                || STANDALONE_PDD_CODE_PATTERN.matcher(compactDigits).find()) {
            return true;
        }
        // Rule 5: 8-10 digit invite code with context keywords
        if (INVITE_CODE_WITH_CONTEXT_PATTERN.matcher(clean).find()
                || INVITE_CODE_WITH_CONTEXT_PATTERN.matcher(compactDigits).find()) {
            return true;
        }
        // Rule 6: Hard PDD kouling / mini-program / red-packet step instructions
        if (HARD_SPAM_TEXT_PATTERN.matcher(clean).find()) {
            return true;
        }
        // Rule 7: Short team-up / red-packet phrases (either with an image and <= 35 chars, or text-only <= 20 chars)
        if (hasImage && len <= 35 && IMAGE_SHORT_SPAM_PHRASE_PATTERN.matcher(clean).find()) {
            return true;
        }
        if (!hasImage && len <= 12 && TEXT_SHORT_SPAM_PHRASE_PATTERN.matcher(clean).find()) {
            return true;
        }
        return false;
    }

    public static void filterNativeCommentData(Object dataObj) {
        if (dataObj == null) return;
        Object rows = ReflectUtils.getField(dataObj, "rows");
        if (rows instanceof List<?>) {
            filterCommentItemList((List<?>) rows);
        }
        Object top = ReflectUtils.getField(dataObj, "top");
        if (top instanceof List<?>) {
            filterCommentItemList((List<?>) top);
        }
    }

    public static void filterCommentItemList(List<?> list) {
        if (list == null || list.isEmpty()) return;
        Iterator<?> it = list.iterator();
        while (it.hasNext()) {
            Object item = it.next();
            if (item == null) continue;
            if (isSpamCommentItemBean(item)) {
                it.remove();
                continue;
            }
            Object cObj = ReflectUtils.getField(item, "content");
            if (cObj instanceof String && ((String) cObj).length() >= 30) {
                String stripped = stripLeadingInviteCodeIfLongReview((String) cObj);
                if (!stripped.equals(cObj)) {
                    ReflectUtils.setField(item, "content", stripped);
                }
            }
            Object childListObj = ReflectUtils.getField(item, "child_list");
            if (childListObj instanceof List<?>) {
                List<?> childList = (List<?>) childListObj;
                if (!childList.isEmpty()) {
                    int before = childList.size();
                    filterCommentItemList(childList);
                    int removed = before - childList.size();
                    if (removed > 0) {
                        Object totalObj = ReflectUtils.getField(item, "child_total");
                        int total = totalObj instanceof Number ? ((Number) totalObj).intValue() : before;
                        int nextTotal = childList.isEmpty() ? 0 : Math.max(0, total - removed);
                        ReflectUtils.setField(item, "child_total", nextTotal);
                    }
                }
            }
        }
    }

    public static boolean isSpamCommentItemBean(Object item) {
        if (item == null) return false;
        String content = null;
        Object cObj = ReflectUtils.getField(item, "content");
        if (cObj instanceof String && !((String) cObj).isEmpty()) {
            content = (String) cObj;
        } else {
            Object acObj = ReflectUtils.getField(item, "article_content");
            if (acObj instanceof String) {
                content = (String) acObj;
            }
        }

        boolean hasImage = false;
        int width = 0;
        int height = 0;
        Object imgObj = ReflectUtils.getField(item, "comment_image");
        if (imgObj != null) {
            Object urlObj = ReflectUtils.getField(imgObj, "url");
            Object origUrlObj = ReflectUtils.getField(imgObj, "original_url");
            String url = urlObj instanceof String ? (String) urlObj : "";
            String origUrl = origUrlObj instanceof String ? (String) origUrlObj : "";
            if (!url.isEmpty() || !origUrl.isEmpty()) {
                hasImage = true;
                if (SHARE_IMAGE_URL_PATTERN.matcher(url + " " + origUrl).find()) {
                    return true;
                }
            }
            Object wObj = ReflectUtils.getField(imgObj, "width");
            Object hObj = ReflectUtils.getField(imgObj, "height");
            if (wObj instanceof Number) width = ((Number) wObj).intValue();
            else if (wObj instanceof String) {
                try { width = Integer.parseInt(((String) wObj).trim()); } catch (Throwable ignored) {}
            }
            if (hObj instanceof Number) height = ((Number) hObj).intValue();
            else if (hObj instanceof String) {
                try { height = Integer.parseInt(((String) hObj).trim()); } catch (Throwable ignored) {}
            }
        }
        return isSpamComment(content, hasImage, width, height);
    }

    public static String injectHtml(String html) {
        if (html == null || html.isEmpty() || html.contains(MARKER)) {
            return html;
        }
        String scriptTag = "<script>" + getCommentFilterJs() + "</script>";
        int headIdx = html.indexOf("<head>");
        if (headIdx != -1) {
            int insertPos = headIdx + "<head>".length();
            return html.substring(0, insertPos) + scriptTag + html.substring(insertPos);
        }
        int htmlIdx = html.indexOf("<html");
        if (htmlIdx != -1) {
            int closeBracket = html.indexOf('>', htmlIdx);
            if (closeBracket != -1) {
                return html.substring(0, closeBracket + 1) + scriptTag + html.substring(closeBracket + 1);
            }
        }
        return scriptTag + html;
    }

    private static String getCommentFilterJsLegacy() {
        return "(function(){" +
            "if(window." + MARKER + ")return;" +
            "window." + MARKER + "=true;" +
            "var PURE_CODE=/^[\\s\\p{P}]*\\d{7,11}[\\s\\p{P}]*$/u;" +
            "var PDD_CODE=/(?:^|[^\\d])(?!20\\d{2}[01]\\d[0-3]\\d(?!\\d))\\d{8,11}(?!\\d)/;" +
            "var CODE_CTX=/(?:(?:邀请码|助力码|立减码|口令|多多|拼多多|搜索|搜|复制|组队|组团|三人|3人|成团|红包|百亿补贴|有码|新客)[^\\d]{0,12}\\d{7,11})|(?:\\d{7,11}[^\\d]{0,12}(?:邀请码|助力码|立减码|口令|多多|拼多多|搜索|搜|复制|组队|组团|三人|3人|成团|红包|百亿补贴|有码|新客))/;" +
            "var HARD_SPAM=/(?:#小程序:\\/\\/拼多多)|(?:[⇥⇤💲🎵🔐]{1,2}[A-Za-z0-9_\\-]{8,}[⇥⇤💲🎵🔐]{1,2})|(?:打不开[？?]点击上方空白处返回)|(?:第二张的?领完之后|等下再领第三张|领完之后.*再领第[二三23]张)|(?:三个人组个?团|三人团|3人团|互相领红包|互领红包|组团领红包|组队领红包|百补红包|补贴红包|百亿红包|瓜分红包)|(?:有码立减|品牌新客立减|新客立减码|互助码|助力码)|(?:第一步.*(?:第二步|第三步|第三部|领红包|扫码))|(?:(?:首单礼金|首购礼金|首单金)[^。！？!?]{0,60}(?:私我|私信|加我|加V|退款|退货|分成|对半|返现|返利|到账|账户))|(?:(?:私我|私信我|加我|加V|加微信|加企鹅|QQ群)[^。！？!?]{0,50}(?:首单|礼金|红包|退款|分成|返现|返利|上车))|(?:(?:退款|退货|刷单|代拍|薅羊毛|撸货)[^。！？!?]{0,50}(?:分成|对半|五五|返现|返利|账户|到账|私我))/;" +
            "var SHORT_SPAM=/(?:第[一二三123][步部]|图[一二三123]|第[二三23]张|借楼|来个人|来人|差[一二两12]个?人?|二缺一|三缺一|一缺二|组队|组团|拼团|成团|互助|领红包|红包|立减|新客|秒成|热乎|刚开|最新|速来|速度|上车|扫码|扫一扫)/;" +
            "var FILLER=/^(?:刚刚|刚|1+|2+|3+|6+|8+|9+|\\.+|，+|。+|！+|\\?+|？+|~+|up|dd|顶|同|同上|如图|看图|图片|上图|来|冲|好|可)+$/i;" +
            "function cleanText(raw){" +
                "if(!raw)return '';" +
                "return String(raw).replace(/<[^>]*>/g,'').replace(/&nbsp;/g,' ').replace(/\\[[^\\[\\]]{1,8}\\]/g,'').replace(/[\\s\\u00A0\\u200B-\\u200D\\uFEFF]+/g,' ').trim();" +
            "}" +
            "function isSpam(rawText,hasImg,w,h){" +
                "var t=cleanText(rawText);var len=t.length;" +
                "if(hasImg&&len===0)return true;" +
                "if(hasImg&&Number(w)===1200&&(Number(h)===1500||Number(h)===1770||Number(h)===1560||Number(h)===1600))return true;" +
                "if(hasImg&&FILLER.test(t))return true;" +
                "if(PURE_CODE.test(t)||PDD_CODE.test(t)||CODE_CTX.test(t)||HARD_SPAM.test(t))return true;" +
                "if(SHORT_SPAM.test(t)){if(hasImg&&len<=35)return true;if(!hasImg&&len<=20)return true;}" +
                "return false;" +
            "}" +
            "function isSpamItem(item){" +
                "if(!item||typeof item!=='object')return false;" +
                "var raw=item.content||item.article_content||'';" +
                "var img=item.comment_image;" +
                "var hasImg=Boolean(img&&(img.url||img.original_url));" +
                "var w=img?Number(img.width||0):0;" +
                "var h=img?Number(img.height||0):0;" +
                "return isSpam(raw,hasImg,w,h);" +
            "}" +
            "function filterRowList(rows){" +
                "if(!Array.isArray(rows))return [];" +
                "var out=[];" +
                "for(var i=0;i<rows.length;i++){" +
                    "var r=rows[i];" +
                    "if(!r||isSpamItem(r))continue;" +
                    "if(Array.isArray(r.child_list)&&r.child_list.length>0){" +
                        "var before=r.child_list.length;" +
                        "r.child_list=r.child_list.filter(function(c){return !isSpamItem(c);});" +
                        "var removed=before-r.child_list.length;" +
                        "if(removed>0){" +
                            "var ct=Number(r.child_total||before);" +
                            "r.child_total=r.child_list.length===0?0:Math.max(0,ct-removed);" +
                        "}" +
                    "}" +
                    "out.push(r);" +
                "}" +
                "return out;" +
            "}" +
            "var origOpen=XMLHttpRequest.prototype.open;" +
            "var origSend=XMLHttpRequest.prototype.send;" +
            "var origSetHeader=XMLHttpRequest.prototype.setRequestHeader;" +
            "function fetchRawPage(url,bodyStr,headers,withCreds){" +
                "return new Promise(function(resolve){" +
                    "try{" +
                        "var x=new XMLHttpRequest();" +
                        "x.__smzdmBypass=true;" +
                        "origOpen.call(x,'POST',url,true);" +
                        "x.withCredentials=Boolean(withCreds);" +
                        "x.timeout=6000;" +
                        "var hasCt=false;" +
                        "if(headers){for(var k in headers){if(Object.prototype.hasOwnProperty.call(headers,k)){if(k.toLowerCase()==='content-type')hasCt=true;try{origSetHeader.call(x,k,headers[k]);}catch(e){}}}}" +
                        "if(!hasCt){try{origSetHeader.call(x,'Content-Type','application/x-www-form-urlencoded');}catch(e){}}" +
                        "x.onloadend=function(){resolve(x.status===200?x.responseText:null);};" +
                        "x.onerror=function(){resolve(null);};" +
                        "x.ontimeout=function(){resolve(null);};" +
                        "origSend.call(x,bodyStr);" +
                    "}catch(e){resolve(null);}" +
                "});" +
            "}" +
            "async function processCommentJson(xhr,reqBody,rawText){" +
                "var obj=JSON.parse(rawText);" +
                "if(!obj||!obj.data)return rawText;" +
                "var d=obj.data;" +
                "if(Array.isArray(d.top)){d.top=filterRowList(d.top);}" +
                "if(!Array.isArray(d.rows))return JSON.stringify(obj);" +
                "var seen={};" +
                "var kept=filterRowList(d.rows);" +
                "for(var i=0;i<kept.length;i++){if(kept[i].comment_id)seen[kept[i].comment_id]=1;}" +
                "var bodyStr=String(reqBody||'');" +
                "var isRoot=(bodyStr.indexOf('root_id=0')!==-1||bodyStr.indexOf('root_id=')===-1);" +
                "var origHasFold=d.next_has_fold_comment;" +
                "if(isRoot&&kept.length<5){" +
                    "var params=new URLSearchParams(bodyStr);" +
                    "var curFold=params.get('is_fold_comment')||'-1';" +
                    "var hops=0;" +
                    "while(kept.length<5&&hops<5){" +
                        "hops++;" +
                        "if(Number(d.next_page)===1&&d.next_comment_id){" +
                            "params.set('sort_v2',String(d.next_sort_v2||'0'));" +
                            "params.set('self_id',String(d.next_self_id||'0'));" +
                            "params.set('comment_id',String(d.next_comment_id||'0'));" +
                        "}else if(Number(d.next_has_fold_comment)===1&&curFold!=='1'){" +
                            "curFold='1';" +
                            "params.set('is_fold_comment','1');" +
                            "params.set('order','new');" +
                            "params.set('is_zhi','0');" +
                            "params.set('sort_v2','0');" +
                            "params.set('self_id','0');" +
                            "params.set('comment_id','0');" +
                            "d.next_has_fold_comment=0;" +
                        "}else{break;}" +
                        "var nextRaw=await fetchRawPage(xhr.__smzdmUrl,params.toString(),xhr.__smzdmHeaders,xhr.withCredentials);" +
                        "if(!nextRaw)break;" +
                        "var nextObj=null;try{nextObj=JSON.parse(nextRaw);}catch(e){}" +
                        "if(!nextObj||Number(nextObj.error_code)!==0||!nextObj.data||!Array.isArray(nextObj.data.rows))break;" +
                        "var nd=nextObj.data;" +
                        "var nextKept=filterRowList(nd.rows);" +
                        "for(var j=0;j<nextKept.length;j++){" +
                            "var item=nextKept[j];" +
                            "var cid=item.comment_id;" +
                            "if(!cid||!seen[cid]){if(cid)seen[cid]=1;kept.push(item);}" +
                        "}" +
                        "d.next_sort_v2=nd.next_sort_v2;" +
                        "d.next_self_id=nd.next_self_id;" +
                        "d.next_comment_id=nd.next_comment_id;" +
                        "d.next_page=nd.next_page;" +
                        "if(curFold!=='1'){d.next_has_fold_comment=nd.next_has_fold_comment;}" +
                    "}" +
                    "if(kept.length===0&&Number(origHasFold)===1){d.next_has_fold_comment=1;}" +
                "}" +
                "d.rows=kept;" +
                "return JSON.stringify(obj);" +
            "}" +
            "XMLHttpRequest.prototype.open=function(method,url){" +
                "this.__smzdmMethod=method;" +
                "this.__smzdmUrl=String(url||'');" +
                "this.__smzdmHeaders={};" +
                "return origOpen.apply(this,arguments);" +
            "};" +
            "XMLHttpRequest.prototype.setRequestHeader=function(k,v){" +
                "if(this.__smzdmHeaders)this.__smzdmHeaders[k]=v;" +
                "return origSetHeader.apply(this,arguments);" +
            "};" +
            "XMLHttpRequest.prototype.send=function(body){" +
                "if(!this.__smzdmBypass&&this.__smzdmUrl&&this.__smzdmUrl.indexOf('ajax_comment_list')!==-1){" +
                    "var xhr=this;" +
                    "var reqBody=typeof body==='string'?body:'';" +
                    "var cbLoadEnd=xhr.onloadend;" +
                    "var cbReady=xhr.onreadystatechange;" +
                    "var cbLoad=xhr.onload;" +
                    "xhr.onloadend=null;xhr.onreadystatechange=null;xhr.onload=null;" +
                    "Object.defineProperty(xhr,'onloadend',{configurable:true,get:function(){return cbLoadEnd;},set:function(fn){cbLoadEnd=fn;}});" +
                    "Object.defineProperty(xhr,'onreadystatechange',{configurable:true,get:function(){return cbReady;},set:function(fn){cbReady=fn;}});" +
                    "Object.defineProperty(xhr,'onload',{configurable:true,get:function(){return cbLoad;},set:function(fn){cbLoad=fn;}});" +
                    "xhr.addEventListener('loadend',async function(ev){" +
                        "try{" +
                            "if(xhr.status===200&&xhr.responseText){" +
                                "var mod=await processCommentJson(xhr,reqBody,xhr.responseText);" +
                                "if(mod){" +
                                    "Object.defineProperty(xhr,'responseText',{configurable:true,get:function(){return mod;}});" +
                                    "Object.defineProperty(xhr,'response',{configurable:true,get:function(){return mod;}});" +
                                "}" +
                            "}" +
                        "}catch(e){}" +
                        "if(typeof cbReady==='function'){try{cbReady.call(xhr,ev);}catch(e){}}" +
                        "if(typeof cbLoad==='function'){try{cbLoad.call(xhr,ev);}catch(e){}}" +
                        "if(typeof cbLoadEnd==='function'){try{cbLoadEnd.call(xhr,ev);}catch(e){}}" +
                    "});" +
                "}" +
                "return origSend.apply(this,arguments);" +
            "};" +
            "function scanDomComments(){" +
                "var items=document.querySelectorAll('.comment-main-list-item, .comment-main-list-reply-item');" +
                "for(var i=0;i<items.length;i++){" +
                    "var el=items[i];" +
                    "var isReply=el.classList.contains('comment-main-list-reply-item');" +
                    "var txtEl=el.querySelector('.comment-main-list-item-content-comment');" +
                    "var imgEl=el.querySelector(isReply?'.comment-main-list-item-content-image.child img':'.comment-main-list-item-content-image:not(.child) img');" +
                    "var raw=txtEl?txtEl.textContent:'';" +
                    "if(isReply&&raw.indexOf('：')!==-1){" +
                        "raw=raw.substring(raw.indexOf('：')+1);" +
                    "}" +
                    "var hasImg=Boolean(imgEl);" +
                    "if(isSpam(raw,hasImg,0,0)){" +
                        "el.style.setProperty('display','none','important');" +
                    "}" +
                "}" +
            "}" +
            "if(typeof MutationObserver!=='undefined'){" +
                "var mo=new MutationObserver(function(){scanDomComments();});" +
                "var startMo=function(){if(document.documentElement)mo.observe(document.documentElement,{childList:true,subtree:true});scanDomComments();};" +
                "if(document.documentElement)startMo();else document.addEventListener('DOMContentLoaded',startMo);" +
            "}" +
        "})();";
    }

    public static String getCommentFilterJs() {
        return "(function(){" +
            "if(window." + MARKER + ")return;" +
            "window." + MARKER + "=true;" +
            "var PURE_CODE=/^[\\s\\p{P}]*(?:\\d{6,12}|[\\d幺要依有洞零一二三四五六七八九十壹贰叁肆伍陆柒捌玖〇把]{7,12})[来來\\s\\p{P}]*$/u;" +
            "var PDD_CODE=/(?:^|[^\\d])(?!202[0-6][01]\\d[0-3]\\d(?!\\d))\\d{8,11}(?!\\d)/;" +
            "var LEAD_CODE=/^\\s*(?:邀请码?[:：\\s]*)?(?!202[0-6][01]\\d[0-3]\\d(?!\\d))\\d{8,10}[\\s,，。.:：;；\\-]*/;" +
            "var CODE_CTX=/(?:(?:邀请码?|助力码?|立减码?|口令|多多|拼多多|搜索|搜|复制|组队|组团|三人|3人|成团|红包|福袋|五折|5折|百亿补贴|有码|新客)[^\\d]{0,12}\\d{6,12})|(?:\\d{6,12}[^\\d]{0,12}(?:邀请码?|助力码?|立减码?|口令|多多|拼多多|搜索|搜|复制|组队|组团|三人|3人|成团|红包|福袋|五折|5折|百亿补贴|有码|新客))/;" +
            "var HARD_SPAM=/(?:#小程序:\\/\\/拼多多)|(?:[⇥⇤💲🎵🔐]{1,2}[A-Za-z0-9_\\-]{8,}[⇥⇤💲🎵🔐]{1,2})|(?:打不开[？?]点击上方空白处返回)|(?:第二张的?领完之后|等下再领第三张|领完之后.*再领第[二三23]张)|(?:三个人组个?团|三人团|3人团|互相领红包|互领红包|组团领红包|组队领红包|百补红包|补贴红包|百亿红包|瓜分红包)|(?:有码立减|品牌新客立减|新客立减码|互助码|助力码)|(?:打开拼多多APP|搜索以下数字邀请码|抽福袋.*邀请码|福袋专享.*百亿补)|(?:下面[一二三两123]个都可以|就差你了)|(?:取件码|极兔速递|菜鸟驿站)|(?:^[A-Za-z0-9]{6,10}$)|(?:[A-Za-z0-9]{5,}[^\\w\\s]+[A-Za-z0-9]{5,})|(?:第一步.*(?:第二步|第三步|第三部|领红包|扫码))|(?:(?:首单礼金|首购礼金|首单金)[^。！？!?]{0,60}(?:私我|私信|加我|加V|退款|退货|分成|对半|返现|返利|到账|账户))|(?:(?:私我|私信我|加我|加V|加微信|加企鹅|QQ群)[^。！？!?]{0,50}(?:首单|礼金|红包|退款|分成|返现|返利|上车))|(?:(?:退款|退货|刷单|代拍|薅羊毛|撸货)[^。！？!?]{0,50}(?:分成|对半|五五|返现|返利|账户|到账|私我))/;" +
            "var IMG_SHORT_SPAM=/(?:第[一二三123][步部]|图[一二三123]|第[二三23]张|借楼|来个人|来人|来[一二两12]个?人?|差[一二两12]个?人?|二缺一|三缺一|一缺二|组队|组团|拼团|成团|互助|领红包|红包|福袋|五折|5折|立减|新客|秒成|热乎|刚开|最新|搜就行|复制|速来|速度|上车|扫码|扫一扫)/;" +
            "var TXT_SHORT_SPAM=/(?:第[一二三123][步部]|图[一二三123]|第[二三23]张|借楼|来个人|来人|来[一二两12]个?人?|差[一二两12]个?人?|差一|差1|新车|二缺一|三缺一|一缺二|组队|组团|拼团|成团|互助|领红包|一起抽福袋|抽5折|热火的|秒成|热乎|刚开|搜就行|复制|速来|扫码|扫一扫)/;" +
            "var FILLER=/^(?:刚刚|刚|1+|2+|3+|6+|8+|9+|\\.+|，+|。+|！+|\\?+|？+|~+|up|dd|kk|ok|顶|同|同上|如图|看图|图片|上图|来|冲|好|可|加油|复制)+$/i;" +
            "var NON_WORD_DIGIT=/^[\\s\\p{P}\\d]+$/u;" +
            "var spamIdMap={};" +
            "var cleanPools={};" +
            "function cleanText(raw){if(!raw)return '';return String(raw).replace(/<[^>]*>/g,'').replace(/&nbsp;/g,' ').replace(/\\[[^\\[\\]]{1,8}\\]/g,'').replace(/[\\s\\u00A0\\u200B-\\u200D\\uFEFF]+/g,' ').trim();}" +
            "function stripLeadCode(t){if(!t||t.length<30)return t;var s=t.replace(LEAD_CODE,'').trim();return s.length>=20?s:t;}" +
            "function isPosterDim(w,h){" +
                "var nw=Number(w||0),nh=Number(h||0);" +
                "if(nw<=0||nh<=0)return false;" +
                "if(nw===1200&&(nh===1500||nh===1770||nh===1560||nh===1600||nh===2670))return true;" +
                "if(nw===560&&nh===646)return true;" +
                "if(nw===1536&&(nh===2048||nh===2275))return true;" +
                "if(nw===1152&&nh===2048)return true;" +
                "if(nw===480&&nh===854)return true;" +
                "return false;" +
            "}" +
            "function isSpam(rawText,hasImg,w,h){" +
                "var t=stripLeadCode(cleanText(rawText));var len=t.length;" +
                "var tc=t.replace(/(\\d)[\\s\\-.点的_号楼月日年个分秒钟哦是来來]+(?=\\d)/g,'$1');" +
                "if(len<=2||FILLER.test(t)||NON_WORD_DIGIT.test(t))return true;" +
                "if(hasImg&&isPosterDim(w,h))return true;" +
                "if(hasImg&&len<=6)return true;" +
                "if(PURE_CODE.test(t)||PURE_CODE.test(tc)||PDD_CODE.test(t)||PDD_CODE.test(tc)||CODE_CTX.test(t)||CODE_CTX.test(tc)||HARD_SPAM.test(t))return true;" +
                "if(hasImg&&len<=35&&IMG_SHORT_SPAM.test(t))return true;" +
                "if(!hasImg&&len<=12&&TXT_SHORT_SPAM.test(t))return true;" +
                "return false;" +
            "}" +
            "function isSpamItem(item){" +
                "if(!item||typeof item!=='object')return false;" +
                "var raw=item.content||item.article_content||'';" +
                "var img=item.comment_image;" +
                "var hasImg=Boolean(img&&(img.url||img.original_url));" +
                "var w=img?Number(img.width||0):0;" +
                "var h=img?Number(img.height||0):0;" +
                "var res=isSpam(raw,hasImg,w,h);" +
                "if(res&&item.comment_id){spamIdMap[String(item.comment_id)]=1;}" +
                "else if(!res&&typeof item.content==='string'&&item.content.length>=30){" +
                    "var sc=stripLeadCode(cleanText(item.content));" +
                    "if(sc.length>=20&&sc!==cleanText(item.content)){item.content=item.content.replace(LEAD_CODE,'');}" +
                "}" +
                "return res;" +
            "}" +
            "function filterRowList(rows){" +
                "if(!Array.isArray(rows))return [];" +
                "var out=[];" +
                "for(var i=0;i<rows.length;i++){" +
                    "var r=rows[i];" +
                    "if(!r||isSpamItem(r))continue;" +
                    "if(Array.isArray(r.child_list)&&r.child_list.length>0){" +
                        "var before=r.child_list.length;" +
                        "r.child_list=r.child_list.filter(function(c){return !isSpamItem(c);});" +
                        "var removed=before-r.child_list.length;" +
                        "if(removed>0){" +
                            "var ct=Number(r.child_total||before);" +
                            "r.child_total=r.child_list.length===0?0:Math.max(0,ct-removed);" +
                        "}" +
                    "}" +
                    "out.push(r);" +
                "}" +
                "return out;" +
            "}" +
            "var origOpen=XMLHttpRequest.prototype.open;" +
            "var origSend=XMLHttpRequest.prototype.send;" +
            "var origSetHeader=XMLHttpRequest.prototype.setRequestHeader;" +
            "function fetchRawPage(url,bodyStr,headers,withCreds){" +
                "return new Promise(function(resolve){" +
                    "try{" +
                        "var x=new XMLHttpRequest();" +
                        "x.__smzdmBypass=true;" +
                        "origOpen.call(x,'POST',url,true);" +
                        "x.withCredentials=Boolean(withCreds);" +
                        "x.timeout=5000;" +
                        "var hasCt=false;" +
                        "if(headers){for(var k in headers){if(Object.prototype.hasOwnProperty.call(headers,k)){if(k.toLowerCase()==='content-type')hasCt=true;try{origSetHeader.call(x,k,headers[k]);}catch(e){}}}}" +
                        "if(!hasCt){try{origSetHeader.call(x,'Content-Type','application/x-www-form-urlencoded');}catch(e){}}" +
                        "x.onloadend=function(){resolve(x.status===200?x.responseText:null);};" +
                        "x.onerror=function(){resolve(null);};" +
                        "x.ontimeout=function(){resolve(null);};" +
                        "origSend.call(x,bodyStr);" +
                    "}catch(e){resolve(null);}" +
                "});" +
            "}" +
            "function getPool(key){" +
                "if(!cleanPools[key]){cleanPools[key]={byId:{},list:[],delivered:{},hotExhausted:false,isFlooded:false,cursorSort:'',cursorSelfId:'',cursorCommentId:'',nextPage:1,harvestPromise:null,firstWaiters:[],lastOrder:'hot',refreshTimer:null};}" +
                "return cleanPools[key];" +
            "}" +
            "function sortPoolList(pool){" +
                "pool.list.sort(function(a,b){" +
                    "var sa=Number(a.sort_v2||0),sb=Number(b.sort_v2||0);" +
                    "if(sa!==sb)return sb-sa;" +
                    "return Number(a.comment_id||0)-Number(b.comment_id||0);" +
                "});" +
            "}" +
            "function addPoolItems(pool,items){" +
                "var added=false;" +
                "for(var i=0;i<items.length;i++){" +
                    "var it=items[i];" +
                    "var cid=String(it.comment_id||'');" +
                    "if(cid&&!pool.byId[cid]){pool.byId[cid]=it;pool.list.push(it);added=true;}" +
                "}" +
                "if(added){" +
                    "sortPoolList(pool);" +
                    "if(pool.firstWaiters&&pool.firstWaiters.length>0){" +
                        "var ws=pool.firstWaiters.splice(0,pool.firstWaiters.length);" +
                        "for(var w=0;w<ws.length;w++){try{ws[w]();}catch(e){}}" +
                    "}" +
                    "if(!pool.refreshTimer&&Object.keys(pool.delivered||{}).length>0){" +
                        "pool.refreshTimer=setTimeout(function(){pool.refreshTimer=null;triggerHotAutoRefresh(pool);},800);" +
                    "}" +
                "}" +
            "}" +
            "function hasNonTopPoolItem(pool,topIds){" +
                "if(!topIds)return pool.list.length>0;" +
                "for(var i=0;i<pool.list.length;i++){" +
                    "var cid=String(pool.list[i].comment_id||'');" +
                    "if(cid&&!topIds[cid])return true;" +
                "}" +
                "return false;" +
            "}" +
            "function waitFirstPoolItem(pool,topIds,timeoutMs){" +
                "if(hasNonTopPoolItem(pool,topIds)||pool.hotExhausted)return Promise.resolve();" +
                "return new Promise(function(resolve){" +
                    "var done=false;" +
                    "var fin=function(){if(done)return;done=true;resolve();};" +
                    "pool.firstWaiters.push(fin);" +
                    "setTimeout(fin,timeoutMs||2200);" +
                "});" +
            "}" +
            "function triggerHotAutoRefresh(pool){" +
                "try{" +
                    "if(pool.lastOrder!=='hot')return;" +
                    "var delivCount=Object.keys(pool.delivered||{}).length;" +
                    "if(pool.list.length<=delivCount)return;" +
                    "try{window.dispatchEvent(new Event('scroll'));}catch(e){}" +
                    "setTimeout(function(){" +
                        "if(pool.lastOrder!=='hot')return;" +
                        "var dc2=Object.keys(pool.delivered||{}).length;" +
                        "if(pool.list.length<=dc2)return;" +
                        "if(typeof window.COMMENT_FUNC_LOAD_LIST==='function'){try{window.COMMENT_FUNC_LOAD_LIST('top');return;}catch(e){}}" +
                        "var tabs=document.querySelectorAll('.comment-sort-container div, .comment-main-header-right span, .comment-order span, .comment-sort span');" +
                        "var hotTab=null,otherTab=null;" +
                        "for(var i=0;i<tabs.length;i++){" +
                            "var t=(tabs[i].textContent||'').trim();" +
                            "if(t==='精彩'){hotTab=tabs[i];}else if(t==='最早'||t==='最晚'){if(!otherTab)otherTab=tabs[i];}" +
                        "}" +
                        "if(hotTab){" +
                            "if(hotTab.classList.contains('active')||hotTab.classList.contains('selected')||hotTab.classList.contains('cur')||hotTab.getAttribute('aria-selected')==='true'){" +
                                "if(otherTab){try{otherTab.click();setTimeout(function(){try{hotTab.click();}catch(e){}},100);}catch(e){try{hotTab.click();}catch(e2){}}}" +
                                "else{try{hotTab.click();}catch(e){}}" +
                            "}else{" +
                                "try{hotTab.click();}catch(e){}" +
                            "}" +
                        "}" +
                    "},220);" +
                "}catch(e){}" +
            "}" +
            "function harvestHotPool(xhr,baseParams,pool){" +
                "if(pool.hotExhausted)return Promise.resolve();" +
                "if(pool.harvestPromise)return pool.harvestPromise;" +
                "pool.harvestPromise=(async function(){" +
                    "try{" +
                        "var p=new URLSearchParams(baseParams.toString());" +
                        "p.set('order','hot');" +
                        "p.set('root_id','0');" +
                        "p.set('limit','50');" +
                        "p.set('is_fold_comment','-1');" +
                        "p.set('with_detection','');" +
                        "if(!pool.cursorCommentId){" +
                            "p.set('sort_v2','');" +
                            "p.set('self_id','');" +
                            "p.set('comment_id','');" +
                            "var r0=await fetchRawPage(xhr.__smzdmUrl,p.toString(),xhr.__smzdmHeaders,xhr.withCredentials);" +
                            "if(r0){" +
                                "var o0=null;try{o0=JSON.parse(r0);}catch(e){}" +
                                "if(o0&&Number(o0.error_code)===0&&o0.data&&Array.isArray(o0.data.rows)){" +
                                    "addPoolItems(pool,filterRowList(o0.data.rows));" +
                                    "pool.cursorSort=String(o0.data.next_sort_v2!=null?o0.data.next_sort_v2:'');" +
                                    "pool.cursorSelfId=String(o0.data.next_self_id!=null?o0.data.next_self_id:'');" +
                                    "pool.cursorCommentId=String(o0.data.next_comment_id!=null?o0.data.next_comment_id:'');" +
                                    "pool.nextPage=Number(o0.data.next_page||0);" +
                                    "if(pool.nextPage!==1||Number(o0.data.next_sort_v2)<0){pool.hotExhausted=true;}" +
                                "}" +
                            "}" +
                        "}" +
                        "var minCid=Number(pool.cursorCommentId||0);" +
                        "if(!pool.hotExhausted&&minCid>0&&Number(pool.cursorSort||0)>=0){" +
                            "var globalMaxCid=9999999999;" +
                            "var offsets=[0,40000,90000,155000,235000,340000,500000,780000,9999999999];" +
                            "var activeSegs=[];" +
                            "for(var sIdx=0;sIdx<8;sIdx++){" +
                                "activeSegs.push({curCid:minCid+offsets[sIdx],endCid:offsets[sIdx+1]===9999999999?9999999999:(minCid+offsets[sIdx+1]),active:true});" +
                            "}" +
                            "var seenProbes={};" +
                            "var stealSeg=function(){" +
                                "var best=null,bestSpan=38000;" +
                                "for(var k=0;k<activeSegs.length;k++){" +
                                    "var sg=activeSegs[k];" +
                                    "if(!sg.active)continue;" +
                                    "var effEnd=Math.min(sg.endCid,globalMaxCid);" +
                                    "if(effEnd>=9000000000)continue;" +
                                    "var sp=effEnd-sg.curCid;" +
                                    "if(sp>bestSpan){bestSpan=sp;best=sg;}" +
                                "}" +
                                "if(!best)return null;" +
                                "var ee=Math.min(best.endCid,globalMaxCid);" +
                                "var mid=Math.floor((best.curCid+ee)/2);" +
                                "var ns={curCid:mid,endCid:ee,active:true};" +
                                "best.endCid=mid;" +
                                "activeSegs.push(ns);" +
                                "return ns;" +
                            "};" +
                            "var runWorker=async function(initSeg){" +
                                "var seg=initSeg;" +
                                "var steps=0;" +
                                "while(seg&&steps<20){" +
                                    "if(seg.curCid>=Math.min(seg.endCid,globalMaxCid)){seg.active=false;seg=stealSeg();continue;}" +
                                    "var probe=seg.curCid;" +
                                    "if(seenProbes[probe]){seg.active=false;seg=stealSeg();continue;}" +
                                    "seenProbes[probe]=1;" +
                                    "steps++;" +
                                    "var wp=new URLSearchParams(p.toString());" +
                                    "wp.set('sort_v2','0');wp.set('self_id','0');wp.set('comment_id',String(probe));" +
                                    "var raw=await fetchRawPage(xhr.__smzdmUrl,wp.toString(),xhr.__smzdmHeaders,xhr.withCredentials);" +
                                    "if(!raw){seg.active=false;seg=stealSeg();continue;}" +
                                    "var obj=null;try{obj=JSON.parse(raw);}catch(e){}" +
                                    "if(!obj||Number(obj.error_code)!==0||!obj.data||!Array.isArray(obj.data.rows)){seg.active=false;seg=stealSeg();continue;}" +
                                    "var nd=obj.data;" +
                                    "var zeroRows=[];" +
                                    "var lastZeroCid=0;" +
                                    "for(var rI=0;rI<nd.rows.length;rI++){" +
                                        "var rw=nd.rows[rI];" +
                                        "if(rw&&Number(rw.sort_v2!=null?rw.sort_v2:0)>=0){" +
                                            "zeroRows.push(rw);" +
                                            "lastZeroCid=Number(rw.comment_id||0);" +
                                        "}" +
                                    "}" +
                                    "if(zeroRows.length>0){" +
                                        "var nk=filterRowList(zeroRows);" +
                                        "if(nk.length>0)addPoolItems(pool,nk);" +
                                    "}" +
                                    "var nextSort=Number(nd.next_sort_v2);" +
                                    "var nextCid=Number(nd.next_comment_id||0);" +
                                    "if(nextSort<0||Number(nd.next_page)!==1){" +
                                        "var cap=zeroRows.length>0?lastZeroCid:probe;" +
                                        "if(cap>0&&cap<globalMaxCid)globalMaxCid=cap;" +
                                        "seg.active=false;seg=stealSeg();continue;" +
                                    "}" +
                                    "seg.curCid=nextCid;" +
                                    "if(seg.curCid>=Math.min(seg.endCid,globalMaxCid)){seg.active=false;seg=stealSeg();}" +
                                "}" +
                                "if(seg)seg.active=false;" +
                            "};" +
                            "var workers=[];" +
                            "for(var wIdx=0;wIdx<8;wIdx++){workers.push(runWorker(activeSegs[wIdx]));}" +
                            "await Promise.all(workers);" +
                        "}" +
                        "pool.hotExhausted=true;" +
                        "sortPoolList(pool);" +
                        "if(pool.firstWaiters&&pool.firstWaiters.length>0){" +
                            "var fws=pool.firstWaiters.splice(0,pool.firstWaiters.length);" +
                            "for(var fw=0;fw<fws.length;fw++){try{fws[fw]();}catch(e){}}" +
                        "}" +
                        "triggerHotAutoRefresh(pool);" +
                    "}catch(e){}" +
                    "pool.harvestPromise=null;" +
                "})();" +
                "return pool.harvestPromise;" +
            "}" +
            "async function processCommentJson(xhr,reqBody,rawText){" +
                "var obj=JSON.parse(rawText);" +
                "if(!obj||!obj.data)return rawText;" +
                "var d=obj.data;" +
                "if(Array.isArray(d.top)){d.top=filterRowList(d.top);}" +
                "if(!Array.isArray(d.rows))return JSON.stringify(obj);" +
                "var bodyStr=String(reqBody||'');" +
                "var params=new URLSearchParams(bodyStr);" +
                "params.set('with_detection','');" +
                "params.set('limit','50');" +
                "var rootId=params.get('root_id')||'0';" +
                "var isRoot=(rootId==='0'||rootId==='');" +
                "var origLen=d.rows.length;" +
                "var kept=filterRowList(d.rows);" +
                "if(!isRoot){d.rows=kept;return JSON.stringify(obj);}" +
                "var aid=params.get('article_id')||params.get('aid')||'def';" +
                "var isZhi=params.get('is_zhi')||'0';" +
                "var order=params.get('order')||'hot';" +
                "var reqSort=params.get('sort_v2')||'';" +
                "var pool=getPool(aid+'_'+isZhi);" +
                "pool.lastOrder=order;" +
                "if((origLen-kept.length)>=3||(origLen>0&&kept.length===0)){pool.isFlooded=true;}" +
                "if(Array.isArray(d.top)&&d.top.length>0){addPoolItems(pool,d.top);}" +
                "addPoolItems(pool,kept);" +
                "if(order==='hot'&&reqSort===''){" +
                    "if(pool.hotExhausted||(pool.delivered&&Object.keys(pool.delivered).length>0&&pool.list.length>Object.keys(pool.delivered).length)){" +
                        "pool.delivered={};" +
                        "for(var pIdx=0;pIdx<pool.list.length;pIdx++){if(pool.list[pIdx].comment_id)pool.delivered[String(pool.list[pIdx].comment_id)]=1;}" +
                        "var topIds={};if(Array.isArray(d.top)){for(var tI=0;tI<d.top.length;tI++){if(d.top[tI]&&d.top[tI].comment_id)topIds[String(d.top[tI].comment_id)]=1;}}" +
                        "d.rows=pool.list.filter(function(it){return !topIds[String(it.comment_id||'')];});" +
                        "if(pool.hotExhausted){d.next_page=0;d.next_has_fold_comment=0;}" +
                        "return JSON.stringify(obj);" +
                    "}" +
                    "pool.cursorSort=String(d.next_sort_v2!=null?d.next_sort_v2:'');" +
                    "pool.cursorSelfId=String(d.next_self_id!=null?d.next_self_id:'');" +
                    "pool.cursorCommentId=String(d.next_comment_id!=null?d.next_comment_id:'');" +
                    "pool.nextPage=Number(d.next_page||0);" +
                    "if(pool.nextPage!==1||Number(d.next_sort_v2)<0){" +
                        "pool.hotExhausted=true;" +
                        "pool.delivered={};" +
                        "for(var eIdx=0;eIdx<pool.list.length;eIdx++){if(pool.list[eIdx].comment_id)pool.delivered[String(pool.list[eIdx].comment_id)]=1;}" +
                        "d.rows=pool.list.slice();d.next_page=0;d.next_has_fold_comment=0;return JSON.stringify(obj);" +
                    "}" +
                    "harvestHotPool(xhr,params,pool);" +
                    "var topIds2={};if(Array.isArray(d.top)){for(var tI2=0;tI2<d.top.length;tI2++){if(d.top[tI2]&&d.top[tI2].comment_id)topIds2[String(d.top[tI2].comment_id)]=1;}}" +
                    "if(kept.length===0){" +
                        "await waitFirstPoolItem(pool,topIds2,2200);" +
                    "}" +
                    "var initialRows=pool.list.slice();" +
                    "pool.delivered={};" +
                    "for(var iIdx=0;iIdx<initialRows.length;iIdx++){if(initialRows[iIdx].comment_id)pool.delivered[String(initialRows[iIdx].comment_id)]=1;}" +
                    "d.rows=initialRows.filter(function(it){return !topIds2[String(it.comment_id||'')];});" +
                    "if(pool.hotExhausted){d.next_page=0;d.next_has_fold_comment=0;}" +
                    "return JSON.stringify(obj);" +
                "}" +
                "if(order==='hot'&&reqSort!==''&&pool.isFlooded){" +
                    "await harvestHotPool(xhr,params,pool);" +
                    "var rest=[];" +
                    "for(var rIdx=0;rIdx<pool.list.length;rIdx++){" +
                        "var rIt=pool.list[rIdx];" +
                        "var rCid=String(rIt.comment_id||'');" +
                        "if(rCid&&!pool.delivered[rCid]){pool.delivered[rCid]=1;rest.push(rIt);}" +
                    "}" +
                    "d.rows=rest;d.next_page=0;d.next_has_fold_comment=0;" +
                    "return JSON.stringify(obj);" +
                "}" +
                "if((order==='old'||order==='new')&&(pool.isFlooded||kept.length<6||origLen>kept.length)){" +
                    "await harvestHotPool(xhr,params,pool);" +
                    "if(pool.list.length>=kept.length&&pool.list.length>0){" +
                        "var merged=pool.list.slice();" +
                        "merged.sort(function(a,b){" +
                            "var ca=Number(a.comment_id||0),cb=Number(b.comment_id||0);" +
                            "return order==='old'?(ca-cb):(cb-ca);" +
                        "});" +
                        "kept=merged;" +
                        "if(Array.isArray(d.top)){d.top=[];}" +
                        "d.next_page=0;" +
                        "d.next_has_fold_comment=0;" +
                    "}" +
                "}" +
                "if(order==='hot'&&pool.hotExhausted){d.next_page=0;d.next_has_fold_comment=0;}" +
                "d.rows=kept;" +
                "return JSON.stringify(obj);" +
            "}" +
            "XMLHttpRequest.prototype.open=function(method,url){" +
                "this.__smzdmMethod=method;" +
                "this.__smzdmUrl=String(url||'');" +
                "this.__smzdmHeaders={};" +
                "return origOpen.apply(this,arguments);" +
            "};" +
            "XMLHttpRequest.prototype.setRequestHeader=function(k,v){" +
                "if(this.__smzdmHeaders)this.__smzdmHeaders[k]=v;" +
                "return origSetHeader.apply(this,arguments);" +
            "};" +
            "XMLHttpRequest.prototype.send=function(body){" +
                "if(!this.__smzdmBypass&&this.__smzdmUrl&&this.__smzdmUrl.indexOf('ajax_comment_list')!==-1){" +
                    "if(typeof body==='string'){" +
                        "try{" +
                            "var sp=new URLSearchParams(body);" +
                            "var rid=sp.get('root_id')||'0';" +
                            "if(rid==='0'||rid===''){sp.set('limit','50');}" +
                            "if(sp.has('with_detection')){sp.set('with_detection','');}" +
                            "body=sp.toString();" +
                        "}catch(e){}" +
                    "}" +
                    "var xhr=this;" +
                    "var reqBody=typeof body==='string'?body:'';" +
                    "var cbLoadEnd=xhr.onloadend;" +
                    "var cbReady=xhr.onreadystatechange;" +
                    "var cbLoad=xhr.onload;" +
                    "xhr.onloadend=null;xhr.onreadystatechange=null;xhr.onload=null;" +
                    "Object.defineProperty(xhr,'onloadend',{configurable:true,get:function(){return cbLoadEnd;},set:function(fn){cbLoadEnd=fn;}});" +
                    "Object.defineProperty(xhr,'onreadystatechange',{configurable:true,get:function(){return cbReady;},set:function(fn){cbReady=fn;}});" +
                    "Object.defineProperty(xhr,'onload',{configurable:true,get:function(){return cbLoad;},set:function(fn){cbLoad=fn;}});" +
                    "xhr.addEventListener('loadend',async function(ev){" +
                        "try{" +
                            "if(xhr.status===200&&xhr.responseText){" +
                                "var mod=await processCommentJson(xhr,reqBody,xhr.responseText);" +
                                "if(mod){" +
                                    "Object.defineProperty(xhr,'responseText',{configurable:true,get:function(){return mod;}});" +
                                    "Object.defineProperty(xhr,'response',{configurable:true,get:function(){return mod;}});" +
                                "}" +
                            "}" +
                        "}catch(e){}" +
                        "if(typeof cbReady==='function'){try{cbReady.call(xhr,ev);}catch(e){}}" +
                        "if(typeof cbLoad==='function'){try{cbLoad.call(xhr,ev);}catch(e){}}" +
                        "if(typeof cbLoadEnd==='function'){try{cbLoadEnd.call(xhr,ev);}catch(e){}}" +
                        "setTimeout(schedule,30);" +
                        "setTimeout(fixCommentAnchorScroll,80);" +
                    "});" +
                "}" +
                "return origSend.apply(this,arguments);" +
            "};" +
            "function fixCommentAnchorScroll(){" +
                "try{" +
                    "if(window.scrollY<=120)return;" +
                    "var cc=document.querySelector('.comment-container')||document.querySelector('.comment-main');" +
                    "if(!cc)return;" +
                    "var rect=cc.getBoundingClientRect();" +
                    "var vh=window.innerHeight||document.documentElement.clientHeight||800;" +
                    "if(rect.top>vh*0.55&&rect.top<vh+150){" +
                        "window.scrollBy({top:rect.top-120,behavior:'smooth'});" +
                    "}" +
                "}catch(e){}" +
            "}" +
            "function installStyle(){" +
                "try{if(document.getElementById('__smzdm_pure_style'))return;var st=document.createElement('style');st.id='__smzdm_pure_style';st.textContent='.ai-product-intro-wrapper,[class*=\"ai-product-intro\"],[class*=\"ai_product_introduction\"],[module-key=\"ai_product_introduction\"],[data-module-key=\"ai_product_introduction\"],.red-packet,.lifeRed,.app-wiki-info-card,.wiki-info-card,[data-v-7b919764],[data-v-83bb4800],[data-v-d79df87b],[data-v-c0982931],.comment-main-list-loading{display:none !important;}';var head=document.head||document.documentElement;if(head)head.appendChild(st);}catch(e){}" +
            "}" +
            "function hideAiIntro(){" +
                "var nodes=document.querySelectorAll('.ai-product-intro-wrapper,[class*=\"ai-product-intro\"],[module-key=\"ai_product_introduction\"],[data-module-key=\"ai_product_introduction\"],[class*=\"ai_product_introduction\"],.red-packet,.lifeRed,.app-wiki-info-card,.wiki-info-card,[data-v-7b919764],[data-v-83bb4800],[data-v-d79df87b],[data-v-c0982931]');" +
                "for(var i=0;i<nodes.length;i++){nodes[i].style.setProperty('display','none','important');}" +
                "var disc=document.querySelectorAll('.intro-disclaimer');" +
                "for(var j=0;j<disc.length;j++){" +
                    "var wrap=disc[j].closest?disc[j].closest('.ai-product-intro-wrapper'):disc[j].parentElement&&disc[j].parentElement.parentElement;" +
                    "if(wrap)wrap.style.setProperty('display','none','important');" +
                "}" +
                "var cards=document.querySelectorAll('.page-card,.red-packet,.lifeRed,.wiki-info-card,.app-wiki-info-card');" +
                "for(var k=0;k<cards.length;k++){" +
                    "var c=cards[k];" +
                    "var t=String(c.textContent||'');" +
                    "if(t.length>0&&t.length<600&&(t.indexOf('红包小助手')!==-1||t.indexOf('生活省钱小助手')!==-1||t.indexOf('商品百科')!==-1)){" +
                        "c.style.setProperty('display','none','important');" +
                    "}" +
                "}" +
            "}" +
            "function isHidden(el){return !!(el&&el.style&&el.style.display==='none');}" +
            "function imgSignal(img){" +
                "if(!img)return false;" +
                "var s=String((img.currentSrc||img.src||img.getAttribute('data-src')||img.getAttribute('data-original')||'')+' '+(img.alt||'')+' '+(img.className||'')).toLowerCase();" +
                "return s.indexOf('pinduoduo')!==-1||s.indexOf('yangkeduo')!==-1||s.indexOf('pdd')!==-1||s.indexOf('redpacket')!==-1||s.indexOf('red_packet')!==-1||s.indexOf('share')!==-1||s.indexOf('红包')!==-1||s.indexOf('助力')!==-1||s.indexOf('拼团')!==-1;" +
            "}" +
            "function hideItem(el,isReply){" +
                "if(!el||isHidden(el))return;" +
                "var cid=el.getAttribute&&el.getAttribute('comment-id');" +
                "if(cid&&spamIdMap[String(cid)]){el.style.setProperty('display','none','important');return;}" +
                "var block=el.querySelector('.comment-content')||el;" +
                "var txtEl=block.querySelector('.comment-main-list-item-content-comment');" +
                "var imgs=block.querySelectorAll(isReply?'.comment-main-list-item-content-image.child img':'.comment-main-list-item-content-image:not(.child) img');" +
                "var imgEl=imgs.length?imgs[0]:null;" +
                "var raw=txtEl?txtEl.textContent:'';" +
                "if(isReply&&raw.indexOf('：')!==-1){raw=raw.substring(raw.indexOf('：')+1);}" +
                "var hasImg=imgs.length>0;" +
                "var shareImg=false;" +
                "for(var ii=0;ii<imgs.length;ii++){if(imgSignal(imgs[ii])){shareImg=true;break;}}" +
                "var w=0,h=0;" +
                "if(imgEl){w=imgEl.naturalWidth||Number(imgEl.getAttribute('width')||0);h=imgEl.naturalHeight||Number(imgEl.getAttribute('height')||0);}" +
                "if(shareImg||isSpam(raw,hasImg,w,h)){el.style.setProperty('display','none','important');}" +
            "}" +
            "function scan(){" +
                "installStyle();hideAiIntro();" +
                "var items=document.querySelectorAll('.comment-main-list-item');" +
                "for(var i=0;i<items.length;i++){hideItem(items[i],false);}" +
                "var replies=document.querySelectorAll('.comment-main-list-reply-item');" +
                "for(var j=0;j<replies.length;j++){hideItem(replies[j],true);}" +
            "}" +
            "var queued=false;" +
            "function schedule(){if(queued)return;queued=true;var run=function(){queued=false;try{scan();}catch(e){}};if(typeof requestAnimationFrame==='function')requestAnimationFrame(run);else setTimeout(run,16);}" +
            "function start(){" +
                "installStyle();schedule();" +
                "if(typeof MutationObserver!=='undefined'){var mo=new MutationObserver(schedule);try{mo.observe(document.documentElement||document,{childList:true,subtree:true,attributes:true,attributeFilter:['module-key']});}catch(e){}}" +
                "window.addEventListener('scroll',schedule,true);" +
                "document.addEventListener('DOMContentLoaded',schedule);" +
                "try{window.addEventListener('load',schedule);setInterval(schedule,1200);}catch(e){}" +
            "}" +
            "if(document.documentElement)start();else document.addEventListener('DOMContentLoaded',start);" +
        "})();";
    }
}
