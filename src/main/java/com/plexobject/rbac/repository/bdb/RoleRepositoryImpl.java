package com.plexobject.rbac.repository.bdb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.validator.GenericValidator;

import com.plexobject.rbac.repository.NotFoundException;
import com.plexobject.rbac.repository.PersistenceException;
import com.plexobject.rbac.repository.RoleRepository;
import com.plexobject.rbac.domain.Role;
import com.plexobject.rbac.metric.Metric;
import com.plexobject.rbac.metric.Timing;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.SecondaryIndex;

public class RoleRepositoryImpl extends BaseRepositoryImpl<Role, String>
        implements RoleRepository {
    private SecondaryIndex<String, String, Role> subjectNameIndex;

    public RoleRepositoryImpl(final EntityStore store) {
        super(store);
        try {
            subjectNameIndex = store.getSecondaryIndex(primaryIndex,
                    String.class, "subjectIds");
        } catch (DatabaseException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public Role save(Role role) throws PersistenceException {
        if (role != null
                && role.getParentIds().contains(Role.ANONYMOUS.getId())) {
            getOrCreateRole(Role.ANONYMOUS.getId());
        }
        return super.save(role);
    }

    @Override
    public Role getOrCreateRole(String rolename) {
        if (GenericValidator.isBlankOrNull(rolename)) {
            throw new IllegalArgumentException("rolename is not specified");
        }
        try {
            return super.findById(rolename);
        } catch (NotFoundException e) {
            Role role = new Role(rolename);
            if (role.getId().equals(Role.ANONYMOUS.getId())) {
                super.save(role);
            } else {
                save(role);
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("**** getOrCreateRole created role!!! " + role);
            }
            return role;
        }
    }

    @Override
    public Collection<Role> getRolesForSubject(String subjectName) {
        final Timing timer = Metric.newTiming(getClass().getName()
                + ".getRolesForSubject");
        List<Role> roles = new ArrayList<Role>();
        EntityCursor<Role> cursor = null;
        try {
            cursor = subjectNameIndex.subIndex(subjectName).entities();
            Iterator<Role> it = cursor.iterator();
            while (it.hasNext()) {
                Role next = it.next();
                roles.add(next);
            }
            return roles;
        } catch (DatabaseException e) {
            throw new PersistenceException(e);
        } finally {
            timer.stop();
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (DatabaseException e) {
                    LOGGER.error("failed to close cursor", e);
                }
            }
        }
    }
}
