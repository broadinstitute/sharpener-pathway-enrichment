package transformer;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;

import apimodels.Attribute;
import apimodels.GeneInfo;
import apimodels.GeneInfoIdentifiers;
import apimodels.Property;
import apimodels.TransformerInfo;
import apimodels.TransformerQuery;

public class Transformer {

	private static String transformerName                  = "Pathway enrichment";
	private static String MAX_GENES             = "max_genes";
	private static String GENE_PVALUE           = "gene_pvalue";
	private static String PATHWAY_PVALUE        = "pathway_pvale";
	private static String DEFAULT_MAX_GENES      = "100";
	private static String DEFAULT_PATHWAY_PVALUE = "1";
	private static String DEFAULT_GENE_PVALUE    = "1e-5";
	
	private static ObjectMapper mapper = new ObjectMapper();
	
	static {
		mapper.setSerializationInclusion(Include.NON_NULL);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}


	public static TransformerInfo transformerInfo() {
		try {
			String json = new String(Files.readAllBytes(Paths.get("transformer_info.json")));
			TransformerInfo info = mapper.readValue(json, TransformerInfo.class);
			transformerName = info.getName();
			MAX_GENES = info.getParameters().get(0).getName();
			DEFAULT_MAX_GENES = info.getParameters().get(0).getDefault();
			GENE_PVALUE = info.getParameters().get(1).getName();
			DEFAULT_MAX_GENES = info.getParameters().get(1).getDefault();
			DEFAULT_PATHWAY_PVALUE = info.getParameters().get(2).getName();
			DEFAULT_GENE_PVALUE = info.getParameters().get(2).getDefault();
			return info;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
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
		HashMap<String,GeneInfo> inputGenes = new HashMap<String,GeneInfo>();
		for (GeneInfo geneInfo : query.getGenes()) {
			if(geneInfo.getIdentifiers() != null && geneInfo.getIdentifiers().getEntrez() != null) {
				myGENES.append(myGENES.toString().equals("") ? "" : ",").append(geneInfo.getIdentifiers().getEntrez());
				inputGenes.put(geneInfo.getIdentifiers().getEntrez(), geneInfo);
			}
			genes.add(geneInfo);
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
				GeneInfo gene = inputGenes.get(geneId);
				if (gene == null) {
					gene = new GeneInfo().geneId(geneId);
					gene.setIdentifiers(new GeneInfoIdentifiers().entrez(geneId));
					genes.add(gene);
					inputGenes.put(geneId, gene);
				}
				if(!pathwayName.equals("")) {
					gene.addAttributesItem(new Attribute().name("enriched pathway name").value(pathwayName).source(transformerName));
            	    gene.addAttributesItem(new Attribute().name("enriched pathway p-value").value(pathwayPval).source(transformerName));
                	gene.addAttributesItem(new Attribute().name("gene within pathway p-value").value(genePval).source(transformerName));
				}
				 
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
