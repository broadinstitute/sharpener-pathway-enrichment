package transformer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import apimodels.Attribute;
import apimodels.GeneInfo;
import apimodels.Parameter;
import apimodels.Property;
import apimodels.TransformerInfo;
import apimodels.TransformerQuery;

public class Transformer {

	private static final String NAME                  = "Pathway enrichment";
	private static final String MAX_GENES             = "max_genes";
	private static final String GENE_PVALUE           = "gene_pvalue";
	private static final String PATHWAY_PVALUE        = "pathway_pvale";
	private static final String DEFAULT_MAX_GENES      = "100";
	private static final String DEFAULT_PATHWAY_PVALUE = "1";
	private static final String DEFAULT_GENE_PVALUE    = "1e-5";

	private static HashMap<String,ArrayList<GeneInfo>> geneSets = new HashMap<String,ArrayList<GeneInfo>>();

	public static TransformerInfo transformerInfo() {

		TransformerInfo transformerInfo = new TransformerInfo().name(NAME);
		transformerInfo.function(TransformerInfo.FunctionEnum.EXPANDER);
		transformerInfo.description("Pathway enrichment analysis with pathDIP");
		transformerInfo.addParametersItem(
				new Parameter()
					.name(MAX_GENES)
					.type(Parameter.TypeEnum.INT)
					._default(DEFAULT_MAX_GENES)
					.suggestedValues("from 10 to 1000")
				);
		transformerInfo.addParametersItem(
				new Parameter()
					.name(PATHWAY_PVALUE)
					.type(Parameter.TypeEnum.DOUBLE)
					._default(DEFAULT_PATHWAY_PVALUE)
					.suggestedValues("from 1 to 1e-5")
				);
		transformerInfo.addParametersItem(
				new Parameter()
					.name(GENE_PVALUE)
					.type(Parameter.TypeEnum.DOUBLE)
					._default(DEFAULT_GENE_PVALUE)
					.suggestedValues("from 1e-3 to 1e-30")
				);

		transformerInfo.addRequiredAttributesItem("identifiers.entrez");
		return transformerInfo;
	}


	public static List<GeneInfo> produceGeneSet(final TransformerQuery query) {

		String myMAX_GENES      = DEFAULT_MAX_GENES;
		String myGENE_PVALUE    = DEFAULT_GENE_PVALUE;
		String myPATHWAY_PVALUE = DEFAULT_PATHWAY_PVALUE;
		for (Property property : query.getControls()) {
			if (MAX_GENES.equals(property.getName())) {
				myMAX_GENES = property.getValue();
			}
			if (PATHWAY_PVALUE.equals(property.getName())) {
				myPATHWAY_PVALUE = property.getValue();
			}
			if (GENE_PVALUE.equals(property.getName())) {
				myGENE_PVALUE = property.getValue();
			}
		}
		
		StringBuilder myGENES = new StringBuilder("");
		ArrayList<GeneInfo> genes = new ArrayList<GeneInfo>();
		for (GeneInfo geneInfo : query.getGenes()) {
			if(geneInfo.getIdentifiers() != null && geneInfo.getIdentifiers().getEntrez() != null) {
				myGENES.append(myGENES.toString().equals("") ? "" : ",").append(geneInfo.getIdentifiers().getEntrez());
			}
		}

		Runtime rt = Runtime.getRuntime();

		String[] commands = {"perl", "scripts/runPathwayEnrichmentAnalysis.pl", myMAX_GENES, myPATHWAY_PVALUE, myGENE_PVALUE, myGENES.toString()};
	
		try {
			Process proc = rt.exec(commands);

			BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

			//Read the gene list from expander
			String s;
			while ((s = stdInput.readLine()) != null) {
				String[] row = s.split("\t");
				String geneId = "NCBIGene:" + row[0];
				String pathwayName = row[1];
				String pathwayPval = row[2];
				String genePval    = row[3];
				GeneInfo gene = new GeneInfo().geneId(geneId);
				gene.addAttributesItem(new Attribute().name("entrez_gene_id").value(geneId).source(NAME));
				if(!pathwayName.equals("")) {
					gene.addAttributesItem(new Attribute().name("enriched pathway name").value(pathwayName).source(NAME));
            	    gene.addAttributesItem(new Attribute().name("enriched pathway p-value").value(pathwayPval).source(NAME));
                	gene.addAttributesItem(new Attribute().name("gene within pathway p-value").value(genePval).source(NAME));
				}
				genes.add(gene); 
			}

			//Print any errors from the attempted command
			while ((s = stdError.readLine()) != null) {
				System.err.println(s);
			}
		}
		catch(Exception e) {
			System.err.println(e.toString()); 
		}

		return genes;
	}

}
