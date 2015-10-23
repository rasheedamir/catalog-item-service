package no.nb.microservices.catalogitem.core.item.model;

import java.util.Collections;
import java.util.List;

public class RelatedItems {

    private List<Item> constituents;
    private List<Item> hosts;
    public RelatedItems(List<Item> constituents, List<Item> hosts) {
        super();
        this.constituents = constituents;
        this.hosts = hosts;
    }
    
    public List<Item> getConstituents() {
        if (constituents == null) {
            return Collections.emptyList();
        } else {
            return Collections.unmodifiableList(constituents);
        }
    }

    public List<Item> getHosts() {
        if (hosts == null) {
            return Collections.emptyList();
        } else {
            return Collections.unmodifiableList(hosts);
        }
    }
    
}
