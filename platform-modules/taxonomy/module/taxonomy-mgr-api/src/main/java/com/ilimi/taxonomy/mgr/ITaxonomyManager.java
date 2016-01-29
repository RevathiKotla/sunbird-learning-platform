package com.ilimi.taxonomy.mgr;

import java.io.InputStream;
import java.util.List;

import com.ilimi.common.dto.Request;
import com.ilimi.common.dto.Response;

public interface ITaxonomyManager {

    Response findAll(String[] tfields);

    Response find(String id, boolean subgraph, String[] tfields, String[] cfields);

    Response create(String id, InputStream stream);
    
    Response export(String id, String format);

    Response delete(String id);

    Response search(String id, Request request);

    Response updateDefinition(String id, String json);

    Response findAllDefinitions(String id);

    Response findDefinition(String id, String objectType);

    Response deleteDefinition(String id, String objectType);
    
    Response createIndex(String id, List<String> keys, Boolean unique);
    
    Response getSubGraph(String graphId, String id, Integer depth);

}
