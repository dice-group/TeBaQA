#!/usr/bin/env python
# coding: utf-8

from candidateGeneration import *
from RelationExtraction import *
import sys

sentence = sys.argv[1]

entityLabels = sys.argv[2]
# Relation Extraction
# 1. Candidate Generation
relationCandidates = getRelations(sentence, entityLabels)
#print("candidates for RE", relationCandidates)
relationLinks = testdata(sentence, relationCandidates)
#print("Relation candidates: ", relationLinks)
finalResult = createRelationLinks(relationLinks)
#print("Relation links: ", type(finalResult), finalResult)
