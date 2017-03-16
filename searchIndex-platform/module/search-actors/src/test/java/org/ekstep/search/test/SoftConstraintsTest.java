package org.ekstep.search.test;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.ekstep.search.router.SearchRequestRouterPool;
import org.ekstep.searchindex.elasticsearch.ElasticSearchUtil;
import org.ekstep.searchindex.util.CompositeSearchConstants;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ilimi.common.dto.Request;
import com.ilimi.common.dto.Response;
import com.ilimi.common.exception.ResponseCode;

public class SoftConstraintsTest extends BaseSearchActorsTest {
	
	private static ElasticSearchUtil elasticSearchUtil = new ElasticSearchUtil();
	private static ObjectMapper mapper = new ObjectMapper();
	private static String COMPOSITE_SEARCH_INDEX = "testcompositeindex";
	
	@BeforeClass
	public static void beforeTest() throws Exception {
		SearchRequestRouterPool.init();
		createCompositeSearchIndex();
		Thread.sleep(3000);
	}
	
	@AfterClass
	public static void afterTest() throws Exception {
		System.out.println("deleting index: " + COMPOSITE_SEARCH_INDEX);
		elasticSearchUtil.deleteIndex(COMPOSITE_SEARCH_INDEX);
	}
	
	private static void createCompositeSearchIndex() throws Exception {
		COMPOSITE_SEARCH_INDEX = "testcompositeindex";
		System.out.println("creating index: " + COMPOSITE_SEARCH_INDEX);
		String settings = "{ \"settings\": {   \"index\": {     \"index\": \""+CompositeSearchConstants.COMPOSITE_SEARCH_INDEX+"\",     \"type\": \""+CompositeSearchConstants.COMPOSITE_SEARCH_INDEX_TYPE+"\",     \"analysis\": {       \"analyzer\": {         \"cs_index_analyzer\": {           \"type\": \"custom\",           \"tokenizer\": \"standard\",           \"filter\": [             \"lowercase\",             \"mynGram\"           ]         },         \"cs_search_analyzer\": {           \"type\": \"custom\",           \"tokenizer\": \"standard\",           \"filter\": [             \"standard\",             \"lowercase\"           ]         },         \"keylower\": {           \"tokenizer\": \"keyword\",           \"filter\": \"lowercase\"         }       },       \"filter\": {         \"mynGram\": {           \"type\": \"nGram\",           \"min_gram\": 1,           \"max_gram\": 20,           \"token_chars\": [             \"letter\",             \"digit\",             \"whitespace\",             \"punctuation\",             \"symbol\"           ]         }       }     }   } }}";
		String mappings = "{ \""+CompositeSearchConstants.COMPOSITE_SEARCH_INDEX_TYPE+"\" : {    \"dynamic_templates\": [      {        \"longs\": {          \"match_mapping_type\": \"long\",          \"mapping\": {            \"type\": \"long\",            fields: {              \"raw\": {                \"type\": \"long\"              }            }          }        }      },      {        \"booleans\": {          \"match_mapping_type\": \"boolean\",          \"mapping\": {            \"type\": \"boolean\",            fields: {              \"raw\": {                \"type\": \"boolean\"              }            }          }        }      },{        \"doubles\": {          \"match_mapping_type\": \"double\",          \"mapping\": {            \"type\": \"double\",            fields: {              \"raw\": {                \"type\": \"double\"              }            }          }        }      },	  {        \"dates\": {          \"match_mapping_type\": \"date\",          \"mapping\": {            \"type\": \"date\",            fields: {              \"raw\": {                \"type\": \"date\"              }            }          }        }      },      {        \"strings\": {          \"match_mapping_type\": \"string\",          \"mapping\": {            \"type\": \"string\",            \"copy_to\": \"all_fields\",            \"analyzer\": \"cs_index_analyzer\",            \"search_analyzer\": \"cs_search_analyzer\",            fields: {              \"raw\": {                \"type\": \"string\",                \"analyzer\": \"keylower\"              }            }          }        }      }    ],    \"properties\": {      \"all_fields\": {        \"type\": \"string\",        \"analyzer\": \"cs_index_analyzer\",        \"search_analyzer\": \"cs_search_analyzer\",        fields: {          \"raw\": {            \"type\": \"string\",            \"analyzer\": \"keylower\"          }        }      }    }  }}";
		elasticSearchUtil.addIndex(CompositeSearchConstants.COMPOSITE_SEARCH_INDEX,
				CompositeSearchConstants.COMPOSITE_SEARCH_INDEX_TYPE, settings, mappings);
		insertTestRecords();
	}
	
