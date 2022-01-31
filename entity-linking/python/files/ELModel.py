import os
import re
from tqdm import tqdm
import numpy as np
import pandas as pd
import torch
import torch.nn as nn
import transformers
from transformers import BertTokenizer, ReformerConfig, ReformerPreTrainedModel
from torch.utils.data import TensorDataset, DataLoader, RandomSampler, SequentialSampler
from transformers import ReformerPreTrainedModel, ReformerModel, ReformerConfig
import sys
# Load test data
#test_data = pd.read_csv('/home/paramjot/PycharmProjects/NER/testuris.csv')
#test_data = ['Give me the grandchildren of Bruce Lee. ; child', 'Give me the grandchildren of Bruce Lee. ; children', 'Give me the grandchildren of Bruce Lee. ; grandchildren', 'Which other weapons did the designer of the Uzi develop? ; designer', 'Which other weapons did the designer of the Uzi develop? ; knownFor', 'Which other weapons did the designer of the Uzi develop? ; Weapon', 'Which other weapons did the designer of the Uzi develop? ; Device', 'How deep is Lake Placid? ; elevation', 'How deep is Lake Placid? ; averageDepth', 'How deep is Lake Placid? ; areaTotal', 'Show me all museums in London. ; Museum', 'Show me all museums in London. ; location', 'Show me all museums in London. ; politicalLeader', 'Who is the tallest player of the Atlanta Falcons? ; team', 'Who is the tallest player of the Atlanta Falcons? ; height', 'Who is the tallest player of the Atlanta Falcons? ; SportsTeam', 'Who is the tallest player of the Atlanta Falcons? ; AmericanFootballTeam', 'Give me all writers that won the Nobel Prize in literature. ; Writer', 'Give me all writers that won the Nobel Prize in literature. ; Award', 'Give me all writers that won the Nobel Prize in literature. ; TelevisionShow', 'Where do the Red Sox play? ; presbo', 'Where do the Red Sox play? ; ballpark', 'Show a list of soccer clubs that play in the Bundesliga. ; SportsLeague', 'Show a list of soccer clubs that play in the Bundesliga. ; SoccerClub', 'Show a list of soccer clubs that play in the Bundesliga. ; league', 'Show a list of soccer clubs that play in the Bundesliga. ; SoccerLeague', 'Give me the Apollo 14 astronauts. ; mission', 'Give me the Apollo 14 astronauts. ; SpaceMission', 'Give me the Apollo 14 astronauts. ; missionDuration', 'Who wrote the book The pillars of the Earth? ; Book', 'Who wrote the book The pillars of the Earth? ; author', 'Who wrote the book The pillars of the Earth? ; Work', 'Which spaceflights were launched from Baikonur? ; operator', 'Which spaceflights were launched from Baikonur? ; launchPad', 'Give me a list of all trumpet players that were bandleaders. ; occupation', 'Give me a list of all trumpet players that were bandleaders. ; instrument', 'Give me a list of all trumpet players that were bandleaders. ; PersonFunction']
#test_data_list = ['Give me the grandchildren of Bruce Lee. ; child', 'Give me the grandchildren of Bruce Lee. ; children', 'Give me the grandchildren of Bruce Lee. ; grandchildren', 'Which other weapons did the designer of the Uzi develop? ; designer', 'Which other weapons did the designer of the Uzi develop? ; knownFor', 'Which other weapons did the designer of the Uzi develop? ; Weapon', 'Which other weapons did the designer of the Uzi develop? ; Device', 'How deep is Lake Placid? ; elevation', 'How deep is Lake Placid? ; averageDepth', 'How deep is Lake Placid? ; areaTotal', 'Show me all museums in London. ; Museum', 'Show me all museums in London. ; location', 'Show me all museums in London. ; politicalLeader', 'Who is the tallest player of the Atlanta Falcons? ; team', 'Who is the tallest player of the Atlanta Falcons? ; height', 'Who is the tallest player of the Atlanta Falcons? ; SportsTeam', 'Who is the tallest player of the Atlanta Falcons? ; AmericanFootballTeam', 'Give me all writers that won the Nobel Prize in literature. ; Writer', 'Give me all writers that won the Nobel Prize in literature. ; Award', 'Give me all writers that won the Nobel Prize in literature. ; TelevisionShow', 'Where do the Red Sox play? ; presbo', 'Where do the Red Sox play? ; ballpark', 'Show a list of soccer clubs that play in the Bundesliga. ; SportsLeague', 'Show a list of soccer clubs that play in the Bundesliga. ; SoccerClub', 'Show a list of soccer clubs that play in the Bundesliga. ; league', 'Show a list of soccer clubs that play in the Bundesliga. ; SoccerLeague', 'Give me the Apollo 14 astronauts. ; mission', 'Give me the Apollo 14 astronauts. ; SpaceMission', 'Give me the Apollo 14 astronauts. ; missionDuration', 'Who wrote the book The pillars of the Earth? ; Book', 'Who wrote the book The pillars of the Earth? ; author', 'Who wrote the book The pillars of the Earth? ; Work', 'Which spaceflights were launched from Baikonur? ; operator', 'Which spaceflights were launched from Baikonur? ; launchPad', 'Give me a list of all trumpet players that were bandleaders. ; occupation', 'Give me a list of all trumpet players that were bandleaders. ; instrument', 'Give me a list of all trumpet players that were bandleaders. ; PersonFunction']
#print("Enter EL script")
#print("System arg: ", sys.argv[1])
test_data = pd.DataFrame()
teststring = sys.argv[1]
# append columns to an empty DataFrame
#printing the string
#print("Actual String: ",teststring)

