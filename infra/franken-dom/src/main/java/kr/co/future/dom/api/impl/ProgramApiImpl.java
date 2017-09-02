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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import kr.co.future.dom.api.ConfigManager;
import kr.co.future.dom.api.DefaultEntityEventListener;
import kr.co.future.dom.api.DefaultEntityEventProvider;
import kr.co.future.dom.api.EntityEventListener;
import kr.co.future.dom.api.EntityEventProvider;
import kr.co.future.dom.api.MySQLConnectionService;
import kr.co.future.dom.api.ProgramApi;
import kr.co.future.dom.api.Transaction;
import kr.co.future.dom.model.Program;
import kr.co.future.dom.model.ProgramPack;
import kr.co.future.dom.model.ProgramProfile;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import kr.co.future.confdb.ConfigTransaction;
import kr.co.future.confdb.Predicate;
import kr.co.future.confdb.Predicates;
import kr.co.future.dom.api.impl.ProgramApiImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "dom-program-api")
@Provides
public class ProgramApiImpl extends DefaultEntityEventProvider<Program> implements ProgramApi {
	private final Logger logger = LoggerFactory.getLogger(ProgramApiImpl.class.getName());
	
//	private static final Class<ProgramProfile> prof = ProgramProfile.class;
//	private static final String PROF_NOT_FOUND = "program-profile-not-found";
//	private static final String PROF_ALREADY_EXIST = "program-profile-already-exist";
//	private DefaultEntityEventProvider<ProgramProfile> profileEventProvider = new DefaultEntityEventProvider<ProgramProfile>();

//	private static final Class<ProgramPack> pack = ProgramPack.class;
//	private static final String PACK_NOT_FOUND = "program-pack-not-found";
//	private static final String PACK_ALREADY_EXIST = "program-pack-already-exist";
//	private DefaultEntityEventProvider<ProgramPack> packEventProvider = new DefaultEntityEventProvider<ProgramPack>();

//	private static final Class<Program> prog = Program.class;
//	private static final String PROG_NOT_FOUND = "program-not-found";
//	private static final String PROG_ALREADY_EXIST = "program-already-exist";
	
	@Requires
    private MySQLConnectionService connectionService;

//	private EntityEventListener<ProgramPack> packEventListener = new DefaultEntityEventListener<ProgramPack>() {
//		@Override
//		public void entityRemoving(String domain, ProgramPack obj, ConfigTransaction xact, Object state) {
//			List<Program> programs = obj.getPrograms();
//			List<Predicate> preds = new ArrayList<Predicate>();
//			for (Program program : programs) {
//				preds.add(getPred(program.getPack(), program.getName()));
//				program.setPack(null);
//			}
//
//			Transaction x = Transaction.getInstance(xact);
//			cfg.updates(x, prog, preds, programs, null);
//		}
//	};
//
//	private EntityEventListener<Program> programEventListener = new DefaultEntityEventListener<Program>() {
//		@Override
//		public void entityRemoving(String domain, Program obj, ConfigTransaction xact, Object state) {
//			List<Predicate> preds = new ArrayList<Predicate>();
//			List<ProgramProfile> profiles = new ArrayList<ProgramProfile>();
//			for (ProgramProfile profile : getProgramProfiles(domain)) {
//				for (Program program : profile.getPrograms()) {
//					if (program.getPack() == obj.getPack() && program.getName() == obj.getName()) {
//						profile.getPrograms().remove(program);
//						preds.add(getPred(profile.getName()));
//						profiles.add(profile);
//						break;
//					}
//				}
//			}
//
//			Transaction x = Transaction.getInstance(xact);
//			cfg.updates(x, ProgramProfile.class, preds, profiles, null);
//		}
//	};
//
//	@Requires
//	private ConfigManager cfg;
//
//	@Validate
//	public void validate() {
//		packEventProvider.addEntityEventListener(packEventListener);
//		this.addEntityEventListener(programEventListener);
//	}
//
//	@Invalidate
//	public void invalidate() {
//		packEventProvider.removeEntityEventListener(packEventListener);
//		this.removeEntityEventListener(programEventListener);
//	}

//	private Predicate getPred(String name) {
//		return Predicates.field("name", name);
//	}
//
//	private Predicate getPred(String packName, String name) {
//		return Predicates.and(Predicates.field("pack", packName), Predicates.field("name", name));
//	}
//
//	private List<Predicate> getPreds(List<? extends Object> objs) {
//		if (objs == null)
//			return new ArrayList<Predicate>();
//
//		List<Predicate> preds = new ArrayList<Predicate>(objs.size());
//		for (Object obj : objs) {
//			if (obj instanceof ProgramProfile)
//				preds.add(getPred(((ProgramProfile) obj).getName()));
//			else if (obj instanceof ProgramPack)
//				preds.add(getPred(((ProgramPack) obj).getName()));
//			else if (obj instanceof Program)
//				preds.add(getPred(((Program) obj).getName()));
//		}
//		return preds;
//	}