	private static void addToIndex(String uniqueId, Map<String, Object> doc) throws Exception {
		String jsonIndexDocument = mapper.writeValueAsString(doc);
		elasticSearchUtil.addDocumentWithId(COMPOSITE_SEARCH_INDEX,
				CompositeSearchConstants.COMPOSITE_SEARCH_INDEX_TYPE, uniqueId, jsonIndexDocument);
	}
	
	
	private static Map<String, Object> getTestRecord(String id, int index, String prefix, String objectType) {
		Map<String, Object> map = new HashMap<String, Object>();
		if (StringUtils.isNotBlank(id))
			map.put("identifier", id);
		else {
			long suffix = 10000000 + index;
			map.put("identifier", prefix + "_" + suffix);
		}
		map.put("objectType", objectType);
		return map;
	}
	
	private static String[] contentTypes = new String[]{"Story", "Worksheet", "Game", "Collection", "Asset"};
	private static String getContentType() {
		return contentTypes[RandomUtils.nextInt(5)];
	}
	
	private static String[] ageGroup = new String[]{"<5","5-6", "6-7", "7-8","8-10",">10","Other"};
	private static Set<String> getAgeGroup() {
		Set<String> list = new HashSet<String>();
		int count = RandomUtils.nextInt(2);
		for (int i=0; i<count; i++) {
			list.add(ageGroup[RandomUtils.nextInt(6)]);
		}
		return list;
	}
	
	private static String[] gradeLevel = new String[]{"Kindergarten","Grade 1", "Grade 2", "Grade 3", "Grade 4","Grade 5","Other"};
	private static Set<String> getGradeLevel() {
		Set<String> list = new HashSet<String>();
		int count = RandomUtils.nextInt(2);
		for (int i=0; i<count; i++) {
			list.add(gradeLevel[RandomUtils.nextInt(6)]);
		}
		return list;
	}
	
	private static void insertTestRecords() throws Exception {
		for (int i=1; i<=50; i++) {
			Map<String, Object> content = getContentTestRecord(null, i);
			String id = (String) content.get("identifier");
			addToIndex(id, content);
		}
	}
	
	private static Map<String, Object> getContentTestRecord(String id, int index) {
		String objectType = "Content";
		Date d = new Date();
		Map<String, Object> map = getTestRecord(id, index, "do", objectType);
		map.put("name", "Content_" + System.currentTimeMillis() + "_name");
		map.put("code", "code_" + System.currentTimeMillis());
		map.put("contentType", getContentType());
		map.put("createdOn", new Date().toString());
		map.put("lastUpdatedOn", new Date().toString());
		Set<String> ageList = getAgeGroup();
		if (null != ageList && !ageList.isEmpty())
			map.put("ageGroup", ageList);
		Set<String> grades = getGradeLevel();
		if (null != grades && !grades.isEmpty())
			map.put("gradeLevel", grades);
		map.put("downloads", index);
		if (index % 5 == 0) {
			map.put("lastPublishedOn", d.toString());
			map.put("status", "Live");
			map.put("size", 1000432);
		} else {
			map.put("status", "Draft");
			if (index % 3 == 0) 
				map.put("size", 564738);
		}
		return map;
	}
	
