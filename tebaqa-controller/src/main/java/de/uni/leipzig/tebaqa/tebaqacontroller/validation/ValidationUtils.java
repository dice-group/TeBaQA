package de.uni.leipzig.tebaqa.tebaqacontroller.validation;

import de.uni.leipzig.tebaqa.tebaqacontroller.model.dataupload.KnowledgeBase;
import de.uni.leipzig.tebaqa.tebaqacontroller.model.dataupload.KnowledgeBaseForm;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ValidationUtils {
    public static final Pattern KB_NAME_PATTERN = Pattern.compile("[a-zA-Z\\d\\s-_]+");

    public static void validateKnowledgeBaseForm(KnowledgeBaseForm kb, BindingResult bindingResult) {
        boolean uploadedViaLink = kb.getUploadType().equals(KnowledgeBaseForm.UploadType.URL);
        String dataFileLinks = kb.getDataFileLinks();
        String ontologyFileLinks = kb.getOntologyFileLinks();

        if (uploadedViaLink) {
            if (!validateLinks(dataFileLinks))
                bindingResult.addError(new FieldError("knowledgeBaseForm", "dataFileLinks", null, false, null, null, "Please input data file links"));

            if (!validateLinks(ontologyFileLinks))
                bindingResult.addError(new FieldError("knowledgeBaseForm", "ontologyFileLinks", null, false, null, null, "Please input ontology file links"));
        } else {
            MultipartFile[] dataFiles = kb.getDataFiles();
            if (dataFiles == null || dataFiles.length == 0) {
                bindingResult.addError(new FieldError("knowledgeBaseForm", "dataFiles", null, false, null, null, "Please upload data files"));
            } else {
                List<MultipartFile> nonEmptyFiles = Arrays.stream(dataFiles).filter(multipartFile -> !multipartFile.isEmpty()).collect(Collectors.toList());
                if(nonEmptyFiles.size() == 0) {
                    bindingResult.addError(new FieldError("knowledgeBaseForm", "dataFiles", null, false, null, null, "Please upload data files"));
                    kb.setDataFiles(null);
                } else if(nonEmptyFiles.size() != kb.getDataFiles().length) {
                    kb.setDataFiles((MultipartFile[]) nonEmptyFiles.toArray());
                }
            }

            MultipartFile[] ontologyFiles = kb.getOntologyFiles();
            if (ontologyFiles == null || ontologyFiles.length == 0) {
                bindingResult.addError(new FieldError("knowledgeBaseForm", "ontologyFiles", null, false, null, null, "Please upload ontology files"));
            } else {
                List<MultipartFile> nonEmptyFiles = Arrays.stream(ontologyFiles).filter(multipartFile -> !multipartFile.isEmpty()).collect(Collectors.toList());
                if(nonEmptyFiles.size() == 0) {
                    bindingResult.addError(new FieldError("knowledgeBaseForm", "ontologyFiles", null, false, null, null, "Please upload ontology files"));
                    kb.setOntologyFiles(null);
                } else if(nonEmptyFiles.size() != ontologyFiles.length) {
                    kb.setOntologyFiles((MultipartFile[]) nonEmptyFiles.toArray());
                }
            }
        }
    }

    private static boolean validateLinks(String links) {
        return links != null && links.trim().length() > 0;
    }

    private static boolean validateKBName(String name) {
        Matcher matcher = KB_NAME_PATTERN.matcher(name);
        return matcher.matches();
    }


}