	@Override
	public Collection<ProgramProfile> getProgramProfiles(String domain) {
		String query = "select * from ProgramProfile";
		Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;
        
        ArrayList<ProgramProfile> programProfiles = new ArrayList<ProgramProfile>();
        try {
        	con = connectionService.getConnection();
        	if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }
        	
        	psmt = con.prepareStatement(query);
        	rs = psmt.executeQuery();
        	while (rs.next()) {
        		ProgramProfile programProfile = new ProgramProfile();
        		programProfile.setName(rs.getString("name"));
        		programProfile.setDescription(rs.getString("description"));
        		programProfile.setCreated(toDate(rs.getTimestamp("created")));
        		programProfile.setUpdated(toDate(rs.getTimestamp("updated")));
        		programProfile.setPrograms(getPrograms(rs.getString("name")));
        		
        		programProfiles.add(programProfile);
            }
        } catch (Exception e) {
            logger.error("get programs failed: " + e);
        } finally {
            close(rs, psmt, con);
        }
        return programProfiles;
	}
	
	private List<Program> getPrograms(String programProfileName) {
		String query = "select b.* from ProgramProfileRel a inner join Program b on ? = a.programProfileName and a.programName = b.name;";
		Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;
        
        ArrayList<Program> programs = new ArrayList<Program>();
        try {
        	con = connectionService.getConnection();
        	if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }
        	
        	psmt = con.prepareStatement(query);
        	psmt.setString(1, programProfileName);
        	rs = psmt.executeQuery();
        	while (rs.next()) {
        		Program program = new Program();
        		program.setName(rs.getString("name"));
        		program.setPack(rs.getString("pack"));
        		program.setDescription(rs.getString("description"));
        		program.setPath(rs.getString("path"));
        		program.setVisible(rs.getInt("visible")==1 ? true : false);
        		program.setSeq(rs.getInt("seq"));
        		program.setCreated(toDate(rs.getTimestamp("created")));
        		program.setUpdated(toDate(rs.getTimestamp("updated")));
                programs.add(program);
            }
        } catch (Exception e) {
            logger.error("get programs failed: " + e);
        } finally {
            close(rs, psmt, con);
        }
        return programs;
	}

//	@Override
//	public ProgramProfile findProgramProfile(String domain, String name) {
//		return cfg.find(domain, prof, getPred(name));
//	}
//
	@Override
	public ProgramProfile getProgramProfile(String domain, String name) {
		String query = "select * from ProgramProfile WHERE name = ?";
		Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;
        
        ProgramProfile programProfile = null;
        try {
        	con = connectionService.getConnection();
        	if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }
        	
        	psmt = con.prepareStatement(query);
        	psmt.setString(1, name);
        	rs = psmt.executeQuery();
        	if (rs.next()) {
        		programProfile = new ProgramProfile();
        		programProfile.setName(rs.getString("name"));
        		programProfile.setDescription(rs.getString("description"));
        		programProfile.setCreated(toDate(rs.getTimestamp("created")));
        		programProfile.setUpdated(toDate(rs.getTimestamp("updated")));
        		programProfile.setPrograms(getPrograms(rs.getString("name")));
            }
        } catch (Exception e) {
            logger.error("get programs failed: " + e);
        } finally {
            close(rs, psmt, con);
        }
        return programProfile;
	}
