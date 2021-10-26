package de.uni.leipzig.tebaqa.tebaqacontroller.controller;

import de.uni.leipzig.tebaqa.tebaqacontroller.model.dataupload.KnowledgeBase;
import de.uni.leipzig.tebaqa.tebaqacontroller.model.dataupload.KnowledgeBaseForm;
import de.uni.leipzig.tebaqa.tebaqacontroller.service.FilesStorageService;
import de.uni.leipzig.tebaqa.tebaqacontroller.service.KnowledgeBaseService;
import de.uni.leipzig.tebaqa.tebaqacontroller.validation.KBNotFoundException;
import de.uni.leipzig.tebaqa.tebaqacontroller.validation.TeBaQAException;
import de.uni.leipzig.tebaqa.tebaqacontroller.validation.ValidationUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.ByteArrayMultipartFileEditor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class KnowledgeBaseController {

    private static final Logger LOGGER = Logger.getLogger(KnowledgeBaseController.class.getName());

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private FilesStorageService filesStorageService;

    @InitBinder
    protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) throws ServletException {
        binder.registerCustomEditor(byte[].class, new ByteArrayMultipartFileEditor());
    }

//    @RequestMapping(method = RequestMethod.POST, path = "/knowledge-base")
//    public ResponseEntity<KnowledgeBase> create(@RequestParam String name,
//                                                @RequestParam boolean uploadedViaLink,
//                                                @RequestParam String dataFileLinks,
//                                                @RequestParam String ontologyFileLinks,
//                                                HttpServletResponse response) throws TeBaQAException {
//        try {
//            KnowledgeBase kb = new KnowledgeBase(name, uploadedViaLink, dataFileLinks, ontologyFileLinks);
//            ValidationUtils.validateKnowledgeBaseForm(kb);
//            KnowledgeBase knowledgeBase = knowledgeBaseService.saveAndIndex(kb);
//
//            URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
//                    .path("/{id}")
//                    .buildAndExpand(knowledgeBase.getId())
//                    .toUri();
//
//            LOGGER.info("POST /knowledge-base " + HttpStatus.CREATED.value() + " - Created");
//            return ResponseEntity.created(uri)
//                    .body(knowledgeBase);
//
//        } catch (DataIntegrityViolationException e) {
//            String errorMessage = "Knowledge Base name already in use: " + name;
//            LOGGER.error("POST /knowledge-base " + HttpStatus.BAD_REQUEST.value() + " - " + errorMessage);
//            throw new TeBaQAException(errorMessage, e);
//        } catch (TeBaQAException e) {
//            LOGGER.error("POST /knowledge-base " + HttpStatus.BAD_REQUEST.value() + " - " + e.getMessage());
//            throw e;
//        }
//    }

    @RequestMapping(method = RequestMethod.POST, path = "/knowledge-base")
    public ModelAndView create(@ModelAttribute @Valid KnowledgeBaseForm knowledgeBaseForm, BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) throws TeBaQAException {
        try {
            ValidationUtils.validateKnowledgeBaseForm(knowledgeBaseForm, bindingResult);
            if(bindingResult.hasErrors()) {
                redirectAttributes.addFlashAttribute("knowledgeBaseForm", knowledgeBaseForm);
                redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.knowledgeBaseForm", bindingResult);
                return new ModelAndView("redirect:/web/knowledge-base/create");
            }

            KnowledgeBase kb = knowledgeBaseForm.getKnowledgeBaseObject();
            if(knowledgeBaseForm.isFileUpload()) {
                knowledgeBaseService.save(kb); // Necessary to generate ID
                List<Path> savedOntologyFilePaths = new ArrayList<>(knowledgeBaseForm.getOntologyFiles().length);
                for(MultipartFile ontologyFile : knowledgeBaseForm.getOntologyFiles()) {
                    Path path = filesStorageService.saveOntologyFile(kb.getId(), ontologyFile);
                    savedOntologyFilePaths.add(path);
                }
                List<Path> savedDataFilePaths = new ArrayList<>(knowledgeBaseForm.getOntologyFiles().length);
                for(MultipartFile dataFile : knowledgeBaseForm.getDataFiles()) {
                    Path path = filesStorageService.saveDataFile(kb.getId(), dataFile);
                    savedDataFilePaths.add(path);
                }

                String savedOntologyPathsConcat = savedOntologyFilePaths.stream().map(path -> path.toAbsolutePath().toString()).collect(Collectors.joining("\n"));
                String savedDataPathsConcat = savedDataFilePaths.stream().map(path -> path.toAbsolutePath().toString()).collect(Collectors.joining("\n"));

                kb.setOntologyFileLinksRaw(savedOntologyPathsConcat);
                kb.setDataFileLinksRaw(savedDataPathsConcat);
            }

            kb = knowledgeBaseService.saveAndIndex(kb);

            LOGGER.info("POST /knowledge-base " + HttpStatus.CREATED.value() + " - Created");
            return new ModelAndView("redirect:/web/knowledge-base/" + kb.getId());

        } catch (DataIntegrityViolationException e) {
            String errorMessage = "Knowledge base name already in use: " + knowledgeBaseForm.getName();
            LOGGER.error("POST /knowledge-base " + HttpStatus.BAD_REQUEST.value() + " - " + errorMessage);

            redirectAttributes.addFlashAttribute("knowledgeBaseForm", knowledgeBaseForm);
            bindingResult.addError(new ObjectError("name", errorMessage));
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.knowledgeBaseForm", bindingResult);
            return new ModelAndView("redirect:/web/knowledge-base/create");

        } catch (Exception e) {
            LOGGER.error("POST /knowledge-base " + HttpStatus.INTERNAL_SERVER_ERROR.value() + " - " + e.getMessage());
            throw new TeBaQAException("Unexpected error", e);
        }
    }



    @RequestMapping(method = RequestMethod.GET, path = "/web/knowledge-base/create")
    public ModelAndView createForm(Model model) {
        LOGGER.info("GET /web/knowledge-base/create");

        ModelAndView modelAndView = new ModelAndView("knowledge-base-create");
        if (model.containsAttribute("knowledgeBaseForm")) {
            modelAndView.addObject("knowledgeBaseForm", model.getAttribute("knowledgeBaseForm"));
        } else {
            modelAndView.addObject("knowledgeBaseForm", new KnowledgeBaseForm());
        }

        return modelAndView;
    }

    @RequestMapping(method = RequestMethod.GET, path = "/knowledge-base/{id}")
    public KnowledgeBase get(@PathVariable int id,
                             HttpServletResponse response) {
        LOGGER.info("GET /knowledge-base/" + id);
        return knowledgeBaseService.getById(id);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/web/knowledge-base/{id}")
    public ModelAndView getWebPage(@PathVariable int id,
                                   HttpServletResponse response) throws KBNotFoundException {
        LOGGER.info("GET /web/knowledge-base/" + id);

        KnowledgeBase knowledgeBase = knowledgeBaseService.getById(id);

        if(knowledgeBase == null)
            throw new KBNotFoundException("Knowledge base not found", null);

        ModelAndView modelAndView = new ModelAndView("knowledge-base");
        modelAndView.addObject("kb", knowledgeBase);
        return modelAndView;
    }

    @RequestMapping(method = RequestMethod.GET, path = "/knowledge-base")
    public List<KnowledgeBase> getAll(HttpServletResponse response) {
        LOGGER.info("GET /knowledge-base");
        return knowledgeBaseService.getAllKnowledgeBases();
    }

    @RequestMapping(method = RequestMethod.GET, path = "/web/knowledge-base")
    public ModelAndView getAllWebPage() {
        LOGGER.info("GET /web/knowledge-base");

        ModelAndView modelAndView = new ModelAndView("knowledge-base-list");
        modelAndView.addObject("kbs", knowledgeBaseService.getAllKnowledgeBases());
        return modelAndView;
    }



}
