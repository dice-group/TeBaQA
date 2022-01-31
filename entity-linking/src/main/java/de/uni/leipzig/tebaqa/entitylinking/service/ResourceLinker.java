package de.uni.leipzig.tebaqa.entitylinking.service;

import de.uni.leipzig.tebaqa.entitylinking.nlp.StopWordsUtil;
import de.uni.leipzig.tebaqa.entitylinking.util.PropertyUtil;
import de.uni.leipzig.tebaqa.tebaqacommons.elasticsearch.SearchService;
import de.uni.leipzig.tebaqa.tebaqacommons.model.*;
import de.uni.leipzig.tebaqa.tebaqacommons.nlp.Lang;
import de.uni.leipzig.tebaqa.tebaqacommons.nlp.SemanticAnalysisHelper;
import de.uni.leipzig.tebaqa.tebaqacommons.util.TextUtilities;
import edu.stanford.nlp.semgraph.SemanticGraph;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

public class ResourceLinker {

	private static final Logger LOGGER = LogManager.getLogger(ResourceLinker.class);

	private String question;
	private final Lang language;

	private Set<String> coOccurrences;
	private Set<String> propertyUris;
	private Set<EntityCandidate> entityCandidates;
	private Set<PropertyCandidate> propertyCandidates;
	private Set<ClassCandidate> classCandidates;
	private Set<EntityCandidate> literalCandidates;

	private final SemanticAnalysisHelper semanticAnalysisHelper;
	private final SearchService searchService;
	private final DisambiguationService disambiguationService;

	public ResourceLinker(String question, Lang language) throws IOException {
		this.question = question;
		this.language = language;
		this.coOccurrences = new HashSet<>();
		this.propertyUris = new HashSet<>();
		this.entityCandidates = new HashSet<>();
		this.propertyCandidates = new HashSet<>();
		this.classCandidates = new HashSet<>();
		this.searchService = new SearchService(PropertyUtil.getElasticSearchConnectionProperties());
		this.disambiguationService = new DisambiguationService(this.searchService);
		RestServiceConfiguration nlpServiceProps = PropertyUtil.getNLPServiceConnectionProperties();
		if(nlpServiceProps == null)
			this.semanticAnalysisHelper = language.getSemanticAnalysisHelper();
		else
			this.semanticAnalysisHelper = new SemanticAnalysisHelper(nlpServiceProps, language);
	}

	public String getQuestion() {
		return question;
	}

	public Lang getLanguage() {
		return language;
	}

	public Set<String> getCoOccurrences() {
		return coOccurrences;
	}

	public void setCoOccurrences(Set<String> coOccurrences) {
		this.coOccurrences = coOccurrences;
	}

	public Set<String> getPropertyUris() {
		return propertyUris;
	}

	public Set<EntityCandidate> getEntityCandidates() {
		return entityCandidates;
	}

	public Set<PropertyCandidate> getPropertyCandidates() {
		return propertyCandidates;
	}

	public Set<ClassCandidate> getClassCandidates() {
		return classCandidates;
	}

	public Set<EntityCandidate> getLiteralCandidates() {
		return literalCandidates;
	}

