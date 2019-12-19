package controllers;

import apimodels.ErrorMsg;
import apimodels.GeneInfo;
import apimodels.TransformerInfo;
import apimodels.TransformerQuery;

import transformer.Transformer;

import play.mvc.Http;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.FileInputStream;
import javax.validation.constraints.*;
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaPlayFrameworkCodegen", date = "2019-12-19T19:52:42.700Z")

public class TransformerApiControllerImp implements TransformerApiControllerImpInterface {
    @Override
    public List<GeneInfo> transformPost(TransformerQuery query) throws Exception {
		return Transformer.produceGeneSet(query);
    }

    @Override
    public TransformerInfo transformerInfoGet() throws Exception {
		return Transformer.transformerInfo();
    }

}
