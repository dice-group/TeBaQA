package de.uni.leipzig.tebaqa.entitylinking.spring;

import de.uni.leipzig.tebaqa.tebaqacommons.model.EntityLinkingResponseBean;
import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletResponse;

@RestController
public class EntityLinkingController {

    private static final Logger LOGGER = Logger.getLogger(EntityLinkingController.class.getName());

    @RequestMapping(method = RequestMethod.POST, path = "/entity-linking")
    public EntityLinkingResponseBean entityLinking(@RequestParam String question,
                                                   @RequestParam String lang,
                                                   HttpServletResponse servletResponse) {
        LOGGER.info(String.format("/entity-linking received POST request with: question='%s' & lang=%s", question, lang));

        if (question.isEmpty() || lang.isEmpty()) {
            LOGGER.error("Received request with empty parameter(s)!");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide valid question and language!");
        }


        return new EntityLinkingResponseBean();
    }


}
