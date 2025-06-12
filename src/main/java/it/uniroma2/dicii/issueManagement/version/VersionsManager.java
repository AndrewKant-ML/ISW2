package it.uniroma2.dicii.issueManagement.version;

import it.uniroma2.dicii.issueManagement.model.Version;

import java.util.List;

public interface VersionsManager {

    void getVersionsInfo();

    void getVersionsInfo(double percentage );

    List<Version> getVersions();

}
