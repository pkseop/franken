/*
 * Copyright 2011 Future Systems, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kr.co.future.dom.api.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import kr.co.future.dom.api.ConfigManager;
import kr.co.future.dom.api.DOMException;
import kr.co.future.dom.api.DefaultEntityEventProvider;
import kr.co.future.dom.api.MySQLConnectionService;
import kr.co.future.dom.api.OrganizationUnitApi;
import kr.co.future.dom.model.OrganizationUnit;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import kr.co.future.dom.api.impl.OrganizationUnitApiImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "dom-org-unit-api")
@Provides
public class OrganizationUnitApiImpl extends DefaultEntityEventProvider<OrganizationUnit> implements OrganizationUnitApi {
    private final Logger logger = LoggerFactory.getLogger(OrganizationUnitApiImpl.class.getName());

//	private static final Class<OrganizationUnit> cls = OrganizationUnit.class;
	private static final String NOT_FOUND = "org-unit-not-found";
//	private static final String ALREADY_EXIST = "org-unit-already-exist";

	@Requires
	private ConfigManager cfg;

    @Requires
    private MySQLConnectionService connectionService;
    
//	private Predicate getPred(String guid) {
//		return Predicates.field("guid", guid);
//	}

//	private List<Predicate> getPreds(List<OrganizationUnit> orgUnits) {
//		if (orgUnits == null)
//			return new ArrayList<Predicate>();
//
//		List<Predicate> preds = new ArrayList<Predicate>(orgUnits.size());
//		for (OrganizationUnit orgUnit : orgUnits)
//			preds.add(getPred(orgUnit.getGuid()));
//		return preds;
//	}

	@Override
	public Collection<OrganizationUnit> getOrganizationUnits(String domain) {
		return getOrganizationUnits(domain, true);
	}

	@Override
	public Collection<OrganizationUnit> getOrganizationUnits(String domain, boolean includeChildren) {
        Collection<OrganizationUnit> orgUnits = new ArrayList<OrganizationUnit>();

        String query = "SELECT * FROM OrganizationUnit ORDER BY name";

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }
            
            HashMap<String, OrganizationUnit> map = new HashMap<String, OrganizationUnit>();

            psmt = con.prepareStatement(query);
            rs = psmt.executeQuery();
            // 결과값 반환
            while (rs.next()) {
                OrganizationUnit ou = new OrganizationUnit();
                
                String guid = rs.getString("guid");
                ou.setGuid(guid);
                ou.setName(rs.getString("name"));
                ou.setParent(rs.getString("parent"));
                ou.setName_desc(rs.getString("description"));
                ou.setSourceType(rs.getString("source_type"));
                ou.setUpdated(toDate(rs.getTimestamp("updated")));
                ou.setCreated(toDate(rs.getTimestamp("created")));

                orgUnits.add(ou);

                logger.debug("fetched org unit guid: [{}] name: [{}]", new Object[]{ou.getGuid(), ou.getName()});
                
                if(includeChildren) {
                	if(!map.containsKey(guid))
                		map.put(guid, ou);
                }
            }
            
            if(includeChildren) {
            	for(OrganizationUnit ou : orgUnits) {
            		if(ou.getParent() != null) {
	            		OrganizationUnit orgUnit = map.get(ou.getParent());
	            		orgUnit.getChildren().add(ou);
	            	} 
            	}
            }
        } catch (Exception e) {
            logger.error("failed to get org units", e);
        } finally {
            close(rs, psmt, con);
        }

        logger.debug("includeChildren: [{}]", includeChildren);

        return orgUnits;
    }

    public static java.util.Date toDate(java.sql.Timestamp timestamp) {
        long milliseconds = timestamp.getTime() + (timestamp.getNanos() / 1000000);
        return new java.util.Date(milliseconds);
    }

	@Override
	public OrganizationUnit findOrganizationUnit(String domain, String guid) {
		OrganizationUnit orgUnit = null;

        String query = "SELECT * FROM OrganizationUnit WHERE guid = ?";

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, guid);
            rs = psmt.executeQuery();

            // 결과값 반환
            while (rs.next()) {
                orgUnit = new OrganizationUnit();
                orgUnit.setGuid(rs.getString("guid"));
                orgUnit.setName(rs.getString("name"));
                orgUnit.setName_desc(rs.getString("description"));
                orgUnit.setParent(rs.getString("parent"));
                orgUnit.setSourceType(rs.getString("source_type"));
                orgUnit.setUpdated(toDate(rs.getTimestamp("updated")));
                orgUnit.setCreated(toDate(rs.getTimestamp("created")));
            }
        } catch (Exception e) {
        	logger.error("find org unit failed", e);
        } finally {
            close(rs, psmt, con);
        }

		if (orgUnit == null)
			return null;

		orgUnit.setChildren(getChildrens(domain, orgUnit.getGuid()));

		return orgUnit;
	}

	@Override
	public OrganizationUnit getOrganizationUnit(String domain, String guid) {
        OrganizationUnit orgUnit = null;

        String query = "SELECT * FROM OrganizationUnit WHERE guid = ?";

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, guid);
            rs = psmt.executeQuery();

            while (rs.next()) {
                orgUnit = new OrganizationUnit();
                orgUnit.setGuid(rs.getString("guid"));
                orgUnit.setName(rs.getString("name"));
                orgUnit.setName_desc(rs.getString("description"));
                orgUnit.setParent(rs.getString("parent"));
                orgUnit.setSourceType(rs.getString("source_type"));
                orgUnit.setUpdated(toDate(rs.getTimestamp("updated")));
                orgUnit.setCreated(toDate(rs.getTimestamp("created")));
            }
        } catch (Exception e) {
        	logger.error("get org unit failed", e);
        } finally {
            close(rs, psmt, con);
        }

        if (orgUnit == null)
            throw new DOMException(NOT_FOUND);

        orgUnit.setChildren(getChildrens(domain, orgUnit.getGuid()));

		return orgUnit;
	}
	
	@Override
	public Collection<String> getGuidsByParent(String domain, String parentGuid) {
		Collection<String> guids = new ArrayList<String>();
		guids.add(parentGuid);		//본 부서의 guid를 포함시킴
		
		HashMap<String, ArrayList<String>> map = new HashMap<String, ArrayList<String>>(); 
		
        String query = "SELECT guid, parent FROM OrganizationUnit WHERE parent IS NOT NULL";

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            rs = psmt.executeQuery();

            while (rs.next()) {
            	String parent = rs.getString("parent");
            	String guid = rs.getString("guid");
            	if(map.containsKey(parent)) {
            		map.get(parent).add(guid);
            	} else {
            		ArrayList<String> list = new ArrayList<String>();
            		list.add(guid);
            		map.put(parent, list);
            	}
            }
            retrieveChildGuids(guids, map, parentGuid);
        } catch (Exception e) {
        	logger.error("get org unit failed", e);
        } finally {
            close(rs, psmt, con);
        }
		return guids;
	}
	
	private void retrieveChildGuids(Collection<String> guids, HashMap<String, ArrayList<String>> map, String parentGuid) {
		if(map.containsKey(parentGuid)) {
			ArrayList<String> list = map.get(parentGuid);
			guids.addAll(list);
			for(String guid : list) {
				retrieveChildGuids(guids, map, guid);
			}
		}
	}

	@Override
	public OrganizationUnit findOrganizationUnitByName(String domain, String... names) {
		OrganizationUnit orgUnit = null;

		for (String name : names) {
            String query = "SELECT * FROM OrganizationUnit WHERE name = ?";

            Connection con = null;
            PreparedStatement psmt = null;
            ResultSet rs = null;

            try {
                con = connectionService.getConnection();

                if (con == null) {
                    logger.error("Can't connect to database. check database status.");
                    throw new Exception("database connect fail");
                }

                psmt = con.prepareStatement(query);
                psmt.setString(1, name);
                rs = psmt.executeQuery();

                while (rs.next()) {
                    orgUnit = new OrganizationUnit();
                    orgUnit.setGuid(rs.getString("guid"));
                    orgUnit.setName(rs.getString("name"));
                    orgUnit.setName_desc(rs.getString("description"));
                    orgUnit.setParent(rs.getString("parent"));
                    orgUnit.setSourceType(rs.getString("source_type"));
                    orgUnit.setUpdated(toDate(rs.getTimestamp("updated")));
                    orgUnit.setCreated(toDate(rs.getTimestamp("created")));
                }
            } catch (Exception e) {
            	logger.error("find org unit by name failed", e);
            } finally {
                close(rs, psmt, con);
            }

			if (orgUnit == null)
				return null;
//			parentGuid = orgUnit.getGuid();
		}
		orgUnit.setChildren(getChildrens(domain, orgUnit.getGuid()));

		return orgUnit;
	}

	private List<OrganizationUnit> getChildrens(String domain, String guid) {
        String childrenQurey = "SELECT * FROM OrganizationUnit WHERE parent = ? ORDER BY name";

        List<OrganizationUnit> childrenOrgUnit = new ArrayList<OrganizationUnit>();
        Connection con2 = null;
        PreparedStatement psmt2 = null;
        ResultSet rs2 = null;

        try {
            con2 = connectionService.getConnection();

            if (con2 == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt2 = con2.prepareStatement(childrenQurey);
            psmt2.setString(1, guid);
            rs2 = psmt2.executeQuery();

            while (rs2.next()) {
                OrganizationUnit childrenOU = new OrganizationUnit();
                childrenOU.setGuid(rs2.getString("guid"));
                childrenOU.setName(rs2.getString("name"));
                childrenOU.setParent(rs2.getString("parent"));
                childrenOU.setName_desc(rs2.getString("description"));
                childrenOU.setSourceType(rs2.getString("source_type"));
                childrenOU.setUpdated(toDate(rs2.getTimestamp("updated")));
                childrenOU.setCreated(toDate(rs2.getTimestamp("created")));
                childrenOU.setChildren(getChildrens(domain, rs2.getString("guid")));

                childrenOrgUnit.add(childrenOU);
                logger.debug("childrenOU.getGuid(): [{}] , childrenOU.getName(): [{}]", new Object[]{childrenOU.getGuid(), childrenOU.getName()});
            }
        } catch (Exception e) {
        	logger.error("get childrens failed", e);
        } finally {
            close(rs2, psmt2, con2);
        }

		return childrenOrgUnit;
	}

	@Override
	public void createOrganizationUnits(String domain, Collection<OrganizationUnit> orgUnits) {
		List<OrganizationUnit> orgUnitList = new ArrayList<OrganizationUnit>(orgUnits);

        String query = "INSERT INTO OrganizationUnit (guid, name, description, parent, source_type, created, updated) VALUES (?,?,?,?,?,?,?)";

        int insertCnt = 0;

        for (OrganizationUnit organizationUnit : orgUnitList) {
            Connection con = null;
            PreparedStatement psmt = null;

            try {
                con = connectionService.getConnection();

                if (con == null) {
                    logger.error("Can't connect to database. check database status.");
                    throw new Exception("database connect fail");
                }

                psmt = con.prepareStatement(query);
                psmt.setString(1, organizationUnit.getGuid());
                psmt.setString(2, organizationUnit.getName());
                psmt.setString(3, organizationUnit.getName_desc());
                psmt.setString(4, organizationUnit.getParent());
                psmt.setString(5, organizationUnit.getSourceType());
                psmt.setTimestamp(6, getCurrentTimeStamp());
                psmt.setTimestamp(7, getCurrentTimeStamp());

                insertCnt = insertCnt + psmt.executeUpdate();
                
                fireEntityAdded("localhost", organizationUnit);
            } catch (SQLException e) {
                logger.error("OrganizationUnit info insert error", e);
            } catch (Exception e) {
                logger.error("OrganizationUnit info insert error", e);
            } finally {
                close(psmt, con);
            }
        }

        logger.debug("OrganizationUnit info insert success. count: [{}]", insertCnt);
	}

	@Override
	public void createOrganizationUnit(String domain, OrganizationUnit orgUnit) {
        String query = "INSERT INTO OrganizationUnit (guid, name, description, parent, source_type, created, updated) VALUES (?,?,?,?,?,?,?)";

        Connection con = null;
        PreparedStatement psmt = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, orgUnit.getGuid());
            psmt.setString(2, orgUnit.getName());
            psmt.setString(3, orgUnit.getName_desc());
            psmt.setString(4, orgUnit.getParent());
            psmt.setString(5, orgUnit.getSourceType());
            psmt.setTimestamp(6, getCurrentTimeStamp());
            psmt.setTimestamp(7, getCurrentTimeStamp());

            psmt.executeUpdate();
            
            fireEntityAdded("localhost", orgUnit);
        } catch (SQLException e) {
            logger.error("OrganizationUnit info insert error", e);
        } catch (Exception e) {
            logger.error("OrganizationUnit info insert error", e);
        } finally {
            close(psmt, con);
        }
	}

	@Override
	public void updateOrganizationUnits(String domain, Collection<OrganizationUnit> orgUnits) {
        List<OrganizationUnit> orgUnitList = new ArrayList<OrganizationUnit>(orgUnits);

        String query = "UPDATE OrganizationUnit SET name = ?, description = ?, parent = ?, source_type = ?, updated = ? WHERE guid = ?";

        int updateCnt = 0;

        for (OrganizationUnit organizationUnit : orgUnitList) {
            Connection con = null;
            PreparedStatement psmt = null;

            try {
                con = connectionService.getConnection();

                if (con == null) {
                    logger.error("Can't connect to database. check database status.");
                    throw new Exception("database connect fail");
                }

                psmt = con.prepareStatement(query);
                psmt.setString(1, organizationUnit.getName());
                psmt.setString(2, organizationUnit.getName_desc());
                psmt.setString(3, organizationUnit.getParent());
                psmt.setString(4, organizationUnit.getSourceType());
                psmt.setTimestamp(5, getCurrentTimeStamp());
                psmt.setString(6, organizationUnit.getGuid());

                updateCnt = updateCnt + psmt.executeUpdate();
                
                fireEntityUpdated("localhost", organizationUnit);
            } catch (SQLException e) {
                logger.error("OrganizationUnit info update error", e);
            } catch (Exception e) {
                logger.error("OrganizationUnit info update error", e);
            } finally {
                close(psmt, con);
            }
        }

        logger.debug("OrganizationUnit info update success. count: [{}]",  updateCnt);
	}

	@Override
	public void updateOrganizationUnit(String domain, OrganizationUnit orgUnit) {
        String query = "UPDATE OrganizationUnit SET name = ?, description = ?, parent = ?, source_type = ?, updated = ? WHERE guid = ?";

        Connection con = null;
        PreparedStatement psmt = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, orgUnit.getName());
            psmt.setString(2, orgUnit.getName_desc());
            psmt.setString(3, orgUnit.getParent());
            psmt.setString(4, orgUnit.getSourceType());
            psmt.setTimestamp(5, getCurrentTimeStamp());
            psmt.setString(6, orgUnit.getGuid());

            psmt.executeUpdate();
            
            fireEntityUpdated("localhost", orgUnit);
        } catch (SQLException e) {
            logger.error("OrganizationUnit info update error", e);
        } catch (Exception e) {
            logger.error("OrganizationUnit info update error", e);
        } finally {
            close(psmt, con);
        }
	}

	@Override
	public void removeOrganizationUnits(String domain, Collection<String> guids) {
		removeOrganizationUnits(domain, guids, false);
	}

	@Override
	public void removeOrganizationUnits(String domain, Collection<String> guids, boolean moveUser) {
        String query = "DELETE FROM OrganizationUnit WHERE guid = ?";

        int deleteCnt = 0;
        
        List<OrganizationUnit> orgUnits = new ArrayList<OrganizationUnit>();
        for (String guid : guids) {
        	List<OrganizationUnit> orgUnits2 = getOrganizationUnitTree(getOrganizationUnit(domain, guid));
        	orgUnits.addAll(orgUnits2);
		}

        for (OrganizationUnit orgUnit : orgUnits) {
            Connection con = null;
            PreparedStatement psmt = null;
            ResultSet rs = null;

            try {
                con = connectionService.getConnection();

                if (con == null) {
                    logger.error("Can't connect to database. check database status.");
                    throw new Exception("database connect fail");
                }

                psmt = con.prepareStatement(query);
                psmt.setString(1, orgUnit.getGuid());
                psmt.executeUpdate();
                
                fireEntityRemoved("localhost", orgUnit);
            } catch (SQLException e) {
                logger.error("OrganizationUnit info delete error", e);
            } catch (Exception e) {
                logger.error("OrganizationUnit info delete error", e);
            } finally {
                close(rs, psmt, con);
            }
        }

        logger.debug("OrganizationUnit info delete success. count: [{}]", deleteCnt);
	}

	@Override
	public void removeOrganizationUnit(String domain, String guid) {
		removeOrganizationUnit(domain, guid, false);
	}

	@Override
	public void removeOrganizationUnit(String domain, String guid, boolean moveUser) {
        String query = "DELETE FROM OrganizationUnit WHERE guid = ?";
        
        List<OrganizationUnit> childOrgUnits = getOrganizationUnitTree(getOrganizationUnit(domain, guid));

        for (OrganizationUnit organizationUnit : childOrgUnits) {
            Connection con = null;
            PreparedStatement psmt = null;
            ResultSet rs = null;

            try {
                con = connectionService.getConnection();

                if (con == null) {
                    logger.error("Can't connect to database. check database status.");
                    throw new Exception("database connect fail");
                }

                psmt = con.prepareStatement(query);
                psmt.setString(1, organizationUnit.getGuid());
                psmt.executeUpdate();
                
                fireEntityRemoved("localhost", organizationUnit);
            } catch (SQLException e) {
                logger.error("OrganizationUnit info delete error", e);
            } catch (Exception e) {
                logger.error("OrganizationUnit info delete error", e);
            } finally {
                close(rs, psmt, con);
            }
        }
	}
	
	@Override
	public String getOrgUnitNameByGuid(String guid) {
		String query = "SELECT name FROM OrganizationUnit WHERE guid = ?";

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        String result = null;
        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, guid);
            rs = psmt.executeQuery();

            if (rs.next()) {
                result = rs.getString(1);
            }
        } catch (Exception e) {
            logger.error("getOrgUnitName: ", e);
        } finally {
            close(rs, psmt, con);
        }

        return result;
	}

	private List<OrganizationUnit> getOrganizationUnitTree(OrganizationUnit orgUnit) {
		List<OrganizationUnit> orgUnits = new ArrayList<OrganizationUnit>();
		for (OrganizationUnit child : orgUnit.getChildren()) {
			orgUnits.addAll(getOrganizationUnitTree(child));
		}
		orgUnits.add(orgUnit);
		return orgUnits;
	}

    private static Timestamp getCurrentTimeStamp() {
        Date today = new Date();
        return new Timestamp(today.getTime());
    }

    private void close(PreparedStatement psmt, Connection con) {
        logger.trace("dom-org-unit-api: close sql jdbc");
        if (psmt != null)
            try {
                psmt.close();
            } catch (SQLException e) {
            	logger.error("error occurred during closing PreparedStatement", e);
            }
        if (con != null)
            try {
                con.close();
            } catch (SQLException e) {
            	logger.error("error occurred during closing Connection", e);
            }
    }

    private void close(ResultSet rs, PreparedStatement psmt, Connection con) {
        logger.trace("dom-org-unit-api: close sql jdbc");
        if(rs != null){
            try {
                rs.close();
            } catch (SQLException e) {
            	logger.error("error occurred during closing ResultSet", e);
            }
        }
        if (psmt != null) {
            try {
                psmt.close();
            } catch (SQLException e) {
            	logger.error("error occurred during closing PreparedStatement", e);
            }
        }
        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
            	logger.error("error occurred during closing Connection", e);
            }
        }
    }
}
