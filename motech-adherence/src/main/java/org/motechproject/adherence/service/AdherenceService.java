package org.motechproject.adherence.service;

import org.joda.time.LocalDate;
import org.motechproject.adherence.dao.AllAdherenceLogs;
import org.motechproject.adherence.domain.AdherenceLog;
import org.motechproject.adherence.domain.Concept;
import org.motechproject.adherence.domain.ErrorFunction;
import org.motechproject.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class AdherenceService {

    public static final String ERROR_CORRECTION = "error_Correction";
    public static Concept GENERIC_CONCEPT = null;
    private AllAdherenceLogs allAdherenceLogs;

    @Autowired
    public AdherenceService(AllAdherenceLogs allAdherenceLogs) {
        this.allAdherenceLogs = allAdherenceLogs;
    }

    public void recordUnitAdherence(String externalId, Concept concept, boolean taken, ErrorFunction errorFunction, Map<String, Object> meta) {
        AdherenceLog latestLog = allAdherenceLogs.findLatestLog(externalId, concept);
        LocalDate today = DateUtil.today();
        int dosesTaken = taken ? 1 : 0;
        if (latestLog == null) {
            AdherenceLog newLog = AdherenceLog.create(externalId, concept, meta, today, dosesTaken);
            allAdherenceLogs.insert(newLog);
        } else {
            AdherenceLog fillerLog = correctError(externalId, concept, latestLog, errorFunction, today);
            latestLog = (fillerLog == null) ? latestLog : fillerLog;
            AdherenceLog newLog = AdherenceLog.initialize(meta, latestLog, dosesTaken);
            allAdherenceLogs.insert(newLog);
        }
    }

    public void recordAdherence(String externalId, Concept concept, int taken, int totalDoses, LocalDate logDate, ErrorFunction errorFunction, Map<String, Object> meta) {
        recordAdherence(externalId, concept, taken, totalDoses, logDate, logDate, errorFunction, meta);
    }

    public void recordAdherence(String externalId, Concept concept, int taken, int totalDoses, LocalDate fromDate, LocalDate toDate, ErrorFunction errorFunction, Map<String, Object> meta) {
        AdherenceLog latestLog = allAdherenceLogs.findLatestLog(externalId, concept);
        LocalDate today = DateUtil.today();
        if (latestLog == null) {
            AdherenceLog newLog = AdherenceLog.create(externalId, concept, taken, totalDoses, fromDate, toDate, meta, today);
            allAdherenceLogs.insert(newLog);
        } else {
            AdherenceLog fillerLog = correctError(externalId, concept, latestLog, errorFunction, today);
            latestLog = (fillerLog == null) ? latestLog : fillerLog;
            AdherenceLog newLog = latestLog.addAdherence(taken, totalDoses);
            AdherenceLog.initialize(fromDate, toDate, meta, newLog);
            allAdherenceLogs.insert(newLog);
        }
    }

    public double getRunningAverageAdherence(String externalId, Concept concept) {
        AdherenceLog latestLog = allAdherenceLogs.findLatestLog(externalId, concept);
        if (latestLog == null) {
            return 0;
        } else {
            return ((double) latestLog.getDosesTaken()) / latestLog.getTotalDoses();
        }
    }

    public double getRunningAverageAdherence(String externalId, Concept concept, LocalDate on) {
        AdherenceLog log = allAdherenceLogs.findByDate(externalId, concept, on);
        if (log == null) {
            return 0;
        } else {
            return ((double) log.getDosesTaken()) / log.getTotalDoses();
        }
    }

    public double getDeltaAdherence(String externalId, Concept concept) {
        AdherenceLog latestLog = allAdherenceLogs.findLatestLog(externalId, concept);
        if (latestLog == null) {
            return 0;
        } else {
            return ((double) latestLog.getDeltaDosesTaken()) / latestLog.getDeltaTotalDoses();
        }
    }

    public double getDeltaAdherence(String externalId, Concept concept, LocalDate fromDate, LocalDate toDate) {
        List<AdherenceLog> logs = Collections.emptyList();
        double dosesTaken = 0;
        double totalDoses = 0;
        for (AdherenceLog adherenceLog : logs) {
            dosesTaken += adherenceLog.getDeltaDosesTaken();
            totalDoses += adherenceLog.getDeltaTotalDoses();
        }
        return totalDoses == 0 ? 0 : dosesTaken / totalDoses;
    }

    public void updateLatestAdherence(String externalId, Concept concept, int dosesTaken, int totalDoses) {
        AdherenceLog latestLog = allAdherenceLogs.findLatestLog(externalId, concept);
        latestLog.updateDeltaDosesTaken(dosesTaken);
        latestLog.updateDeltaTotalDoses(totalDoses);
        allAdherenceLogs.update(latestLog);
    }

    public LocalDate getLatestAdherenceDate(String externalId, Concept concept) {
        AdherenceLog latestAdherenceLog = allAdherenceLogs.findLatestLog(externalId, concept);
        if (latestAdherenceLog == null)
            return null;
        else
            return latestAdherenceLog.getToDate();
    }

    public Map<String, Object> getMetaOn(String externalId, Concept concept, LocalDate date) {
        AdherenceLog latestLog = allAdherenceLogs.findByDate(externalId, concept, date);
        if (latestLog == null) {
            return Collections.<String, Object>emptyMap();
        } else {
            return latestLog.getMeta() == null ? Collections.<String, Object>emptyMap() : latestLog.getMeta();
        }
    }

    private void restartAdherenceRecording(AdherenceLog latestLog, AdherenceLog newLog, ErrorFunction errorFunction) {
        allAdherenceLogs.remove(latestLog);
        latestLog = allAdherenceLogs.findLatestLog(newLog.getExternalId(), newLog.getConcept());
        correctError(newLog.getExternalId(), newLog.getConcept(), latestLog, errorFunction, newLog.getFromDate());
        allAdherenceLogs.insert(newLog);
    }

    private AdherenceLog correctError(String externalId, Concept concept, AdherenceLog latestLog, ErrorFunction errorFunction, LocalDate fromDate) {
        if (latestLog.isNotOn(fromDate.minusDays(1))) {
            AdherenceLog fillerLog = AdherenceLog.create(externalId, concept, latestLog.getFromDate().plusDays(1), fromDate.minusDays(1));
            fillerLog = fillerLog.addAdherence(latestLog.getDosesTaken() + errorFunction.getDosesTaken(), latestLog.getTotalDoses() + errorFunction.getTotalDoses());
            fillerLog.putMeta(ERROR_CORRECTION, true);
            allAdherenceLogs.insert(fillerLog);
            return fillerLog;
        }
        return null;
    }
}
