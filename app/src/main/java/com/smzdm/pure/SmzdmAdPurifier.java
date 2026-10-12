package com.smzdm.pure;

import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import io.github.libxposed.api.XposedInterface;

public final class SmzdmAdPurifier {
    private static final String TAG = "SMZDMPure";
    // Cell types for Home/Haojia kingkong icon bars, rolling banners, and operation promo cards
    private static final Set<Integer> BLOCKED_CELL_TYPES = new HashSet<>(Arrays.asList(
            10000, // Feed10000Bean (top theme header)
            10001, // Feed10001Bean (Pangle / third-party native feed ad card)
            21028, // Haojia 5-icon bar
            23002, // Operation banner
            23003, // Feed23003Bean (Haojia / Home rolling banner ad)
            23004, // Feed23004Bean (Home double-column rolling banner ad)
            23006, // Operation banner
            23008, // Operation banner
            23011, // Operation banner
            23012, // Operation banner
            23021, // Haojia red-packet / PDD subsidy banner (11.1.95)
            23018, // Feed23018Bean operation card
            23023, // Operation card
            23025, // Operation card
            33012, // Operation card
            33031, // Operation card
            35004, // Haojia 今日神价 / 白菜专区 dual card
            35005, // Haojia top promo tag bar
            39015, // Feed39015Bean (Home single-column rolling banner ad)
            39023  // Operation card
    ));

    // Home icon bar (Feed20007Bean / Feed20024Bean) keeps the 排行榜 entry reachable, but the
    // promotional icons that share that bar stay hidden.
    private static final java.util.regex.Pattern BLOCKED_ICON_TITLE_PATTERN = java.util.regex.Pattern.compile(
            "签到|新品|国家补贴|国补|兴趣|红包|神券|免单|抽奖|众测"
    );

    private SmzdmAdPurifier() {
    }

