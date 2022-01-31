#!/usr/bin/env python
# coding: utf-8

import numpy as np
# In[1]:
import torch
import transformers
from torch.nn import CrossEntropyLoss
from transformers import BertTokenizer
from transformers import ReformerPreTrainedModel, ReformerModel
from transformers.file_utils import add_start_docstrings_to_model_forward, add_code_sample_docstrings
from transformers.modeling_outputs import TokenClassifierOutput
import sys
# In[5]:

_CONFIG_FOR_DOC = "ReformerConfig"
_TOKENIZER_FOR_DOC = "ReformerTokenizer"
_CHECKPOINT_FOR_DOC = "reformer-base-uncased"
REFORMER_INPUTS_DOCSTRING = r"""

    Args:
        input_ids (:obj:`torch.LongTensor` of shape :obj:`(batch_size, sequence_length)`):
            Indices of input sequence tokens in the vocabulary. During training the input_ids sequence_length has to be
            a multiple of the relevant model's chunk lengths (lsh's, local's or both). During evaluation, the indices
            are automatically padded to be a multiple of the chunk length.

            Indices can be obtained using :class:`~transformers.ReformerTokenizer`. See
            :meth:`transformers.PreTrainedTokenizer.encode` and :meth:`transformers.PreTrainedTokenizer.__call__` for
            details.

            `What are input IDs? <../glossary.html#input-ids>`__
        attention_mask (:obj:`torch.FloatTensor` of shape :obj:`(batch_size, sequence_length)`, `optional`):
            Mask to avoid performing attention on padding token indices. Mask values selected in ``[0, 1]``:

            - 1 for tokens that are **not masked**,
            - 0 for tokens that are **masked**.

            `What are attention masks? <../glossary.html#attention-mask>`__
        position_ids (:obj:`torch.LongTensor` of shape :obj:`(batch_size, sequence_length)`, `optional`):
            Indices of positions of each input sequence tokens in the position embeddings. Selected in the range ``[0,
            config.max_position_embeddings - 1]``.

            `What are position IDs? <../glossary.html#position-ids>`__
        head_mask (:obj:`torch.FloatTensor` of shape :obj:`(num_heads,)` or :obj:`(num_layers, num_heads)`, `optional`):
            Mask to nullify selected heads of the self-attention modules. Mask values selected in ``[0, 1]``:

            - 1 indicates the head is **not masked**,
            - 0 indicates the head is **masked**.

        inputs_embeds (:obj:`torch.FloatTensor` of shape :obj:`(batch_size, sequence_length, hidden_size)`, `optional`):
            Optionally, instead of passing :obj:`input_ids` you can choose to directly pass an embedded representation.
            This is useful if you want more control over how to convert :obj:`input_ids` indices into associated
            vectors than the model's internal embedding lookup matrix.
        num_hashes (:obj:`int`, `optional`):
            The number of hashing rounds that should be performed during bucketing. Setting this argument overwrites
            the default defined in :obj:`config.num_hashes`.

            For more information, see :obj:`num_hashes` in :class:`~transformers.ReformerConfig`.
        past_buckets_states (:obj:`List[Tuple(torch.LongTensor, torch.FloatTensor)]`, `optional`):
            List of :obj:`Tuple(torch.LongTensor, torch.FloatTensor` of length :obj:`config.n_layers`, with the first
            element being the previous `buckets` of shape :obj:`(batch_size, num_heads, num_hashes, sequence_length)`)
            and the second being the previous `hidden_states` of shape :obj:`(batch_size, sequence_length,
            hidden_size)`).

            Contains precomputed hidden-states and buckets (only relevant for LSH Self-Attention). Can be used to speed
            up sequential decoding.
        use_cache (:obj:`bool`, `optional`):
            If set to :obj:`True`, :obj:`past_key_values` key value states are returned and can be used to speed up
            decoding (see :obj:`past_key_values`).
        output_attentions (:obj:`bool`, `optional`):
            Whether or not to return the attentions tensors of all attention layers. See ``attentions`` under returned
            tensors for more detail.
        output_hidden_states (:obj:`bool`, `optional`):
            Whether or not to return the hidden states of all layers. See ``hidden_states`` under returned tensors for
            more detail.
        return_dict (:obj:`bool`, `optional`):
            Whether or not to return a :class:`~transformers.file_utils.ModelOutput` instead of a plain tuple.
"""


