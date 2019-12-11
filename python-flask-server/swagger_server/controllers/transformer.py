from swagger_server.models.gene_info import GeneInfo
from swagger_server.models.gene_info import GeneInfoIdentifiers
from swagger_server.models.transformer_info import TransformerInfo

import json

valid_controls = ['max_genes', 'gene_pvalue', 'pathway_pvalue']
control_names = {'max_genes': 'maximum number of genes', 'gene_pvalue': 'gene p-value', 'pathway_pvalue': 'pathway p-value'}
default_control_values = {'max_genes': 64, 'gene_pvalue': 1e-3, 'pathway_pvalue': 1e-5}
default_control_types = {'max_genes': 'int', 'gene_pvalue': 'double', 'pathway_pvalue': 'double'}


def get_control(controls, control):
    value = controls[control_names[control]] if control_names[control] in controls else default_control_values[control]
    if default_control_types[control] == 'double':
        return float(value)
    elif default_control_types[control] == 'Boolean':
        return bool(value)
    elif default_control_types[control] == 'int':
        return int(value)
    else:
        return value


def entrez_gene_id(gene: GeneInfo):
    """
        Return value of the entrez_gene_id attribute
    """
    if (gene.identifiers is not None and gene.identifiers.entrez is not None):
        if (gene.identifiers.entrez.startswith('NCBIGene:')):
            return gene.identifiers.entrez[9:]
        else:
            return gene.identifiers.entrez
    return None


def transform(query):
    controls = {control.name:control.value for control in query.controls}
    max_number = get_control(controls, 'max_genes')
    gene_pvalue = get_control(controls, 'gene_pvalue')
    pathway_pvalue = get_control(controls, 'pathway_pvalue')
    input_genes = [entrez_gene_id(gene) for gene in query.genes]

    output_genes = [] # TO DO call pathDIP

    genes = {}
    gene_list = []
    for gene in query.genes:
        genes[entrez_gene_id(gene)] = gene
        gene_list.append(gene)
    for gene_id in output_genes:
        if gene_id not in genes:
            gene_entrez_id = "NCBIGene:%s" % gene_id
            gene = GeneInfo(
                gene_id = gene_entrez_id,
                identifiers = GeneInfoIdentifiers(entrez = gene_entrez_id),
                attributes=[]
                )
            genes[entrez_gene_id(gene)] = gene
            gene_list.append(gene)
    return gene_list


def transformer_info():
    """
        Return information for this expander
    """
    global control_names

    with open("transformer_info.json",'r') as f:
        info = TransformerInfo.from_dict(json.loads(f.read()))
        control_names = dict((name,parameter.name) for name, parameter in zip(valid_controls, info.parameters))
        return info
