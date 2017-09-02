package kr.co.future.sslvpn.core.impl;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import kr.co.future.api.PrimitiveParseCallback;
import kr.co.future.confdb.*;
import kr.co.future.dom.api.MySQLConnectionService;
import kr.co.future.dom.api.UserApi;
import kr.co.future.dom.model.*;
import kr.co.future.sslvpn.core.impl.InitialDomSchema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.Date;

@Component(name = "init-dom-schema")
@Provides
public class InitialDomSchema {
    final Logger logger = LoggerFactory.getLogger(InitialDomSchema.class.getName());

    @Requires
    private MySQLConnectionService connectionService;

	public void generate(ConfigService conf, UserApi domUserApi) {
		final Logger logger = LoggerFactory.getLogger(InitialDomSchema.class.getName());
		ConfigDatabase db = conf.ensureDatabase("kraken-dom-localhost");

		try {
			Organization org = createOrganization(db);
			ProgramPack systemPack = createSystemPack(db, org);
			ProgramPack frodoPack = createFrodoPack(db, org);

			ProgramProfile all = createAllProgramProfile(db, org, conf);
			ProgramProfile mon = createMonitorProgramProfile(db, org, conf);
			ProgramProfile acc = createAccountProgramProfile(db, org, conf);
			ProgramProfile dev = createDeviceProgramProfile(db, org, conf);

			boolean isSysModified = createSystemPrograms(db, systemPack, all, mon, acc);
			boolean isFrodoModified = createFrodoPrograms(db, frodoPack, all, mon, acc, dev);

			if (isFrodoModified || isSysModified) {
				// update profile
                programProfileUpdate(all);
                programProfileUpdate(mon);
                programProfileUpdate(acc);
                programProfileUpdate(dev);

				logger.info("kraken dom: updated program profile system package [{}], frodo package [{}]", isSysModified,
						isFrodoModified);
			}

			Role master = createRoles(db);
			createAdminUser(db, org, master, all, domUserApi);
            createSmcMonUser(db, org, getRole("member", 2), mon, domUserApi);
		} catch (Exception e) {
			logger.error("kraken dom: schema init failed", e);
			throw new RuntimeException("cannot create schema", e);
		}
	}

    private void programProfileUpdate(ProgramProfile programProfile) {
        List<Program> programList = programProfile.getPrograms();
        String programProfileName = programProfile.getName();

        resetProgramProfileRel(programProfileName);

        for (Program program : programList) {
            insertProgramProfileRel(programProfileName, program.getName());
        }
    }

    private void insertProgramProfileRel(String programProfileName, String programName) {
        String query = "INSERT INTO ProgramProfileRel (programProfileName, programName) VALUES (?,?)";

        Connection con = null;
        PreparedStatement psmt = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, programProfileName);
            psmt.setString(2, programName);

