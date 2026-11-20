package com.smzdm.pure;

import com.smzdm.client.android.bean.component_bean.ComponentListBean;
import com.smzdm.client.android.bean.holder_bean.Feed21001Bean;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SmzdmHomeRecommendationTest {
    private static final class UnknownCard {
        int cell_type = 21001;
        String article_title = "Looks like a deal";
        String article_price = "19.99";
        String article_mall = "mall";
    }

    @Test
    public void filtersFirstAndLaterBatchesWithoutChangingSourceOrOrder() {
        Feed21001Bean first = new Feed21001Bean();
        Feed21001Bean last = new Feed21001Bean();
        Feed21001Bean ad = new Feed21001Bean();
        ad.promotion_type = 1;
        Feed21001Bean mismatched = new Feed21001Bean();
        mismatched.cell_type = 22001;
        List<?> page = Arrays.asList(new UnknownCard(), null, first, ad, mismatched, last);

        SmzdmAdPurifier.HomeRows result = SmzdmAdPurifier.filterHomeRows(page, 4, true);
        assertEquals(List.of(first, last), result.rows);
        assertEquals(1, result.boundary);
        assertEquals(6, page.size());

        Feed21001Bean next = new Feed21001Bean();
        assertEquals(List.of(next), SmzdmAdPurifier.filterHomeRows(List.of(new Object(), next), -1, true).rows);
        SmzdmAdPurifier.HomeRows empty = SmzdmAdPurifier.filterHomeRows(List.of(new UnknownCard()), -1, true);
        assertTrue(empty.rows.isEmpty());
        assertEquals(-1, empty.boundary);
    }

    @Test
    public void filtersNestedReadOnlyListsAndDropsUnknownOrEmptyContainers() {
        Feed21001Bean product = new Feed21001Bean();
        Feed21001Bean child = new Feed21001Bean();
        ComponentListBean inner = new ComponentListBean(List.of(new UnknownCard(), child));
        product.sub_rows = List.of(new UnknownCard(), inner);
        ComponentListBean empty = new ComponentListBean(List.of(new UnknownCard()));
        ComponentListBean unknownType = new ComponentListBean(List.of(child));
        unknownType.zz_type = "banner";
        ComponentListBean component = new ComponentListBean(List.of(product, empty, unknownType));

        List<?> components = Arrays.asList(component);
        SmzdmAdPurifier.filterHomeComponents(components);
        assertEquals(List.of(product), component.zz_content);
        assertEquals(List.of(inner), product.sub_rows);
        assertEquals(List.of(child), inner.zz_content);
    }

    @Test
    public void removesCyclesFromNestedRows() {
        Feed21001Bean product = new Feed21001Bean();
        product.sub_rows = List.of(product);
        assertEquals(List.of(product), SmzdmAdPurifier.filterHomeRows(List.of(product), -1, false).rows);
        assertTrue(product.sub_rows.isEmpty());
    }
}
