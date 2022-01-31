
import numpy as np
import pandas as pd
import torch
import torch.nn as nn
from transformers import BertTokenizer, ReformerConfig, ReformerPreTrainedModel
from torch.utils.data import TensorDataset, DataLoader, RandomSampler, SequentialSampler
from transformers import ReformerPreTrainedModel, ReformerModel, ReformerConfig
import torch.nn.functional as F
from transformers import AdamW, get_linear_schedule_with_warmup


clist ,result_list = [], []
MAX_LEN = 128
def testdata(question, candidatelist):
    for n in candidatelist:
        s = question+' ; '+n
        clist.append(s)
    return clist

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


class ReformerClassifier(ReformerPreTrainedModel):
    def __init__(self, config):
        super().__init__(config)
        self.num_labels = config.num_labels
        self.config = config

        self.reformer = ReformerModel(config)
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

def initialize_model(epochs=50):
    #print(config)
    reformer_classifier = ReformerClassifier(config)
    optimizer = AdamW(reformer_classifier.parameters(),
                      lr=3e-5,
                      eps=1e-12
                      )
    total_steps = len(train_dataloader) * epochs
    scheduler = get_linear_schedule_with_warmup(optimizer,
                                                num_warmup_steps=0,
                                                num_training_steps=total_steps)
    return reformer_classifier, optimizer, scheduler



device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
n_gpu = torch.cuda.device_count()
def reformer_predict(model, test_dataloader):
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
    all_logits = torch.cat(all_logits, dim=0)
    probs = F.softmax(all_logits, dim=1).cpu().numpy()

    return probs

def createRelationLinks(clist):
    test_data = pd.DataFrame()
    test_data['text'] = clist
    test_data = test_data[['text']]

    config = ReformerConfig(num_labels=2, vocab_size=30522, axial_pos_shape=[16, 8],
                            dropout=0.5,
                            attn_layers=['local', 'lsh', 'local', 'lsh', 'local', 'lsh'])
    reformer_classifier = ReformerClassifier(config)
    reformer_classifier.load_state_dict(
        torch.load("entity-linking/python/ModelBT100RE.pt", map_location=torch.device('cpu')))

    test_inputs, test_masks = preprocessing(test_data.text)

    # Create the DataLoader for our test set
    test_dataset = TensorDataset(test_inputs, test_masks)
    test_sampler = SequentialSampler(test_dataset)
    test_dataloader = DataLoader(test_dataset, sampler=test_sampler, batch_size=32)
    # Compute predicted probabilities on the test set
    probs = reformer_predict(reformer_classifier, test_dataloader)



    # Get predictions from the probabilities
    threshold = 0.50
    preds = np.where(probs[:, 1] > threshold, 1, 0)

    # Number of tweets predicted non-negative
    #print("Relations Number of entity labels predicted as 1: ", preds.sum())

    output = test_data[preds == 1]
    count = preds.sum()
    # print(count)
    list(output.sample(count).text)
    output_list = list(output.sample(count).text)
    # print(output_list)

    for listitem in output_list:
        result = listitem.split(';', 1)[1]
        result_list.append(result)
    print("Relations: ",result_list)

    return result_list
