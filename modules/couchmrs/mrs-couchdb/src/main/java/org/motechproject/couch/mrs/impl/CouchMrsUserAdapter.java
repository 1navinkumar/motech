package org.motechproject.couch.mrs.impl;

import org.motechproject.couch.mrs.model.CouchMrsUser;
import org.motechproject.couch.mrs.repository.AllCouchMrsUsers;
import org.motechproject.couch.mrs.util.CouchMRSConverterUtil;
import org.motechproject.mrs.domain.MRSUser;
import org.motechproject.mrs.exception.UserAlreadyExistsException;
import org.motechproject.mrs.services.MRSUserAdapter;
import org.motechproject.openmrs.model.Password;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CouchMrsUserAdapter implements MRSUserAdapter {

    private static final int PASSWORD_LENGTH = 8;

    @Autowired
    private AllCouchMrsUsers allCouchMrsUsers;

    @Override
    public void changeCurrentUserPassword(String currentPassword, String newPassword) {

    }

    @Override
    public Map<String, Object> saveUser(MRSUser mrsUser) throws UserAlreadyExistsException {
        CouchMrsUser couchMrsUser = CouchMRSConverterUtil.convertUserToCouchUser(mrsUser);
        allCouchMrsUsers.saveUser(couchMrsUser);
        Map<String, Object> values = new HashMap<String, Object>();
        values.put(USER_KEY, mrsUser);
        values.put(PASSWORD_KEY, new Password(PASSWORD_LENGTH).create());
        return values;
    }

    @Override
    public String setNewPasswordForUser(String userId) {
        return null;
    }

    @Override
    public List<MRSUser> getAllUsers() {
        return null;
    }

    @Override
    public MRSUser getUserByUserName(String userName) {
        return allCouchMrsUsers.getUserByUserName(userName);
    }

    @Override
    public Map<String, Object> updateUser(MRSUser mrsUser) {
        return null;
    }
}