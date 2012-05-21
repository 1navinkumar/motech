package org.motechproject.commcare.service.impl;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.httpclient.NameValuePair;
import org.motechproject.commcare.domain.CaseInfo;
import org.motechproject.commcare.domain.CaseXml;
import org.motechproject.commcare.domain.CaseResponseJson;
import org.motechproject.commcare.service.CommcareCaseService;
import org.motechproject.commcare.util.CommCareAPIHttpClient;
import org.motechproject.dao.MotechJsonReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.reflect.TypeToken;

@Service
public class CommcareCaseServiceImpl implements CommcareCaseService {

	private MotechJsonReader motechJsonReader;

	private CommCareAPIHttpClient commcareHttpClient;

	@Autowired
	public CommcareCaseServiceImpl(CommCareAPIHttpClient commcareHttpClient) {
		this.commcareHttpClient = commcareHttpClient;
		this.motechJsonReader = new MotechJsonReader();
	}

	@Override
	public CaseInfo getCaseByCaseIdAndUserId(String caseId, String userId) {
		NameValuePair[] queryParams = new NameValuePair[2];
		queryParams[0] = new NameValuePair("user_id", userId);
		queryParams[1] = new NameValuePair("case_id", caseId);
		String response = commcareHttpClient.casesRequest(queryParams);
		List<CaseResponseJson> caseResponses = parseCasesFromResponse(response);
		List<CaseInfo> cases = generateCasesFromCaseResponse(caseResponses);
		return cases.get(0);
	}

	@Override
	public CaseInfo getCaseByCaseId(String caseId) {
		NameValuePair[] queryParams = new NameValuePair[1];
		queryParams[0] = new NameValuePair("case_id", caseId);
		String response = commcareHttpClient.casesRequest(queryParams);
		List<CaseResponseJson> caseResponses = parseCasesFromResponse(response);
		List<CaseInfo> cases = generateCasesFromCaseResponse(caseResponses);
		return cases.get(0);
	}

	@Override
	public List<CaseInfo> getAllCases() {
		String response = commcareHttpClient.casesRequest(null);
		List<CaseResponseJson> caseResponses = parseCasesFromResponse(response);
		List<CaseInfo> cases = generateCasesFromCaseResponse(caseResponses);
		return cases;
	}

	@Override
	public List<CaseInfo> getAllCasesByType(String type) {
		NameValuePair[] queryParams = new NameValuePair[1];
		queryParams[0] = new NameValuePair("properties/case_type=", type);
		String response = commcareHttpClient.casesRequest(queryParams);
		List<CaseResponseJson> caseResponses = parseCasesFromResponse(response);
		List<CaseInfo> cases = generateCasesFromCaseResponse(caseResponses);
		return cases;
	}

	@Override
	public List<CaseInfo> getAllCasesByUserId(String userId) {
		NameValuePair[] queryParams = new NameValuePair[1];
		queryParams[0] = new NameValuePair("user_id", userId);
		String response = commcareHttpClient.casesRequest(queryParams);
		List<CaseResponseJson> caseResponses = parseCasesFromResponse(response);
		List<CaseInfo> cases = generateCasesFromCaseResponse(caseResponses);
		return cases;
	}

	@Override
	public List<CaseInfo> getAllCasesByUserIdAndType(String userId, String type) {
		NameValuePair[] queryParams = new NameValuePair[2];
		queryParams[0] = new NameValuePair("user_id", type);
		queryParams[1] = new NameValuePair("properties/case_type=", type);
		String response = commcareHttpClient.casesRequest(queryParams);
		List<CaseResponseJson> caseResponses = parseCasesFromResponse(response);
		List<CaseInfo> cases = generateCasesFromCaseResponse(caseResponses);
		return cases;
	}

	private List<CaseResponseJson> parseCasesFromResponse(String response) {
		Type commcareCaseType = new TypeToken<List<CaseResponseJson>>() {}.getType();
        
        List<CaseResponseJson> allCases = (List<CaseResponseJson>) motechJsonReader.readFromString(response, commcareCaseType);
		
        return allCases;
	}
	
	private List<CaseInfo> generateCasesFromCaseResponse(List<CaseResponseJson> caseResponses) {
		List<CaseInfo> caseList = new ArrayList<CaseInfo>();
		
		for (CaseResponseJson caseResponse : caseResponses) {
			CaseInfo caseInfo = new CaseInfo();
			
			Map<String, String> properties = caseResponse.getProperties();
			
			String case_type = properties.get("case_type");
			String date_opened = properties.get("date_opened");
			String owner_id = properties.get("owner_id)");
			String case_name = properties.get("case_name");
			
			caseInfo.setCase_type(case_type);
			caseInfo.setDate_opened(date_opened);
			caseInfo.setOwner_id(owner_id);
			caseInfo.setCase_name(case_name);
			
			properties.remove("case_type");
			properties.remove("date_opened");
			properties.remove("owner_id");
			properties.remove("case_name");
			
			caseInfo.setField_values(properties);
			caseInfo.setClosed(caseResponse.isClosed());
			caseInfo.setDate_closed(caseResponse.getDate_closed());
			caseInfo.setDomain(caseResponse.getDomain());
			caseInfo.setIndices(caseResponse.getIndices());
			caseInfo.setServer_date_modified(caseResponse.getServer_date_modified());
			caseInfo.setServer_date_opened(caseResponse.getServer_date_opened());
			caseInfo.setVersion(caseResponse.getVersion());
			caseInfo.setXform_ids(caseResponse.getXform_ids());
			caseInfo.setCase_id(caseResponse.getCase_id());
			caseInfo.setUser_id(caseResponse.getUser_id());
			
			caseList.add(caseInfo);
			
		}
		
		return caseList;
	}

}
