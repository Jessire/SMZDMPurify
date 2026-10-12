# 什么值得买净化 (SMZDMPurify)

什么值得买 (`com.smzdm.client.android`) 的 LSPosed 净化模块, 使用 libxposed API 102, 针对 11.1.95 核对并测试.

## 最新版本

**1.0** (`versionCode=1`)

- 首页底部仅保留「首页 / 好价 / 我的」, 移除发布加号与社区入口.
- 移除首页 KingKong 图标栏与兴趣广场等推广入口, 搜索栏固定不随上滑收起.
- 将排行榜入口合并到「推荐 | 关注 | 排行榜」同一行右侧.
- 过滤开屏、信息流、运营弹窗与浮窗广告.
- 屏蔽百亿补贴红包分享图、邀请码与变形口令评论, 保留真实商品评论并支持稳定排序.
- 隐藏好价详情页 AI 商品介绍、红包小助手与商品百科.
- 精简我的页与搜索页推广内容.

完整更新内容见 [更新日志](CHANGELOG.md).

## 安装与更新

从 [Releases](https://github.com/Jessire/SMZDMPurify/releases) 下载并覆盖安装 APK, 在 LSPosed 中启用模块, 作用域勾选什么值得买. 强制停止并重新打开什么值得买后生效, 无需卸载旧版或清除数据.

## 源码

[GitHub 源码](https://github.com/Jessire/SMZDMPurify)

## 核心功能

## 核心功能

1. **去广告**：
   - 跳过冷/热启动开屏广告（`WelComeActivity`、`SplashScreenAnimDialogFragment`、`InterstitialAdDialog`）。
   - 拦截首页与全局运营弹窗、升级弹窗、拉新弹窗与浮窗挂件（`YunyingDialogFragment`、`UpdateDialogFragment`、`InviteRewardDialog`、`HandleFloatView`、`iv_float`）。
   - 过滤首页、好价、搜索与排行榜信息流中的穿山甲广告与运营卡片（`cell_type=10001`、`Feed10001Bean`、`Holder10001`、`W9.J`、`adWrapper`、`adx_template`、`zdm_template`、`FeedYunYingBean`、`model_type=ads/yunying`、`promotion_type>0`、`ad_campaign_id`）。
   - 首页信息流只保留商品/好价卡（有价格或有商城），过滤资讯/新闻类 Feed（11.1.95 的 `cell_type=22001` 等）。
   - 好价页去掉顶部滚动推广、今日神价/白菜专区、`rv_banner`/`banner_v2` 运营位。
   - 首页移除最上方 KingKong 图标栏与右上角签到/兴趣广场入口，顶部搜索栏固定不随上滑收起，并将「排行榜」入口内嵌到「推荐 | 关注 | 排行榜」同一行右侧。
2. **百亿补贴评论默认屏蔽**：
   - 针对爆料详情页（H5 `DetailWebViewClient` / `ajax_comment_list` 与原生 `CommentNewBean` / `CommentListResponse`），默认拦截拼多多百亿补贴三人团红包分享图（`1200x1500`、`1200x1770`、`1200x2670`、`560x646`、`1536x2048`、`1536x2275`、`1152x2048` 等）、单图无文字/短词配图（如 `"五折衷"`、`"搜就行"`、`"5元"`）、6-12 位邀请码（含 `20xxxxxx` 码、夹杂汉字词缀如 `"11500哦6348"` / `"146分钟83846"` / `"9224号068"` / `"167799695來"`、谐音码如 `"依依5924743"` / `"要70158867"` / `"有把有946866"`）、取件码/随机口令及组团话术（`"下面三个都可以"`、`"就差你了"`）；对前导带邀请码但正文 `>= 20` 字的真实长评自动剥离前导邀请码并保留正文。
   - 主评论接口自动启用 `limit=50`，第 1 页有有效评论时 **0 延迟直出首屏**（点进卡片 0.87s~1.16s 显示评论；若前几页全空则通过 8 路并发一旦扫出首条真实评论即刻返回，耗时 1.66s~2.59s）；后台通过 8 路并发自适应区间窃取（Work-Stealing）全量扫描 `sort_v2 >= 0` 的全部页面并自动刷新补齐列表，解决 50K~98K 商品后半段真实评论漏加载问题；切换「精彩 / 最早 / 最晚」时将置顶评论并入净评池按 `comment_id` 升序/降序秒切，不再出现空白“当前无人评论”。
   - 隐藏好价详情页中的「AI 商品介绍」（`.ai-product-intro-wrapper`）、「红包小助手」（`.red-packet` / `.lifeRed` / `[data-v-7b919764]` / `[data-v-83bb4800]`）与「商品百科」（`.app-wiki-info-card` / `.wiki-info-card` / `[data-v-d79df87b]` / `[data-v-c0982931]`）模块及对应原生卡片。
3. **底栏精简**：
   - 移除底部中间发布加号（`ib_pub`、`bg_round`、`hongbao_icon`、`publish_bubble`、`ll_tab_publish` 等）与「社区」Tab（`tab_article`），仅保留「首页」「好价」「我的」并均分宽度。
   - 11.1.95 新兴趣首页底栏首 tab 由「兴趣」改为「首页」。
4. **「我的」页面精简**：
   - 移除流动横幅广告（`v_container_banner_module`、`banner_user_center`、`uc_ad_container`、`uc_gen2_top_ad`、`creator_center_banner`、`uc_invite_sw_ctl_area`）。
   - 移除钱袋卡、社区新人福利卡、更多服务与底部「服务卡片」（`uc_gen2_wallet_card`、`uc_gen2_creator_shequ_area`、`user_center2_more_service`、`uc_service_card_area`、`uc_rv_service_list`）。
5. **搜索页精简**：
   - 搜索输入页不显示推广词，保留搜索结果页（`SearchResultV2Activity`）顶部用户输入词；历史搜索上方的天天领红包横幅（`fl_hongbao`、`cv_hongbao`）与下方的热榜/发现区（`layout_faxian`、`hot_search_container_*`、`rv_find` 等）默认隐藏。
