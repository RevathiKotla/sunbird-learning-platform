package org.sunbird.jobs.samza.service.util;

import com.datastax.driver.core.Row;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.ekstep.common.Platform;
import org.ekstep.jobs.samza.util.JobLogger;
import org.ekstep.searchindex.elasticsearch.ElasticSearchUtil;
import org.sunbird.jobs.samza.util.ESUtil;
import org.sunbird.jobs.samza.util.SunbirdCassandraUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BatchEnrolmentSync extends BaseCourseBatchUpdater {

    private static JobLogger LOGGER = new JobLogger(BatchEnrolmentSync.class);
    private String keyspace = Platform.config.hasPath("courses.keyspace.name")
            ? Platform.config.getString("courses.keyspace.name")
            : "sunbird_courses";
    private String table = "user_courses";
    private static final String ES_INDEX_NAME = "user-courses";
    private static final String ES_DOC_TYPE = "_doc";

    public BatchEnrolmentSync() {
        ElasticSearchUtil.initialiseESClient(ES_INDEX_NAME, Platform.config.getString("search.es_conn_info"));
    }

    public void syncEnrolment(Map<String, Object> edata) throws Exception {
        String courseId = (String) edata.get("courseId");
        Map<String, Object> content = getContent(courseId, "leafNodes");
        List<String> leafNodes = (List<String>) content.get("leafNodes");
        if (CollectionUtils.isEmpty((List<String>) content.get("leafNodes"))) {
            LOGGER.info("Content does not have leafNodes : " + courseId);
        } else {
            Map<String, Object> dataToUpdate = getStatusUpdateData(edata, leafNodes);
            if (MapUtils.isNotEmpty(dataToUpdate)) {
                Map<String, Object> dataToSelect = new HashMap<String, Object>() {{
                    put("batchid", edata.get("batchId"));
                    put("userid", edata.get("userId"));
                }};
                if (CollectionUtils.isNotEmpty((List) edata.get("reset"))) {
                    List<String> dataToReset = (List) edata.get("reset");

                    Map<String, Object> finalDataToUpdate = dataToUpdate.entrySet().stream()
                            .filter(entry -> dataToReset.contains(entry.getKey()))
                            .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));

                    SunbirdCassandraUtil.update(keyspace, table, finalDataToUpdate, dataToSelect);
                    dataToUpdate = finalDataToUpdate;
                }
                ESUtil.updateCoureBatch(ES_INDEX_NAME, ES_DOC_TYPE, dataToUpdate, dataToSelect);

            }
        }
    }

    private Map<String, Object> getStatusUpdateData(Map<String, Object> edata, List<String> leafNodes) throws IOException {
        Map<String, Object> contentStatus = new HashMap<>();
        Map<String, Object> dataToSelect = new HashMap<String, Object>() {{
            put("batchid", edata.get("batchId"));
            put("userid", edata.get("userId"));
        }};
        List<Row> rows = SunbirdCassandraUtil.read(keyspace, table, dataToSelect);
        if (CollectionUtils.isNotEmpty(rows)) {
            Map<String, Integer> contentStatusMap = rows.get(0).getMap("contentStatus", String.class, Integer.class);
            if (MapUtils.isNotEmpty(contentStatusMap))
                contentStatus.putAll(contentStatusMap);
        }

        List<String> completedIds = contentStatus.entrySet().stream()
                .filter(entry -> (2 == ((Number) entry.getValue()).intValue()))
                .map(entry -> entry.getKey()).distinct().collect(Collectors.toList());

        int size = CollectionUtils.intersection(completedIds, leafNodes).size();
        double completionPercentage = (((Number) size).doubleValue() / ((Number) leafNodes.size()).doubleValue()) * 100;

        int status = (size == leafNodes.size()) ? 2 : 1;

        return new HashMap<String, Object>() {{
            put("status", status);
            put("completionPercentage", ((Number) completionPercentage).intValue());
            put("progress", size);
        }};
    }
}