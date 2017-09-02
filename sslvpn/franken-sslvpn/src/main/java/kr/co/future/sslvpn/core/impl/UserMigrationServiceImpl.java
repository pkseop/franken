package kr.co.future.sslvpn.core.impl;

import kr.co.future.sslvpn.core.UserMigrationServiceApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import kr.co.future.confdb.Predicates;
import kr.co.future.dom.api.ConfigManager;
import kr.co.future.dom.api.DOMException;
import kr.co.future.dom.api.OrganizationUnitApi;
import kr.co.future.dom.api.UserApi;
import kr.co.future.dom.model.OrganizationUnit;
import kr.co.future.dom.model.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by CRChoi on 2015-05-14.
 */
@Component(name = "frodo-user-migration")
@Provides
public class UserMigrationServiceImpl implements UserMigrationServiceApi {
    private final Logger logger = LoggerFactory.getLogger(UserMigrationServiceImpl.class.getName());
    private static final Class<User> userClass = User.class;
    private static final Class<OrganizationUnit> organizationUnitClass = OrganizationUnit.class;

    private static final String LOCALHOST = "localhost";

    @Requires
    private ConfigManager cfg;

    @Requires
    private UserApi userApi;

    @Requires
    private OrganizationUnitApi organizationUnitApi;

    @Override
    public Map<String, Integer> startMigration() {
        Map<String, Integer> result = new HashMap<String, Integer>();

        Collection<User> confdbUserList =  cfg.all("localhost", userClass);

        logger.info("confdbUserList.size(): " + confdbUserList.size());

        int createUserCnt = 0;

        for (User user : confdbUserList) {
            if (!user.getLoginName().equals("admin")) {
                if (userApi.getUser(LOCALHOST, user.getLoginName()) == null) {
                    userApi.createUser(LOCALHOST, user, true);
                    createUserCnt++;
                    logger.info(user.getLoginName() + " is create on MySQL user table.");
                } else {
                    logger.info(user.getLoginName() + " is exist on MySQL user table. skip create user.");
                }

            }
        }

        logger.info(createUserCnt + " users create on MySQL user table");
        result.put("userCnt", createUserCnt);

        Collection<OrganizationUnit> orgUnits = cfg.all(LOCALHOST, organizationUnitClass);

        for (OrganizationUnit orgUnit : orgUnits)
            orgUnit.setChildren(getChildrens(LOCALHOST, orgUnit.getGuid()));

        int createOrgUnitCnt = 0;

        for (OrganizationUnit organizationUnit : orgUnits) {
            logger.info(organizationUnit.getGuid() + ":" + organizationUnit.getName());

            OrganizationUnit unit = null;

            try {
                unit = organizationUnitApi.findOrganizationUnit(LOCALHOST, organizationUnit.getGuid());
            } catch (DOMException ex) {

            }

            if ( unit == null) {
                organizationUnitApi.createOrganizationUnit(LOCALHOST, organizationUnit);
                createOrgUnitCnt++;
                logger.info(organizationUnit.getName() + " is create on MySQL organizationUnit table.");
            } else {
                logger.info(organizationUnit.getName() + " is exist on MySQL organizationUnit table. skip create organizationUnit.");
            }
        }

        logger.info(createOrgUnitCnt + " organizationUnits create on MySQL organizationUnit table");
        result.put("orgUnitCnt", createOrgUnitCnt);

        return result;
    }

    private List<OrganizationUnit> getChildrens(String domain, String guid) {
        Collection<OrganizationUnit> orgUnits = cfg.all(domain, organizationUnitClass, Predicates.field("parent", guid));
        for (OrganizationUnit orgUnit : orgUnits)
            orgUnit.setChildren(getChildrens(domain, orgUnit.getGuid()));
        return (List<OrganizationUnit>) orgUnits;
    }
}