	public void linkEntities() {

		// Remove unnecessary

		question = this.cleanQuestion(question);
		SemanticGraph semanticGraph = semanticAnalysisHelper.extractDependencyGraph(question);
		String[] wordsFromQuestion = question.replaceAll("[\\-.?¿!,;]", "").split("\\s+");

		// Prepare co-occurences
		List<String> coOccurrenceList = TextUtilities.getNeighborCoOccurrencePermutations(Arrays.asList(wordsFromQuestion));
		coOccurrenceList.sort((s1, s2) -> -(s1.length() - s2.length())); // sort ascending based on length

		List<String> filteredCoOccurrences = new ArrayList<>();
		if (false) { // TODO if(semanticGraph != null) but this removes some important co-occurrences
			for (String coOccurrence : coOccurrenceList) {
				if (!StopWordsUtil.containsOnlyStopwords(coOccurrence, language) && TextUtilities.isDependent(coOccurrence, semanticGraph))
					filteredCoOccurrences.add(coOccurrence);
			}
		} else {
			for (String coOccurrence : coOccurrenceList) {
				if (!StopWordsUtil.containsOnlyStopwords(coOccurrence, language))
					filteredCoOccurrences.add(coOccurrence);
			}
		}
		coOccurrenceList = filteredCoOccurrences;


		coOccurrenceList.sort((s1, s2) -> -(s1.length() - s2.length()));
		coOccurrenceList = coOccurrenceList.stream().filter(s -> s.split("\\s+").length < 7).collect(Collectors.toList());
		this.coOccurrences.addAll(coOccurrenceList);
		//System.out.println("Cooccurance List: "+ coOccurrenceList);
		List<String> occuranceList = new ArrayList<>();
		List<String> relationList = new ArrayList<>();

		HashMap<String, Set<EntityCandidate>> ambiguousEntityCandidates = new HashMap<>();
		List<String> nerList = new ArrayList<String>();
		List<String> reList = new ArrayList<String>();
		// NER named entities from REFORMER model
		String NERoutput = null;
		long startTime = System.nanoTime();
		try {

			// Process p = Runtime.getRuntime().exec("python3 src/main/java/NERModel.py ques");
			Process p = new ProcessBuilder("python3", "entity-linking/python/project.py", question).start();
			System.out.println("process: " + p);
			BufferedReader stdInput = new BufferedReader(new
					InputStreamReader(p.getInputStream()));

			BufferedReader stdError = new BufferedReader(new
					InputStreamReader(p.getErrorStream()));

			// read the output from the command
			System.out.println("Here is the standard output of the command:\n");
			while ((NERoutput = stdInput.readLine()) != null) {
				NERoutput = NERoutput.replaceAll("[\\[\\]'(){}]","");
				nerList.add(NERoutput);
				NERoutput = NERoutput.replaceAll("[\\[\\]\\(\\)]", "");
				occuranceList = new ArrayList<String>(Arrays.asList(NERoutput.split(",")));
				System.out.println("list: :"+ occuranceList);
				//occuranceList.add(NERoutput);
			}
		} catch (IOException e) {
			System.out.println("exception happened - here's what I know: ");
			e.printStackTrace();
			System.exit(-1);
		}

		for (String coOccurrence : occuranceList) {
		// 1. Link entities
			System.out.println("coOccurrence: "+ coOccurrence);
		Set<EntityCandidate> matchedEntities = searchService.searchEntities(coOccurrence);
		if (matchedEntities.size() > 0 && matchedEntities.size() <= 20) {
			entityCandidates.addAll(matchedEntities);
		} else if (matchedEntities.size() > 20) {
			ambiguousEntityCandidates.put(coOccurrence, matchedEntities);
		}
		//search for Countries
		Set<EntityCandidate> countryEntities = searchService.searchEntitiesOfType(coOccurrence, "http://dbpedia.org/ontology/Country");
		if (countryEntities.size() < 100) {
			entityCandidates.addAll(countryEntities);
		}

		// 2. Link classes
		String searchTerm = coOccurrence;
		if (!coOccurrence.contains(" ")) {
			String stripped = coOccurrence.replace("'s", "");
			Map<String, String> lemmas = semanticAnalysisHelper.getLemmas(stripped);
			String lemma = lemmas.get(stripped);
			if (lemma != null) {
				searchTerm = lemma;
			}
		}
		classCandidates.addAll(searchService.searchClasses(searchTerm));

		// 3. Link properties


		// TODO ? 4. literal linking
//            literalCandidates.addAll(index.searchLiteral(coOccurrence, 100));
//            literalCandidates.forEach(lc -> propertyUris.addAll(((EntityCandidate) lc).getConnectedPropertiesObject()));
	}
		//Property matching
		String REoutput = null;
		System.out.println("Occurrence list of entities: "+ occuranceList.get(0));
		for (String entities : occuranceList) {
			try {
				Process p1 = new ProcessBuilder("python3", "entity-linking/python/RECandidates.py", question, entities).start();
				System.out.println("process1: " + p1);
				BufferedReader stdInput = new BufferedReader(new
						InputStreamReader(p1.getInputStream()));

				BufferedReader stdError = new BufferedReader(new
						InputStreamReader(p1.getErrorStream()));

				// read the output from the command
				System.out.println("Here is the standard output of the command:\n");
				while ((REoutput = stdInput.readLine()) != null) {
					REoutput = REoutput.replaceAll("[\\[\\]'(){}]", "");
					reList = new ArrayList<String>(Arrays.asList(REoutput.split(",")));
					System.out.println("Python relations output: " + reList);
					//relationList.add(REoutput);
				}
			} catch (IOException e) {
				System.out.println("exception happened - here's what I know: ");
				e.printStackTrace();
				System.exit(-1);
			}
		}
		for(String relationOccurrence : reList){
			propertyCandidates.addAll(searchService.searchProperties(relationOccurrence));
		}
		long endTime = System.nanoTime();
		long timeElapsed = endTime - startTime;
		System.out.println("Execution time in seconds: " + timeElapsed / 1000000000);

	Set<EntityCandidate> disambiguatedEntities = this.disambiguationService.disambiguateEntities(ambiguousEntityCandidates, this.entityCandidates, this.propertyCandidates, Optional.empty());
		entityCandidates.addAll(disambiguatedEntities);

		this.removeDuplicates();

		LOGGER.info("EntityExtraction finished");
}