    public static void install(XposedInterface xposed, ClassLoader cl) {
        // 1. Cold start splash ad in WelComeActivity
        Class<?> splashPresenterClass = ReflectUtils.findClass("og.c", cl);
        if (splashPresenterClass != null) {
            ReflectUtils.hookMethodNoOp(xposed, splashPresenterClass, "a", null);
            ReflectUtils.hookMethodNoOp(xposed, splashPresenterClass, "b", null);
        }
        Class<?> welcomeClass = ReflectUtils.findClass("com.smzdm.client.android.app.WelComeActivity", cl);
        if (welcomeClass != null) {
            XposedInterface.Hooker skipSplashHooker = chain -> {
                Object act = chain.getThisObject();
                if (act != null) {
                    ReflectUtils.setField(act, "W", null);
                    ReflectUtils.setBooleanField(act, "z0", true);
                    ReflectUtils.invokeMethod(act, "h7");
                }
                return null;
            };
            ReflectUtils.hookAllMethods(xposed, welcomeClass, "D2", skipSplashHooker);
            ReflectUtils.hookAllMethods(xposed, welcomeClass, "j4", skipSplashHooker);
            ReflectUtils.hookAllMethods(xposed, welcomeClass, "E7", skipSplashHooker);
        }

        // 2. Hot splash, interstitial, update & operation popup dialogs
        String[] blockedDialogClasses = new String[]{
                "com.smzdm.client.android.module.splash.SplashScreenAnimDialogFragment",
                "com.smzdm.client.android.app.Home11140NewGuideDialogFragment",
                "com.smzdm.client.android.view.dialog.YunyingDialogFragment",
                "com.smzdm.client.android.module.mine.fragment.UpdateDialogFragment",
                "com.smzdm.client.android.view.dialog.InviteRewardDialog",
                "com.smzdm.client.android.module.ad.InterstitialAdDialog"
        };
        for (String dlgName : blockedDialogClasses) {
            Class<?> dlgClass = ReflectUtils.findClass(dlgName, cl);
            if (dlgClass != null) {
                ReflectUtils.hookMethodNoOp(xposed, dlgClass, "show", null);
                ReflectUtils.hookMethodNoOp(xposed, dlgClass, "showNow", null);
                ReflectUtils.hookMethodNoOp(xposed, dlgClass, "t9", null);
            }
        }

        // 3. HomeActivity popup & floating widget triggers
        Class<?> homeActClass = ReflectUtils.findClass("com.smzdm.client.android.app.HomeActivity", cl);
        if (homeActClass != null) {
            String[] noOpHomeMethods = new String[]{"F9", "z9", "M8", "U3", "d5", "b9", "m9"};
            for (String mName : noOpHomeMethods) {
                ReflectUtils.hookMethodNoOp(xposed, homeActClass, mName, null);
            }
        }

        // 4. ZDMHomeFragment floating widget ad triggers
        Class<?> homeFragClass = ReflectUtils.findClass("com.smzdm.client.android.app.home.ZDMHomeFragment", cl);
        if (homeFragClass != null) {
            String[] noOpFragMethods = new String[]{"Ac", "tc", "yc", "uc", "rc", "sc"};
            for (String mName : noOpFragMethods) {
                ReflectUtils.hookMethodNoOp(xposed, homeFragClass, mName, null);
            }
        }

        // 5. Feed ad & operation beans in Home and Haojia feeds
        String[] feedDataClasses = new String[]{
                "com.smzdm.client.android.bean.ZDMHomeFeedBean$Data",
                "com.smzdm.client.android.bean.NewZDMHomeFeedBean$Data",
                "com.smzdm.client.android.bean.ZDMHaojiaHomeFeedBean$Data",
                "com.smzdm.client.android.module.haojia.home.bean.HaojiaHomeBean$Data",
                "com.smzdm.client.android.module.haojia.home.bean.HaojiaHomeBean$DataBean",
                "com.smzdm.client.android.bean.SearchResultBean$SearchInnerData",
                "com.smzdm.client.android.bean.RankListBean$Data",
                "com.smzdm.client.android.module.haojia.rank.bean.RankListHaowuBean$Data",
                "com.smzdm.client.android.module.haojia.rank.bean.RankZhiListBean$Data"
        };
        for (String dataClassName : feedDataClasses) {
            Class<?> dataClass = ReflectUtils.findClass(dataClassName, cl);
            if (dataClass != null) {
                ReflectUtils.hookMethodNoOp(xposed, dataClass, "getWidget", null);
                ReflectUtils.hookMethodNoOp(xposed, dataClass, "getBanner", null);
                ReflectUtils.hookMethodNoOp(xposed, dataClass, "getHeader_operation", null);
                ReflectUtils.hookAllMethods(xposed, dataClass, "getRows", chain -> {
                    Object listObj = chain.proceed();
                    if (listObj instanceof List<?>) {
                        filterFeedRows((List<?>) listObj);
                    }
                    return listObj;
                });
                ReflectUtils.hookAllMethods(xposed, dataClass, "getBanner_v2", chain -> {
                    Object listObj = chain.proceed();
                    if (listObj instanceof List<?>) {
                        filterFeedRows((List<?>) listObj);
                    }
                    return listObj;
                });
            }
        }

        // 6. Home v3 SaaS component feed (HomeListBean$Data & ComponentListBean & HomeRecFragment)
        Class<?> homeListDataClass = ReflectUtils.findClass("com.smzdm.client.android.bean.saas.HomeListBean$Data", cl);
        if (homeListDataClass != null) {
            ReflectUtils.hookMethodNoOp(xposed, homeListDataClass, "getHeader_operation", null);
            ReflectUtils.hookAllMethods(xposed, homeListDataClass, "getComponent", chain -> {
                Object compObj = chain.proceed();
                if (compObj instanceof List<?>) {
                    filterHomeComponents((List<?>) compObj);
                }
                return compObj;
            });
        }

        Class<?> compListClass = ReflectUtils.findClass("com.smzdm.client.android.bean.component_bean.ComponentListBean", cl);
        if (compListClass != null) {
            XposedInterface.Hooker compListHooker = chain -> {
                Object listObj = chain.proceed();
                if (listObj instanceof List<?>) {
                    filterFeedRows((List<?>) listObj);
                }
                return listObj;
            };
            ReflectUtils.hookAllMethods(xposed, compListClass, "getZz_content", compListHooker);
            ReflectUtils.hookAllMethods(xposed, compListClass, "convert", compListHooker);
        }

        Class<?> homeRecFragClass = ReflectUtils.findClass("com.smzdm.client.android.app.recommend.HomeRecFragment", cl);
        if (homeRecFragClass == null) {
            homeRecFragClass = ReflectUtils.findClass("t5.ViewOnClickListenerC0576A", cl);
        }
        if (homeRecFragClass != null) {
            XposedInterface.Hooker bindListHooker = chain -> {
                Object arg0 = chain.getArg(0);
                Object arg1 = chain.getArg(1);
                if (arg0 instanceof List<?>) {
                    List<?> list = (List<?>) arg0;
                    int filterIdx = arg1 instanceof Integer ? (Integer) arg1 : -1;
                    int idx = 0;
                    Iterator<?> it = list.iterator();
                    while (it.hasNext()) {
                        Object row = it.next();
                        if (isAdFeedRow(row)) {
                            it.remove();
                            if (filterIdx > 0 && idx < filterIdx) {
                                filterIdx--;
                            }
                        }
                        idx++;
                    }
                    if (arg1 instanceof Integer && filterIdx != (Integer) arg1) {
                        Object[] newArgs = new Object[6];
                        newArgs[0] = list;
                        newArgs[1] = filterIdx;
                        newArgs[2] = 0;
                        newArgs[3] = chain.getArg(3);
                        newArgs[4] = chain.getArg(4);
                        newArgs[5] = chain.getArg(5);
                        return chain.proceed(newArgs);
                    }
                }
                return chain.proceed();
            };
            ReflectUtils.hookAllMethods(xposed, homeRecFragClass, "J3", bindListHooker);
            ReflectUtils.hookAllMethods(xposed, homeRecFragClass, "D6", bindListHooker);
            XposedInterface.Hooker listArg0Hooker = chain -> {
                Object arg0 = chain.getArg(0);
                if (arg0 instanceof List<?>) {
                    filterFeedRows((List<?>) arg0);
                }
                return chain.proceed();
            };
            ReflectUtils.hookAllMethods(xposed, homeRecFragClass, "M0", listArg0Hooker);
            ReflectUtils.hookAllMethods(xposed, homeRecFragClass, "B2", listArg0Hooker);
            ReflectUtils.hookAllMethods(xposed, homeRecFragClass, "M1", listArg0Hooker);
        }

        // 7. 11.1.95 newstyle Home recommend feed (o5.o & com.smzdm.client.android.app.recommend.b)
        Class<?> newHomeFeedFragClass = ReflectUtils.findClass("o5.o", cl);
        if (newHomeFeedFragClass != null) {
            ReflectUtils.hookAllMethods(xposed, newHomeFeedFragClass, "Ca", chain -> {
                Object arg0 = chain.getArg(0);
                if (arg0 instanceof List<?>) {
                    filterHomeFeedNews((List<?>) arg0);
                    filterFeedRows((List<?>) arg0);
                }
                return chain.proceed();
            });
        }
        Class<?> recommendAdapterClass = ReflectUtils.findClass("com.smzdm.client.android.app.recommend.b", cl);
        if (recommendAdapterClass != null) {
            XposedInterface.Hooker recListHooker = chain -> {
                Object arg0 = chain.getArg(0);
                if (arg0 instanceof List<?>) {
                    filterHomeFeedNews((List<?>) arg0);
                    filterFeedRows((List<?>) arg0);
                }
                return chain.proceed();
            };
            for (String m : new String[]{"K", "w", "T0"}) {
                ReflectUtils.hookAllMethods(xposed, recommendAdapterClass, m, recListHooker);
            }
        }

        // 8. Pangle (穿山甲) / third-party async feed ad injector & Holder10001
        Class<?> adInjectorClass = ReflectUtils.findClass("lk.x", cl);
        if (adInjectorClass != null) {
            XposedInterface.Hooker blockAdInjectorHooker = chain -> {
                for (int i = 0; i < 6; i++) {
                    try {
                        Object arg = chain.getArg(i);
                        if (arg instanceof List<?>) {
                            filterFeedRows((List<?>) arg);
                        }
                    } catch (Throwable ignored) {
                        break;
                    }
                }
                return null;
            };
            for (String m : new String[]{"a", "b", "c", "d", "g", "h", "i", "j"}) {
                ReflectUtils.hookAllMethods(xposed, adInjectorClass, m, blockAdInjectorHooker);
            }
        }
        Class<?> adPresenterHelperClass = ReflectUtils.findClass("M4.a", cl);
        if (adPresenterHelperClass != null) {
            ReflectUtils.hookMethodNoOp(xposed, adPresenterHelperClass, "C", null);
        }
        Class<?> searchAdHelperClass = ReflectUtils.findClass("O4.a", cl);
        if (searchAdHelperClass != null) {
            ReflectUtils.hookMethodNoOp(xposed, searchAdHelperClass, "a", null);
            ReflectUtils.hookMethodNoOp(xposed, searchAdHelperClass, "d", null);
        }
        for (String holderClassName : new String[]{
                "com.smzdm.client.android.zdmholder.holders.new_type.Holder10001",
                "W9.J"
        }) {
            Class<?> holder10001Class = ReflectUtils.findClass(holderClassName, cl);
            if (holder10001Class != null) {
                XposedInterface.Hooker collapseHolderHooker = chain -> {
                    Object self = chain.getThisObject();
                    Object itemView = self != null ? ReflectUtils.getField(self, "itemView") : null;
                    if (itemView instanceof android.view.View) {
                        SmzdmLayoutPurifier.collapseView((android.view.View) itemView);
                    }
                    return null;
                };
                for (String m : new String[]{"onBindData", "bindData", "w0"}) {
                    ReflectUtils.hookAllMethods(xposed, holder10001Class, m, collapseHolderHooker);
                }
            }
        }

        String[] haojiaFragClassNames = new String[]{
                "com.smzdm.client.android.module.haojia.home.HaojiaHomeFragment",
                "k9.r"
        };
        for (String haojiaClassName : haojiaFragClassNames) {
            Class<?> haojiaFragClass = ReflectUtils.findClass(haojiaClassName, cl);
            if (haojiaFragClass == null) continue;
            XposedInterface.Hooker haojiaHooker = chain -> {
                Object res = chain.proceed();
                Object root = ReflectUtils.invokeMethod(chain.getThisObject(), "getView");
                if (root instanceof android.view.View) {
                    for (String entryName : new String[]{"rv_banner", "cl_banner", "top_bg", "iv_skin"}) {
                        SmzdmLayoutPurifier.hideViewByEntryName((android.view.View) root, entryName);
                    }
                }
                return res;
            };
            for (String m : new String[]{"db", "eb", "aa", "onResume", "onViewCreated"}) {
                ReflectUtils.hookAllMethods(xposed, haojiaFragClass, m, haojiaHooker);
            }
        }

        Log.i(TAG, "SmzdmAdPurifier hooks installed.");
    }