	//Mode is given and softConstraints not mentioned in the request body, provide in config
	@SuppressWarnings("unchecked")
	@Test
	public void testSearch1() {
		Request request = getSearchRequest();
		request.put("mode", "soft");
		Map<String, Object> filters = new HashMap<String, Object>();
		List<String> objectTypes = new ArrayList<String>();
		objectTypes.add("Content");
		filters.put("objectType", objectTypes);
		List<String> contentTypes = new ArrayList<String>();
		contentTypes.add("Story");
		contentTypes.add("Worksheet");
		contentTypes.add("Collection");
		contentTypes.add("Game");
		List<String> ageGroup = new ArrayList<>();
		ageGroup.add("5-6");
		filters.put("ageGroup", ageGroup);
		List<String> gradeLevel = new ArrayList<>();
		gradeLevel.add("Grade 1");
		filters.put("gradeLevel", gradeLevel);
		filters.put("contentType", contentTypes);
		List<String> status = new ArrayList<String>();
		status.add("Live");
		filters.put("status", status);
		request.put("filters", filters);
		Response response = getSearchResponse(request);
		Map<String, Object> result = response.getResult();
		List<Object> list = (List<Object>) result.get("results");
		Assert.assertNotNull(list);
		Assert.assertTrue(list.size() > 0);
		ResponseCode res = response.getResponseCode();
		boolean statusCode = false;
		if (res == ResponseCode.OK) {
			statusCode = true;
		}
		Assert.assertTrue(statusCode);
		boolean found = false;
		Map<String, Object> content = (Map<String, Object>) list.get(0);
		if (null != content && content.containsKey("ageGroup")) {
			found = true;
		}
		Assert.assertTrue(found);
	}
	//Mode and softConstraints are given in the request body
	@SuppressWarnings("unchecked")
	@Test
	public void testSearch2() {
		Request request = getSearchRequest();
		request.put("mode", "soft");
		Map<String, Object> filters = new HashMap<String, Object>();
		List<String> objectTypes = new ArrayList<String>();
		objectTypes.add("Content");
		filters.put("objectType", objectTypes);
		List<String> contentTypes = new ArrayList<String>();
		contentTypes.add("Story");
		contentTypes.add("Worksheet");
		contentTypes.add("Collection");
		contentTypes.add("Game");
		filters.put("contentType", contentTypes);
		List<String> ageGroup = new ArrayList<>();
		ageGroup.add("5-6");
		filters.put("ageGroup", ageGroup);
		List<String> gradeLevel = new ArrayList<>();
		gradeLevel.add("Grade 1");
		filters.put("gradeLevel", gradeLevel);
		List<String> status = new ArrayList<String>();
		status.add("Live");
		filters.put("status", status);
		Map<String, Integer> softConstraints = new HashMap<>();
		softConstraints.put("ageGroup", 3);
		softConstraints.put("gradeLevel", 4);
		request.put("softConstraints", softConstraints);
		request.put("filters", filters);
		Response response = getSearchResponse(request);
		Map<String, Object> result = response.getResult();
		List<Object> list = (List<Object>) result.get("results");
		Assert.assertNotNull(list);
		Assert.assertTrue(list.size() > 0);
		ResponseCode res = response.getResponseCode();
		boolean statusCode = false;
		if (res == ResponseCode.OK) {
			statusCode = true;
		}
		Assert.assertTrue(statusCode);
		boolean found = false;
		Map<String, Object> content = (Map<String, Object>) list.get(0);
		if (null != content && content.containsKey("ageGroup")) {
			found = true;
		}
		Assert.assertTrue(found);
	}
	//Mode is not provided, SoftConstraints are mentioned in the request
	@SuppressWarnings("unchecked")
	@Test
	public void testSearch3() {
		Request request = getSearchRequest();
		Map<String, Object> filters = new HashMap<String, Object>();
		List<String> objectTypes = new ArrayList<String>();
		objectTypes.add("Content");
		filters.put("objectType", objectTypes);
		List<String> contentTypes = new ArrayList<String>();
		contentTypes.add("Story");
		contentTypes.add("Worksheet");
		contentTypes.add("Collection");
		contentTypes.add("Game");
		filters.put("contentType", contentTypes);
		List<String> ageGroup = new ArrayList<>();
		ageGroup.add("5-6");
		filters.put("ageGroup", ageGroup);
		List<String> gradeLevel = new ArrayList<>();
		gradeLevel.add("Grade 1");
		filters.put("gradeLevel", gradeLevel);
		List<String> status = new ArrayList<String>();
		status.add("Live");
		filters.put("status", status);
		Map<String, Integer> softConstraints = new HashMap<>();
		softConstraints.put("ageGroup", 3);
		softConstraints.put("gradeLevel", 4);
		request.put("softConstraints", softConstraints);
		request.put("filters", filters);
		Response response = getSearchResponse(request);
		Map<String, Object> result = response.getResult();
		List<Object> list = (List<Object>) result.get("results");
		Assert.assertNotNull(list);
		Assert.assertTrue(list.size() > 0);
		ResponseCode res = response.getResponseCode();
		boolean statusCode = false;
		if (res == ResponseCode.OK) {
			statusCode = true;
		}
		Assert.assertTrue(statusCode);
		boolean found = false;
		Map<String, Object> content = (Map<String, Object>) list.get(0);
		if (null != content && content.containsKey("ageGroup")) {
			found = true;
		}
		Assert.assertTrue(found);
	}
	//Both Mode and softConstraints are not mentioned in the request
	@SuppressWarnings("unchecked")
	@Test
	public void testSearch4() {
		Request request = getSearchRequest();
		Map<String, Object> filters = new HashMap<String, Object>();
		List<String> objectTypes = new ArrayList<String>();
		objectTypes.add("Content");
		filters.put("objectType", objectTypes);
		List<String> contentTypes = new ArrayList<String>();
		contentTypes.add("Story");
		contentTypes.add("Worksheet");
		contentTypes.add("Collection");
		contentTypes.add("Game");
		filters.put("contentType", contentTypes);
		List<String> ageGroup = new ArrayList<>();
		ageGroup.add("5-6");
		filters.put("ageGroup", ageGroup);
		List<String> gradeLevel = new ArrayList<>();
		gradeLevel.add("Grade 1");
		filters.put("gradeLevel", gradeLevel);
		List<String> status = new ArrayList<String>();
		status.add("Live");
		filters.put("status", status);
		request.put("filters", filters);
		Response response = getSearchResponse(request);
		Map<String, Object> result = response.getResult();
		List<Object> list = (List<Object>) result.get("results");
		Assert.assertNotNull(list);
		Assert.assertTrue(list.size() > 0);
		ResponseCode res = response.getResponseCode();
		boolean statusCode = false;
		if (res == ResponseCode.OK) {
			statusCode = true;
		}
		Assert.assertTrue(statusCode);
		boolean found = false;
		Map<String, Object> content = (Map<String, Object>) list.get(0);
		if (null != content && content.containsKey("ageGroup")){
			found = true;
		}
		Assert.assertTrue(found);
	}
	//Mode is Hard and softConstraints are given in the request body
	@SuppressWarnings("unchecked")
	@Test
	public void testSearch5() {
		Request request = getSearchRequest();
		request.put("mode", "Hard");
		Map<String, Object> filters = new HashMap<String, Object>();
		List<String> objectTypes = new ArrayList<String>();
		objectTypes.add("Content");
		filters.put("objectType", objectTypes);
		List<String> contentTypes = new ArrayList<String>();
		contentTypes.add("Story");
		contentTypes.add("Worksheet");
		contentTypes.add("Collection");
		contentTypes.add("Game");
		filters.put("contentType", contentTypes);
		List<String> ageGroup = new ArrayList<>();
		ageGroup.add("5-6");
		filters.put("ageGroup", ageGroup);
		List<String> gradeLevel = new ArrayList<>();
		gradeLevel.add("Grade 1");
		filters.put("gradeLevel", gradeLevel);
		List<String> status = new ArrayList<String>();
		status.add("Live");
		filters.put("status", status);
		Map<String, Integer> softConstraints = new HashMap<>();
		softConstraints.put("ageGroup", 3);
		softConstraints.put("gradeLevel", 4);
		request.put("softConstraints", softConstraints);
		request.put("filters", filters);
		Response response = getSearchResponse(request);
		Map<String, Object> result = response.getResult();
		List<Object> list = (List<Object>) result.get("results");
		Assert.assertNotNull(list);
		Assert.assertTrue(list.size() > 0);
		ResponseCode res = response.getResponseCode();
		boolean statusCode = false;
		if (res == ResponseCode.OK) {
			statusCode = true;
		}
		Assert.assertTrue(statusCode);
		boolean found = false;
		Map<String, Object> content = (Map<String, Object>) list.get(0);
		if (null != content && content.containsKey("ageGroup")){
			found = true;
		}
		Assert.assertTrue(found);
	}
	//Mode is invalid and softConstraints are given in the request
	@SuppressWarnings("unchecked")
	@Test
	public void testSearch6() {
		Request request = getSearchRequest();
		request.put("mode", "xyz");
		Map<String, Object> filters = new HashMap<String, Object>();
		List<String> objectTypes = new ArrayList<String>();
		objectTypes.add("Content");
		filters.put("objectType", objectTypes);
		List<String> contentTypes = new ArrayList<String>();
		contentTypes.add("Story");
		contentTypes.add("Worksheet");
		contentTypes.add("Collection");
		contentTypes.add("Game");
		filters.put("contentType", contentTypes);
		List<String> ageGroup = new ArrayList<>();
		ageGroup.add("5-6");
		filters.put("ageGroup", ageGroup);
		List<String> gradeLevel = new ArrayList<>();
		gradeLevel.add("Grade 1");
		filters.put("gradeLevel", gradeLevel);
		List<String> status = new ArrayList<String>();
		status.add("Live");
		filters.put("status", status);
		Map<String, Integer> softConstraints = new HashMap<>();
		softConstraints.put("ageGroup", 3);
		softConstraints.put("gradeLevel", 4);
		request.put("softConstraints", softConstraints);
		request.put("filters", filters);
		Response response = getSearchResponse(request);
		Map<String, Object> result = response.getResult();
		List<Object> list = (List<Object>) result.get("results");
		Assert.assertNotNull(list);
		Assert.assertTrue(list.size() > 0);
		ResponseCode res = response.getResponseCode();
		boolean statusCode = false;
		if (res == ResponseCode.OK) {
			statusCode = true;
		}
		Assert.assertTrue(statusCode);
		boolean found = false;
		Map<String, Object> content = (Map<String, Object>) list.get(0);
		if (null != content && content.containsKey("ageGroup")){
			found = true;
		}
		Assert.assertTrue(found);
	}
	//Mode is soft and softConstraints in the request is blank, provided in Config
	@SuppressWarnings("unchecked")
	@Test
	public void testSearch7() {
		Request request = getSearchRequest();
		request.put("mode", "soft");
		Map<String, Object> filters = new HashMap<String, Object>();
		List<String> objectTypes = new ArrayList<String>();
		objectTypes.add("Content");
		filters.put("objectType", objectTypes);
		List<String> contentTypes = new ArrayList<String>();
		contentTypes.add("Story");
		contentTypes.add("Worksheet");
		contentTypes.add("Collection");
		contentTypes.add("Game");
		filters.put("contentType", contentTypes);
		List<String> ageGroup = new ArrayList<>();
		ageGroup.add("5-6");
		filters.put("ageGroup", ageGroup);
		List<String> gradeLevel = new ArrayList<>();
		gradeLevel.add("Grade 1");
		filters.put("gradeLevel", gradeLevel);
		List<String> status = new ArrayList<String>();
		status.add("Live");
		filters.put("status", status);
		Map<String, Integer> softConstraints = new HashMap<>();
		request.put("softConstraints", softConstraints);
		request.put("filters", filters);
		Response response = getSearchResponse(request);
		Map<String, Object> result = response.getResult();
		List<Object> list = (List<Object>) result.get("results");
		Assert.assertNotNull(list);
		Assert.assertTrue(list.size() > 0);
		ResponseCode res = response.getResponseCode();
		boolean statusCode = false;
		if (res == ResponseCode.OK) {
			statusCode = true;
		}
		Assert.assertTrue(statusCode);
		boolean found = false;
		Map<String, Object> content = (Map<String, Object>) list.get(0);
		if (null != content && content.containsKey("ageGroup")){
			found = true;
		}
		Assert.assertTrue(found);
	}
	// Mode is soft and softConstraints are given in the request and is same in config
	@SuppressWarnings("unchecked")
	@Test
	public void testSearch8() {
		Request request = getSearchRequest();
		request.put("mode", "soft");
		Map<String, Object> filters = new HashMap<String, Object>();
		List<String> objectTypes = new ArrayList<String>();
		objectTypes.add("Content");
		filters.put("objectType", objectTypes);
		List<String> contentTypes = new ArrayList<String>();
		contentTypes.add("Story");
		contentTypes.add("Worksheet");
		contentTypes.add("Collection");
		contentTypes.add("Game");
		filters.put("contentType", contentTypes);
		List<String> ageGroup = new ArrayList<>();
		ageGroup.add("5-6");
		filters.put("ageGroup", ageGroup);
		List<String> gradeLevel = new ArrayList<>();
		gradeLevel.add("Grade 1");
		filters.put("gradeLevel", gradeLevel);
		List<String> status = new ArrayList<String>();
		status.add("Live");
		filters.put("status", status);
		Map<String, Integer> softConstraints = new HashMap<>();
		softConstraints.put("ageGroup", 3);
		softConstraints.put("gradeLevel", 4);
		request.put("softConstraints", softConstraints);
		request.put("filters", filters);
		Response response = getSearchResponse(request);
		Map<String, Object> result = response.getResult();
		List<Object> list = (List<Object>) result.get("results");
		Assert.assertNotNull(list);
		Assert.assertTrue(list.size() > 0);
		ResponseCode res = response.getResponseCode();
		boolean statusCode = false;
		if (res == ResponseCode.OK) {
			statusCode = true;
		}
		Assert.assertTrue(statusCode);
		boolean found = false;
		Map<String, Object> content = (Map<String, Object>) list.get(0);
		if (null != content && content.containsKey("ageGroup")){
			found = true;
		}
		Assert.assertTrue(found);
	}
	// Mode is soft and softConstraints are given in the request is different from Config
	@SuppressWarnings("unchecked")
	@Test
	public void testSearch9() {
		Request request = getSearchRequest();
		request.put("mode", "soft");
		Map<String, Object> filters = new HashMap<String, Object>();
		List<String> objectTypes = new ArrayList<String>();
		objectTypes.add("Content");
		filters.put("objectType", objectTypes);
		List<String> contentTypes = new ArrayList<String>();
		contentTypes.add("Story");
		contentTypes.add("Worksheet");
		contentTypes.add("Collection");
		contentTypes.add("Game");
		filters.put("contentType", contentTypes);
		List<String> ageGroup = new ArrayList<>();
		ageGroup.add("5-6");
		filters.put("ageGroup", ageGroup);
		List<String> gradeLevel = new ArrayList<>();
		gradeLevel.add("Grade 1");
		filters.put("gradeLevel", gradeLevel);
		List<String> status = new ArrayList<String>();
		status.add("Live");
		filters.put("status", status);
		Map<String, Integer> softConstraints = new HashMap<>();
		softConstraints.put("gradeLevel", 4);
		request.put("softConstraints", softConstraints);
		request.put("filters", filters);
		Response response = getSearchResponse(request);
		Map<String, Object> result = response.getResult();
		List<Object> list = (List<Object>) result.get("results");
		Assert.assertNotNull(list);
		Assert.assertTrue(list.size() > 0);
		ResponseCode res = response.getResponseCode();
		boolean statusCode = false;
		if (res == ResponseCode.OK) {
			statusCode = true;
		}
		Assert.assertTrue(statusCode);
		boolean found = false;
		Map<String, Object> content = (Map<String, Object>) list.get(0);
		if (null != content && content.containsKey("gradeLevel")){
			found = true;
		}
		Assert.assertTrue(found);
	}
	//Mode is soft, softConstraints are provided and config is also blank
	@SuppressWarnings("unchecked")
	@Test
	public void testSearch10() {
		Request request = getSearchRequest();
		request.put("mode", "soft");
		Map<String, Object> filters = new HashMap<String, Object>();
		List<String> objectTypes = new ArrayList<String>();
		objectTypes.add("Content");
		filters.put("objectType", objectTypes);
		List<String> contentTypes = new ArrayList<String>();
		contentTypes.add("Story");
		contentTypes.add("Worksheet");
		contentTypes.add("Collection");
		contentTypes.add("Game");
		filters.put("contentType", contentTypes);
		List<String> ageGroup = new ArrayList<>();
		ageGroup.add("5-6");
		filters.put("ageGroup", ageGroup);
		List<String> gradeLevel = new ArrayList<>();
		gradeLevel.add("Grade 1");
		filters.put("gradeLevel", gradeLevel);
		List<String> status = new ArrayList<String>();
		status.add("Live");
		filters.put("status", status);
		Map<String, Integer> softConstraints = new HashMap<>();
		softConstraints.put("ageGroup", 3);
		softConstraints.put("gradeLevel", 4);
		request.put("softConstraints", softConstraints);
		request.put("filters", filters);
		Response response = getSearchResponse(request);
		Map<String, Object> result = response.getResult();
		List<Object> list = (List<Object>) result.get("results");
		Assert.assertNotNull(list);
		Assert.assertTrue(list.size() > 0);
		ResponseCode res = response.getResponseCode();
		boolean statusCode = false;
		if (res == ResponseCode.OK) {
			statusCode = true;
		}
		Assert.assertTrue(statusCode);
		boolean found = false;
		Map<String, Object> content = (Map<String, Object>) list.get(0);
		if (null != content && content.containsKey("ageGroup")){
			found = true;
		}
		Assert.assertTrue(found);
	}
	//mode is soft, softConstraints is not provided and is config is also blank
	@SuppressWarnings("unchecked")
	@Test
	public void testSearch11() {
		Request request = getSearchRequest();
		request.put("mode", "soft");
		Map<String, Object> filters = new HashMap<String, Object>();
		List<String> objectTypes = new ArrayList<String>();
		objectTypes.add("Content");
		filters.put("objectType", objectTypes);
		List<String> contentTypes = new ArrayList<String>();
		contentTypes.add("Story");
		contentTypes.add("Worksheet");
		contentTypes.add("Collection");
		contentTypes.add("Game");
		filters.put("contentType", contentTypes);
		List<String> ageGroup = new ArrayList<>();
		ageGroup.add("5-6");
		filters.put("ageGroup", ageGroup);
		List<String> gradeLevel = new ArrayList<>();
		gradeLevel.add("Grade 1");
		filters.put("gradeLevel", gradeLevel);
		List<String> status = new ArrayList<String>();
		status.add("Live");
		filters.put("status", status);
		/*Map<String, Integer> softConstraints = new HashMap<>();
		softConstraints.put("ageGroup", 3);
		softConstraints.put("gradeLevel", 4);
		request.put("softConstraints", softConstraints);*/
		request.put("filters", filters);
		Response response = getSearchResponse(request);
		Map<String, Object> result = response.getResult();
		List<Object> list = (List<Object>) result.get("results");
		Assert.assertNotNull(list);
		Assert.assertTrue(list.size() > 0);
		ResponseCode res = response.getResponseCode();
		boolean statusCode = false;
		if (res == ResponseCode.OK) {
			statusCode = true;
		}
		Assert.assertTrue(statusCode);
		boolean found = false;
		Map<String, Object> content = (Map<String, Object>) list.get(0);
		if (null != content && content.containsKey("ageGroup")){
			found = true;
		}
		Assert.assertTrue(found);
	}
	
