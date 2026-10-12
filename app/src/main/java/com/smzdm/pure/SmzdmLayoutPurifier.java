package com.smzdm.pure;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.github.libxposed.api.XposedInterface;

public final class SmzdmLayoutPurifier {
    private static final String TAG = "SMZDMPure";
    private static final String TARGET_PKG = "com.smzdm.client.android";

    // Resource entry names to hide across bottom bar, Home, Haojia, Search, floating widgets, and "Mine" page
    private static final Set<String> HIDDEN_ENTRY_NAMES = new HashSet<>(Arrays.asList(
            // Bottom bar '+' publish button & overlays & '社区' tab
            "ib_pub",
            "bg_round",
            "hongbao_icon",
            "publish_bubble",
            "tab_article",
            // 11.1.95 newstyle bottom bar: publish pill
            "ll_tab_publish",
            "iv_tab_publish",
            "tv_tab_publish",
            // Home top-right icons (兴趣广场 / 我的兴趣 / 签到) & right drawer
            "iv_interest_square",
            "iv_my_interest",
            "iv_sign",
            "iv_checkin_entry",
            "drawer_interest",
            // Haojia top promo area (今日神价 / 白菜专区 / 滚动推广 / 顶部图标栏)
            "cl_banner",
            "rv_banner",
            "top_bg",
            "iv_skin",
            "iv_top_right",
            // Search bar promo icon & animation cover
            "iv_search_icon",
            "iv_search_icon_anim",
            "view_cover",
            // Search page discovery & hot search rank below search history
            "fl_hongbao",
            "cv_hongbao",
            "layout_faxian",
            "tag_faxian",
            "rv_find",
            "tv_hide_faxian_desc",
            "hot_search_container_old",
            "hot_search_container_new",
            "hot_search_container",
            "hot_search_recycler",
            "tv_hide_hot_search",
            // Floating ad widgets
            "iv_float",
            "iv_float_close",
            "vs_widget_plus",
            "vs_subsidy_widget_plus",
            // "Mine" (UserCenterFragment) flowing banner ads, more services & bottom service cards
            "v_container_banner_module",
            "banner_user_center",
            "uc_ad_container",
            "creator_center_banner",
            "creator_center_banner_container",
            "uc_invite_sw_ctl_area",
            "v_container_toolbar_module",
            "v_container_more_service_module",
            "tv_label_more_service",
            "rcv_container_more_service",
            "uc_service_card_area",
            "uc_rv_service_list",
            "uc_tv_sc_title",
            "layout_creator_guide",
            // 11.1.95 "Mine" page modules
            "user_center2_big_banner",
            "user_center2_big_banner_container",
            "user_center2_more_service",
            "uc_gen2_more_service_list",
            "uc_gen2_more_service_title",
            "uc_gen2_creator_banner",
            "uc_gen2_creator_banner_container",
            "uc_gen2_top_ad",
            "uc_gen2_wallet_card",
            "uc_gen2_wallet_red_dot",
            "uc_gen2_creator_shequ_area",
            "uc_gen2_creator_shequ_divider",
            "uc_gen2_creator_shequ_card",
            "uc_gen2_more_service_card",
            "uc_gen2_more_service_list",
            "uc_gen2_more_service_title",
            "user_center2_creator_center",
            "user_center2_selected_activity",
            "uc_gen2_selected_activity_card",
            "user_center2_bottom_tips",
            "v_container_user_creator_center_module",
            "rcv_container_user_creator_center",
            "uc_creator_right_container",
            "group_creator_center_message",
            "mqv_creator_center_message",
            "user_center2_sign_guide",
            // 11.1.95 newstyle home header
            "float_widget",
            "home_float_widget",
            "rv_home_header",
            "iv_kingkong_icon_bg",
            "iv_kingkong_skin_bg",
            "layout_sticky_header"
    ));

    private static final ConcurrentHashMap<Integer, Boolean> ID_DECISION_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Integer> ENTRY_ID_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, String> ID_ENTRY_CACHE = new ConcurrentHashMap<>();

    private SmzdmLayoutPurifier() {
    }

    public static boolean shouldHideResourceEntryName(String entryName) {
        return entryName != null && HIDDEN_ENTRY_NAMES.contains(entryName);
    }

    public static boolean shouldHideView(View view) {
        if (view == null) return false;
        String className = view.getClass().getName();
        if (className.endsWith("HandleFloatView")
                || className.endsWith("UserCenterAdvertisementView")
                || className.endsWith("UserCreatorGuideView")) {
            return true;
        }
        int id = view.getId();
        if (id == View.NO_ID || (id >>> 24) != 0x7f) {
            return false;
        }
        Boolean cached = ID_DECISION_CACHE.get(id);
        if (cached != null) {
            return cached;
        }
        boolean hide = false;
        try {
            Resources res = view.getResources();
            if (res != null) {
                String entryName = idEntryName(res, id);
                hide = shouldHideResourceEntryName(entryName);
                ID_DECISION_CACHE.put(id, hide);
                return hide;
            }
        } catch (Throwable ignored) {
        }
        hide = shouldHideResourceEntryName(entryNameForHiddenId(view.getContext(), id));
        ID_DECISION_CACHE.put(id, hide);
        return hide;
    }