            psmt.executeUpdate();
        } catch (SQLException e) {
        	logger.error("error during insertProgramProfileRel", e);
        } catch (Exception e) {
        	logger.error("error during insertProgramProfileRel", e);
        } finally {
            close(psmt, con);
        }
    }

    private void resetProgramProfileRel(String programProfileName) {
        String query = "DELETE FROM ProgramProfileRel WHERE programProfileName = ?";

        Connection con = null;
        PreparedStatement psmt = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, programProfileName);

            psmt.executeUpdate();
        } catch (SQLException e) {
        	logger.error("error during resetProgramProfileRel", e);
        } catch (Exception e) {
        	logger.error("error during resetProgramProfileRel", e);
        } finally {
            close(psmt, con);
        }
    }

	public static Organization createOrganization(ConfigDatabase db) {
		Config c = db.findOne(Organization.class, null);
		if (c != null)
			return c.getDocument(Organization.class);

		Organization org = new Organization();
		org.setDomain("localhost");
		org.setName("SSLplus");
		org.setCreated(new Date());
		org.setEnabled(true);
		org.getParameters().put("default_program_profile_id", "all");
		org.getParameters().put("dom.admin.trust_hosts", "");
		org.getParameters().put("dom.admin.users_count", "0");
		
		db.add(org);
		return org;
	}

	public static void createAdminUser(ConfigDatabase db, Organization org, Role master, ProgramProfile all, UserApi domUserApi) {
		User user = domUserApi.getMasterUser();
		if (user != null)
			return;

		user = new User();
		user.setLoginName("admin");
		user.setName("admin");
		user.setSourceType("local");
		user.setPassword("9e774f8c5bd3a6ea23ba56da79fc176245423e086ad6982e60a70fef66efc6cf");

		Admin admin = new Admin();
		admin.setUseLoginLock(true);
		admin.setUseIdleTimeout(true);
		admin.setLoginLockCount(5);
		admin.setIdleTimeout(3600);
		admin.setRole(master);
		admin.setProfile(all);
		admin.setLang("ko");
		admin.setEnabled(true);
		user.getExt().put("admin", admin);

		domUserApi.createUser("localhost", user, true);
	}

    public static void createSmcMonUser(ConfigDatabase db, Organization org, Role member, ProgramProfile mon, UserApi domUserApi) {
        User user = domUserApi.findUser("localhost", "smcmon");
        if (user != null)
            return;

        user = new User();
        user.setLoginName("smcmon");
        user.setName("smcmon");
        user.setSourceType("local");
        user.setPassword("9e774f8c5bd3a6ea23ba56da79fc176245423e086ad6982e60a70fef66efc6cf");

        Admin admin = new Admin();
        admin.setUseLoginLock(true);
        admin.setUseIdleTimeout(true);
        admin.setLoginLockCount(5);
        admin.setIdleTimeout(Integer.MAX_VALUE);
        admin.setRole(member);
        admin.setProfile(mon);
        admin.setLang("ko");
        admin.setEnabled(true);
        user.getExt().put("admin", admin);

        domUserApi.createUser("localhost", user, true);
    }

    private ProgramProfile createProgramProfile(String name) {
        String query = "INSERT INTO ProgramProfile (name, description, created, updated) VALUES (?,?,?,?)";

        Connection con = null;
        PreparedStatement psmt = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, name);
            psmt.setString(2, null);
            psmt.setTimestamp(3, getCurrentTimeStamp());
            psmt.setTimestamp(4, getCurrentTimeStamp());

            psmt.executeUpdate();
        } catch (SQLException e) {
        	logger.error("error during createProgramProfile", e);
        } catch (Exception e) {
        	logger.error("error during createProgramProfile", e);
        } finally {
            close(psmt, con);
        }

        return null;
    }


    private ProgramProfile getProgramProfile(String name) {
        String query = "SELECT * FROM ProgramProfile WHERE name = ?";

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                return null;
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, name);

            rs = psmt.executeQuery();

            if (rs.next()) {
                ProgramProfile programProfile = new ProgramProfile();
                programProfile.setName(rs.getString("name"));
                programProfile.setDescription(rs.getString("description"));

                return programProfile;
            }
        } catch (Exception e) {
        	logger.error("error during getProgramProfile", e);
        } finally {
            close(rs, psmt, con);
        }

        return null;
    }

	public ProgramProfile createAllProgramProfile(ConfigDatabase db, Organization org, ConfigService conf) {
        ProgramProfile programProfile = getProgramProfile("all");

		if (programProfile != null)
			return programProfile;

		ProgramProfile pp = new ProgramProfile();
		pp.setName("all");
       
		createProgramProfile(pp.getName());

		return pp;
	}

	private ProgramProfile createMonitorProgramProfile(ConfigDatabase db, Organization org, ConfigService conf) {
        ProgramProfile programProfile = getProgramProfile("모니터링");

        if (programProfile != null)
            return programProfile;

		ProgramProfile pp = new ProgramProfile();
		pp.setName("모니터링");
      
		createProgramProfile(pp.getName());

		return pp;
	}

	private ProgramProfile createAccountProgramProfile(ConfigDatabase db, Organization org, ConfigService conf) {
        ProgramProfile programProfile = getProgramProfile("계정 관리");

        if (programProfile != null)
            return programProfile;

		ProgramProfile pp = new ProgramProfile();
		pp.setName("계정 관리");

		createProgramProfile(pp.getName());

		return pp;
	}

	private ProgramProfile createDeviceProgramProfile(ConfigDatabase db, Organization org, ConfigService conf) {
        ProgramProfile programProfile = getProgramProfile("단말 관리");

        if (programProfile != null)
            return programProfile;

		ProgramProfile pp = new ProgramProfile();
		pp.setName("단말 관리");

		createProgramProfile(pp.getName());

		return pp;
	}

	private static PrimitiveParseCallback newParseCallback(final ConfigService conf) {
		return new PrimitiveParseCallback() {
			@Override
			public <T> T onParse(Class<T> clazz, Map<String, Object> referenceKey) {
				ConfigDatabase db = conf.ensureDatabase("kraken-dom-localhost");
				Config c = db.findOne(clazz, Predicates.field(referenceKey));
				if (c == null)
					return null;
				return c.getDocument(clazz, newParseCallback(conf));
			}
		};
	}
	
	private ProgramPack getProgramPack(String name) {
        String query = "SELECT * FROM ProgramPack WHERE name = ?";

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                return null;
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, name);

            rs = psmt.executeQuery();

            if (rs.next()) {
                ProgramPack programPack = new ProgramPack();
                programPack.setName(rs.getString("name"));
                programPack.setDll(rs.getString("dll"));
                programPack.setDescription(rs.getString("description"));
                programPack.setStarter(rs.getString("starter"));
                programPack.setSeq(rs.getInt("seq"));
                programPack.setCreated(toDate(rs.getTimestamp("created")));
                programPack.setUpdated(toDate(rs.getTimestamp("updated")));

                return programPack;
            }
        } catch (Exception e) {
        	logger.error("error during getProgramPack", e);
        } finally {
            close(rs, psmt, con);
        }

        return null;
    }
	
	private ProgramProfile createProgramPack(ProgramPack pack) {
        String query = "INSERT INTO ProgramPack (name, dll, description, starter, seq, created, updated) VALUES (?,?,?,?,?,?,?)";

        Connection con = null;
        PreparedStatement psmt = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, pack.getName());
            psmt.setString(2, pack.getDll());
            psmt.setString(3,  pack.getDescription());
            psmt.setString(4, pack.getStarter());
            psmt.setInt(5, pack.getSeq());
            psmt.setTimestamp(6, getCurrentTimeStamp());
            psmt.setTimestamp(7, getCurrentTimeStamp());

            psmt.executeUpdate();
        } catch (SQLException e) {
        	logger.error("error during createProgramPack", e);
        } catch (Exception e) {
        	logger.error("error during createProgramPack", e);
        } finally {
            close(psmt, con);
        }

        return null;
    }

	public ProgramPack createSystemPack(ConfigDatabase db, Organization org) {
		ProgramPack pack = getProgramPack("System");
		if(pack != null)
			return pack;

		pack = new ProgramPack();
		pack.setName("System");
		pack.setDll("Nchovy.WatchCat.Plugins.Core.dll");
		pack.setSeq(1);
		
		createProgramPack(pack);

		return pack;
	}

	public ProgramPack createFrodoPack(ConfigDatabase db, Organization org) {
		ProgramPack pack = getProgramPack("Frodo");
		if(pack != null)
			return pack;

		pack = new ProgramPack();
		pack.setName("Frodo");
		pack.setDll("FutureSystems.Frodo.dll");
		pack.setSeq(2);

		createProgramPack(pack);

		return pack;
	}

	public boolean createSystemPrograms(ConfigDatabase db, ProgramPack pack, ProgramProfile all, ProgramProfile mon,
			ProgramProfile acc) {
		boolean isModified = false;
		Program p1 = createProgram(db, pack, "Account Manager", "Nchovy.WatchCat.Plugins.Core.AccountManager.AccountPlugin", 1);
		Program p2 = createProgram(db, pack, "Object Manager", "Nchovy.WatchCat.Plugins.Core.ObjectManager.ObjectPlugin", 2);
		Program p3 = createProgram(db, pack, "Task Manager", "Nchovy.WatchCat.Plugins.Core.TaskManager.TaskManager", 3);
		Program p4 = createProgram(db, pack, "Run", "Nchovy.WatchCat.Plugins.Core.Run.Run", 4);

		if (!all.getPrograms().containsAll(Arrays.asList(p1, p2, p3, p4))) {
			all.getPrograms().removeAll(Arrays.asList(p1, p2, p3, p4));
			all.getPrograms().add(p1);
			all.getPrograms().add(p2);
			all.getPrograms().add(p3);
			all.getPrograms().add(p4);
			isModified = true;
		}

		if (!mon.getPrograms().containsAll(Arrays.asList(p3, p4))) {
			mon.getPrograms().removeAll(Arrays.asList(p3, p4));
			mon.getPrograms().add(p3);
			mon.getPrograms().add(p4);
			isModified = true;
		}

		if (!acc.getPrograms().containsAll(Arrays.asList(p1, p3, p4))) {
			acc.getPrograms().removeAll(Arrays.asList(p1, p3, p4));
			acc.getPrograms().add(p1);
			acc.getPrograms().add(p3);
			acc.getPrograms().add(p4);
			isModified = true;
		}
		return isModified;
	}

	public boolean createFrodoPrograms(ConfigDatabase db, ProgramPack pack, ProgramProfile all, ProgramProfile mon,
			ProgramProfile acc, ProgramProfile dev) {
		boolean isModified = false;
		Program p1 = createProgram(db, pack, "Dashboard", "FutureSystems.Frodo.Dashboard.Dashboard", 1);
		Program p2 = createProgram(db, pack, "Configuration", "FutureSystems.Frodo.Configuration.Configuration", 2);
		Program p3 = createProgram(db, pack, "Report", "FutureSystems.Frodo.Report.Report", 3);
		Program p4 = createProgram(db, pack, "Device Manager", "FutureSystems.Frodo.DeviceManager.DeviceManager", 4);

		if (!all.getPrograms().containsAll(Arrays.asList(p1, p2, p3, p4))) {
			all.getPrograms().removeAll(Arrays.asList(p1, p2, p3, p4));
			all.getPrograms().add(p1);
			all.getPrograms().add(p2);
			all.getPrograms().add(p3);
			all.getPrograms().add(p4);
			isModified = true;
		}

		if (!mon.getPrograms().containsAll(Arrays.asList(p1, p3, p4))) {
			mon.getPrograms().removeAll(Arrays.asList(p1, p3, p4));
			mon.getPrograms().add(p1);
			mon.getPrograms().add(p3);
			mon.getPrograms().add(p4);
			isModified = true;
		}

		if (!acc.getPrograms().containsAll(Arrays.asList(p1, p3, p4))) {
			acc.getPrograms().removeAll(Arrays.asList(p1, p3, p4));
			acc.getPrograms().add(p1);
			acc.getPrograms().add(p3);
			acc.getPrograms().add(p4);
			isModified = true;
		}

		if (!dev.getPrograms().containsAll(Arrays.asList(p4))) {
			dev.getPrograms().removeAll(Arrays.asList(p4));
			dev.getPrograms().add(p4);
			isModified = true;
		}

		return isModified;
	}

	public Program createProgram(ConfigDatabase db, ProgramPack pack, String name, String type, int seq) {
        Program program = getProgram(type);

        if (program != null)
			return program;

		Program p = new Program();
		p.setName(name);
		p.setPack(pack.getName());
		p.setSeq(seq);
		p.setPath(type);
		p.setVisible(true);

        createProgram(p);

		return p;
	}

    private Program getProgram(String type) {
        String query = "SELECT * FROM Program WHERE path = ?";

        Connection con = null;
        PreparedStatement psmt = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                return null;
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, type);

            ResultSet rs = psmt.executeQuery();

            if (rs.next()) {
                Program program = new Program();
                program.setName(rs.getString("name"));
                program.setPack(rs.getString("pack"));
                program.setSeq(rs.getInt("seq"));
                program.setPath(rs.getString("path"));
                program.setVisible(rs.getBoolean("visible"));

                return program;
            }
        } catch (Exception e) {
        	logger.error("error during getProgram", e);
        } finally {
            close(psmt, con);
        }

        return null;
    }

    private Program createProgram(Program program) {
        String query = "INSERT INTO Program (name, pack, description, path, visible, seq, created, updated) VALUES (?,?,?,?,?,?,?,?)";

        Connection con = null;
        PreparedStatement psmt = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, program.getName());
            psmt.setString(2, program.getPack());
            psmt.setString(3, program.getDescription());
            psmt.setString(4, program.getPath());
            psmt.setBoolean(5, program.isVisible());
            psmt.setInt(6, program.getSeq());
            psmt.setTimestamp(7, getCurrentTimeStamp());
            psmt.setTimestamp(8, getCurrentTimeStamp());

            psmt.executeUpdate();
        } catch (SQLException e) {
        	logger.error("error during createProgram", e);
        } catch (Exception e) {
        	logger.error("error during createProgram", e);
        } finally {
            close(psmt, con);
        }

        return null;
    }

	private static Predicate byRole(String name, int level) {
		return Predicates.and(Predicates.field("name", name), Predicates.field("level", level));
	}

    private Role getRole(String name, int level) {
        String query = "SELECT * FROM Role WHERE name = ? AND level = ?";

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                return null;
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, name);
            psmt.setInt(2, level);

            rs = psmt.executeQuery();

            if (rs.next()) {
                Role role = new Role();
                role.setName(rs.getString("name"));
                role.setLevel(rs.getInt("level"));
                role.setCreated(toDate(rs.getTimestamp("created")));
                role.setUpdated(toDate(rs.getTimestamp("updated")));
                role.setPermissions(getRolePermissions(rs.getString("name")));

                return role;
            }
        } catch (Exception e) {
        	logger.error("error during getRole", e);
        } finally {
            close(rs, psmt, con);
        }

        return null;
    }
    
    private List<Permission> getRolePermissions(String roleName) {
        String query = "SELECT * FROM RolePermission WHERE roleName = ?";

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        List<Permission> permissions = new ArrayList<Permission>();
        try {
            con = connectionService.getConnection();

            if (con == null) {
                logger.error("Can't connect to database. check database status.");
                return null;
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, roleName);

            rs = psmt.executeQuery();

            while (rs.next()) {
            	Permission perm = new Permission();
            	perm.setGroup(rs.getString("groupName"));
            	perm.setPermission(rs.getString("permission"));
            	
            	permissions.add(perm);
            }
        } catch (Exception e) {
        	logger.error("error during getRolePermissions", e);
        } finally {
            close(rs, psmt, con);
        }

        return permissions;
    }

    private List<Permission> getPermission(String name) {
        List<Permission> permissions = new ArrayList<Permission>();

        String query = "SELECT * FROM Permission WHERE groupName = ?";

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, name);
            rs = psmt.executeQuery();

            while (rs.next()) {
                Permission permission = new Permission();
                permission.setGroup(rs.getString("groupName"));
                permission.setPermission(rs.getString("permission"));
                permission.setDescription(rs.getString("description"));

                permissions.add(permission);
            }
        } catch (Exception e) {
        	logger.error("error during getPermission", e);
        } finally {
            close(rs, psmt, con);
        }
        return null;
    }

    private void createRole(Role role) {
        String query = "INSERT INTO Role (name, level, created, updated) VALUES (?,?,?,?)";

        Connection con = null;
        PreparedStatement psmt = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, role.getName());
            psmt.setInt(2, role.getLevel());
            psmt.setTimestamp(3, getCurrentTimeStamp());
            psmt.setTimestamp(4, getCurrentTimeStamp());

            psmt.executeUpdate();
        } catch (SQLException e) {
        	logger.error("error during createRole", e);
        } catch (Exception e) {
        	logger.error("error during createRole", e);
        } finally {
            close(psmt, con);
        }
    }

    private void updateRole(Role role) {
        List<Permission> permissionList = role.getPermissions();

        for (Permission permission : permissionList) {
            String query = "INSERT RolePermission SET roleName = ?, groupName = ?, permission = ?";

            Connection con = null;
            PreparedStatement psmt = null;

            try {
                con = connectionService.getConnection();

                if (con == null) {
                    throw new Exception("database connect fail");
                }

                psmt = con.prepareStatement(query);
                psmt.setString(1, role.getName());
                psmt.setString(2, permission.getGroup());
                psmt.setString(3, permission.getPermission());

                psmt.executeUpdate();
            } catch (SQLException e) {
            	logger.error("error during updateRole", e);
            } catch (Exception e) {
            	logger.error("error during updateRole", e);
            } finally {
                close(psmt, con);
            }
        }
    }

	public Role createRoles(ConfigDatabase db) {
		Permission frodoView = setPermission(db, "frodo", "config_view");
		Permission frodoEdit = setPermission(db, "frodo", "config_edit");
		Permission frodoUserEdit = setPermission(db, "frodo", "user_edit");
		Permission frodoDeviceEdit = setPermission(db, "frodo", "device_edit");
		Permission frodoDeviceView = setPermission(db, "frodo", "device_view");
		Permission frodoDeviceTransfer = setPermission(db, "frodo", "device_transfer");
		Permission domAdminGrant = setPermission(db, "dom", "admin_grant");
		Permission domEdit = setPermission(db, "dom", "config_edit");
		Permission domUserEdit = setPermission(db, "dom", "user_edit");
		Permission domView = setPermission(db, "dom", "config_view");

		Role superManager = getRole("master", 4);
		if (superManager == null) {
			superManager = new Role();
			superManager.setName("master");
			superManager.setLevel(4);

			createRole(superManager);
		}

		if (!superManager.getPermissions().containsAll(
				Arrays.asList(frodoEdit, frodoView, domAdminGrant, domEdit, domView, frodoUserEdit, domUserEdit, frodoDeviceEdit,
						frodoDeviceView, frodoDeviceTransfer))) {
			deleteRolePermissions("master");
			
			superManager.getPermissions().clear();
			superManager.getPermissions().add(domAdminGrant);
			superManager.getPermissions().add(frodoEdit);
			superManager.getPermissions().add(frodoView);
			superManager.getPermissions().add(frodoUserEdit);
			superManager.getPermissions().add(frodoDeviceEdit);
			superManager.getPermissions().add(frodoDeviceView);
			superManager.getPermissions().add(frodoDeviceTransfer);
			superManager.getPermissions().add(domUserEdit);
			superManager.getPermissions().add(domEdit);
			superManager.getPermissions().add(domView);

            updateRole(superManager);
		}

		Role manager = getRole("admin", 3);
		if (manager == null) {
			manager = new Role();
			manager.setName("admin");
			manager.setLevel(3);

			createRole(manager);
		}

		if (!manager.getPermissions().containsAll(
				Arrays.asList(frodoEdit, frodoView, domEdit, domView, frodoUserEdit, domUserEdit, frodoDeviceView,
						frodoDeviceEdit, frodoDeviceTransfer))) {
			deleteRolePermissions("admin");
			
			manager.getPermissions().clear();
			manager.getPermissions().add(frodoEdit);
			manager.getPermissions().add(frodoView);
			manager.getPermissions().add(frodoUserEdit);
			manager.getPermissions().add(frodoDeviceEdit);
			manager.getPermissions().add(frodoDeviceView);
			manager.getPermissions().add(domEdit);
			manager.getPermissions().add(domView);
			manager.getPermissions().add(domUserEdit);
			manager.getPermissions().add(frodoDeviceTransfer);

			updateRole(manager);
		}

		Role viewer = getRole("member", 2);
		if (viewer == null) {
			viewer = new Role();
			viewer.setName("member");
			viewer.setLevel(2);

			createRole(viewer);
		}

		if (!viewer.getPermissions().containsAll(Arrays.asList(frodoView, domView, frodoDeviceView))) {
			deleteRolePermissions("member");
			
			viewer.getPermissions().clear();
			viewer.getPermissions().add(frodoView);
			viewer.getPermissions().add(frodoDeviceView);
			viewer.getPermissions().add(domView);

			updateRole(viewer);
		}

		Role hr = getRole("hr", 2);
		if (hr == null) {
			hr = new Role();
			hr.setName("hr");
			hr.setLevel(2);

			createRole(hr);
		}

		if (!hr.getPermissions().containsAll(
				Arrays.asList(frodoView, domView, frodoUserEdit, domUserEdit, frodoDeviceView, frodoDeviceEdit,
						frodoDeviceTransfer))) {
			deleteRolePermissions("hr");
			
			hr.getPermissions().clear();
			hr.getPermissions().add(frodoView);
			hr.getPermissions().add(frodoUserEdit);
			hr.getPermissions().add(frodoDeviceEdit);
			hr.getPermissions().add(frodoDeviceView);
			hr.getPermissions().add(frodoDeviceTransfer);
			hr.getPermissions().add(domView);
			hr.getPermissions().add(domUserEdit);

			updateRole(hr);
		}

		Role device = getRole("device", 2);

		if (device == null) {
			device = new Role();
			device.setName("device");
			device.setLevel(2);

			createRole(device);
		}

		if (!device.getPermissions().containsAll(
				Arrays.asList(frodoView, domView, frodoDeviceView, frodoDeviceEdit, frodoDeviceTransfer))) {
			deleteRolePermissions("device");
			
			device.getPermissions().clear();
			device.getPermissions().add(frodoView);
			device.getPermissions().add(frodoDeviceTransfer);
			device.getPermissions().add(frodoDeviceView);
			device.getPermissions().add(frodoDeviceEdit);
			device.getPermissions().add(domView);

			updateRole(device);
		}

		return superManager;
	}

	private Permission setPermission(ConfigDatabase db, String group, String name) {
		return setPermission(group, name);
	}

    private Permission getPermission(String group, String name) {
        String query = "SELECT * FROM Permission WHERE groupName = ? AND permission = ?";

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, group);
            psmt.setString(2, name);

            rs = psmt.executeQuery();

            if (rs.next()) {
                Permission permission = new Permission();
                permission.setGroup(rs.getString("groupName"));
                permission.setPermission(rs.getString("permission"));
                permission.setDescription(rs.getString("description"));

                return permission;
            }
        } catch (Exception e) {
        	logger.error("error during getPermission", e);
        } finally {
        	close(rs, psmt, con);
        }

        return null;
    }

    private Permission setPermission(String group, String name) {
        Permission permission = getPermission(group, name);

        if (permission != null)
            return permission;

        String query = "INSERT INTO Permission (groupName, permission, description) VALUES (?,?,?)";

        Connection con = null;
        PreparedStatement psmt = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, group);
            psmt.setString(2, name);
            psmt.setString(3, null);

            psmt.executeUpdate();
        } catch (SQLException e) {
        	logger.error("error during setPermission", e);
        } catch (Exception e) {
        	logger.error("error during setPermission", e);
        } finally {
            close(psmt, con);
        }

        permission = getPermission(group, name);

        if (permission != null)
            return permission;
        else
            return null;
    }
    
    private void deleteRolePermissions(String roleName) {
        String query = "DELETE FROM RolePermission WHERE roleName = ?";

        Connection con = null;
        PreparedStatement psmt = null;

        try {
            con = connectionService.getConnection();

            if (con == null) {
                throw new Exception("database connect fail");
            }

            psmt = con.prepareStatement(query);
            psmt.setString(1, roleName);

            psmt.executeUpdate();
        } catch (SQLException e) {
        	logger.error("error during deleteRolePermissions", e);
        } catch (Exception e) {
        	logger.error("error during deleteRolePermissions", e);
        } finally {
            close(psmt, con);
        }
    }

    private void close(PreparedStatement psmt, Connection con) {
        if (psmt != null)
            try {
                psmt.close();
            } catch (SQLException e) {
            	logger.error("error during close", e);
            }
        if (con != null)
            try {
                con.close();
            } catch (SQLException e) {
            	logger.error("error during close", e);
            }
    }
    
    private void close(ResultSet rs, PreparedStatement psmt, Connection con) {
    	if (rs != null)
			try {
				rs.close();
			} catch (SQLException e1) {
				logger.error("error during close", e1);
			}
        close(psmt, con);
    }

    public static Date toDate(Timestamp timestamp) {
        long milliseconds = timestamp.getTime() + (timestamp.getNanos() / 1000000);
        return new Date(milliseconds);
    }

    private static Timestamp getCurrentTimeStamp() {
        Date today = new Date();
        return new Timestamp(today.getTime());
    }
}
