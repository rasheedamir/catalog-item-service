package no.nb.microservices.catalogitem.core.search.service;

import no.nb.commons.web.util.UserUtils;
import no.nb.commons.web.xforwarded.feign.XForwardedFeignInterceptor;
import no.nb.microservices.catalogitem.core.index.model.SearchResult;
import no.nb.microservices.catalogitem.core.index.service.IndexService;
import no.nb.microservices.catalogitem.core.item.model.Item;
import no.nb.microservices.catalogitem.core.item.service.ItemWrapperService;
import no.nb.microservices.catalogitem.core.item.service.SecurityInfo;
import no.nb.microservices.catalogitem.core.search.exception.LatchException;
import no.nb.microservices.catalogitem.core.search.model.ItemWrapper;
import no.nb.microservices.catalogitem.core.search.model.SearchAggregated;
import no.nb.microservices.catalogitem.core.search.model.SearchRequest;
import org.apache.htrace.Trace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

@Service
public class SearchServiceImpl implements ISearchService {
    private final IndexService indexService;
    private final ItemWrapperService itemWrapperService;

    @Autowired
    public SearchServiceImpl(ItemWrapperService itemWrapperService, IndexService indexService) {
        this.itemWrapperService = itemWrapperService;
        this.indexService = indexService;
    }

    @Override
    public SearchAggregated search(SearchRequest searchRequest, Pageable pageable) {
        SearchResult result = indexService.search(searchRequest, pageable, new SecurityInfo());
        List<Item> items = consumeItems(searchRequest, result);
        Page<Item> page = new PageImpl<>(items, pageable, result.getTotalElements());
        return new SearchAggregated(page, result.getAggregations());
    }

    private List<Item> consumeItems(SearchRequest searchRequest, SearchResult result) {
        final CountDownLatch latch = new CountDownLatch(result.getIds().size());
        List<Item> items = Collections.synchronizedList(new ArrayList<>());
        List<Future<Item>> workList = new ArrayList<>();

        for (String id : result.getIds()) {
            ItemWrapper itemWrapper = createItemWrapper(latch, items, id, searchRequest);
            Future<Item> item = itemWrapperService.getById(itemWrapper);
            workList.add(item);
        }

        waitForAllItemsToFinish(latch);

        for (Future<Item> item : workList) {
            try {
                items.add(item.get());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return items;
    }

    private void waitForAllItemsToFinish(final CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException ex) {
            throw new LatchException(ex);
        }
    }

    private ItemWrapper createItemWrapper(final CountDownLatch latch, List<Item> items, String id, SearchRequest searchRequest) {
        ItemWrapper itemWrapper = new ItemWrapper(id, latch, items, searchRequest);
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

        itemWrapper.getSecurityInfo().setxHost(request.getHeader(XForwardedFeignInterceptor.X_FORWARDED_HOST));
        itemWrapper.getSecurityInfo().setxPort(request.getHeader(XForwardedFeignInterceptor.X_FORWARDED_PORT));
        itemWrapper.getSecurityInfo().setxRealIp(UserUtils.getClientIp(request));
        itemWrapper.getSecurityInfo().setSsoToken(UserUtils.getSsoToken(request));
        itemWrapper.setSpan(Trace.currentSpan());

        return itemWrapper;
    }

}