    public static void filterHomeComponents(List<?> components) {
        if (components == null || components.isEmpty()) return;
        Iterator<?> it = components.iterator();
        while (it.hasNext()) {
            Object comp = it.next();
            if (comp == null) {
                it.remove();
                continue;
            }
            Object zzTypeObj = ReflectUtils.getField(comp, "zz_type");
            String zzType = zzTypeObj instanceof String ? ((String) zzTypeObj).trim() : "";
            // Keep 'list' (main feed), 'filter' (filter bar) and 'circular_banner' (round icon bar
            // that holds 排行榜); drop 'banner', 'screen_banner', 'hongbao', 'yixing_banner', etc.
            if (!"list".equals(zzType) && !"filter".equals(zzType) && !"circular_banner".equals(zzType)) {
                it.remove();
                continue;
            }
            if ("circular_banner".equals(zzType)) {
                Object iconContent = ReflectUtils.getField(comp, "zz_content");
                if (iconContent instanceof List<?>) {
                    filterIconList((List<?>) iconContent);
                }
                continue;
            }
            if ("list".equals(zzType)) {
                Object contentObj = ReflectUtils.getField(comp, "zz_content");
                if (contentObj instanceof List<?>) {
                    filterHomeFeedNews((List<?>) contentObj);
                    filterFeedRows((List<?>) contentObj);
                }
            }
        }
    }