//
//	@Override
//	public void createProgramProfiles(String domain, Collection<ProgramProfile> profiles) {
//		List<ProgramProfile> profileList = new ArrayList<ProgramProfile>(profiles);
//		cfg.adds(domain, prof, getPreds(profileList), profileList, PROF_ALREADY_EXIST, profileEventProvider);
//	}
//
//	@Override
//	public void createProgramProfile(String domain, ProgramProfile profile) {
//		cfg.add(domain, prof, getPred(profile.getName()), profile, PROF_ALREADY_EXIST, profileEventProvider);
//	}
//
//	@Override
//	public void updateProgramProfiles(String domain, Collection<ProgramProfile> profiles) {
//		List<ProgramProfile> profileList = new ArrayList<ProgramProfile>(profiles);
//		for (ProgramProfile profile : profileList)
//			profile.setUpdated(new Date());
//		cfg.updates(domain, prof, getPreds(profileList), profileList, PROF_NOT_FOUND, profileEventProvider);
//	}
//
//	@Override
//	public void updateProgramProfile(String domain, ProgramProfile profile) {
//		profile.setUpdated(new Date());
//		cfg.update(domain, prof, getPred(profile.getName()), profile, PROF_NOT_FOUND, profileEventProvider);
//	}
//
//	@Override
//	public void removeProgramProfiles(String domain, Collection<String> names) {
//		List<Predicate> preds = new ArrayList<Predicate>();
//		for (String name : names)
//			preds.add(getPred(name));
//		cfg.removes(domain, prof, preds, PROF_NOT_FOUND, profileEventProvider);
//	}
//
//	@Override
//	public void removeProgramProfile(String domain, String name) {
//		cfg.remove(domain, prof, getPred(name), PROF_NOT_FOUND, profileEventProvider);
//	}
//
//	@Override
//	public EntityEventProvider<ProgramProfile> getProgramProfileEventProvider() {
//		return profileEventProvider;
//	}

	@Override
	public Collection<ProgramPack> getProgramPacks(String domain) {
		String query = "select * from ProgramPack";
		Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;
        
        ArrayList<ProgramPack> packs = new ArrayList<ProgramPack>();
        try {
        	con = connectionService.getConnection();
        	if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }
        	
        	psmt = con.prepareStatement(query);
        	rs = psmt.executeQuery();
        	while (rs.next()) {
        		ProgramPack programPack = new ProgramPack();
                programPack.setName(rs.getString("name"));
                programPack.setDll(rs.getString("dll"));
                programPack.setDescription(rs.getString("description"));
                programPack.setStarter(rs.getString("starter"));
                programPack.setSeq(rs.getInt("seq"));
                programPack.setCreated(toDate(rs.getTimestamp("created")));
                programPack.setUpdated(toDate(rs.getTimestamp("updated")));
                
                packs.add(programPack);
            }
        } catch (Exception e) {
            logger.error("countUser: " + e);
        } finally {
            close(rs, psmt, con);
        }
        
		for (ProgramPack p : packs)
			p.setPrograms((List<Program>) getPrograms(domain, p.getName()));
		return packs;
	}

//	@Override
//	public ProgramPack findProgramPack(String domain, String name) {
//		ProgramPack p = cfg.find(domain, pack, getPred(name));
//		if (p == null)
//			return null;
//		p.setPrograms((List<Program>) getPrograms(domain, p.getName()));
//		return p;
//	}
//
//	@Override
//	public ProgramPack getProgramPack(String domain, String name) {
//		ProgramPack p = cfg.get(domain, pack, getPred(name), PACK_NOT_FOUND);
//		p.setPrograms((List<Program>) getPrograms(domain, p.getName()));
//		return p;
//	}
//
//	@Override
//	public void createProgramPacks(String domain, Collection<ProgramPack> packs) {
//		List<ProgramPack> packList = new ArrayList<ProgramPack>(packs);
//		cfg.adds(domain, pack, getPreds(packList), packList, PACK_ALREADY_EXIST, packEventProvider);
//	}
//
//	@Override
//	public void createProgramPack(String domain, ProgramPack pack) {
//		cfg.add(domain, ProgramApiImpl.pack, getPred(pack.getName()), pack, PACK_ALREADY_EXIST, packEventProvider);
//	}
//
//	@Override
//	public void updateProgramPacks(String domain, Collection<ProgramPack> packs) {
//		List<ProgramPack> packList = new ArrayList<ProgramPack>(packs);
//		for (ProgramPack pack : packList)
//			pack.setUpdated(new Date());
//		cfg.updates(domain, pack, getPreds(packList), packList, PACK_NOT_FOUND, packEventProvider);
//	}
//
//	@Override
//	public void updateProgramPack(String domain, ProgramPack pack) {
//		pack.setUpdated(new Date());
//		cfg.update(domain, ProgramApiImpl.pack, getPred(pack.getName()), pack, PACK_NOT_FOUND, packEventProvider);
//	}
//
//	@Override
//	public void removeProgramPacks(String domain, Collection<String> names) {
//		List<Predicate> preds = new ArrayList<Predicate>();
//		for (String name : names)
//			preds.add(getPred(name));
//		cfg.removes(domain, pack, preds, PACK_NOT_FOUND, packEventProvider);
//	}
//
//	@Override
//	public void removeProgramPack(String domain, String name) {
//		cfg.remove(domain, ProgramApiImpl.pack, getPred(name), PACK_NOT_FOUND, packEventProvider);
//		for (Program p : getPrograms(domain, name))
//			removeProgram(domain, name, p.getName());
//	}
//
//	@Override
//	public EntityEventProvider<ProgramPack> getProgramPackEventProvider() {
//		return packEventProvider;
//	}