	//mode is soft, softConstraints provided is blank and is config is also blank
	@SuppressWarnings("unchecked")
	@Test
	public void testSearch12() {
		Request request = getSearchRequest();
		request.put("mode", "soft");
		Map<String, Object> filters = new HashMap<String, Object>();
		List<String> objectTypes = new ArrayList<String>();
		objectTypes.add("Content");
		filters.put("objectType", objectTypes);
		List<String> contentTypes = new ArrayList<String>();
		contentTypes.add("Story");
		contentTypes.add("Worksheet");
		contentTypes.add("Collection");
		contentTypes.add("Game");
		filters.put("contentType", contentTypes);
		List<String> ageGroup = new ArrayList<>();
		ageGroup.add("5-6");
		filters.put("ageGroup", ageGroup);
		List<String> gradeLevel = new ArrayList<>();
		gradeLevel.add("Grade 1");
		filters.put("gradeLevel", gradeLevel);
		List<String> status = new ArrayList<String>();
		status.add("Live");
		filters.put("status", status);
		Map<String, Integer> softConstraints = new HashMap<>();
		/*softConstraints.put("ageGroup", 3);
		softConstraints.put("gradeLevel", 4);*/
		request.put("softConstraints", softConstraints);
		request.put("filters", filters);
		Response response = getSearchResponse(request);
		Map<String, Object> result = response.getResult();
		List<Object> list = (List<Object>) result.get("results");
		Assert.assertNotNull(list);
		Assert.assertTrue(list.size() > 0);
		ResponseCode res = response.getResponseCode();
		boolean statusCode = false;
		if (res == ResponseCode.OK) {
			statusCode = true;
		}
		Assert.assertTrue(statusCode);
		boolean found = false;
		Map<String, Object> content = (Map<String, Object>) list.get(0);
		if (null != content && content.containsKey("ageGroup")){
			found = true;
		}
		Assert.assertTrue(found);
	}
	//mode is soft, softConstraints provided is partial in request and config is also blank
	@SuppressWarnings("unchecked")
	@Test
	public void testSearch13() {
		Request request = getSearchRequest();
		request.put("mode", "soft");
		Map<String, Object> filters = new HashMap<String, Object>();
		List<String> objectTypes = new ArrayList<String>();
		objectTypes.add("Content");
		filters.put("objectType", objectTypes);
		List<String> contentTypes = new ArrayList<String>();
		contentTypes.add("Story");
		contentTypes.add("Worksheet");
		contentTypes.add("Collection");
		contentTypes.add("Game");
		filters.put("contentType", contentTypes);
		List<String> ageGroup = new ArrayList<>();
		filters.put("ageGroup", ageGroup);
		List<String> gradeLevel = new ArrayList<>();
		gradeLevel.add("Grade 1");
		filters.put("gradeLevel", gradeLevel);
		List<String> status = new ArrayList<String>();
		status.add("Live");
		filters.put("status", status);
		Map<String, Integer> softConstraints = new HashMap<>();
		softConstraints.put("ageGroup", 3);
		softConstraints.put("gradeLevel", 4);
		request.put("softConstraints", softConstraints);
		request.put("filters", filters);
		Response response = getSearchResponse(request);
		Map<String, Object> result = response.getResult();
		List<Object> list = (List<Object>) result.get("results");
		Assert.assertNotNull(list);
		Assert.assertTrue(list.size() > 0);
		ResponseCode res = response.getResponseCode();
		boolean statusCode = false;
		if (res == ResponseCode.OK) {
			statusCode = true;
		}
		Assert.assertTrue(statusCode);
	}
	//mode is blank and softConstraints is not provided in request
	@SuppressWarnings("unchecked")
	@Test
	public void testSearch14() {
		Request request = getSearchRequest();
		request.put("mode", "");
		Map<String, Object> filters = new HashMap<String, Object>();
		List<String> objectTypes = new ArrayList<String>();
		objectTypes.add("Content");
		filters.put("objectType", objectTypes);
		List<String> contentTypes = new ArrayList<String>();
		contentTypes.add("Story");
		contentTypes.add("Worksheet");
		contentTypes.add("Collection");
		contentTypes.add("Game");
		filters.put("contentType", contentTypes);
		List<String> ageGroup = new ArrayList<>();
		ageGroup.add("5-6");
		filters.put("ageGroup", ageGroup);
		List<String> gradeLevel = new ArrayList<>();
		gradeLevel.add("Grade 1");
		filters.put("gradeLevel", gradeLevel);
		List<String> status = new ArrayList<String>();
		status.add("Live");
		filters.put("status", status);
		Map<String, Integer> softConstraints = new HashMap<>();
		softConstraints.put("ageGroup", 3);
		softConstraints.put("gradeLevel", 4);
		request.put("softConstraints", softConstraints);
		request.put("filters", filters);
		Response response = getSearchResponse(request);
		Map<String, Object> result = response.getResult();
		List<Object> list = (List<Object>) result.get("results");
		Assert.assertNotNull(list);
		Assert.assertTrue(list.size() > 0);
		ResponseCode res = response.getResponseCode();
		boolean statusCode = false;
		if (res == ResponseCode.OK) {
			statusCode = true;
		}
		Assert.assertTrue(statusCode);
		boolean found = false;
		Map<String, Object> content = (Map<String, Object>) list.get(0);
		if (null != content && content.containsKey("ageGroup")){
			found = true;
		}
		Assert.assertTrue(found);
	}
}
