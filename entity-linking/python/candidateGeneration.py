from SPARQLWrapper import SPARQLWrapper, JSON
#test_data_list = ['Give me the grandchildren of Bruce Lee. ; child', 'Give me the grandchildren of Bruce Lee. ; children', 'Give me the grandchildren of Bruce Lee. ; grandchildren', 'Which other weapons did the designer of the Uzi develop? ; designer', 'Which other weapons did the designer of the Uzi develop? ; knownFor', 'Which other weapons did the designer of the Uzi develop? ; Weapon', 'Which other weapons did the designer of the Uzi develop? ; Device', 'How deep is Lake Placid? ; elevation', 'How deep is Lake Placid? ; averageDepth', 'How deep is Lake Placid? ; areaTotal', 'Show me all museums in London. ; Museum', 'Show me all museums in London. ; location', 'Show me all museums in London. ; politicalLeader', 'Who is the tallest player of the Atlanta Falcons? ; team', 'Who is the tallest player of the Atlanta Falcons? ; height', 'Who is the tallest player of the Atlanta Falcons? ; SportsTeam', 'Who is the tallest player of the Atlanta Falcons? ; AmericanFootballTeam', 'Give me all writers that won the Nobel Prize in literature. ; Writer', 'Give me all writers that won the Nobel Prize in literature. ; Award', 'Give me all writers that won the Nobel Prize in literature. ; TelevisionShow', 'Where do the Red Sox play? ; presbo', 'Where do the Red Sox play? ; ballpark', 'Show a list of soccer clubs that play in the Bundesliga. ; SportsLeague', 'Show a list of soccer clubs that play in the Bundesliga. ; SoccerClub', 'Show a list of soccer clubs that play in the Bundesliga. ; league', 'Show a list of soccer clubs that play in the Bundesliga. ; SoccerLeague', 'Give me the Apollo 14 astronauts. ; mission', 'Give me the Apollo 14 astronauts. ; SpaceMission', 'Give me the Apollo 14 astronauts. ; missionDuration', 'Who wrote the book The pillars of the Earth? ; Book', 'Who wrote the book The pillars of the Earth? ; author', 'Who wrote the book The pillars of the Earth? ; Work', 'Which spaceflights were launched from Baikonur? ; operator', 'Which spaceflights were launched from Baikonur? ; launchPad', 'Give me a list of all trumpet players that were bandleaders. ; occupation', 'Give me a list of all trumpet players that were bandleaders. ; instrument', 'Give me a list of all trumpet players that were bandleaders. ; PersonFunction']
sparql = SPARQLWrapper("http://dbpedia.org/sparql")
sparqllist = []
candidateList = []

def word2vec(word):
	from collections import Counter
	from math import sqrt
	cw = Counter(word.lower())
	sw = set(cw)
	lw = sqrt(sum(c*c for c in cw.values()))
	return cw, sw, lw

def cosdis(v1, v2):
	common = v1[1].intersection(v2[1])
	return sum(v1[0][word]*v2[0][word] for word in common)/v1[2]/v2[2]

def getEntities(entitylist):
	for en in entitylist:
		nerentity = en
		lowerentity = nerentity.lower()
		sparql.setQuery(
			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
			"SELECT ?subject ?label WHERE {	?subject rdfs:label ?label;	rdf:type ?type."
			"FILTER(lang(?label)='en')  FILTER ( regex (?label,""\"" + lowerentity + "\""", ""\" i \""") )}")

		#print(sparql.queryString)
		sparql.setReturnFormat(JSON)
		results = sparql.query().convert()
		#print(results)

		for result in results["results"]["bindings"]:
			#print(result["label"]["value"])
			sparqloutput = result["label"]["value"]
			sparqllist.append(sparqloutput)
		#print(sparqllist)

		list_A = entitylist
		list_B = sparqllist
		threshold = 0.80  # if needed
		for key in list_A:
			for word in list_B:
				try:
					res = cosdis(word2vec(word), word2vec(key))
					if res > threshold:
						#print("The cosine similarity between : {} and : {} is: {}".format(word, key, res * 100))
						candidateList.append(word)
				except IndexError:
					pass
		#print(candidateList)
		finalCandidateList = list(dict.fromkeys(candidateList))
	return finalCandidateList

def getRelations(question, en):
	en1 = en.lstrip()
	enn = en1.replace(" ", "_")
	propertyList, propertyCandidates = [], []
	uri = "dbr:"
	inputUri = uri.strip() + enn.strip()


	sparql.setQuery(
		"PREFIX dbr: <http://dbpedia.org/resource/> SELECT ?property ?label WHERE { "+inputUri+" ?property ?value. ?property rdfs:label ?label. FILTER(lang(?label)='en') }")

	print(sparql.queryString)
	sparql.setReturnFormat(JSON)
	results = sparql.query().convert()

	for result in results["results"]["bindings"]:
		propertyOutput = result["label"]["value"]
		propertyList.append(propertyOutput)

	list_A = [question]
	list_B = propertyList
	threshold = 0.80
	for key in list_A:
		candidateList.clear()
		for word in list_B:
			try:
				res = cosdis(word2vec(word), word2vec(key))
				if res > threshold:
					print("The cosine similarity between : {} and : {} is: {}".format(word, key, res * 100))
					#print("Found a word with cosine distance > 90 : {} with original word: {}".format(word, key))
					propertyCandidates.append(word)
			except IndexError:
				pass
		finalRelationLinks = list(dict.fromkeys(propertyCandidates))
	return finalRelationLinks