    public static void collapseView(View view) {
        if (view == null) return;
        try {
            if (view.getVisibility() != View.GONE) {
                view.setVisibility(View.GONE);
            }
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            if (lp != null) {
                boolean changed = false;
                if (lp.height != 0) {
                    lp.height = 0;
                    changed = true;
                }
                if (lp instanceof LinearLayout.LayoutParams) {
                    LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) lp;
                    if (llp.weight != 0f || llp.width != 0) {
                        llp.weight = 0f;
                        llp.width = 0;
                        changed = true;
                    }
                }
                if (lp instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                    if (mlp.topMargin != 0 || mlp.bottomMargin != 0) {
                        mlp.topMargin = 0;
                        mlp.bottomMargin = 0;
                        changed = true;
                    }
                }
                if (changed) {
                    view.setLayoutParams(lp);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static void hideViewByEntryName(View root, String entryName) {
        View target = findViewByEntryName(root, entryName);
        if (target != null) {
            collapseView(target);
        }
    }

    public static void install(XposedInterface xposed, ClassLoader cl) {
        // 1. Hook View.setVisibility(int) to prevent async callbacks from re-showing hidden views
        ReflectUtils.hookMethod(xposed, View.class, "setVisibility", new Class<?>[]{int.class}, chain -> {
            Object self = chain.getThisObject();
            if (self instanceof View) {
                View v = (View) self;
                if (shouldHideView(v)) {
                    collapseZeroParams(v);
                    return chain.proceed(new Object[]{View.GONE});
                }
            }
            return chain.proceed();
        });

        // 2. Hook View.onAttachedToWindow() to immediately collapse target views when added
        ReflectUtils.hookMethod(xposed, View.class, "onAttachedToWindow", new Class<?>[0], chain -> {
            Object res = chain.proceed();
            Object self = chain.getThisObject();
            if (self instanceof View) {
                View v = (View) self;
                if (shouldHideView(v)) {
                    collapseView(v);
                    // If iv_float is inside a floating container RelativeLayout, hide that container too
                    hideFloatParentIfApplicable(v);
                } else if (v instanceof TextView) {
                    sanitizeSearchTextView((TextView) v);
                }
            }
            return res;
        });

        // 2b. Hook TextView.setText and TextView.setHint to strip search bar promotional keywords
        ReflectUtils.hookMethod(xposed, TextView.class, "setText",
                new Class<?>[]{CharSequence.class, TextView.BufferType.class}, chain -> {
                    Object self = chain.getThisObject();
                    if (self instanceof TextView && !(self instanceof EditText)) {
                        TextView tv = (TextView) self;
                        if (isSearchBarPlaceholderView(tv)) {
                            CharSequence text = (CharSequence) chain.getArg(0);
                            if (text != null && text.length() > 0 && !"搜索".equals(text.toString().trim())) {
                                return chain.proceed(new Object[]{"", chain.getArg(1)});
                            }
                        }
                        Object result = chain.proceed();
                        hideNativeAiIntroduction(tv);
                        return result;
                    }
                    return chain.proceed();
                });
        ReflectUtils.hookMethod(xposed, TextView.class, "setHint",
                new Class<?>[]{CharSequence.class}, chain -> {
                    Object self = chain.getThisObject();
                    if (self instanceof TextView) {
                        TextView tv = (TextView) self;
                        if (isSearchInputEditText(tv) || isSearchBarPlaceholderView(tv)) {
                            return chain.proceed(new Object[]{""});
                        }
                    }
                    return chain.proceed();
                });

        // 3. Hook HomeActivity for bottom navigation bar layout (remove '+' spacer and '社区')
        Class<?> homeActClass = ReflectUtils.findClass("com.smzdm.client.android.app.HomeActivity", cl);
        if (homeActClass != null) {
            XposedInterface.Hooker afterHomeLayoutHooker = chain -> {
                Object res = chain.proceed();
                purifyHomeActivity(chain.getThisObject());
                return res;
            };
            String[] homeLayoutMethods = new String[]{"initView", "onResume", "X8", "o8", "x9"};
            for (String m : homeLayoutMethods) {
                ReflectUtils.hookAllMethods(xposed, homeActClass, m, afterHomeLayoutHooker);
            }
            for (String m : new String[]{"initView", "onResume", "D9", "o8", "X8"}) {
                ReflectUtils.hookAllMethods(xposed, homeActClass, m, afterHomeLayoutHooker);
            }

            // Redirect tab index 3 ('社区') to 0 ('首页')
            ReflectUtils.hookMethod(xposed, homeActClass, "c9", new Class<?>[]{int.class}, chain -> {
                Object arg0 = chain.getArg(0);
                if (arg0 instanceof Integer && ((Integer) arg0) == 3) {
                    return chain.proceed(new Object[]{0});
                }
                return chain.proceed();
            });
            ReflectUtils.hookMethod(xposed, homeActClass, "d9", new Class<?>[]{int.class, boolean.class}, chain -> {
                Object arg0 = chain.getArg(0);
                if (arg0 instanceof Integer && ((Integer) arg0) == 3) {
                    return chain.proceed(new Object[]{0, chain.getArg(1)});
                }
                return chain.proceed();
            });
        }

        // 4. Hook ZDMHomeFragment (11.1.63) / M (11.1.95) to hide top icons, search promo and floating widgets
        String[] homeFragClassNames = new String[]{
                "com.smzdm.client.android.app.home.ZDMHomeFragment",
                "com.smzdm.client.android.app.home.M"
        };
        for (String homeFragClassName : homeFragClassNames) {
            Class<?> homeFragClass = ReflectUtils.findClass(homeFragClassName, cl);
            if (homeFragClass == null) continue;
            XposedInterface.Hooker homeFragHooker = chain -> {
                Object res = chain.proceed();
                purifyHomeFragment(chain.getThisObject());
                return res;
            };
            String[] homeFragMethods = new String[]{
                    "initView", "V9", "T9", "onResume", "nc", "Rb", "cc",
                    "aa", "Ub", "ob", "dc"
            };
            for (String m : homeFragMethods) {
                ReflectUtils.hookAllMethods(xposed, homeFragClass, m, homeFragHooker);
            }

            // Block extra top tabs (such as 国家补贴 'guobu'); keep only 推荐 & 关注
            ReflectUtils.hookMethodNoOp(xposed, homeFragClass, "vc", null);
            ReflectUtils.hookAllMethods(xposed, homeFragClass, "xc", chain -> {
                trimHomeTopTabs(chain.getThisObject());
                Object res = chain.proceed();
                purifyHomeFragment(chain.getThisObject());
                return res;
            });
            ReflectUtils.hookAllMethods(xposed, homeFragClass, "Ob", chain -> {
                trimHomeTopTabs(chain.getThisObject());
                Object res = chain.proceed();
                purifyHomeFragment(chain.getThisObject());
                return res;
            });
            ReflectUtils.hookAllMethods(xposed, homeFragClass, "Dc", chain -> {
                Object res = chain.proceed();
                trimHomeTopTabs(chain.getThisObject());
                return res;
            });
        }

        // 4b. Hook HomeRecFragment (11.1.95 = t5.ViewOnClickListenerC0576A) for feed cleaning fallback
        String[] homeRecClassNames = new String[]{
                "com.smzdm.client.android.app.recommend.HomeRecFragment",
                "t5.ViewOnClickListenerC0576A"
        };
        for (String homeRecClassName : homeRecClassNames) {
            Class<?> homeRecFragClass = ReflectUtils.findClass(homeRecClassName, cl);
            if (homeRecFragClass == null) continue;
            XposedInterface.Hooker feedListHooker = chain -> {
                Object res = chain.proceed();
                for (int i = 0; i < 6; i++) {
                    Object arg = chain.getArg(i);
                    if (arg instanceof List<?>) {
                        SmzdmAdPurifier.filterFeedRows((List<?>) arg);
                    }
                }
                return res;
            };
            for (String m : new String[]{"D6", "J3", "Y5", "f5"}) {
                ReflectUtils.hookAllMethods(xposed, homeRecFragClass, m, feedListHooker);
            }
        }

        // 4c. Hook search-bar keyword helpers. Base class in 11.1.95 is C5.GestureDetectorOnGestureListenerC0224n.
        String[] searchBarFragClassNames = new String[]{
                "com.smzdm.client.android.base.BaseFragment",
                "C5.GestureDetectorOnGestureListenerC0224n"
        };
        for (String searchBarFragClassName : searchBarFragClassNames) {
            Class<?> searchBarFragClass = ReflectUtils.findClass(searchBarFragClassName, cl);
            if (searchBarFragClass == null) continue;
            ReflectUtils.hookAllMethods(xposed, searchBarFragClass, "s9", chain -> {
                Object self = chain.getThisObject();
                Object tvObj = chain.getArg(0);
                Object ivObj = chain.getArg(1);
                ReflectUtils.setField(self, "l", "");
                ReflectUtils.setField(self, "i", tvObj);
                ReflectUtils.setField(self, "j", ivObj);
                if (tvObj instanceof TextView) {
                    ((TextView) tvObj).setText("");
                }
                if (ivObj instanceof View) {
                    collapseView((View) ivObj);
                }
                return null;
            });
            ReflectUtils.hookAllMethods(xposed, searchBarFragClass, "x9", chain -> {
                Object self = chain.getThisObject();
                Object tvObj = chain.getArg(0);
                Object ivObj = chain.getArg(1);
                ReflectUtils.setField(self, "B", "");
                if (tvObj instanceof TextView) {
                    TextView tv = (TextView) tvObj;
                    tv.setText("");
                    tv.setHint("");
                }
                if (ivObj instanceof View) {
                    collapseView((View) ivObj);
                }
                return null;
            });
            ReflectUtils.hookAllMethods(xposed, searchBarFragClass, "P9", chain -> {
                Object self = chain.getThisObject();
                ReflectUtils.setField(self, "l", "");
                ReflectUtils.setField(self, "B", "");
                Object tvObj = ReflectUtils.getField(self, "i");
                if (tvObj instanceof TextView) {
                    ((TextView) tvObj).setText("");
                }
                Object ivObj = ReflectUtils.getField(self, "j");
                if (ivObj instanceof View) {
                    collapseView((View) ivObj);
                }
                return null;
            });
            ReflectUtils.hookAllMethods(xposed, searchBarFragClass, "T9", chain -> {
                ReflectUtils.setField(chain.getThisObject(), "B", "");
                return null;
            });
            ReflectUtils.hookAllMethods(xposed, searchBarFragClass, "U9", chain -> {
                ReflectUtils.setField(chain.getThisObject(), "B", "");
                return null;
            });
            ReflectUtils.hookAllMethods(xposed, searchBarFragClass, "S9", chain -> {
                ReflectUtils.setField(chain.getThisObject(), "v", "");
                return null;
            });
        }

        Class<?> searchBarHelperClass = ReflectUtils.findClass("H2.a", cl);
        if (searchBarHelperClass != null) {
            ReflectUtils.hookMethodNoOp(xposed, searchBarHelperClass, "b", null);
            ReflectUtils.hookMethodNoOp(xposed, searchBarHelperClass, "c", Boolean.FALSE);
        }

        Class<?> searchKeywordMgrClass = ReflectUtils.findClass("ya.f", cl);
        if (searchKeywordMgrClass == null) {
            searchKeywordMgrClass = ReflectUtils.findClass("ye.g", cl);
        }
        if (searchKeywordMgrClass != null) {
            ReflectUtils.hookMethodNoOp(xposed, searchKeywordMgrClass, "k", null);
            ReflectUtils.hookMethodNoOp(xposed, searchKeywordMgrClass, "s", null);
            ReflectUtils.hookMethodNoOp(xposed, searchKeywordMgrClass, "p", null);
        }

        Class<?> searchWordBeanClass = ReflectUtils.findClass("xd.c", cl);
        if (searchWordBeanClass != null) {
            ReflectUtils.hookMethodNoOp(xposed, searchWordBeanClass, "d", "");
            ReflectUtils.hookMethodNoOp(xposed, searchWordBeanClass, "e", "");
            ReflectUtils.hookMethodNoOp(xposed, searchWordBeanClass, "f", "");
            ReflectUtils.hookMethodNoOp(xposed, searchWordBeanClass, "h", "");
        }

        // 5. Hook UserCenterFragment ("我的" page) to collapse flowing ads and service cards
        String[] ucFragClassNames = new String[]{
                "com.smzdm.client.android.user_center.UserCenterFragment",
                "com.smzdm.client.android.user_center.S",
                "com.smzdm.client.android.user_center.P0"
        };
        for (String ucClassName : ucFragClassNames) {
            Class<?> ucFragClass = ReflectUtils.findClass(ucClassName, cl);
            if (ucFragClass == null) continue;
            XposedInterface.Hooker ucHooker = chain -> {
                Object res = chain.proceed();
                purifyUserCenterFragment(chain.getThisObject());
                return res;
            };
            String[] ucMethods = new String[]{"onViewCreated", "onResume", "onCreateView",
                    "y5", "H5", "u5", "x5", "G5", "Oc", "Pc", "Rc", "Sc"};
            for (String m : ucMethods) {
                ReflectUtils.hookAllMethods(xposed, ucFragClass, m, ucHooker);
            }
        }

        // 5b. 11.1.95 newstyle home activity: hide header/checkin entries and publish pill
        Class<?> newStyleHomeClass = ReflectUtils.findClass(
                "com.smzdm.client.android.app.home.newstyle.MainActivity", cl);
        if (newStyleHomeClass != null) {
            XposedInterface.Hooker newStyleHooker = chain -> {
                Object res = chain.proceed();
                Object act = chain.getThisObject();
                if (act instanceof Activity && ((Activity) act).getWindow() != null) {
                    View decor = ((Activity) act).getWindow().getDecorView();
                    if (decor != null) {
                        purgeViewTree(decor);
                    }
                }
                return res;
            };
            for (String m : new String[]{"onCreate", "onResume", "onStart", "initView"}) {
                ReflectUtils.hookAllMethods(xposed, newStyleHomeClass, m, newStyleHooker);
            }
        }

        // 5b2. 11.1.95 interest home: hide top KingKong bar and place "排行榜" inline next to "推荐 | 关注".
        Class<?> interestHomeClass = ReflectUtils.findClass(
                "com.smzdm.client.android.app.home.newstyle.d", cl);
        if (interestHomeClass != null) {
            ReflectUtils.hookAllMethods(xposed, interestHomeClass, "Ob", chain -> {
                Object listObj = chain.getArg(0);
                if (listObj instanceof List<?>) {
                    trimKingKongTabs((List<?>) listObj);
                }
                return chain.proceed();
            });
            XposedInterface.Hooker interestHomeViewHooker = chain -> {
                Object res = chain.proceed();
                purifyInterestHomeFragment(chain.getThisObject());
                return res;
            };
            for (String m : new String[]{"Z9", "onResume", "Ib", "Vb"}) {
                ReflectUtils.hookAllMethods(xposed, interestHomeClass, m, interestHomeViewHooker);
            }
        }
        Class<?> kingKongAdapterClass = ReflectUtils.findClass("C5.C4167O", cl);
        if (kingKongAdapterClass != null) {
            ReflectUtils.hookAllMethods(xposed, kingKongAdapterClass, "T", chain -> {
                Object listObj = chain.getArg(0);
                if (listObj instanceof List<?>) {
                    trimKingKongTabs((List<?>) listObj);
                }
                return chain.proceed();
            });
        }
        Class<?> interestScrollHelperClass = ReflectUtils.findClass("g5.C", cl);
        if (interestScrollHelperClass != null) {
            ReflectUtils.hookAllMethods(xposed, interestScrollHelperClass, "t", chain -> {
                try {
                    Object cObj = chain.getArg(0);
                    Object appBarObj = chain.getArg(1);
                    if (appBarObj instanceof ViewGroup) {
                        disableAppBarScroll((ViewGroup) appBarObj);
                    }
                    Object binding = cObj != null ? ReflectUtils.getField(cObj, "c") : null;
                    Object refreshLayout = binding != null ? ReflectUtils.getField(binding, "refreshLayout") : null;
                    if (refreshLayout instanceof ViewGroup) {
                        ((ViewGroup) refreshLayout).setClipToPadding(true);
                    }
                } catch (Throwable ignored) {
                }
                return null;
            });
        }

        Class<?> homeChannelVmClass = ReflectUtils.findClass("c5.N", cl);
        if (homeChannelVmClass != null) {
            ReflectUtils.hookAllMethods(xposed, homeChannelVmClass, "G", chain -> {
                Object res = chain.proceed();
                if (res instanceof List<?>) {
                    return buildHomeChannelTabsWithRank((List<?>) res, cl);
                }
                return res;
            });
            ReflectUtils.hookAllMethods(xposed, homeChannelVmClass, "B", chain -> {
                Object arg0 = chain.getArg(0);
                if (arg0 instanceof List<?>) {
                    return chain.proceed(new Object[]{java.util.Collections.emptyList(), chain.getArg(1)});
                }
                return chain.proceed();
            });
        }
        Class<?> homeTabCoordinatorClass = ReflectUtils.findClass("c5.k", cl);
        if (homeTabCoordinatorClass != null) {
            ReflectUtils.hookAllMethods(xposed, homeTabCoordinatorClass, "p", chain -> {
                Object arg0 = chain.getArg(0);
                if (arg0 instanceof List<?>) {
                    List<Object> tabs = buildHomeChannelTabsWithRank((List<?>) arg0, cl);
                    return chain.proceed(new Object[]{tabs});
                }
                return chain.proceed();
            });
        }
        Class<?> homePagerAdapterClass = ReflectUtils.findClass(
                "com.smzdm.client.android.app.home.newstyle.a$b", cl);
        if (homePagerAdapterClass != null) {
            ReflectUtils.hookMethod(xposed, homePagerAdapterClass, "createFragment",
                    new Class<?>[]{int.class}, chain -> {
                        try {
                            Object self = chain.getThisObject();
                            int pos = (Integer) chain.getArg(0);
                            Object listObj = ReflectUtils.invokeMethod(self, "w");
                            if (!(listObj instanceof List<?>)) {
                                listObj = ReflectUtils.getField(self, "a");
                            }
                            if (listObj instanceof List<?>) {
                                List<?> list = (List<?>) listObj;
                                if (pos >= 0 && pos < list.size()) {
                                    Object item = list.get(pos);
                                    String tabId = stringValue(ReflectUtils.invokeMethod(item, "a"));
                                    if (tabId.isEmpty()) {
                                        tabId = stringValue(ReflectUtils.getField(item, "a"));
                                    }
                                    if ("rank".equals(tabId)) {
                                        Object rankFrag = createEmbeddedRankFragment(cl);
                                        if (rankFrag != null) {
                                            return rankFrag;
                                        }
                                    }
                                }
                            }
                        } catch (Throwable t) {
                            Log.w(TAG, "createFragment rank hook error: " + t);
                        }
                        return chain.proceed();
                    });
        }

        // 5c. Rename the newstyle first tab label from "兴趣" to "首页".
        Class<?> mainBottomNavBarClass = ReflectUtils.findClass(
                "com.smzdm.client.android.app.home.newstyle.MainBottomNavBar", cl);
        if (mainBottomNavBarClass != null) {
            XposedInterface.Hooker navTextHooker = chain -> {
                Object res = chain.proceed();
                try {
                    Object self = chain.getThisObject();
                    Object a = ReflectUtils.getField(self, "a");
                    Object tvObj = a != null ? ReflectUtils.getField(a, "tvTabInterest") : null;
                    if (tvObj instanceof TextView) {
                        ((TextView) tvObj).setText("首页");
                    }
                } catch (Throwable ignored) {
                }
                return res;
            };
            for (String m : new String[]{"o", "setScrolledToTop", "setSelectedTab"}) {
                ReflectUtils.hookAllMethods(xposed, mainBottomNavBarClass, m, navTextHooker);
            }
        }

        // 5d. Haojia detail native AI introduction card.
        Class<?> haojiaDetailClass = ReflectUtils.findClass(
                "com.smzdm.client.android.module.haojia.detail.HaojiaDetailActivity", cl);
        if (haojiaDetailClass != null) {
            XposedInterface.Hooker detailHooker = chain -> {
                Object res = chain.proceed();
                Object self = chain.getThisObject();
                if (self instanceof Activity) {
                    Activity activity = (Activity) self;
                    if (activity.getWindow() != null) {
                        hideNativeAiIntroduction(activity.getWindow().getDecorView());
                    }
                }
                return res;
            };
            for (String m : new String[]{"onCreate", "onStart", "onResume", "onWindowFocusChanged"}) {
                ReflectUtils.hookAllMethods(xposed, haojiaDetailClass, m, detailHooker);
            }
        }

        Log.i(TAG, "SmzdmLayoutPurifier hooks installed.");
    }

    /**
     * Native Haojia detail pages render the AI product introduction as a normal View card in
     * 11.1.95. Hide the nearest card when its child text identifies the AI block.
     */
    public static void hideNativeAiIntroduction(View root) {
        if (root == null) return;
        try {
            if (root instanceof TextView) {
                CharSequence text = ((TextView) root).getText();
                CharSequence hint = ((TextView) root).getHint();
                if (isAiIntroductionText(text) || isAiIntroductionText(hint)) {
                    View card = findAiIntroductionCard(root);
                    collapseView(card != null ? card : root);
                    return;
                }
            }
            if (root instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) root;
                for (int i = vg.getChildCount() - 1; i >= 0; i--) {
                    hideNativeAiIntroduction(vg.getChildAt(i));
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean isAiIntroductionText(CharSequence value) {
        if (value == null) return false;
        String text = normalizeText(value);
        if (text.isEmpty()) return false;
        return text.contains("AI商品介绍")
                || text.contains("AI生成内容仅供参考")
                || text.contains("红包小助手")
                || text.contains("生活省钱小助手")
                || text.contains("商品百科")
                || text.contains("看商品百科");
    }

    private static View findAiIntroductionCard(View view) {
        View current = view;
        for (int depth = 0; depth < 6 && current != null; depth++) {
            try {
                if (current instanceof ViewGroup && isAiIntroductionCard((ViewGroup) current)) {
                    return current;
                }
            } catch (Throwable ignored) {
            }
            Object parent = current.getParent();
            current = parent instanceof View ? (View) parent : null;
        }
        return view;
    }

    private static boolean isAiIntroductionCard(ViewGroup group) {
        if (group == null || group.getChildCount() < 2 || group.getChildCount() > 12) return false;
        String text = normalizeText(collectText(group, 0));
        if (text.length() > 1500) return false;
        return text.contains("AI生成内容仅供参考")
                || (text.contains("AI商品介绍") && text.contains("问AI"))
                || text.contains("红包小助手")
                || text.contains("生活省钱小助手")
                || (text.contains("商品百科") && text.length() < 600);
    }

    private static String collectText(View view, int depth) {
        if (view == null || depth > 6) return "";
        if (view instanceof TextView) {
            CharSequence text = ((TextView) view).getText();
            return text != null ? text.toString() : "";
        }
        StringBuilder sb = new StringBuilder();
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                sb.append(collectText(vg.getChildAt(i), depth + 1));
            }
        }
        return sb.toString();
    }

    private static String normalizeText(CharSequence value) {
        if (value == null) return "";
        return value.toString()
                .replace(" ", "")
                .replace("\u00A0", "")
                .replace("\n", "")
                .replace("\r", "")
                .trim();
    }

    private static void collapseZeroParams(View view) {
        try {
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            if (lp != null) {
                lp.height = 0;
                if (lp instanceof LinearLayout.LayoutParams) {
                    ((LinearLayout.LayoutParams) lp).weight = 0f;
                    lp.width = 0;
                }
                if (lp instanceof ViewGroup.MarginLayoutParams) {
                    ((ViewGroup.MarginLayoutParams) lp).topMargin = 0;
                    ((ViewGroup.MarginLayoutParams) lp).bottomMargin = 0;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void hideFloatParentIfApplicable(View v) {
        try {
            Resources res = v.getResources();
            if (res == null || v.getId() == View.NO_ID) return;
            String name = res.getResourceEntryName(v.getId());
            if ("iv_float".equals(name) || "iv_float_close".equals(name)) {
                ViewParent parent = v.getParent();
                if (parent instanceof ViewGroup) {
                    ViewGroup vg = (ViewGroup) parent;
                    if (vg.getId() == View.NO_ID && vg.getChildCount() <= 3) {
                        collapseView(vg);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static void purifyHomeActivity(Object homeAct) {
        if (homeAct == null) return;
        try {
            // 11.1.63: L=ib_pub, G=tab_article
            // 11.1.95: S=ib_pub, N=tab_article, L=ViewPager, so never treat L as a button here.
            for (String plusField : new String[]{"S", "L"}) {
                Object plusBtn = ReflectUtils.getField(homeAct, plusField);
                if (plusBtn instanceof View && !(plusBtn instanceof ViewGroup)) {
                    View v = (View) plusBtn;
                    if (isResourceEntryName(v, "ib_pub", "ll_tab_publish", "iv_tab_publish", "tv_tab_publish")) {
                        collapseView(v);
                    }
                }
            }
            for (String articleField : new String[]{"N", "G"}) {
                Object articleTab = ReflectUtils.getField(homeAct, articleField);
                if (articleTab instanceof View) {
                    collapseView((View) articleTab);
                }
            }

            // Clean RadioGroup rg_tab (field D)
            Object rgObj = ReflectUtils.getField(homeAct, "D");
            if (!(rgObj instanceof RadioGroup)) {
                rgObj = ReflectUtils.getField(homeAct, "K");
            }
            if (rgObj instanceof RadioGroup) {
                RadioGroup rg = (RadioGroup) rgObj;
                int count = rg.getChildCount();
                for (int i = 0; i < count; i++) {
                    View child = rg.getChildAt(i);
                    if (child == null) continue;
                    // Child at index 2 is the un-ID'd View spacer for the center '+' button.
                    // The '+' publish slot may also be a normal clickable view with id ib_pub.
                    if (shouldHideView(child)) {
                        collapseView(child);
                    } else if (i == 2 && child.getClass() == View.class && child.getId() == View.NO_ID) {
                        collapseView(child);
                    } else {
                        ViewGroup.LayoutParams lp = child.getLayoutParams();
                        if (lp instanceof LinearLayout.LayoutParams) {
                            LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) lp;
                            if (llp.weight != 1.0f || llp.width != 0) {
                                llp.weight = 1.0f;
                                llp.width = 0;
                                child.setLayoutParams(llp);
                            }
                        }
                    }
                }
            }

            if (homeAct instanceof Activity) {
                View decor = ((Activity) homeAct).getWindow() != null
                        ? ((Activity) homeAct).getWindow().getDecorView() : null;
                if (decor != null) {
                    purgeViewTree(decor);
                    for (String entryName : new String[]{"ib_pub", "ll_tab_publish", "iv_tab_publish",
                            "tv_tab_publish", "tab_article"}) {
                        View v = findViewByEntryName(decor, entryName);
                        if (v != null) {
                            collapseView(v);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "purifyHomeActivity error: " + t);
        }
    }

    public static void purifyHomeFragment(Object homeFrag) {
        if (homeFrag == null) return;
        // Field names differ between 11.1.63 (Q/R/S/K + X) and 11.1.95 (j0/k0/l0/d0/c0 + B0/C0/I0/w0/x0).
        String[] hiddenFields = new String[]{
                "Q", "R", "S", "K", "X", "p0", "i0", "j0",
                "B0", "C0", "I0", "w0", "x0",
                "d0", "c0", "k0", "l0"
        };
        for (String f : hiddenFields) {
            Object v = ReflectUtils.getField(homeFrag, f);
            if (v instanceof View) {
                collapseView((View) v);
            } else if (v instanceof android.view.ViewStub) {
                collapseView((View) v);
            }
        }
        // Clear search keyword field l and search placeholder TextView J
        ReflectUtils.setField(homeFrag, "l", "");
        ReflectUtils.setField(homeFrag, "e1", "");
        for (String textField : new String[]{"J", "c0"}) {
            Object tvSearch = ReflectUtils.getField(homeFrag, textField);
            if (tvSearch instanceof TextView) {
                ((TextView) tvSearch).setText("");
                ((TextView) tvSearch).setHint("");
            }
        }
        Object rootView = ReflectUtils.getField(homeFrag, "B");
        if (!(rootView instanceof View)) {
            rootView = ReflectUtils.getField(homeFrag, "U");
        }
        if (rootView instanceof View) {
            purgeViewTree((View) rootView);
        }
    }

    public static void trimHomeTopTabs(Object homeFrag) {
        if (homeFrag == null) return;
        try {
            Object listObj = ReflectUtils.getField(homeFrag, "Q0");
            if (!(listObj instanceof List<?>)) {
                listObj = ReflectUtils.getField(homeFrag, "x0");
            }
            if (listObj instanceof List<?>) {
                List<?> list = (List<?>) listObj;
                Iterator<?> it = list.iterator();
                while (it.hasNext()) {
                    Object tab = it.next();
                    if (tab == null) {
                        it.remove();
                        continue;
                    }
                    String id = stringValue(ReflectUtils.getField(tab, "id"));
                    if (id.isEmpty()) id = stringValue(ReflectUtils.invokeMethod(tab, "getId"));
                    if (id.isEmpty()) id = stringValue(ReflectUtils.getField(tab, "channel_id"));
                    String name = stringValue(ReflectUtils.invokeMethod(tab, "getChannel_name"));
                    if (name.isEmpty()) name = stringValue(ReflectUtils.getField(tab, "channel_name"));
                    boolean keep = id.startsWith("tuijian") || id.startsWith("guanzhu")
                            || "recommend".equals(id) || "follow".equals(id)
                            || "推荐".equals(name) || "关注".equals(name);
                    if (!keep) {
                        it.remove();
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static void trimKingKongTabs(List<?> tabs) {
        if (tabs == null || tabs.isEmpty()) return;
        try {
            Iterator<?> it = tabs.iterator();
            while (it.hasNext()) {
                Object tab = it.next();
                if (tab == null) {
                    it.remove();
                    continue;
                }
                String id = stringValue(ReflectUtils.invokeMethod(tab, "getId"));
                if (id.isEmpty()) id = stringValue(ReflectUtils.getField(tab, "id"));
                String title = stringValue(ReflectUtils.invokeMethod(tab, "getTitle"));
                if (title.isEmpty()) title = stringValue(ReflectUtils.getField(tab, "title"));
                boolean keep = "home".equals(id) || "首页".equals(title);
                if (!keep) {
                    it.remove();
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static List<Object> buildHomeChannelTabsWithRank(List<?> source, ClassLoader cl) {
        java.util.ArrayList<Object> result = new java.util.ArrayList<>();
        boolean hasRank = false;
        if (source != null) {
            for (Object tab : source) {
                if (tab == null) continue;
                String id = stringValue(ReflectUtils.invokeMethod(tab, "d"));
                if (id.isEmpty()) id = stringValue(ReflectUtils.getField(tab, "a"));
                if (id.isEmpty()) id = stringValue(ReflectUtils.getField(tab, "id"));
                String title = stringValue(ReflectUtils.invokeMethod(tab, "e"));
                if (title.isEmpty()) title = stringValue(ReflectUtils.getField(tab, "b"));
                if (title.isEmpty()) title = stringValue(ReflectUtils.getField(tab, "title"));
                boolean keep = "tuijian".equals(id) || "guanzhu".equals(id) || "rank".equals(id)
                        || "推荐".equals(title) || "关注".equals(title) || "排行榜".equals(title);
                if (!keep) continue;
                if ("rank".equals(id) || "排行榜".equals(title)) {
                    hasRank = true;
                }
                ReflectUtils.setField(tab, "e", result.size());
                result.add(tab);
            }
        }
        if (!hasRank && cl != null) {
            Object rankBean = createHomeChannelTabBean(cl, "rank", "排行榜", result.size());
            if (rankBean != null) {
                result.add(rankBean);
            }
        }
        return result;
    }

    private static Object createHomeChannelTabBean(ClassLoader cl, String id, String title, int index) {
        try {
            Class<?> beanClass = ReflectUtils.findClass("c5.j", cl);
            if (beanClass == null) return null;
            java.lang.reflect.Constructor<?> ctor = beanClass.getDeclaredConstructor(
                    String.class, String.class, String.class, float.class, int.class);
            ctor.setAccessible(true);
            return ctor.newInstance(id, title, "", 0.0f, index);
        } catch (Throwable t) {
            Log.w(TAG, "createHomeChannelTabBean error: " + t);
            return null;
        }
    }

    private static Object createEmbeddedRankFragment(ClassLoader cl) {
        try {
            Class<?> fragClass = ReflectUtils.findClass(
                    "com.smzdm.client.android.module.haojia.rank.RankContainerFragment", cl);
            if (fragClass == null) return null;
            Object frag = fragClass.getDeclaredConstructor().newInstance();
            android.os.Bundle bundle = new android.os.Bundle();
            bundle.putInt("initial_tab", 0);
            bundle.putInt("haojia_channel_id", 0);
            bundle.putInt("community_channel_id", 76);
            bundle.putInt("daren_channel_id", 1);
            bundle.putString("from_source", "home");
            bundle.putBoolean("embed_in_home", true);
            ReflectUtils.invokeMethod(frag, "setArguments", new Class<?>[]{android.os.Bundle.class}, bundle);
            return frag;
        } catch (Throwable t) {
            Log.w(TAG, "createEmbeddedRankFragment error: " + t);
            return null;
        }
    }

    public static void purifyInterestHomeFragment(Object frag) {
        if (frag == null) return;
        try {
            Object rootObj = ReflectUtils.invokeMethod(frag, "getView");
            if (!(rootObj instanceof View)) return;
            View root = (View) rootObj;
            for (String entryName : new String[]{
                    "rv_home_header", "iv_kingkong_icon_bg", "iv_kingkong_skin_bg",
                    "layout_sticky_header", "iv_checkin_entry"
            }) {
                hideViewByEntryName(root, entryName);
            }
            View searchHeader = findViewByEntryName(root, "layout_search_header");
            if (searchHeader != null && searchHeader.getLayoutParams() != null) {
                ReflectUtils.setField(searchHeader.getLayoutParams(), "a", 0);
            }
            View appBar = findViewByEntryName(root, "app_bar_layout");
            if (appBar instanceof ViewGroup) {
                disableAppBarScroll((ViewGroup) appBar);
            }
            purgeViewTree(root);
        } catch (Throwable ignored) {
        }
    }

    private static void disableAppBarScroll(ViewGroup appBar) {
        if (appBar == null) return;
        try {
            boolean changed = false;
            int count = appBar.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = appBar.getChildAt(i);
                if (child == null) continue;
                ViewGroup.LayoutParams lp = child.getLayoutParams();
                if (lp != null) {
                    Object flags = ReflectUtils.getField(lp, "a");
                    if (flags instanceof Integer && ((Integer) flags) != 0) {
                        ReflectUtils.setField(lp, "a", 0);
                        changed = true;
                    }
                }
            }
            if (changed) {
                ReflectUtils.setField(appBar, "b", -1);
                ReflectUtils.setField(appBar, "c", -1);
                ReflectUtils.setField(appBar, "d", -1);
                ReflectUtils.invokeMethod(appBar, "setExpanded",
                        new Class<?>[]{boolean.class, boolean.class}, true, false);
                appBar.requestLayout();
            }
        } catch (Throwable ignored) {
        }
    }

    public static void purifyUserCenterFragment(Object ucFrag) {
        if (ucFrag == null) return;
        // Known fields in UserCenterFragment:
        // d1: v_container_banner_module, c1: banner_user_center
        // D0: v_container_more_service_module, o1: rcv_container_more_service, e1: v_container_toolbar_module
        // E2: uc_service_card_area, D2: uc_rv_service_list
        // x2: uc_ad_container, q2: uc_invite_sw_ctl_area, j1: creator_center_banner
        String[] ucHiddenFields = new String[]{"d1", "c1", "D0", "o1", "e1", "E2", "D2", "x2", "q2", "j1"};
        for (String f : ucHiddenFields) {
            Object v = ReflectUtils.getField(ucFrag, f);
            if (v instanceof View) {
                collapseView((View) v);
            }
        }
        Object rootView = ReflectUtils.invokeMethod(ucFrag, "getView");
        if (rootView instanceof View) {
            purgeViewTree((View) rootView);
        }
    }

    public static void purgeViewTree(View root) {
        if (root == null) return;
        if (shouldHideView(root)) {
            collapseView(root);
            hideFloatParentIfApplicable(root);
        } else if (root instanceof TextView) {
            sanitizeSearchTextView((TextView) root);
        }
        if (root instanceof RadioGroup) {
            RadioGroup rg = (RadioGroup) root;
            if (isResourceEntryName(rg, "rg_tab")) {
                for (int i = 0; i < rg.getChildCount(); i++) {
                    View c = rg.getChildAt(i);
                    if (c != null && i == 2 && c.getClass() == View.class && c.getId() == View.NO_ID) {
                        collapseView(c);
                    }
                }
            }
        }
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            int count = vg.getChildCount();
            for (int i = 0; i < count; i++) {
                purgeViewTree(vg.getChildAt(i));
            }
        }
    }

    private static void sanitizeSearchTextView(TextView tv) {
        if (tv == null) return;
        try {
            if (isSearchInputEditText(tv)) {
                CharSequence hint = tv.getHint();
                if (hint != null && hint.length() > 0) {
                    tv.setHint("");
                }
            } else if (isSearchBarPlaceholderView(tv)) {
                CharSequence text = tv.getText();
                if (text != null && text.length() > 0 && !"搜索".equals(text.toString().trim())) {
                    tv.setText("");
                }
                CharSequence hint = tv.getHint();
                if (hint != null && hint.length() > 0) {
                    tv.setHint("");
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean isSearchInputEditText(TextView tv) {
        if (!(tv instanceof EditText)) return false;
        return isResourceEntryName(tv, "edit_text_search", "et_search", "search_edit");
    }

    private static boolean isSearchBarPlaceholderView(TextView tv) {
        if (tv == null || (tv instanceof EditText)) return false;
        if (!isResourceEntryName(tv, "tv_search")) {
            return false;
        }
        // In SearchActivity, tv_search is the right-side "搜索" action button; in Home/Haojia top bar,
        // tv_search is inside cl_search (0x7f0a038c / 0x7f0a0390) and displays promotional keywords.
        try {
            if (tv.getContext() != null) {
                String clsName = tv.getContext().getClass().getName();
                if (clsName.endsWith("SearchActivity") || clsName.endsWith("SearchResultV2Activity")) {
                    return false;
                }
            }
        } catch (Throwable ignored) {
        }
        return true;
    }

    private static boolean isResourceEntryName(View v, String expected) {
        if (v == null || v.getId() == View.NO_ID) return false;
        try {
            Resources r = v.getResources();
            return r != null && expected.equals(r.getResourceEntryName(v.getId()));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isResourceEntryName(View v, String... expected) {
        if (v == null || v.getId() == View.NO_ID || expected == null) return false;
        try {
            Resources r = v.getResources();
            if (r == null) return false;
            String actual = r.getResourceEntryName(v.getId());
            for (String name : expected) {
                if (name != null && name.equals(actual)) return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static String idEntryName(Resources res, int id) {
        if (res == null || id == View.NO_ID) return null;
        String cached = ID_ENTRY_CACHE.get(id);
        if (cached != null) return cached;
        try {
            String name = res.getResourceEntryName(id);
            if (name != null) {
                ID_ENTRY_CACHE.put(id, name);
            }
            return name;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String stringValue(Object value) {
        return value instanceof String ? ((String) value).trim() : "";
    }

    private static String entryNameForHiddenId(Context context, int id) {
        if (id == View.NO_ID) return null;
        String cached = ID_ENTRY_CACHE.get(id);
        if (cached != null) return cached;
        Resources res = resourcesOf(context);
        if (res == null) return null;
        for (String name : HIDDEN_ENTRY_NAMES) {
            int resolved = resolveEntryId(res, name);
            if (resolved == id) {
                ID_ENTRY_CACHE.put(id, name);
                return name;
            }
        }
        return null;
    }

    private static int resolveEntryId(Resources res, String entryName) {
        if (res == null || entryName == null) return View.NO_ID;
        Integer cached = ENTRY_ID_CACHE.get(entryName);
        if (cached != null) return cached;
        int id = View.NO_ID;
        try {
            id = res.getIdentifier(entryName, "id", TARGET_PKG);
        } catch (Throwable ignored) {
        }
        ENTRY_ID_CACHE.put(entryName, id);
        if (id != View.NO_ID) {
            ID_ENTRY_CACHE.putIfAbsent(id, entryName);
        }
        return id;
    }

    private static View findViewByEntryName(View root, String entryName) {
        if (root == null || entryName == null) return null;
        Resources res = root.getResources();
        int id = resolveEntryId(res, entryName);
        if (id == View.NO_ID) return null;
        try {
            return root.findViewById(id);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Resources resourcesOf(Context context) {
        Context current = context;
        while (current != null) {
            try {
                Resources res = current.getResources();
                if (res != null) return res;
            } catch (Throwable ignored) {
            }
            if (current instanceof ContextWrapper) {
                current = ((ContextWrapper) current).getBaseContext();
            } else {
                break;
            }
        }
        return null;
    }
}