#gives us the type of string1
#print("Type of string: ",type(teststring))
teststring1 = teststring.strip('[]')
#print("String coverted to list :",teststring1.split(','))
test_data_list = teststring1.split(',')
#print(type(test_data_list))
#prints the list given by split()
test_data['text'] = test_data_list

test_data = test_data[['text']]
#print(test_data.head(5))


# Load the BERT tokenizer
tokenizer = BertTokenizer.from_pretrained('bert-base-uncased', do_lower_case=True)


# Create a function to tokenize a set of texts
def preprocessing(data):
    # Create empty lists to store outputs
    input_ids = []
    attention_masks = []

    # For every sentence...
    for sent in data:
        encoded_sent = tokenizer.encode_plus(
            text=sent,  # Preprocess sentence
            add_special_tokens=True,  # Add `[CLS]` and `[SEP]`
            max_length=MAX_LEN,  # Max length to truncate/pad
            pad_to_max_length=True,  # Pad sentence to max length
            # return_tensors='pt',           # Return PyTorch tensor
            return_attention_mask=True  # Return attention mask
        )

        # Add the outputs to the lists
        input_ids.append(encoded_sent.get('input_ids'))
        attention_masks.append(encoded_sent.get('attention_mask'))

    # Convert lists to tensors
    input_ids = torch.tensor(input_ids)
    attention_masks = torch.tensor(attention_masks)

    return input_ids, attention_masks

config = ReformerConfig(num_labels = 2, vocab_size=30522,axial_pos_shape=[16,8],
                                     dropout=0.7,
                                     attn_layers=['local', 'lsh', 'local', 'lsh', 'local', 'lsh','local', 'lsh', 'local', 'lsh', 'local', 'lsh'])