class ReformerTokenClassification(ReformerPreTrainedModel):
    _keys_to_ignore_on_load_unexpected = [r"pooler"]

    def __init__(self, config):
        super().__init__(config)
        self.num_labels = config.num_labels
        self.reformer = ReformerModel(config)
        self.classifier = torch.nn.Linear(2 * config.hidden_size, config.num_labels)

        self.init_weights()

    @add_start_docstrings_to_model_forward(REFORMER_INPUTS_DOCSTRING.format("batch_size, sequence_length"))
    @add_code_sample_docstrings(
        tokenizer_class=_TOKENIZER_FOR_DOC,
        checkpoint=_CHECKPOINT_FOR_DOC,
        output_type=TokenClassifierOutput,
        config_class=_CONFIG_FOR_DOC,
    )
    def forward(
            self,
            input_ids=None,
            attention_mask=None,
            # token_type_ids=None,
            position_ids=None,
            head_mask=None,
            inputs_embeds=None,
            labels=None,
            output_attentions=None,
            output_hidden_states=None,
            return_dict=None,
    ):
        r"""
        labels (:obj:`torch.LongTensor` of shape :obj:`(batch_size, sequence_length)`, `optional`):
            Labels for computing the token classification loss. Indices should be in ``[0, ..., config.num_labels -
            1]``.
        """
        return_dict = return_dict if return_dict is not None else self.config.use_return_dict

        outputs = self.reformer(
            input_ids,
            attention_mask=attention_mask,
            # token_type_ids=token_type_ids,
            position_ids=position_ids,
            head_mask=head_mask,
            inputs_embeds=inputs_embeds,
            output_attentions=output_attentions,
            output_hidden_states=output_hidden_states,
            return_dict=return_dict,
        )

        sequence_output = outputs[0]
        # print("------------","sequence_output: ", sequence_output,sequence_output.size(), "-------------------")
        logits = self.classifier(sequence_output)
        # print("------------","logits: ", logits,logits.size(),"-------------------")
        loss = None
        if labels is not None:
            loss_fct = CrossEntropyLoss()
            # Only keep active parts of the loss
            if attention_mask is not None:
                active_loss = attention_mask.view(-1) == 1
                # print("------------","active_loss: ", active_loss,active_loss.size(),"-------------------")
                active_logits = logits.view(-1, self.num_labels)
                # print("------------","active_logits: ", active_logits,active_logits.size(), "-------------------")
                active_labels = torch.where(
                    active_loss, labels.view(-1), torch.tensor(loss_fct.ignore_index).type_as(labels)
                )
                # print("------------","active_labels: ", active_labels,active_labels.size(),"-------------------")
                loss = loss_fct(active_logits, active_labels)
                # print("------------","loss: ", loss,loss.size(), "-------------------")
            else:
                loss = loss_fct(logits.view(-1, self.num_labels), labels.view(-1))

        if not return_dict:
            output = (logits,) + outputs[2:]
            print("------------", "output: ", output, "-------------------")
            return ((loss,) + output) if loss is not None else output

        return TokenClassifierOutput(
            loss=loss,
            logits=logits,
            hidden_states=outputs.hidden_states,
            attentions=outputs.attentions,
        )


# In[6]:


config = transformers.ReformerConfig(num_buckets=128, num_labels=18, vocab_size=30522, axial_pos_shape=[16, 8],
                                     dropout=0.7,
                                     attn_layers=['local', 'lsh', 'local', 'lsh', 'local', 'lsh', 'local', 'lsh',
                                                  'local', 'lsh', 'local', 'lsh'])
nermodel = ReformerTokenClassification(config)
config = nermodel.config
nermodel = ReformerTokenClassification(config=config)
#print("System: ", sys.argv[1])
test_sentence = sys.argv[1]
#print ("Test sentence: ",test_sentence)
testtag_vals = ['B-tim', 'I-geo', 'B-per', 'I-nat', 'I-gpe', 'B-nat', 'B-org', 'B-gpe', 'O', 'B-art', 'I-org', 'I-art',
                'B-geo', 'I-eve', 'I-per', 'B-eve', 'I-tim', 'PAD']

device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
n_gpu = torch.cuda.device_count()
#print(device)
nermodel.load_state_dict(torch.load("entity-linking/python/ModelBT100E38KNERDATASET.pt",  map_location=torch.device('cpu')))
tokenizer = BertTokenizer.from_pretrained('bert-base-cased', do_lower_case=False)
nermodel.eval()
tokenized_sentence = tokenizer.encode(test_sentence)
input_ids = torch.tensor([tokenized_sentence])
with torch.no_grad():
    output = nermodel(input_ids)
label_indices = np.argmax(output[0].numpy(), axis=2)
#print(label_indices)
tokens = tokenizer.convert_ids_to_tokens(input_ids.numpy()[0])
# print(tokens)
new_tokens, new_labels = [], []
for token, label_idx in zip(tokens, label_indices[0]):
    if token.startswith("##"):
        new_tokens[-1] = new_tokens[-1] + token[2:]
    else:
        new_labels.append(testtag_vals[label_idx])
        # print(new_labels)
        new_tokens.append(token)
entities = []
for token, label in zip(new_tokens, new_labels):
    if label != "O":
        #print("{}\t{}".format(label, token))
        # entities.append(token.lower())
        entities.append(token)
#print(test_sentence)
print(entities)




