package com.smzdm.pure;

import android.util.Log;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
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

        installMarketingPopupHooks(xposed, cl);

        // Keep the existing, separate splash/interstitial ad hooks.
        String[] blockedDialogClasses = new String[]{
                "com.smzdm.client.android.module.splash.SplashScreenAnimDialogFragment",
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

        // Legacy Home recommendation: clean before binding and remap the original filter boundary.
        for (String name : new String[]{"com.smzdm.client.android.app.recommend.HomeRecFragment", "t5.A"}) {
            Class<?> fragment = ReflectUtils.findClass(name, cl);
            if (fragment == null) continue;
            for (String method : new String[]{"J3", "D6"}) {
                ReflectUtils.hookAllMethods(xposed, fragment, method, chain -> {
                    Object[] args = chain.getArgs().toArray();
                    if (args.length != 6 || !(args[0] instanceof List<?>)
                            || !(args[1] instanceof Integer) || !(args[2] instanceof Integer)) {
                        return chain.proceed();
                    }
                    int boundary = (Integer) args[1];
                    HomeRows filtered = filterHomeRows((List<?>) args[0], boundary, true);
                    args[0] = filtered.rows;
                    args[1] = filtered.boundary;
                    return chain.proceed(args);
                });
            }
            for (String method : new String[]{"M0", "B2", "M1", "Y5", "f5"}) {
                ReflectUtils.hookAllMethods(xposed, fragment, method, chain -> proceedWithHomeRows(chain, 0));
            }
        }

        // 11.1.95 recommendation adapter: replace/refresh (K, T0, C0) and legacy append (w).
        Class<?> recommendAdapter = ReflectUtils.findClass("com.smzdm.client.android.app.recommend.b", cl);
        if (recommendAdapter != null) {
            for (String method : new String[]{"K", "w", "T0"}) {
                ReflectUtils.hookAllMethods(xposed, recommendAdapter, method,
                        chain -> proceedWithHomeRows(chain, 0));
            }
            ReflectUtils.hookAllMethods(xposed, recommendAdapter, "C0",
                    chain -> proceedWithHomeRows(chain, 1));
            // o5.o.La uses the inherited Ve.c.y for new-style pagination. Ve.c is shared;
            // leave all other adapters' invocations untouched.
            ReflectUtils.hookMethod(xposed, recommendAdapter, "y", new Class<?>[]{List.class},
                    chain -> recommendAdapter.isInstance(chain.getThisObject())
                            ? proceedWithHomeRows(chain, 0) : chain.proceed());
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

    private static void installMarketingPopupHooks(XposedInterface xposed, ClassLoader cl) {
        // 11.1.95: this caller only starts /activity/cooperation_popup. Do not hook
        // the clipboard dispatcher: other branches resolve product/share commands.
        hookMarketingEntry(xposed, cl, "com.smzdm.client.base.clipboard.d", "s", "void",
                chain -> null, String.class, android.app.Activity.class);

        // /util/update is shared with app updates and other features. Intercept just
        // the operation_float consumer, before it expands templates or loads creatives.
        String manager = "com.smzdm.module.advertise.home.n";
        hookMarketingEntry(xposed, cl, manager, "f", "void", chain -> {
            if (!(chain.getArg(0) instanceof List<?>) || ((List<?>) chain.getArg(0)).isEmpty()) {
                return chain.proceed();
            }
            // Keep the original no-campaign path (including its widget refresh callback).
            return chain.proceed(new Object[]{new ArrayList<>()});
        }, List.class);

        // Both HomeActivity and MainActivity treat null as "no ad" before scheduling.
        // Also covers old tab_dialog cache before the next config response arrives.
        hookMarketingEntry(xposed, cl, manager, "c",
                "com.smzdm.client.base.bean.UpdateBean$HomeDialogAdsBean", chain -> null, int.class);
    }

    private static void hookMarketingEntry(XposedInterface xposed, ClassLoader cl, String owner,
                                           String name, String returnType,
                                           XposedInterface.Hooker hooker, Class<?>... paramTypes) {
        String target = owner + "#" + name;
        try {
            Class<?> type = Class.forName(owner, false, cl);
            Method method = type.getDeclaredMethod(name, paramTypes);
            if (!method.getReturnType().getName().equals(returnType)
                    || Modifier.isStatic(method.getModifiers())) {
                Log.w(TAG, "Marketing popup hook skipped (unexpected signature): " + target);
                return;
            }
            method.setAccessible(true);
            xposed.hook(method).intercept(hooker);
            Log.i(TAG, "Marketing popup hook installed: " + target);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            Log.w(TAG, "Marketing popup hook unavailable: " + target, e);
        }
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
                // Retain the empty top-level list component for the host's page processing.
                if (contentObj != null && (!(contentObj instanceof List<?>) || !sanitizeHomeChildList(
                        comp, "zz_content", (List<?>) contentObj, newHomePath(), 0, false))) {
                    it.remove();
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

    private static final String HOLDER_BEAN = "com.smzdm.client.android.bean.holder_bean.";
    private static final String COMPONENT_LIST = "com.smzdm.client.android.bean.component_bean.ComponentListBean";
    // Explicit 11.1.95 deal holders (BaseHaojiaBean family). Do not infer product identity
    // from title, price, mall, a broad package prefix, or an entire cell_type range.
    private static final Set<Integer> HOME_PRODUCT_CELLS = new HashSet<>(Arrays.asList(
            20001, 20040, 21001, 21014, 21021, 21022, 21023, 21024, 21025, 21026, 21030, 21201
    ));
    private static final String[] HOME_CHILD_FIELDS = {"sub_rows", "rows", "zz_content"};
    private static final int MAX_HOME_NESTING = 16;

    private static Object proceedWithHomeRows(XposedInterface.Chain chain, int listIndex) throws Throwable {
        Object[] args = chain.getArgs().toArray();
        if (listIndex >= args.length || !(args[listIndex] instanceof List<?>)) {
            return chain.proceed();
        }
        args[listIndex] = filterHomeRows((List<?>) args[listIndex], -1, true).rows;
        return chain.proceed(args);
    }

    static final class HomeRows {
        final List<Object> rows;
        final int boundary;

        HomeRows(List<Object> rows, int boundary) {
            this.rows = rows;
            this.boundary = boundary;
        }
    }

    static HomeRows filterHomeRows(List<?> source, int boundary, boolean keepFilterControl) {
        return filterHomeRows(source, boundary, keepFilterControl, newHomePath(), 0);
    }

    private static Set<Object> newHomePath() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    private static HomeRows filterHomeRows(List<?> source, int boundary, boolean keepFilterControl,
                                           Set<Object> path, int depth) {
        List<Object> kept = new ArrayList<>();
        int keptBeforeBoundary = 0;
        int index = 0;
        if (depth < MAX_HOME_NESTING && path.add(source)) {
            try {
                for (Object row : source) {
                    if (keepHomeRow(row, keepFilterControl, path, depth)) {
                        kept.add(row);
                        if (index < boundary) keptBeforeBoundary++;
                    }
                    index++;
                }
            } finally {
                path.remove(source);
            }
        }
        return new HomeRows(kept, boundary < 0 ? boundary : keptBeforeBoundary);
    }

    private static boolean keepHomeRow(Object row, boolean keepFilterControl, Set<Object> path, int depth) {
        if (row == null || !path.add(row)) return false;
        try {
            if (isAdFeedRow(row)) return false;
            String name = row.getClass().getName();
            if (COMPONENT_LIST.equals(name) && "list".equals(stringField(row, "zz_type"))) {
                Object children = ReflectUtils.getField(row, "zz_content");
                return children instanceof List<?> && sanitizeHomeChildList(row, "zz_content",
                        (List<?>) children, path, depth + 1, true);
            }
            Object cell = ReflectUtils.getField(row, "cell_type");
            if (!(cell instanceof Integer)) return false;
            int type = (Integer) cell;
            // The adapter's filter bar is a UI control, not a recommendation card.
            if (!name.equals(HOLDER_BEAN + "Feed" + type + "Bean")
                    || !(HOME_PRODUCT_CELLS.contains(type) || keepFilterControl && type == 20010)) {
                return false;
            }
            for (String field : HOME_CHILD_FIELDS) {
                Object children = ReflectUtils.getField(row, field);
                if (children != null && (!(children instanceof List<?>) || !sanitizeHomeChildList(
                        row, field, (List<?>) children, path, depth + 1, false))) {
                    return false;
                }
            }
            return true;
        } finally {
            path.remove(row);
        }
    }

    private static boolean sanitizeHomeChildList(Object owner, String field, List<?> original,
                                                  Set<Object> path, int depth, boolean requireRows) {
        List<Object> kept = filterHomeRows(original, -1, false, path, depth).rows;
        if (requireRows && kept.isEmpty()) return false;
        if (original.size() == kept.size()) return true;
        // Replace rather than removing from a possibly read-only list. Drop the parent if
        // the filtered list cannot be installed, rather than rendering untouched children.
        ReflectUtils.setField(owner, field, kept);
        return ReflectUtils.getField(owner, field) == kept;
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
