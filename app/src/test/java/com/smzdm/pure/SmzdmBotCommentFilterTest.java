package com.smzdm.pure;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SmzdmBotCommentFilterTest {
    private static final String REVIEW = "用了几天，实际续航和做工都比预期好。";
    private static final String MENTION = "我问过张大妈机器人，但还是想看看值友的实测。";

    private static final class Comment {
        final Object comment_type;
        final String display_name;
        final String content;
        String article_content;
        String receive_name;
        List<Comment> child_list;
        int child_total;

        Comment(Object type, String name, String text) {
            comment_type = type;
            display_name = name;
            content = text;
        }
    }

    private static final class Data {
        final List<Comment> rows = new ArrayList<>();
        final List<Comment> top = new ArrayList<>();
    }

    @Test
    public void removesBotRootsAndPinnedCommentsUsingCommentMetadata() {
        Data data = new Data();
        Comment human = new Comment("0", "值友", REVIEW);
        data.rows.add(new Comment("16", "其他展示名称", REVIEW));
        data.rows.add(new Comment("0", "　张大妈 ", REVIEW));
        data.rows.add(human);
        data.top.add(new Comment(null, " 张大妈机器人 ", REVIEW));
        data.top.add(new Comment(null, "张大妈", REVIEW));
        data.top.add(human);

        SmzdmCommentFilter.filterNativeCommentData(data);

        assertEquals(Arrays.asList(human), data.rows);
        assertEquals(Arrays.asList(human), data.top);
    }

    @Test
    public void filtersOnlyBotRepliesAndKeepsTheirHumanParent() {
        Comment parent = new Comment("0", "楼主", REVIEW);
        Comment humanReply = new Comment("0", "值友", MENTION);
        humanReply.receive_name = "张大妈";
        parent.child_list = new ArrayList<>(Arrays.asList(
                new Comment(16, null, REVIEW), humanReply,
                new Comment(null, "张大妈", REVIEW),
                new Comment(null, "张大妈机器人", REVIEW)));
        parent.child_total = 6;
        List<Comment> rows = new ArrayList<>(Arrays.asList(parent));

        SmzdmCommentFilter.filterCommentItemList(rows);
        SmzdmCommentFilter.filterCommentItemList(rows);

        assertEquals(Arrays.asList(parent), rows);
        assertEquals(Arrays.asList(humanReply), parent.child_list);
        assertEquals(3, parent.child_total);
        assertEquals(MENTION, humanReply.content);
    }

    @Test
    public void leavesMentionsNearNamesAndOtherCommentTypesToExistingRules() {
        assertFalse(SmzdmCommentFilter.isSpamComment(MENTION, false, 0, 0));
        assertFalse(SmzdmCommentFilter.isSpamCommentItemBean(new Comment(null, "张大妈粉丝", MENTION)));
        assertFalse(SmzdmCommentFilter.isSpamCommentItemBean(new Comment(null, "我是张大妈", REVIEW)));
        assertFalse(SmzdmCommentFilter.isSpamCommentItemBean(new Comment(null, "张 大妈", REVIEW)));
        assertFalse(SmzdmCommentFilter.isSpamCommentItemBean(new Comment(null, "值友", "张大妈")));
        assertFalse(SmzdmCommentFilter.isSpamCommentItemBean(new Comment("160", "张大妈机器人粉丝", REVIEW)));
        assertFalse(SmzdmCommentFilter.isSpamCommentItemBean(new Comment(16.1, null, REVIEW)));
        Comment fallbackText = new Comment("0", "值友", "");
        fallbackText.article_content = "张大妈机器人：" + REVIEW;
        assertFalse(SmzdmCommentFilter.isSpamCommentItemBean(fallbackText));
        assertTrue(SmzdmCommentFilter.isSpamCommentItemBean(new Comment("0", "值友", "138983593")));
    }
}