//	@Override
//	public Collection<Program> getPrograms(String domain) {
//		return cfg.all(domain, prog);
//	}

	@Override
	public Collection<Program> getPrograms(String domain, String pack) {
		String query = "select * from Program where pack = ?";
		Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;
        
        ArrayList<Program> programs = new ArrayList<Program>();
        try {
        	con = connectionService.getConnection();
        	if (con == null) {
                logger.error("Can't connect to database. check database status.");
                throw new Exception("database connect fail");
            }
        	
        	psmt = con.prepareStatement(query);
        	psmt.setString(1, pack);
        	rs = psmt.executeQuery();
        	while (rs.next()) {
        		Program program = new Program();
        		program.setName(rs.getString("name"));
        		program.setPack(rs.getString("pack"));
        		program.setDescription(rs.getString("description"));
        		program.setPath(rs.getString("path"));
        		program.setVisible(rs.getInt("visible")==1 ? true : false);
        		program.setSeq(rs.getInt("seq"));
        		program.setCreated(toDate(rs.getTimestamp("created")));
        		program.setUpdated(toDate(rs.getTimestamp("updated")));
                programs.add(program);
            }
        } catch (Exception e) {
            logger.error("get programs failed: " + e);
        } finally {
            close(rs, psmt, con);
        }
        return programs;
	}

//	@Override
//	public Program findProgram(String domain, String pack, String name) {
//		return cfg.find(domain, prog, getPred(pack, name));
//	}
//
//	@Override
//	public Program getProgram(String domain, String pack, String name) {
//		return cfg.get(domain, prog, getPred(pack, name), PROG_NOT_FOUND);
//	}
//
//	@Override
//	public void createPrograms(String domain, Collection<Program> programs) {
//		List<Program> programList = new ArrayList<Program>(programs);
//		cfg.adds(domain, prog, getPreds(programList), programList, PROG_ALREADY_EXIST, this);
//	}
//
//	@Override
//	public void createProgram(String domain, Program program) {
//		cfg.add(domain, prog, getPred(program.getPack(), program.getName()), program, PROG_ALREADY_EXIST, this);
//	}
//
//	@Override
//	public void updatePrograms(String domain, Collection<Program> programs) {
//		List<Program> programList = new ArrayList<Program>(programs);
//		for (Program program : programList)
//			program.setUpdated(new Date());
//		cfg.updates(domain, prog, getPreds(programList), programList, PROG_NOT_FOUND, this);
//	}
//
//	@Override
//	public void updateProgram(String domain, Program program) {
//		program.setUpdated(new Date());
//		cfg.update(domain, prog, getPred(program.getPack(), program.getName()), program, PROG_NOT_FOUND, this);
//	}
//
//	@Override
//	public void removePrograms(String domain, Collection<String> names) {
//		List<Predicate> preds = new ArrayList<Predicate>();
//		for (String name : names)
//			preds.add(getPred(name));
//		cfg.removes(domain, prog, preds, PROG_NOT_FOUND, this);
//	}
//
//	@Override
//	public void removeProgram(String domain, String packName, String name) {
//		cfg.remove(domain, prog, getPred(packName, name), PROG_NOT_FOUND, this);
//	}
	
	private void close(ResultSet rs, PreparedStatement psmt, Connection con) {
        logger.trace("frodo-sql-auth-service: close sql jdbc");
        if(rs != null){
            try {
                rs.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if (psmt != null) {
            try {
                psmt.close();
            } catch (SQLException e) {
            }
        }
        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
            }
        }
    }
	
	private static java.util.Date toDate(java.sql.Timestamp timestamp) {
        long milliseconds = timestamp.getTime() + (timestamp.getNanos() / 1000000);
        return new java.util.Date(milliseconds);
    }
}