class ReformerClassifier(ReformerPreTrainedModel):
    def __init__(self, config):
        super().__init__(config)
        self.num_labels = config.num_labels
        self.config = config

        self.reformer = ReformerModel(config)
    #    classifier_dropout = (
     #       config.classifier_dropout if config.classifier_dropout is not None else config.hidden_dropout_prob
     #   )
     #   self.dropout = nn.Dropout(classifier_dropout)
        # self.classifier = nn.Linear(2*config.hidden_size, config.num_labels)
        self.classifier = nn.Linear(2 * config.hidden_size, config.num_labels)
        self.init_weights()

    def forward(self, input_ids, attention_mask):
        outputs = self.reformer(input_ids=input_ids,
                                attention_mask=attention_mask)

        # Extract the last hidden state of the token `[CLS]` for classification task
        last_hidden_state_cls = outputs[0][:, 0, :]

        # Feed input to classifier to compute logits
        logits = self.classifier(last_hidden_state_cls)

        return logits


from transformers import AdamW, get_linear_schedule_with_warmup


def initialize_model(epochs=50):
    #print(config)
    reformer_classifier = ReformerClassifier(config)
    # reformer_classifier.to(device)
    optimizer = AdamW(reformer_classifier.parameters(),
                      lr=3e-5,
                      eps=1e-12
                      )
    #print(optimizer)
    total_steps = len(train_dataloader) * epochs
    scheduler = get_linear_schedule_with_warmup(optimizer,
                                                num_warmup_steps=0,
                                                num_training_steps=total_steps)
    return reformer_classifier, optimizer, scheduler


reformer_classifier = ReformerClassifier(config)
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
n_gpu = torch.cuda.device_count()
#print(device)
#device = torch.device('cpu')
#nermodel=torch.load("/home/paramjot/PycharmProjects/NER/ModelBT100E38KNERDATASET.pt")
#nermodel = nermodel.load_state_dict(torch.load('/home/paramjot/PycharmProjects/NER/ModelBT100E38KNERDATASET.pt', map_location=torch.device('cpu')))

# In[9]:

#nermodel.load_state_dict(torch.load("/home/paramjot/PycharmProjects/NER/ModelBT100E38KNERDATASET.pt",  map_location=torch.device('cpu')))

reformer_classifier.load_state_dict(torch.load("entity-linking/python/ModelBT100EL.pt",  map_location=torch.device('cpu')))

# Run `preprocessing_for_bert` on the test set

MAX_LEN = 128
# Run `preprocessing_for_bert` on the test set
#print('Tokenizing data...')
test_inputs, test_masks = preprocessing(test_data.text)

# Create the DataLoader for our test set
test_dataset = TensorDataset(test_inputs, test_masks)
test_sampler = SequentialSampler(test_dataset)
test_dataloader = DataLoader(test_dataset, sampler=test_sampler, batch_size=32)
import torch.nn.functional as F


def reformer_predict(model, test_dataloader):
    """Perform a forward pass on the trained BERT model to predict probabilities
    on the test set.
    """
    # Put the model into the evaluation mode. The dropout layers are disabled during
    # the test time.
    model.eval()

    all_logits = []

    # For each batch in our test set...
    for batch in test_dataloader:
        # Load batch to GPU
        b_input_ids, b_attn_mask = tuple(t for t in batch)[:2]

        # Compute logits
        with torch.no_grad():
            logits = model(b_input_ids, b_attn_mask)
        all_logits.append(logits)

    # Concatenate logits from each batch
    all_logits = torch.cat(all_logits, dim=0)

    # Apply softmax to calculate probabilities
    probs = F.softmax(all_logits, dim=1).cpu().numpy()

    return probs
# Compute predicted probabilities on the test set
probs = reformer_predict(reformer_classifier, test_dataloader)

# Get predictions from the probabilities
threshold = 0.5
preds = np.where(probs[:, 1] > threshold, 1, 0)

# Number of tweets predicted non-negative
#print("Number of entity labels predicted as 1: ", preds.sum())

output = test_data[preds==1]

list(output.sample(1).text)
output_list = list(output.sample(1).text)
#print(output_list)
result_list = []
for list in output_list:
    result = list.split(';', 1)[1]
    result_list.append(result)
print(result_list)
