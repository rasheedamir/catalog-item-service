package no.nb.microservices.catalogitem.rest.controller;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import no.nb.microservices.catalogitem.core.item.model.Item;
import no.nb.microservices.catalogitem.rest.model.*;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceAssembler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 
 * @author ronnymikalsen
 * @author rolfmathisen
 *
 */
public class ItemResultResourceAssembler implements ResourceAssembler<Item, ItemResource> {

    @Override
    public ItemResource toResource(Item item) {
        ItemResource resource = new ItemResource(item.getId());
        
        populateLinks(item, resource);
        populateMetadata(item, resource);
        populateAccessInfo(item, resource);
        populatePeople(item, resource);
        populateOriginInfo(item, resource);
        populateClassification(item, resource);
        
        return resource;
    }

    private void populateLinks(Item item, ItemResource resource) {
        resource.add(createSelfLink(item));
        resource.add(createModsLink(item));
        resource.add(createPresentationLink(item));
    }

    private void populateOriginInfo(Item item, ItemResource resource) {
        if (item.getOrigin() == null) {
            return;
        }
        OriginInfo originInfo = new OriginInfo();
        originInfo.setPublisher(item.getOrigin().getPublisher());
        originInfo.setCaptured(item.getOrigin().getDateCaptured());
        originInfo.setCreated(item.getOrigin().getDateCreated());
        originInfo.setEdition(item.getOrigin().getEdition());
        originInfo.setFrequency(item.getOrigin().getFrequency());
        originInfo.setIssued(item.getOrigin().getDateIssued());
        originInfo.setModified(item.getOrigin().getDateModified());

        resource.getMetadata().setOriginInfo(originInfo);
    }

    private void populateMetadata(Item item, ItemResource resource) {
        Metadata metadata = new Metadata();
        TitleInfo titleInfo = new TitleInfo();
        titleInfo.setTitle(item.getTitleInfo().getTitle());
        metadata.setTitleInfo(titleInfo);
        resource.setMetadata(metadata);
    }

    private void populateAccessInfo(Item item, ItemResource resource) {
        AccessInfo accessInfo = new AccessInfo();
        accessInfo.setDigital(item.getAccessInfo().isDigital());
        accessInfo.setPublicDomain(item.getAccessInfo().isPublicDomain());
        accessInfo.setAccessAllowedFrom(item.getAccessInfo().accessAllowedFrom());
        accessInfo.setViewability(item.getAccessInfo().getViewability());
        resource.setAccessInfo(accessInfo);
    }

    private void populatePeople(Item item, ItemResource resource) {
        if (item.getPersons() == null || item.getPersons().isEmpty()) {
            return;
        }
        List<Person> people = new ArrayList<>();
        for (int i = 0; i < item.getPersons().size(); i++) {
            Person person = new Person();
            person.setName(item.getPersons().get(i).getName());
            person.setDate(item.getPersons().get(i).getBirthAndDeathYear());
            if (item.getPersons().get(i).getRoles() == null) {
                people.add(person);
                continue;
            }
            List<Role> roles = new ArrayList<>();
            for (String roleName : item.getPersons().get(i).getRoles()) {
                Role role = new Role();
                role.setName(roleName);
                roles.add(role);
            }
            person.setRoles(roles);
            people.add(person);
        }
        resource.getMetadata().setPeople(people);
    }

    private void populateClassification(Item item, ItemResource resource) {
       Classification classification = new Classification();
       
       populateDdc(item, classification);
       populateUdc(item, classification);
       
       resource.getMetadata().setClassification(classification);
        
    }

    private void populateDdc(Item item, Classification classification) {
        Iterator<String> iter = item.getClassification().getDdc().iterator();
           while (iter.hasNext()) {
               classification.addDdc(iter.next());
           }
    }

    private void populateUdc(Item item, Classification classification) {
        Iterator<String> iter = item.getClassification().getUdc().iterator();
           while (iter.hasNext()) {
               classification.addUdc(iter.next());
           }
    }
    
    private Link createSelfLink(Item item) {
        return linkTo(ItemController.class).slash(item).withSelfRel();
    }

    private Link createModsLink(Item item) {
        return ResourceLinkBuilder.linkTo(ResourceTemplateLink.MODS, item.getId()).withRel("mods");
    }

    private Link createPresentationLink(Item item) {
        return ResourceLinkBuilder.linkTo(ResourceTemplateLink.PRESENTATION, item.getId()).withRel("presentation");
    }

}
