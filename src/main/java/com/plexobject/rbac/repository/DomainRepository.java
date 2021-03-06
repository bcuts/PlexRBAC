package com.plexobject.rbac.repository;

import com.plexobject.rbac.domain.Domain;

public interface DomainRepository extends BaseRepository<Domain, String> {

    /**
     * 
     * @param domain
     * @return
     */
    Domain getOrCreateDomain(String domain);

    /**
     * 
     * @param domain
     * @param subjectName
     * @return
     */
    boolean isSubjectOwner(String domain, String subjectName);

}