    /** Drops the promotional icons from a home icon-bar list while keeping 排行榜 and similar. */
    public static void filterIconList(List<?> icons) {
        if (icons == null || icons.isEmpty()) return;
        Iterator<?> it = icons.iterator();
        while (it.hasNext()) {
            Object icon = it.next();
            if (icon == null) {
                it.remove();
                continue;
            }
            String title = iconTitle(icon);
            if (!title.isEmpty() && BLOCKED_ICON_TITLE_PATTERN.matcher(title).find()) {
                it.remove();
            }
        }
    }

    private static String iconTitle(Object icon) {
        Object inner = ReflectUtils.getField(icon, "zz_content");
        String title = inner != null ? stringField(inner, "article_title") : "";
        if (title.isEmpty()) title = stringField(icon, "article_title");
        if (title.isEmpty()) title = stringField(icon, "title");
        return title;
    }

    /**
     * Home feed mixes product/deal cards with article/news cards. Keep only items that expose
     * product signals (price or mall) so the home feed stays shopping-oriented.
     */
    public static void filterHomeFeedNews(List<?> rows) {
        if (rows == null || rows.isEmpty()) return;
        Iterator<?> it = rows.iterator();
        while (it.hasNext()) {
            Object row = it.next();
            if (row == null) continue;
            if (!isHomeFeedRow(row)) continue;
            String title = stringField(row, "article_title");
            if (title.isEmpty()) {
                title = stringField(row, "single_article_title");
            }
            if (title.isEmpty()) continue;

            String price = stringField(row, "article_price");
            if (price.isEmpty()) {
                price = stringField(row, "article_price_num");
            }
            boolean hasMall = hasMall(row);
            if (price.isEmpty() && !hasMall) {
                it.remove();
            }
        }
    }

