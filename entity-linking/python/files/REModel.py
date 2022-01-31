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

#print("Enter the RE script")
#print("System arg: ", sys.argv[1])
test_data = pd.DataFrame()
restring = sys.argv[1]
# append columns to an empty DataFrame
#printing the string
#print("Actual RE String: ",restring)

#gives us the type of string1
#print("Type of RE string: ",type(restring))
restring1 = restring.strip('[]')
#print("String coverted RE string to list :",restring1.split(','))
test_data_list = restring1.split(',')
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
                        dropout=0.5,
                        attn_layers=['local', 'lsh', 'local', 'lsh','local', 'lsh', 'local', 'lsh'])

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

reformer_classifier.load_state_dict(torch.load("entity-linking/python/ModelBT100RE.pt",  map_location=torch.device('cpu')))

# Run `preprocessing_for_bert` on the test set
#print('Tokenizing data...')
MAX_LEN = 128
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
threshold = 0.6
preds = np.where(probs[:, 1] > threshold, 1, 0)

# Number of tweets predicted non-negative
#print("Number of entity labels predicted as 1: ", preds.sum())

output = test_data[preds==1]

list(output.sample(2).text)
output_list = list(output.sample(2).text)
#print(output_list)
result_list = []
for list in output_list:
    result = list.split(';', 1)[1]
    result_list.append(result)
print(result_list)