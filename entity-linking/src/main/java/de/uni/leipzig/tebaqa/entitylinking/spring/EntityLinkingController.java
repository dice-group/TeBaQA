package de.uni.leipzig.tebaqa.entitylinking.spring;

import de.uni.leipzig.tebaqa.entitylinking.service.EntityLinkingService;
import de.uni.leipzig.tebaqa.tebaqacommons.model.EntityLinkingResponseBean;
import de.uni.leipzig.tebaqa.tebaqacommons.nlp.Lang;
import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
public class EntityLinkingController {

    private static final Logger LOGGER = Logger.getLogger(EntityLinkingController.class.getName());
    private static final EntityLinkingService linkingService = new EntityLinkingService();

    @RequestMapping(method = RequestMethod.POST, path = "/entity-linking")
    public EntityLinkingResponseBean entityLinking(@RequestParam String question,
                                                   @RequestParam String lang,
                                                   HttpServletResponse servletResponse) {
        LOGGER.info(String.format("/entity-linking received POST request with: question='%s' & lang=%s", question, lang));

        Lang language = Lang.getForCode(lang);
        if (question.isEmpty() || language == null) {
            LOGGER.error("Received request with invalid parameter(s)!");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide valid question and language!");
        }

        EntityLinkingResponseBean linkedEntities;
        try {
            linkedEntities = linkingService.findEntitiesFrom(question, language);
            printInfos(linkedEntities);
            return linkedEntities;
        } catch (IOException e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to perform entity linking!");
        }

    }

    private static void printInfos(EntityLinkingResponseBean linkedResources) {
        LOGGER.info("Entity linking finished");
        LOGGER.info("Classes found: " + linkedResources.getClassCandidates().size());
        linkedResources.getClassCandidates().forEach(s -> LOGGER.info(s.getCoOccurrence() + " --> " + s.getUri()));

        LOGGER.info("Properties found: " + linkedResources.getPropertyCandidates().size());
        linkedResources.getPropertyCandidates().forEach(s -> LOGGER.info(s.getCoOccurrence() + " --> " + s.getUri()));

        LOGGER.info("Entities found: " + linkedResources.getEntityCandidates().size());
        linkedResources.getEntityCandidates().forEach(s -> LOGGER.info(s.getCoOccurrence() + " --> " + s.getUri()));
    }


}
