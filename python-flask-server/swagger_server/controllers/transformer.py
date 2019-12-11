from swagger_server.models.transformer_info import TransformerInfo

import json

valid_controls = ['max_genes', 'gene_pvalue', 'pathway_pvalue']
control_names = {'max_genes': 'maximum number of genes', 'gene_pvalue': 'gene p-value', 'pathway_pvalue': 'pathway p-value'}
default_control_values = {'max_genes': 64, 'gene_pvalue': 1e-3, 'pathway_pvalue': 1e-5}
default_control_types = {'max_genes': 'int', 'gene_pvalue': 'double', 'pathway_pvalue': 'double'}


def transformer_info():
    """
        Return information for this expander
    """
    global control_names

    with open("transformer_info.json",'r') as f:
        info = TransformerInfo.from_dict(json.loads(f.read()))
        control_names = dict((name,parameter.name) for name, parameter in zip(valid_controls, info.parameters))
        return info