	private String cleanQuestion(String question) {
		question = semanticAnalysisHelper.removeQuestionWords(question);
		question = question.replace("(", "");
		question = question.replace(")", "");
		return question;
	}

	private void removeDuplicates() {
		Map<String, List<EntityCandidate>> entitiesByUri = this.entityCandidates.stream().collect(Collectors.groupingBy(EntityCandidate::getUri));
		Set<EntityCandidate> uniqueEntityCandidates = entitiesByUri.keySet().stream().map(
						uri -> {
							double bestScore = entitiesByUri.get(uri).stream().mapToDouble(ResourceCandidate::getDistanceScore).min().getAsDouble();
							EntityCandidate bestForThisUri = entitiesByUri.get(uri).stream()
									.filter(entityCandidate -> entityCandidate.getDistanceScore() == bestScore)
									.max(Comparator.comparingInt(value -> value.getCoOccurrence().length())).get();
							return bestForThisUri;
						})
				.collect(Collectors.toSet());
		this.entityCandidates.clear();
		this.entityCandidates.addAll(uniqueEntityCandidates);
		this.entityCandidates.forEach(entityCandidate -> {
			propertyUris.addAll(entityCandidate.getConnectedPropertiesSubject());
			propertyUris.addAll(entityCandidate.getConnectedPropertiesObject());
		});

		Map<String, List<PropertyCandidate>> propertiesByUri = this.propertyCandidates.stream().collect(Collectors.groupingBy(PropertyCandidate::getUri));
		Set<PropertyCandidate> uniquePropertyCandidates = propertiesByUri.keySet().stream().map(
						uri -> {
							double bestScore = propertiesByUri.get(uri).stream().mapToDouble(ResourceCandidate::getDistanceScore).min().getAsDouble();
							PropertyCandidate bestForThisUri = propertiesByUri.get(uri).stream()
									.filter(candidate -> candidate.getDistanceScore() == bestScore)
									.max(Comparator.comparingInt(value -> value.getCoOccurrence().length())).get();
							return bestForThisUri;
						})
				.collect(Collectors.toSet());
		this.propertyCandidates.clear();
		this.propertyCandidates.addAll(uniquePropertyCandidates);

		Map<String, List<ClassCandidate>> classesByUri = this.classCandidates.stream().collect(Collectors.groupingBy(ClassCandidate::getUri));
		Set<ClassCandidate> uniqueClassCandidates = classesByUri.keySet().stream().map(
						uri -> {
							double bestScore = classesByUri.get(uri).stream().mapToDouble(ResourceCandidate::getDistanceScore).min().getAsDouble();
							ClassCandidate bestForThisUri = classesByUri.get(uri).stream()
									.filter(candidate -> candidate.getDistanceScore() == bestScore)
									.max(Comparator.comparingInt(value -> value.getCoOccurrence().length())).get();
							return bestForThisUri;
						})
				.collect(Collectors.toSet());
		this.classCandidates.clear();
		this.classCandidates.addAll(uniqueClassCandidates);
	}

	public void printInfos() {
		System.out.println("Entities");
		entityCandidates.forEach(ent -> System.out.println(ent.getCoOccurrence() + "->(" + ent.getUri() + ";" + Collections.singletonList(ent.getResourceLabels()).get(0) + ")"));
		System.out.println("Properties");
		propertyCandidates.forEach(ent -> System.out.println(ent.getCoOccurrence() + "->(" + ent.getUri() + ";" + Collections.singletonList(ent.getResourceLabels()).get(0) + ")"));
		System.out.println("Class");
		classCandidates.forEach(ent -> System.out.println(ent.getCoOccurrence() + "->(" + ent.getUri() + ";" + Collections.singletonList(ent.getResourceLabels()).get(0) + ")"));
	}


	public static void main(String[] args) throws IOException {
		ResourceLinker linker = new ResourceLinker("Who is the developer of Skype ?", Lang.EN);
		linker.linkEntities();
		linker.printInfos();
	}

}