    private static boolean isHomeFeedRow(Object row) {
        String className = row.getClass().getName();
        return className.endsWith("FeedHolderBean") || className.startsWith("com.smzdm.client.android.bean.");
    }

    private static boolean hasMall(Object row) {
        Object mallObj = ReflectUtils.getField(row, "article_mall");
        if (mallObj instanceof List<?>) {
            return !((List<?>) mallObj).isEmpty();
        }
        if (mallObj != null) {
            return true;
        }
        String mall = stringField(row, "article_mall");
        return !mall.isEmpty();
    }

    private static String stringField(Object row, String fieldName) {
        Object value = ReflectUtils.getField(row, fieldName);
        return value instanceof String ? ((String) value).trim() : "";
    }

    public static void filterFeedRows(List<?> rows) {
        if (rows == null || rows.isEmpty()) return;
        Iterator<?> it = rows.iterator();
        while (it.hasNext()) {
            Object row = it.next();
            if (filterHomeIconBar(row)) {
                it.remove();
                continue;
            }
            if (isAdFeedRow(row)) {
                it.remove();
            }
        }
    }

    /**
     * Feed20007Bean / Feed20024Bean are the home circular icon bar. The whole bar used to be
     * dropped, which also removed the 排行榜 entry. Keep the bar, drop only the promotional
     * icons inside it, and report true when nothing useful is left.
     */
    public static boolean filterHomeIconBar(Object row) {
        if (row == null) return false;
        String className = row.getClass().getName();
        if (!className.endsWith("Feed20007Bean") && !className.endsWith("Feed20024Bean")) {
            return false;
        }
        Object contentObj = ReflectUtils.getField(row, "zz_content");
        if (!(contentObj instanceof List<?>)) return false;
        List<?> icons = (List<?>) contentObj;
        filterIconList(icons);
        return icons.isEmpty();
    }

    public static boolean isAdFeedRow(Object row) {
        if (row == null) return false;
        if (isYunYingOrBannerClass(row.getClass())) {
            return true;
        }
        Object cellTypeObj = ReflectUtils.getField(row, "cell_type");
        if (cellTypeObj instanceof Number && BLOCKED_CELL_TYPES.contains(((Number) cellTypeObj).intValue())) {
            return true;
        }
        Object modelType = ReflectUtils.getField(row, "model_type");
        if (modelType instanceof String) {
            String mt = ((String) modelType).trim().toLowerCase();
            if ("ads".equals(mt) || "yunying".equals(mt) || "ad".equals(mt)) {
                return true;
            }
        }
        Object promoType = ReflectUtils.getField(row, "promotion_type");
        if (promoType instanceof Number && ((Number) promoType).intValue() > 0) {
            return true;
        }
        Object campaignId = ReflectUtils.getField(row, "ad_campaign_id");
        if (campaignId instanceof String && !((String) campaignId).isEmpty() && !"0".equals(campaignId)) {
            return true;
        }
        Object bannerId = ReflectUtils.getField(row, "ad_banner_id");
        if (bannerId instanceof String && !((String) bannerId).isEmpty() && !"0".equals(bannerId)) {
            return true;
        }
        if (ReflectUtils.getField(row, "adWrapper") != null) {
            return true;
        }
        if (!stringField(row, "adx_template").isEmpty()
                || !stringField(row, "zdm_template").isEmpty()
                || !stringField(row, "template").isEmpty()) {
            return true;
        }
        String tag = stringField(row, "tag");
        if ("广告".equals(tag) || tag.contains("广告")) {
            return true;
        }
        return false;
    }

    private static boolean isYunYingOrBannerClass(Class<?> clazz) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            String name = current.getName();
            if (name.endsWith("FeedYunYingBean")
                    || name.endsWith("BaseBannerBean")
                    || name.endsWith("BaseYunyingBean")
                    || name.contains("AdBean")) {
                return true;
            }
            current = current.getSuperclass();
        }
        return false;
    }
}
