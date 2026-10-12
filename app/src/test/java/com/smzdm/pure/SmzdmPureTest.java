package com.smzdm.pure;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SmzdmPureTest {

    @Test
    public void testBlockSingleImageAndPddPosterComments() {
        // Single image with empty text or emotion-only text
        assertTrue(SmzdmCommentFilter.isSpamComment("", true, 1080, 2400));
        assertTrue(SmzdmCommentFilter.isSpamComment("<img class=\"emotion\" src=\"x.png\"> [尴尬] ", true, 1080, 2400));
        // PDD 1200x1500 / 1200x1770 share poster even with arbitrary text
        assertTrue(SmzdmCommentFilter.isSpamComment("大家都来看看这个活动", true, 1200, 1500));
        assertTrue(SmzdmCommentFilter.isSpamComment("实付价格不错", true, 1200, 1770));
        // PDD 560x646 / 1536x2048 red-packet share card & short stub with image ("五折衷", "搜就行", "5元", "3.2.1")
        assertTrue(SmzdmCommentFilter.isSpamComment("", true, 560, 646));
        assertTrue(SmzdmCommentFilter.isSpamComment("五折", true, 1536, 2048));
        assertTrue(SmzdmCommentFilter.isSpamComment("五折衷", true, 0, 0));
        assertTrue(SmzdmCommentFilter.isSpamComment("搜就行", true, 0, 0));
        assertTrue(SmzdmCommentFilter.isSpamComment("3.2.1", true, 817, 969));
    }

    @Test
    public void testBlockRedPacketTeamUpAndInviteCodeComments() {
        // 8-10 digit invite codes
        assertTrue(SmzdmCommentFilter.isSpamComment("138983593", false, 0, 0));
        assertTrue(SmzdmCommentFilter.isSpamComment("20040725", false, 0, 0));
        assertTrue(SmzdmCommentFilter.isSpamComment("20190528", false, 0, 0));
        assertTrue(SmzdmCommentFilter.isSpamComment("20270201", false, 0, 0));
        assertTrue(SmzdmCommentFilter.isSpamComment("200 959454", false, 0, 0));
        assertTrue(SmzdmCommentFilter.isSpamComment("17点598822", false, 0, 0));
        assertTrue(SmzdmCommentFilter.isSpamComment("17的926934", false, 0, 0));
        assertTrue(SmzdmCommentFilter.isSpamComment("11500哦6348", false, 0, 0));
        assertTrue(SmzdmCommentFilter.isSpamComment("146分钟83846", false, 0, 0));
        assertTrue(SmzdmCommentFilter.isSpamComment("9224号068", false, 0, 0));
        assertTrue(SmzdmCommentFilter.isSpamComment("依依5924743", false, 0, 0));
        assertTrue(SmzdmCommentFilter.isSpamComment("要70158867", false, 0, 0));
        assertTrue(SmzdmCommentFilter.isSpamComment("有把有946866", false, 0, 0));
        assertTrue(SmzdmCommentFilter.isSpamComment("幺四六幺六九三", false, 0, 0));
        assertTrue(SmzdmCommentFilter.isSpamComment("167799695來", false, 0, 0));
        assertTrue(SmzdmCommentFilter.isSpamComment("来一个", false, 0, 0));
        assertTrue(SmzdmCommentFilter.isSpamComment("192", false, 0, 0));
        assertTrue(SmzdmCommentFilter.isSpamComment("复制", false, 0, 0));
        assertTrue(SmzdmCommentFilter.isSpamComment("kk", false, 0, 0));
        assertTrue(SmzdmCommentFilter.isSpamComment("加油", false, 0, 0));
        assertTrue(SmzdmCommentFilter.isSpamComment("168042799 \n 这玩意适合啥？", false, 0, 0));
        assertTrue(SmzdmCommentFilter.isSpamComment("167995829 \n 好价", false, 0, 0));
        assertTrue(SmzdmCommentFilter.isSpamComment("邀请码 351477605 速来", false, 0, 0));
        assertTrue(SmzdmCommentFilter.isSpamComment("搜索 849201928 领红包", false, 0, 0));
        // PDD mini-program & kouling & OCR popup
        assertTrue(SmzdmCommentFilter.isSpamComment("#小程序://拼多多/百亿补贴/abc123", false, 0, 0));
        assertTrue(SmzdmCommentFilter.isSpamComment("复制 ⇥aBc123XyZ987⇤ 打开", false, 0, 0));
        assertTrue(SmzdmCommentFilter.isSpamComment("打不开？点击上方空白处返回", false, 0, 0));
        assertTrue(SmzdmCommentFilter.isSpamComment("您的包裹已到菜鸟驿站，请凭取件码8-2-102取件", false, 0, 0));
        assertTrue(SmzdmCommentFilter.isSpamComment("mE2yDWD", false, 0, 0));
        // 3-person red packet instructions & filler with image
        assertTrue(SmzdmCommentFilter.isSpamComment("第二张的领完之后，等下再领第三张", true, 1080, 2340));
        assertTrue(SmzdmCommentFilter.isSpamComment("第三步", true, 1080, 2340));
        assertTrue(SmzdmCommentFilter.isSpamComment("刚刚", true, 1080, 2340));
        assertTrue(SmzdmCommentFilter.isSpamComment("借楼差一人", true, 1080, 2340));
        assertTrue(SmzdmCommentFilter.isSpamComment("三个人组个团互相领红包", false, 0, 0));
    }

    @Test
    public void testKeepGenuineTextAndReviewImageComments() {
        // Genuine text comments
        assertFalse(SmzdmCommentFilter.isSpamComment("五折卷 25 拿下", false, 0, 0));
        assertFalse(SmzdmCommentFilter.isSpamComment("根本没有能用的券", false, 0, 0));
        assertFalse(SmzdmCommentFilter.isSpamComment("没有需要组团抽券，直接购买太省事了，赞", false, 0, 0));
        assertFalse(SmzdmCommentFilter.isSpamComment("前天5699入手的，充电速度确实快，屏幕通透", false, 0, 0));
        assertFalse(SmzdmCommentFilter.isSpamComment("请问这个需要叠加上面的品类券吗？为什么我是原价", false, 0, 0));
        assertFalse(SmzdmCommentFilter.isSpamComment("20261010下单的，今天刚到货，音质不错", false, 0, 0));
        // Long genuine review with leading invite code should be preserved and have leading code stripped
        String longWithCode = "188696302不值，买了一个，这东西不支持65w pd，所以不能给笔记本用，而且纹波偏大";
        assertFalse(SmzdmCommentFilter.isSpamComment(longWithCode, false, 0, 0));
        assertEquals("不值，买了一个，这东西不支持65w pd，所以不能给笔记本用，而且纹波偏大",
                SmzdmCommentFilter.stripLeadingInviteCodeIfLongReview(longWithCode));
        // Genuine product review with text + non-poster photo
        assertFalse(SmzdmCommentFilter.isSpamComment("已经收到货了，包装完好，生产日期是上个月的，给大家参考实拍图", true, 3000, 4000));
    }

    @Test
    public void testInjectHtmlAddsScriptOnce() {
        String raw = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"></head><body><div id=\"app\"></div></body></html>";
        String injected = SmzdmCommentFilter.injectHtml(raw);
        assertTrue(injected.contains(SmzdmCommentFilter.MARKER));
        assertTrue(injected.contains(".red-packet"));
        assertTrue(injected.contains(".app-wiki-info-card"));
        assertTrue(injected.contains("红包小助手"));
        assertTrue(injected.contains("商品百科"));
        assertEquals(injected, SmzdmCommentFilter.injectHtml(injected));
    }

    @Test
    public void testLayoutHiddenEntryNames() {
        assertTrue(SmzdmLayoutPurifier.shouldHideResourceEntryName("ib_pub"));
        assertTrue(SmzdmLayoutPurifier.shouldHideResourceEntryName("tab_article"));
        assertTrue(SmzdmLayoutPurifier.shouldHideResourceEntryName("iv_interest_square"));
        assertTrue(SmzdmLayoutPurifier.shouldHideResourceEntryName("iv_my_interest"));
        assertTrue(SmzdmLayoutPurifier.shouldHideResourceEntryName("iv_sign"));
        assertTrue(SmzdmLayoutPurifier.shouldHideResourceEntryName("cl_banner"));
        assertTrue(SmzdmLayoutPurifier.shouldHideResourceEntryName("layout_faxian"));
        assertTrue(SmzdmLayoutPurifier.shouldHideResourceEntryName("hot_search_container_new"));
        assertTrue(SmzdmLayoutPurifier.shouldHideResourceEntryName("v_container_banner_module"));
        assertTrue(SmzdmLayoutPurifier.shouldHideResourceEntryName("v_container_more_service_module"));
        assertTrue(SmzdmLayoutPurifier.shouldHideResourceEntryName("uc_service_card_area"));
        assertTrue(SmzdmLayoutPurifier.shouldHideResourceEntryName("rv_home_header"));
        assertTrue(SmzdmLayoutPurifier.shouldHideResourceEntryName("iv_kingkong_icon_bg"));
        assertTrue(SmzdmLayoutPurifier.shouldHideResourceEntryName("layout_sticky_header"));
        assertFalse(SmzdmLayoutPurifier.shouldHideResourceEntryName("tab_home"));
        assertFalse(SmzdmLayoutPurifier.shouldHideResourceEntryName("tab_haojia"));
        assertFalse(SmzdmLayoutPurifier.shouldHideResourceEntryName("tab_usercenter"));
    }

    static class DummyFeedRow {
        String model_type;
        int promotion_type;
        String ad_campaign_id;
        int cell_type;
        Object adWrapper;
        String adx_template;
        String tag;

        DummyFeedRow(String modelType, int promo, String campaign) {
            this.model_type = modelType;
            this.promotion_type = promo;
            this.ad_campaign_id = campaign;
            this.cell_type = 0;
        }

        DummyFeedRow(String modelType, int promo, String campaign, int cellType) {
            this.model_type = modelType;
            this.promotion_type = promo;
            this.ad_campaign_id = campaign;
            this.cell_type = cellType;
        }

        static DummyFeedRow withPangleAd() {
            DummyFeedRow r = new DummyFeedRow("article", 0, "", 10001);
            r.adWrapper = new Object();
            r.tag = "广告";
            return r;
        }

        static DummyFeedRow withAdxTemplate() {
            DummyFeedRow r = new DummyFeedRow("article", 0, "", 21001);
            r.adx_template = "pangle_feed_template";
            return r;
        }
    }

    static class DummyHomeComp {
        String zz_type;

        DummyHomeComp(String zzType) {
            this.zz_type = zzType;
        }
    }

    @Test
    public void testFilterFeedRows() {
        List<DummyFeedRow> rows = new ArrayList<>();
        rows.add(new DummyFeedRow("article", 0, ""));
        rows.add(new DummyFeedRow("ads", 0, ""));
        rows.add(new DummyFeedRow("yunying", 0, ""));
        rows.add(new DummyFeedRow("article", 1, ""));
        rows.add(new DummyFeedRow("article", 0, "99812"));
        rows.add(new DummyFeedRow("article", 0, "", 23003));
        rows.add(new DummyFeedRow("article", 0, "", 39015));
        rows.add(DummyFeedRow.withPangleAd());
        rows.add(DummyFeedRow.withAdxTemplate());
        SmzdmAdPurifier.filterFeedRows(rows);
        assertEquals(1, rows.size());
        assertEquals("article", rows.get(0).model_type);
    }

    static class DummyHomeTab {
        String a;
        String b;
        int e;

        DummyHomeTab(String id, String title, int index) {
            this.a = id;
            this.b = title;
            this.e = index;
        }
    }

    @Test
    public void testBuildHomeChannelTabsWithRank() {
        List<DummyHomeTab> source = new ArrayList<>();
        source.add(new DummyHomeTab("tuijian", "推荐", 0));
        source.add(new DummyHomeTab("guanzhu", "关注", 1));
        source.add(new DummyHomeTab("shaiwu", "晒物", 2));
        source.add(new DummyHomeTab("rank", "排行榜", 3));
        List<Object> filtered = SmzdmLayoutPurifier.buildHomeChannelTabsWithRank(source, null);
        assertEquals(3, filtered.size());
        assertEquals("tuijian", ((DummyHomeTab) filtered.get(0)).a);
        assertEquals("guanzhu", ((DummyHomeTab) filtered.get(1)).a);
        assertEquals("rank", ((DummyHomeTab) filtered.get(2)).a);
        assertEquals(2, ((DummyHomeTab) filtered.get(2)).e);
    }

    @Test
    public void testFilterHomeComponents() {
        List<DummyHomeComp> comps = new ArrayList<>();
        comps.add(new DummyHomeComp("circular_banner"));
        comps.add(new DummyHomeComp("banner"));
        comps.add(new DummyHomeComp("screen_banner"));
        comps.add(new DummyHomeComp("filter"));
        comps.add(new DummyHomeComp("list"));
        SmzdmAdPurifier.filterHomeComponents(comps);
        assertEquals(3, comps.size());
        assertEquals("circular_banner", comps.get(0).zz_type);
        assertEquals("filter", comps.get(1).zz_type);
        assertEquals("list", comps.get(2).zz_type);
    }
}
